// Noise generation shader.
// Creates a noise pattern in the red channel for elevation, and another in the green channel for moisture.
#version 330 core
precision highp float;

#include <shader/lib/luma.glsl>
#include <shader/lib/noise/common.glsl>
#include <shader/lib/noise/simplex.glsl>
#include <shader/lib/noise/perlin.glsl>

// Blank texture.
uniform sampler2D u_texture0;

// The viewport dimensions along X and Y.
uniform vec2 u_viewport;
// Water level.
uniform float u_waterLevel;
// Scale in x, y and z.
uniform vec3 u_scale;
// Noise color.
uniform vec4 u_color;
// Noise seed.
uniform float u_seed;
// The initial amplitude.
uniform float u_amplitude;
// The persistence, factor by which the amplitude decreases in successive layers.
uniform float u_persistence;
// The initial frequency.
uniform float u_frequency;
// The lacunarity, factor by which the frequency increases in successive layers.
uniform float u_lacunarity;
// The power, the exponent to apply to the generated noise in a power function.
uniform float u_power;
// The number of octaves (layers) of noise.
uniform int u_octaves;
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
            float power,
            bool turbulence,
            bool ridge,
            int n_terraces,
            float terrace_exp,
            vec3 scale,
            int octaves,
            float seed) {
    // Fill up opts.
    gln_tFBMOpts opts = gln_tFBMOpts(seed,
                                     u_amplitude,
                                     u_persistence,
                                     u_frequency,
                                     u_lacunarity,
                                     scale,
                                     power,
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

    float val_ch1_original = noise(p, u_type, u_power, u_turbulence, u_ridge, u_numTerraces, u_terraceExp, u_scale, u_octaves, u_seed);
    float val_ch1 = max(u_waterLevel, val_ch1_original);

    if (u_channels <= 1) {
        // Channel 1 (elevation).
        fragColor = vec4((vec3(val_ch1) * u_color.rgb) * u_color.a, 1.0);

    } else {
        // Perlin always (0) in moisture (channel 2).
        float val_ch2 = noise(p, 0, 1.0, u_turbulence, u_ridge, 0, 0.0, u_scale, u_octaves, u_seed + 2.023);
        if (u_channels == 2) {
            // Channel 2 (moisture).
            fragColor = vec4(val_ch1, val_ch2, 0.0, 1.0);
        } else {
            // Channel 3 (temperature).
            float val_ch3 = noise(p, u_type, 2.0, false, false, 0, 0.0, u_scale, u_octaves, u_seed + 1.4325);
            fragColor = vec4(val_ch1, val_ch2, val_ch3, 1.0);
        }
    }

    #ifdef extraTarget
    // Generate emission pattern with white channel.
    // High-scale
    float emi = noise(p, 1, 3.0, false, false, 0, 0.0, vec3(10.0, 10.0, 10.0), 4, u_seed + 1.4325) * 4.5;
    // Low-scale
    float val_ch4 = noise(p, u_type, 2.7, false, false, 0, 0.0, u_scale, u_octaves, u_seed + 1.4325);
    val_ch4 = emi  * val_ch4 * step(u_waterLevel, val_ch1_original);
    emissionColor = vec4(val_ch4, val_ch4 * 0.8, val_ch4 * 0.6, 1.0);
    #endif // extraTarget
}
