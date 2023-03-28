#version 330 core

#include shader/lib_logdepthbuff.glsl

uniform sampler2D u_texture0;
uniform float u_radius;
uniform float u_apparent_angle;
uniform float u_inner_rad;
uniform float u_time;
// Distance in u to the star
uniform float u_distance;
// Whether light scattering is enabled or not
uniform int u_lightScattering;
uniform float u_zfar;
uniform float u_k;

// v_uv are UV coordinates in [0..1]
in vec2 v_uv;
in vec4 v_color;

layout (location = 0) out vec4 fragColor;

// Time multiplier
#define time u_time * 0.02

// Constants as a factor of the radius
#define model_const 172.4643429
#define rays_const 50000000.0

// Decays
#define light_decay 0.05

#ifdef ssrFlag
#include shader/lib_ssr.frag.glsl
#endif // ssrFlag

#ifdef velocityBufferFlag
#include shader/lib_velbuffer.frag.glsl
#endif

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

float starTexture(vec2 uv){
    return average(texture(u_texture0, uv));
}


vec4 draw() {
    float dist = clamp(distance(vec2(0.5), v_uv.xy) * 2.0, 0.0, 1.0);

    // level = 1 if u_distance == u_radius * model_const
    // level = 0 if u_distance == radius
    // level > 1 if u_distance > u_radius * model_const
    float level = (u_distance - u_radius) / ((u_radius * model_const) - u_radius);


    if (level >= 1.0) {
        // We are far away from the star
        level = u_distance / (u_radius * rays_const);

        if (u_lightScattering == 1) {
            // Light scattering, simple star
            float core = core(dist, u_inner_rad);
            float light = light(dist, light_decay);
            return (v_color + (core * 5.0)) * (light + core) * v_color.a;
        } else {
            // No light scattering, star rays
            level = min(level, 1.0);
            float corona = starTexture(v_uv);
            float light = light(dist, light_decay * 2.0);
            float core = core(dist, u_inner_rad);

            return (v_color + core) * (corona * (1.0 - level) + light + core) * v_color.a;
        }
    } else {
        // We are close to the star
        level = min(level, 1.0);
        float level_corona = float(u_lightScattering) * level;

        float corona = starTexture(v_uv);
        float light = light(dist, light_decay * 2.0);
        float core = core(dist, u_inner_rad);

        return (v_color + core) * (corona * (1.0 - level_corona) + light + level * core) * v_color.a;
    }
}

void main() {
    fragColor = draw();

    // Add outline
    //if (v_uv.x > 0.99 || v_uv.x < 0.01 || v_uv.y > 0.99 || v_uv.y < 0.01) {
    //    fragColor = vec4(1.0, 1.0, 0.0, 1.0);
    //}

    // Logarithmic depth buffer
    gl_FragDepth = getDepthValue(u_zfar, u_k);

    #ifdef ssrFlag
    ssrBuffers();
    #endif // ssrFlag

    #ifdef velocityBufferFlag
    velocityBuffer(fragColor.a);
    #endif
}
