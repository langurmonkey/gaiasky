#version 330 core
// Simple lens flare implementation by Toni Sagrista. This implementation needs the position of the light.

uniform sampler2D u_texture0;

#define MAX_LIGHTS 10

// Viewport dimensions along X and Y
uniform vec2 u_viewport;
uniform float u_intensity;
uniform vec2 u_lightPositions[MAX_LIGHTS];
uniform float u_lightIntensities[MAX_LIGHTS];
uniform int u_nLights;
uniform vec3 u_color;

in vec2 v_texCoords;
layout (location = 0) out vec4 fragColor;

#ifdef simpleLensFlare
// =================
// SIMPLE LENS FLARE
// =================

#ifdef useLensDirt
#define STRENGTH 0.35
#else
#define STRENGTH 1.0
#endif // useLensDirt

vec3 flare_simple(vec2 uv, vec2 pos, float intensity) {
    vec2 main = uv - pos;
    vec2 uvd = uv * (length(uv));

    float dist = length(main);
    dist = pow(dist, 0.1);

    float f1 = max(0.01-pow(length(uv+1.2*pos), 1.9), 0.0)*7.0;

    float f2 = max(1.0/(1.0+32.0*pow(length(uvd+0.8*pos), 2.0)), 0.0)*00.1;
    float f22 = max(1.0/(1.0+32.0*pow(length(uvd+0.85*pos), 2.0)), 0.0)*00.08;
    float f23 = max(1.0/(1.0+32.0*pow(length(uvd+0.9*pos), 2.0)), 0.0)*00.06;

    vec2 uvx = mix(uv, uvd, -0.5);

    float f4 = max(0.01-pow(length(uvx+0.4*pos), 2.4), 0.0)*6.0;
    float f42 = max(0.01-pow(length(uvx+0.47*pos), 2.4), 0.0)*5.0;
    float f43 = max(0.01-pow(length(uvx+0.54*pos), 2.4), 0.0)*3.0;

    uvx = mix(uv, uvd, -.4);

    float f5 = max(0.01-pow(length(uvx+0.2*pos), 5.5), 0.0)*2.0;
    float f52 = max(0.01-pow(length(uvx+0.4*pos), 5.5), 0.0)*2.0;
    float f53 = max(0.01-pow(length(uvx+0.6*pos), 5.5), 0.0)*2.0;

    uvx = mix(uv, uvd, -0.5);

    float f6 = max(0.01-pow(length(uvx-0.3*pos), 1.6), 0.0)*6.0;
    float f62 = max(0.01-pow(length(uvx-0.325*pos), 1.6), 0.0)*3.0;
    float f63 = max(0.01-pow(length(uvx-0.35*pos), 1.6), 0.0)*5.0;

    vec3 c = vec3(.0);

    c.r += f2+f4+f5+f6;
    c.g += f22+f42+f52+f62;
    c.b += f23+f43+f53+f63;
    c = c * 1.5 - vec3(length(uvd) * 0.05);

    return c * intensity;
}

vec3 cc(vec3 color, float factor, float factor2) {
    float w = color.x + color.y + color.z;
    return mix(color, vec3(w) * factor, w * factor2);
}

vec4 lens_flare(vec2 uv, float intensity, vec2 light_pos) {
    vec3 color = u_color * flare_simple(uv, light_pos, intensity) * STRENGTH;
    color = cc(color, 0.5, 0.1);
    return vec4(color, 1.0);
}
#endif// simpleLensFlare

#ifdef complexLensFlare
// ===================
// COMOPLEX LENS FLARE
// ===================

#ifdef useLensDirt
#define STRENGTH 0.35
#else
#define STRENGTH 0.4
#endif // useLensDirt

float rnd(vec2 p) {
    float f = fract(sin(dot(p, vec2(12.1234, 72.8392))*45123.2));
    return f;
}

float rnd(float w) {
    float f = fract(sin(w)*1000.0);
    return f;
}

float regShape(vec2 p, int N) {
    float f;

    float a=atan(p.x, p.y)+0.2;
    float b=6.28319/float(N);
    f=smoothstep(0.5, 0.51, cos(floor(0.5+a/b)*b-a)*length(p.xy));

    return f;
}

