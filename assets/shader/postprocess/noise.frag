// Noise generation shader.
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
uniform vec3 u_color;
// Final range of the noise values.
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
// Noise type
// 0- PERLIN
// 1- SIMPLEX
// 2- WORLEY
// 3- CURL
// 4- WHITE
uniform int u_type;


in vec2 v_texCoords;
layout (location = 0) out vec4 fragColor;


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

    // Fill up opts.
    gln_tFBMOpts opts = gln_tFBMOpts(u_seed,
                                    u_frequency,
                                    u_persistence,
                                    u_lacunarity,
                                    u_scale,
                                    u_power,
                                    u_octaves,
                                    u_turbulence,
                                    u_ridge);

    float value = 0.0;
    if (u_type == 0) {
        // PERLIN
        value = gln_pfbm(p, opts);

    } else if (u_type == 1) {
        // SIMPLEX
        value = gln_sfbm(p, opts);

    } else if (u_type == 2) {
        // VORONOI
        value = gln_vfbm(p, opts);

    } else if (u_type == 3) {
        // CURL
        value = gln_cfbm(p, opts);

    } else if (u_type == 4) {
        // WHITE
        value = gln_wfbm(p, opts);

    }

    // Set in range.
    value = clamp(gln_map(value, 0.0, 1.0, u_range.x, u_range.y), 0.0, 1.0);

    fragColor = vec4(vec3(value) * u_color, 1.0);
}
