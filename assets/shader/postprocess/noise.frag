// Noise generation shader.
// Creates a noise pattern in the red channel for elevation, and another in the green channel for moisture.
#version 330 core

#include <shader/lib/luma.glsl>
#include <shader/lib/noise/common.glsl>
#include <shader/lib/noise/white.glsl>
#include <shader/lib/noise/simplex.glsl>
#include <shader/lib/noise/perlin.glsl>
#include <shader/lib/noise/curl.glsl>
#include <shader/lib/noise/voronoi.glsl>

// Blank texture.
uniform sampler2D u_texture0;

// The viewport dimensions along X and Y.
uniform vec2 u_viewport;
// Scale in x, y and z.
uniform vec3 u_scale;
// Noise color.
uniform vec4 u_color;
// Final range of the noise values for the first channel. Channels 2 and 3 default to [0, 1].
uniform vec2 u_range;
// Noise seed.
uniform float u_seed;
// The initial frequency.
uniform float u_frequency;
// The persistence, factor by which the amplitude decreases in successive layers.
uniform float u_persistence;
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
// 2- WORLEY
// 3- CURL
// 4- WHITE
uniform int u_type;


in vec2 v_texCoords;

layout (location = 0) out vec4 fragColor;
#ifdef extraTarget
layout (location = 1) out vec4 emissionColor;
#endif // extraTarget

float terraces(float h, int n_terraces, float smoothness) {
    if (n_terraces <= 0) {
        return h;
    }
    h = h * n_terraces;
    return (round(h) + 0.5 * clamp(pow(2.0 * (h - round(h)), smoothness), 0.0, 1.0)) / n_terraces;
}

float noise(vec3 p,
            int type,
            float power,
            bool ridge,
            int n_terraces,
            float terrace_exp,
            vec2 range,
            float seed) {
    // Fill up opts.
    gln_tFBMOpts opts = gln_tFBMOpts(seed,
                                     u_frequency,
                                     u_persistence,
                                     u_lacunarity,
                                     u_scale,
                                     power,
                                     u_octaves,
                                     u_turbulence,
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

    } else if (type == 3) {
        // CURL
        value = gln_cfbm(p, opts);

    } else if (type == 4) {
        // WHITE
        value = gln_wfbm(p, opts);
    }

    // Set in range.
    value = clamp(gln_map(value, 0.0, 1.0, range.x, range.y), 0.0, 1.0);

    // Terraces.
    value = terraces(value, n_terraces, terrace_exp);

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

    float val_ch1 = noise(p, u_type, u_power, u_ridge, u_numTerraces, u_terraceExp, u_range, u_seed);

    if (u_channels <= 1) {
        // Channel 1 (elevation).
        fragColor = vec4((vec3(val_ch1) * u_color.rgb) * u_color.a, 1.0);

    } else {
        // Perlin always (0) in moisture (channel 2).
        float val_ch2 = noise(p, 0, 1.0, u_ridge, 0, 0.0, vec2(0.0, 1.0), u_seed + 2.023);
        if (u_channels == 2) {
            // Channel 2 (moisture).
            fragColor = vec4(val_ch1, val_ch2, 0.0, 1.0);
        } else {
            // Channel 3 (temperature).
            float val_ch3 = noise(p, u_type, 2.0, false, 0, 0.0, vec2(0.0, 1.0), u_seed + 1.4325);
            fragColor = vec4(val_ch1, val_ch2, val_ch3, 1.0);
        }
    }

    #ifdef extraTarget
    // Generate emission pattern with white channel.
    float white = noise(p, 4, 1.0, false, 0, 0.0, vec2(0.0, 1.0), u_seed + 1.4325) * 2.0;
    float val_ch4 = noise(p, u_type, 1.5, false, 0, 0.0, vec2(-0.4, 1.0), u_seed + 1.4325);
    val_ch4 = white * step(0.1, val_ch1) * val_ch4;
    emissionColor = vec4(val_ch4, val_ch4 * 0.8, val_ch4 * 0.6, 1.0);
    #endif // extraTarget
}
