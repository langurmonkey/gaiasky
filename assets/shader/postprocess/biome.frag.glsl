// Noise generation shader.
// Creates a noise pattern in the red channel for elevation, and another in the green channel for moisture.
#version 330 core
precision highp float;

#include <shader/lib/luma.glsl>
#include <shader/lib/noise/common.glsl>
#include <shader/lib/noise/simplex.glsl>
#include <shader/lib/noise/perlin.glsl>
#include <shader/lib/noise/voronoi.glsl>

// Blank texture.
uniform sampler2D u_texture0;

// The viewport dimensions along X and Y.
uniform vec2 u_viewport;
// Base level.
uniform float u_baseLevel;
// Remap values to [0,1] after base level operation.
uniform bool u_remap;
// Scale in x, y and z.
uniform vec3 u_scale;
// Noise color.
uniform vec4 u_color;
// Noise seed.
uniform float u_seed;
// The persistence, factor by which the amplitude decreases in successive layers.
uniform float u_persistence;
// The initial frequency.
uniform float u_frequency;
// The lacunarity, factor by which the frequency increases in successive layers.
uniform float u_lacunarity;
// The number of octaves (layers) of noise.
uniform int u_octaves;
// Whether to apply smoothstep to the elevation or not.
uniform bool u_smoothing;
// Whether to apply an absolute value funciton.
uniform bool u_turbulence;
// Enable/disable ridge noise in fBm. Only when turbulence is on.
uniform bool u_ridge;
// Number of terraces in the height profile. 0 to disable.
uniform int u_numTerraces;
// Exponent for the terraces profile. Must be odd.
uniform float u_terraceExp;
// Different noise patterns in different channels.
// <= 1 - in red.
// == 2 - in red and green.
// >= 3 - in red, green and blue.
uniform int u_channels;
// Noise type
// 0- PERLIN
// 1- SIMPLEX
uniform int u_type;


in vec2 v_texCoords;

layout (location = 0) out vec4 fragColor;
#ifdef extraTarget
layout (location = 1) out vec4 emissionColor;
#endif // extraTarget

float noise(vec3 p,
        int type,
        float frequency,
        bool turbulence,
        bool ridge,
        int n_terraces,
        float terrace_exp,
        vec3 scale,
        int octaves,
        float seed) {
    // Fill up opts.
    gln_tFBMOpts opts = gln_tFBMOpts(seed,
            u_persistence,
            frequency,
            u_lacunarity,
            scale,
            octaves,
            turbulence,
            ridge);

    float value = 0.0;
    if (type == 0) {
        // PERLIN
        value = gln_pfbm(p, opts);

    } else if (type == 1) {
        // SIMPLEX
        value = gln_sfbm(p, opts);

    } else if (type == 2) {
        // VORONOI
        value = gln_vfbm(p, opts);
    }

    return value;

}

void main() {
    // Sample point.
    vec2 xy = v_texCoords * u_viewport;
    // Sample on the surface of a sphere.
    float phiStep = gln_PI / (u_viewport.y - 1);
    float phi = (-gln_PI / 2.0) + xy.y * phiStep;
    float thetaStep = gln_PI * 2.0 / u_viewport.x;
    float theta = xy.x * thetaStep;
    float cosPhi = cos(phi);
    // P is a point in the sphere.
    vec3 p = vec3(
            cosPhi * cos(theta),
            cosPhi * sin(theta),
            sin(phi)
    );

    float val_ch1_original = noise(p, u_type, u_frequency, u_turbulence, u_ridge, u_numTerraces, u_terraceExp, u_scale, u_octaves, u_seed);
    if (u_smoothing) {
        val_ch1_original = smoothstep(0.0, 1.0, val_ch1_original);
    }

    float val_ch1;
    if (u_remap) {
        // Remap after base level. Base level is at 0.
        val_ch1 = gln_map(val_ch1_original, u_baseLevel, 1.0, 0.0, 1.0);
    } else {
        // Clamp.
        val_ch1 = max(u_baseLevel, val_ch1_original);
    }


    if (u_channels <= 1) {
        // Channel 1 (elevation).
        fragColor = vec4((vec3(val_ch1) * u_color.rgb) * u_color.a, 1.0);

    } else {
        // Perlin always (0) in moisture (channel 2).
        float val_ch2 = noise(p, 0, 0.5, u_turbulence, u_ridge, 0, 0.0, u_scale, u_octaves, u_seed + 0.023);
        if (u_channels == 2) {
            // Channel 2 (moisture).
            fragColor = vec4(val_ch1, val_ch2, 0.0, 1.0);
        } else {
            // Channel 3 (temperature).
            float val_ch3 = noise(p, u_type, u_frequency, false, false, 0, 0.0, u_scale, u_octaves, u_seed + 0.4325);
            fragColor = vec4(val_ch1, val_ch2, val_ch3, 1.0);
        }
    }

    #ifdef extraTarget
    // Generate emission pattern with white channel.
    // High-scale
    float emi = noise(p, u_type, u_frequency * 0.5, false, false, 0, 0.0, vec3(8.0, 8.0, 8.0), 5, u_seed + 0.1325);
    emi = emi * smoothstep(0.55, 0.7, emi) * 0.6;
    emi = emi * step(u_baseLevel, val_ch1_original);
    float r = gln_rand(xy + emi) * 0.2 + 0.8;
    float g = gln_rand(xy + emi) * 0.2 + 0.7;
    float b = gln_rand(xy + emi) * 0.2 + 0.5;
    emissionColor = vec4(emi * r, emi * g, emi * b, 1.0);
    #endif // extraTarget
}
