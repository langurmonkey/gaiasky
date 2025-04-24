#version 330 core

#include <shader/lib/logdepthbuff.glsl>

// Color buffer
uniform sampler2D u_texture0;
// Depth buffer (log)
uniform sampler2D u_texture1;
// Noise buffer
uniform sampler2D u_texture2;
// Time in seconds
uniform float u_time;
// Floating position
uniform vec3 u_pos;
// Zfar
uniform vec2 u_zfark;
// Viewport
uniform vec2 u_viewport;
// Use additional.x for bend scaling
uniform vec4 u_additional;

in vec2 v_texCoords;
in vec3 v_ray;
layout (location = 0) out vec4 fragColor;


/**
 GLSL implementation of volumetric cloud rendering as
 described in http://www.blog.sirenix.net/blog/realtime-volumetric-clouds-in-unity
**/
#define PI 3.14159265359
#define ITERATIONS 100.0

#define CLOUD_DENSITY 0.4
#define CLOUD_COLOR vec3(1.0, 1.0, 1.0)


float rand(vec3 p) {
    return fract(sin(dot(p, vec3(12.345, 67.89, 412.12))) * 42123.45) * 2.0 - 1.0;
}

float valueNoise(vec3 p) {
    vec3 u = floor(p);
    vec3 v = fract(p);
    vec3 s = smoothstep(0.0, 1.0, v);

    float a = rand(u);
    float b = rand(u + vec3(1.0, 0.0, 0.0));
    float c = rand(u + vec3(0.0, 1.0, 0.0));
    float d = rand(u + vec3(1.0, 1.0, 0.0));
    float e = rand(u + vec3(0.0, 0.0, 1.0));
    float f = rand(u + vec3(1.0, 0.0, 1.0));
    float g = rand(u + vec3(0.0, 1.0, 1.0));
    float h = rand(u + vec3(1.0, 1.0, 1.0));

    return mix(mix(mix(a, b, s.x), mix(c, d, s.x), s.y),
    mix(mix(e, f, s.x), mix(g, h, s.x), s.y),
    s.z);
}

float fbm(vec3 p) {
    vec3 q = p - vec3(0.1, 0.0, 0.0);
    // fbm
    float ret = 0.5 * valueNoise(q); q *= 2.0;
    ret += 0.25 * valueNoise(q); q *= 2.0;
    ret += 0.125 * valueNoise(q);
    return clamp(ret - p.y, 0.0, 1.0);
}

vec4 raymarch(vec3 ray, vec3 pos, float s) {
    float depth = 0.0;
    vec4 color = vec4(0.0, 0.0, 0.0, 0.0);

    for (int i = 0; i < ITERATIONS; i++)
    {
        vec3 p = pos + depth * ray;
        float density = fbm(p);

        // If density is unignorable...
        if (density > 1e-2)
        {
            // We estimate the color with w.r.t. density
            vec4 c = vec4(mix(CLOUD_COLOR, vec3(0.0, 0.0, 0.0), density), density);
            // Multiply it by a factor so that it becomes softer
            float dif = clamp(density - fbm(p) / 0.3, 0.0, 1.0);
            vec3 lig = vec3(1.0) + vec3(0.9, 0.7, 0.0) * dif;
            c.rgb *= lig;

            c.a *= CLOUD_DENSITY;
            c.rgb *= c.a;
            color += c * (1.0 - color.a);
        }

        // March forward a fixed distance
        depth += max(0.1, 0.03 * depth);

        // Depth test
        if (depth >= s) {
            return vec4(0.0, 0.0, 0.0, 0.0);
        }

    }

    return vec4(clamp(color.rgb, 0.0, 1.0), color.a);
}

float luma(vec4 color) {
    return dot(color.rgb, vec3(0.299, 0.587, 0.114));
}

void main() {
    // ray direction
    vec3 ray = normalize(v_ray);
    // floating position (camPos - pos)
    vec3 pos = u_pos * 1.0;
    // depth buffer
    float depth = 1.0 / recoverWValue(texture(u_texture1, v_texCoords).r, u_zfark.x, u_zfark.y);
    depth *= length(ray);
    // color of pre-existing scene
    vec3 col = texture(u_texture0, v_texCoords).rgb;
    // ray marching
    vec4 rmcol = raymarch(ray, pos, depth);

    // return final color using alpha blending
    fragColor = vec4(col * (1.0 - rmcol.a) + rmcol.rgb * rmcol.a, 1.0);
    //fragColor = vec4(rmcol.rgb, 1.0);
}
