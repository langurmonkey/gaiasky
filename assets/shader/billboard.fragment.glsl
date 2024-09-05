#version 330 core

#include <shader/lib/logdepthbuff.glsl>

uniform sampler2D u_texture0;
uniform float u_radius;
uniform float u_apparent_angle;
uniform float u_inner_rad;
uniform float u_time;
// Distance in u to the billboard
uniform float u_distance;
// Whether light scattering is enabled or not
uniform int u_lightScattering;
uniform float u_zfar;
uniform float u_k;
uniform mat4 u_matrix;

// v_uv are UV coordinates in [0..1]
in vec2 v_uv;
in vec4 v_color;

layout (location = 0) out vec4 fragColor;
layout (location = 1) out vec4 layerBuffer;

// Time multiplier
#define time u_time * 0.02

// Constants as a factor of the radius
#define model_const 172.4643429
#define rays_const 50000000.0

// Decays
#define light_decay 0.05

#ifdef ssrFlag
#include <shader/lib/ssr.frag.glsl>
#endif// ssrFlag

#define saturate(x) clamp(x, 0.0, 1.0)

float core(float distance_center, float inner_rad){
    if (inner_rad == 0.0){
        return 0.0;
    }
    float core = 1.0 - step(inner_rad / 5.0, distance_center);
    float core_glow = smoothstep(inner_rad / 2.0, inner_rad / 6.0, distance_center);
    return core_glow + core;
}

float light(float distance_center, float decay) {
    float light = 1.0 - pow(distance_center, decay);
    return light;
}


float average(vec4 color) {
    return (color.r + color.g + color.b) / 3.0;
}

float billboardTexture(vec2 uv){
    return average(texture(u_texture0, uv));
}

// Comment out to use regular method.
#define detailedCorona

#ifdef detailedCorona
#include <shader/lib/noise.glsl>
// The next two methods are adapted from https://www.shadertoy.com/view/4lfSzS

// Corona ring.
float ring(vec3 ray, vec3 pos, float r, float size) {
    float b = dot(ray, pos);
    float c = dot(pos, pos) - b*b;

    float s=max(0.0, (1.0-size*abs(r-sqrt(c))));

    return s;
}

// Corona rays.
float ringRayNoise(vec3 ray, vec3 pos, float r, float size, mat3 mr, float anim) {
    float b = dot(ray, pos);
    vec3 pr = ray * b - pos;

    float c = length(pr);

    pr *= mr;

    pr = normalize(pr);

    float s = max(0.0, (1.0 - size * abs(r - c)));

    float nd = noise4q(vec4(pr, -anim + c)) * 2.0;
    nd = pow(nd, 2.0);
    float n = 0.4;
    float ns = 1.0;
    if (c > r) {
        n = noise4q(vec4(pr * 10.0, -anim + c));
        ns = noise4q(vec4(pr * 50.0, -anim * 2.5 + c * 2.0)) * 2.0;
    }
    n = n * n * nd * ns;

    return pow(s, 4.0) + s * s * n;
}
#endif// detailedCorona

vec4 farAway(float dist, float level) {
    // We are far away from the object
    level = u_distance / (u_radius * rays_const);

    if (u_lightScattering == 1) {
        // Light scattering
        float core = core(dist, u_inner_rad);
        return saturate((v_color + core * 5.0) * core * v_color.a);
    } else {
        // No light scattering
        level = min(level, 1.0);
        float corona = billboardTexture(v_uv);
        float light = light(dist, light_decay * 2.0);
        float core = core(dist, u_inner_rad);

        return saturate((v_color + core) * (corona * (1.0 - level) + light + core) * v_color.a);
    }
}

vec4 closeUp(float dist, float level) {
    #ifdef detailedCorona

    // This part is adapted from https://www.shadertoy.com/view/4lfSzS

    // Rotation matrix.
    mat3 mr = mat3(u_matrix[0].xyz, u_matrix[1].xyz, u_matrix[2].xyz);

    // Flip u coordinate.
    vec2 uv = v_uv;
    uv.x = 1.0 - uv.x;
    // We need coordinates in [-12.5,12.5].
    vec2 p = (uv * 25.0) - 12.5;
    vec3 ray = normalize(vec3(p, 2.0));
    vec3 pos = vec3(0.0, 0.0, 3.0);

    vec4 color = vec4(0.0, 0.0, 0.0, 1.0);

    color.xyz -= vec3(ring(ray, pos, 1.03, 11.0))*2.0;
    color = max(vec4(0.0), color);

    float s3 = ringRayNoise(ray, pos, 0.96, 1.0, mr, u_time);
    color.xyz += mix(v_color.rgb, vec3(1.0, 0.95, 1.0), pow(s3, 3.0)) * s3 * 0.8 * v_color.a;
    color.xyz = max(color.xyz, pow(saturate(1.0 - length(p)) * 2.6, 3.0) * v_color.rgb);

    return saturate(color);

    #else

    // We are close to the billboard
    level = min(level, 1.0);
    float level_corona = float(u_lightScattering) * level;

    float corona = billboardTexture(v_uv);
    float light = light(dist, light_decay * 2.0);
    float core = core(dist, u_inner_rad);

    return (v_color + core) * (corona * (1.0 - level_corona) + light + level * core) * v_color.a;

    #endif// detailedCorona
}

vec4 draw() {

    float dist = clamp(distance(vec2(0.5), v_uv) * 2.0, 0.0, 1.0);

    // level = 1 if u_distance == u_radius * model_const
    // level = 0 if u_distance == radius
    // level > 1 if u_distance > u_radius * model_const
    float level = (u_distance - u_radius) / ((u_radius * model_const) - u_radius);

    // -------------------------------|-------------------------------|------------x OBJECT
    //                                                                 ---radius---
    //                                 -------------radius * model_const-----------
    //              > 1.0            1.0                              0.0

    if (level < 1.5 && level > 0.5) {
        // Transition between far away and close up.
        return mix(closeUp(dist, level), farAway(dist, level), level - 0.5);
    } else if (level >= 1.5) {
        // Far away.
        return farAway(dist, level);
    } else {
        // Close up.
        return closeUp(dist, level);
    }
}

void main() {
    fragColor = saturate(draw());

    // Add outline
    //if (v_uv.x > 0.99 || v_uv.x < 0.01 || v_uv.y > 0.99 || v_uv.y < 0.01) {
    //    fragColor = vec4(1.0, 1.0, 0.0, 1.0);
    //}

    // Logarithmic depth buffer
    gl_FragDepth = getDepthValue(u_zfar, u_k);
    layerBuffer = vec4(0.0, 0.0, 0.0, 1.0);

    #ifdef ssrFlag
    ssrBuffers();
    #endif// ssrFlag
}
