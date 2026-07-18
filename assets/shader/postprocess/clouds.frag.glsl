// Noise generation shader.
// Creates a noise pattern in the red channel for elevation, and another in the green channel for moisture.
#version 330 core
precision highp float;

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
// Different noise patterns in different channels.
// <= 1 - in red.
// == 2 - in red and green.
// >= 3 - in red, green and blue.
uniform int u_channels;
// Noise type
// 0 - PERLIN
// 1 - SIMPLEX
// 2 - VORONOI
uniform int u_type;
// Plains height and slope.
// - height: plains are between baseLevel and this value.
// - slope: low values create flat plains, while higher values progressively erase them.
uniform vec2 u_plains = vec2(0.0, 0.2);
// Domain warping.
// - strength: how much warping is applied.
// - frequency: frequency of the warping noise.
uniform vec2 u_warp = vec2(0.0, 0.1);

in vec2 v_texCoords;

layout (location = 0) out vec4 fragColor;
#ifdef extraTarget
layout (location = 1) out vec4 emissionColor;
#endif // extraTarget

#include <shader/lib/luma.glsl>
#include <shader/lib/procgen/procgen.glsl>

void main() {
    // Sample point.
    vec2 xy = v_texCoords * u_viewport;
    // Sample on the surface of a sphere.
    float phiStep = gln_PI / (u_viewport.y - 1);
    float phi = (-gln_PI / 2.0) + xy.y * phiStep;
    float thetaStep = gln_PI * 2.0 / u_viewport.x;
    float theta = xy.x * thetaStep;
    vec3 p = sphericalToCartesian(phi, theta);

    float clouds = computeElevation(p, u_baseLevel).x;

    // Only one channel for clouds.
    fragColor = vec4((vec3(clouds) * u_color.rgb) * u_color.a, 1.0);
}