vec3 circle(vec2 p, float size, float decay, vec3 color, vec3 color2, float dist, vec2 mouse) {
    // l is used for making rings.I get the length and pass it through a sinwave
    // but I also use a pow function. pow function + sin function , from 0 and up, = a pulse, at least
    // if you return the max of that and 0.0.

    float l = length(p + mouse*(dist*4.0))+size/2.0;

    // l2 is used in the rings as well
    float l2 = length(p + mouse*(dist*4.0))+size/3.0;

    // Circles big (c), rings (c1).
    float c = max(00.01-pow(length(p + mouse*dist), size*1.4), 0.0) * 35.0;
    float c1 = max(0.001-pow(l-0.3, 1.0/40.0)+sin(l*30.0), 0.0) * 9.0;
    float s = max(0.01-pow(regShape(p*5.0 + mouse*dist*5.0 + 0.9, 6), 1.0), 0.0) * 9.0;

    color = 0.5 + 0.5 * sin(color);
    color = cos(vec3(0.44, 0.24, 0.2) * 8.0 + dist * 4.0) * 0.5 + 0.5;
    vec3 f = c * color;
    f += c1 * color;

    f +=  s * color;
    return f - 0.01;
}

vec4 lens_flare(vec2 uv, float intensity, vec2 light_pos) {
    vec3 circColor = u_color;
    vec3 circColor2 = clamp(u_color + vec3(-0.4, -0.1, 0.4), 0.0, 1.0);

    //now to make the sky not black
    vec3 color = vec3(0.0);

    //this calls the function which adds three circle types every time through the loop based on parameters I
    //got by trying things out. rnd i*2000. and rnd i*20 are just to help randomize things more
    for (int i = 0; i < 10; i++){
        color += circle(uv, pow(rnd(i * 2000.0) * 1.0, 2.0) + 1.41, 0.0, circColor+i, circColor2+i, rnd(i * 20.0) * 3.0 + 0.2 - 0.5, light_pos);
    }
    //get angle and length of the sun (uv - mouse)
    float a = atan(uv.y - light_pos.y, uv.x - light_pos.x);
    float l = max(1.0 - length(uv - light_pos) - 0.84, 0.0);

    float bright = 0.1;//+0.1/1/3.;//add brightness based on how the sun moves so that it is brightest
    //when it is lined up with the center

    //multiply by the exponetial e^x ? of 1.0-length which kind of masks the brightness more so that
    //there is a sharper roll of of the light decay from the sun.
    color *= exp(1.0 - length(uv - light_pos)) / 5.0;
    color = color * intensity * STRENGTH;
    return vec4(color, 1.0);
}
#endif// complexLensFlare

float fx(float t, float a) {
    return a * t * cos(t);
}

float fy(float t, float a) {
    return a * t * sin(t);
}

#include shader/lib_luma.glsl

#define N_SAMPLES 6
void main(void) {
    if (u_intensity > 0.0) {
        vec2 uv = v_texCoords - 0.5;
        float ar = u_viewport.x / u_viewport.y;
        uv.x *= ar;
        #ifdef useLensDirt
        vec4 color = vec4(0.0);
        #else
        vec4 color = texture(u_texture0, v_texCoords);
        #endif // useLensDirt

        for (int light = 0; light < u_nLights; light++) {
            vec2 light_pos = u_lightPositions[light] - 0.5;
            float light_intensity = u_lightIntensities[light];

            // Compute intensity of light.
            float t = 0;
            float a = 0.01;
            float dt = 3.0 * 3.14159 / N_SAMPLES;
            float lum = 0.0;
            for (int idx = 0; idx < N_SAMPLES; idx++){
                vec2 curr_coord = clamp(light_pos + vec2(0.5) + vec2(fx(t, a) / ar, fy(t, a)), 0.0, 1.0);
                lum += (clamp(luma(texture(u_texture0, curr_coord).rgb), 0.0, 1.0));
                t += dt;
            }
            lum /= N_SAMPLES;

            float weight = clamp(1.6 - 2.0 * length(light_pos), 0.0, 1.0);
            float intensity = u_intensity * lum * weight * light_intensity;

            if (intensity > 0.0) {
                color += lens_flare(uv, intensity, light_pos);
            }
        }

        fragColor = color;
    } else {
        #ifdef useLensDirt
        fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        #else
        fragColor = texture(u_texture0, v_texCoords);
        #endif // useLensDirt
    }
}