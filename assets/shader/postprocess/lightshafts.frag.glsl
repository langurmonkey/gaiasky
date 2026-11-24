#version 330 core
// Light shafts (god rays), pass 2: marches the light mask towards the light and adds it to the frame.

// Scene color.
uniform sampler2D u_texture0;
// Light mask of the previous pass.
uniform sampler2D u_texture1;
// Position of the light on screen, in [0,1].
uniform vec2 u_lightPos;
// Color of the light.
uniform vec3 u_lightColor;
// Strength of the effect. At or below zero the frame is copied through.
uniform float u_strength;
// Fraction of the ray to the light that is marched, in [0,1].
uniform float u_density;
// Attenuation of a sample against the previous one.
uniform float u_decay;
// Number of samples along the ray.
uniform int u_samples;
// Time, in seconds, to animate the dithering that hides the banding.
uniform float u_time;

in vec2 v_texCoords;
layout (location = 0) out vec4 fragColor;

float dither(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

void main() {
    vec4 src = texture(u_texture0, v_texCoords);
    if (u_strength <= 0.0) {
        fragColor = src;
        return;
    }

    vec2 delta = (v_texCoords - u_lightPos) * (u_density / float(u_samples));
    // Offset the start of the march with noise, so the accumulation does not band.
    vec2 tc = v_texCoords + delta * dither(v_texCoords + fract(u_time));

    vec3 acc = vec3(0.0);
    float wsum = 0.0;
    float illum = 1.0;
    for (int i = 0; i < u_samples; i++) {
        tc -= delta;
        acc += texture(u_texture1, clamp(tc, 0.0, 1.0)).rgb * illum;
        wsum += illum;
        illum *= u_decay;
    }
    acc /= max(wsum, 1.0e-5);

    fragColor = vec4(src.rgb + acc * u_lightColor * u_strength, src.a);
}
