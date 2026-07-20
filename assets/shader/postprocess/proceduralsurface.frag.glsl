// Merged procedural surface shader.
// Combines noise generation (biome) + surface generation (surfacegen) into a single pass.
// Computes elevation, moisture and temperature from noise, then uses them to
// generate diffuse, specular, normal and emission outputs procedurally.
#version 330 core
precision highp float;

// LUT texture
uniform sampler3D u_texture1;
// LUT hue shift.
uniform float u_lutHueShift;
// LUT saturation value.
uniform float u_lutSaturation;
// Influence of the latitude on the temperature.
uniform float u_latitudeInfluence = 0.8;

// Viewport dimensions.
uniform vec2 u_viewport;
// Base level (water level).
uniform float u_baseLevel;
// Remap to [0,1] after base level operation.
uniform bool u_remap;
// Noise scale in x, y and z.
uniform vec3 u_scale;
// Noise color.
uniform vec4 u_color;
// Noise seed.
uniform float u_seed;
// Persistence, factor by which the amplitude decreases in successive layers.
uniform float u_persistence;
// Initial frequency.
uniform float u_frequency;
// Lacunarity, factor by which the frequency increases in successive layers.
uniform float u_lacunarity;
// Number of octaves (layers) of noise.
uniform int u_octaves;
// Whether to apply smoothstep to the elevation or not.
uniform bool u_smoothing;
// Whether to apply an absolute value function.
uniform bool u_turbulence;
// Enable/disable ridge noise in fBm. Only when turbulence is on.
uniform bool u_ridge;
// Channels: <= 1 = red only, 2 = red+green, >= 3 = red+green+blue.
uniform int u_channels;
// Noise type: 0 = PERLIN, 1 = SIMPLEX, 2 = VORONOI.
uniform int u_type;
// Plains height and slope.
// - height: plains are between baseLevel and this value.
// - slope: low values create flat plains, while higher values progressively erase them.
uniform vec2 u_plains = vec2(0.4, 0.2);
// Domain warping.
// - strength: how much warping is applied.
// - frequency: frequency of the warping noise.
uniform vec2 u_warp = vec2(0.0, 0.1);

in vec2 v_texCoords;

layout (location = 0) out vec4 fragBiome;
layout (location = 1) out vec4 fragDiffuse;
layout (location = 2) out vec4 fragSpecular;
layout (location = 3) out vec4 fragEmission;
#ifdef normalMapFlag
layout (location = 4) out vec4 fragNormal;
#endif // normalMapFlag

#include <shader/lib/colors.glsl>
#include <shader/lib/luma.glsl>

#include <shader/lib/procgen/procgen.glsl>

vec3 diffuseLUT(float elevation, float moisture, float temperature, float baseLevel) {
    float epsilon = 1.0 / 255.0;
    if (elevation <= baseLevel + epsilon) {
        elevation = 0.0;
    }
    elevation = clamp(elevation, 0.0, 1.0);
    moisture = clamp(moisture, 0.0, 1.0);
    temperature = clamp(temperature, 0.0, 1.0);

    vec4 rgba = texture(u_texture1, vec3(moisture, 1.0 - elevation, temperature));

    // Rotate hue, apply saturation.
    vec3 hsv = rgb2hsv(rgba.rgb);
    hsv.x = mod(hsv.x * 360.0 + u_lutHueShift, 360.0) / 360.0;
    hsv.y = clamp(hsv.y * u_lutSaturation, 0.0, 1.0);
    // Back to RGB.
    return hsv2rgb(hsv);
}

#define EPSILON 1.0 / 255.0

#ifdef emissiveMapFlag
float cityMask(vec3 p, float elevation, float waterMask) {
    // Low-frequency simplex/fbm for region-scale placement (which
    // "areas" of the planet get urbanized at all).
    float m = noise(p * 1.5, SIMPLEX, 0.5, 1.3, 2.0, false, false, vec3(1.0), 5, u_seed);
    m = smoothstep(0.5, 0.85, m); // soft edge instead of a hard cut

    // Don't build underwater or high in the mountains -- keep to a "habitable" elevation band.
    float elevGate = waterMask * (1.0 - smoothstep(0.1, 0.2, elevation));

    return m * elevGate;
}
#endif // emissiveMapFlag

void main() {
    // Sample point on sphere surface.
    vec2 xy = v_texCoords * u_viewport;
    float dPhi = gln_PI / (u_viewport.y - 1);
    float phi = (-gln_PI / 2.0) + xy.y * dPhi;
    float dTheta = gln_PI * 2.0 / u_viewport.x;
    float theta = xy.x * dTheta;
    vec3 p = sphericalToCartesian(phi, theta);

    float baseLevel = u_baseLevel;

    ///
    /// BIOME GEN (elevation, moisture, temperature)
    ///
    fragBiome = vec4(0.0, 0.0, 0.0, 1.0);

    // Elevation (channel 1)
    vec2 elv = computeElevation(p, u_baseLevel);
    float elevation = elv.x;
    baseLevel = elv.y;
    float waterMask = step(baseLevel + EPSILON, elevation);
    fragBiome.r = elevation;

    // Moisture (channel 2)
    float moisture = 0.0;
    if (u_channels >= 2) {
        moisture = noise(p, SIMPLEX, 0.5, 0.5, 2.0, u_turbulence, u_ridge, u_scale, u_octaves, u_seed + 0.023);
        fragBiome.g = moisture;
    }

    // Temperature (channel 3)
    float temperature = 0.5;
    if (u_channels >= 3) {
        float latitudeFactor = 1.0 - abs(phi) / (gln_PI * 0.5); // 1 at equator, 0 at poles
        latitudeFactor = smoothstep(0.1, 0.6, latitudeFactor);
        float tempFreq = min(u_frequency * 0.7, 0.5);
        float noiseTemperature = noise(p, u_type, 0.5, tempFreq, 2.0, false, false, u_scale, u_octaves, u_seed + 0.4325);
        temperature = mix(noiseTemperature, latitudeFactor, u_latitudeInfluence);
        fragBiome.b = temperature;
    }

    ///
    /// TEXTURE GEN (diffuse, specular, normal, emission)
    ///

    // Diffuse (procedural or LUT)
    vec3 diffuse = diffuseLUT(elevation, moisture, temperature, baseLevel);
    fragDiffuse = vec4(diffuse, 1.0);

    // Specular
    float waterFac = 1.0 - waterMask;
    fragSpecular = vec4(waterFac, waterFac, waterFac, 1.0);

    // Normal
    #ifdef normalMapFlag
    // Compute slope from finite differences
    vec3 pThetaPlus = sphericalToCartesian(phi, theta + dTheta);
    vec3 pPhiPlus = sphericalToCartesian(min(phi + dPhi, gln_PI * 0.5), theta);

    float elevTheta = computeElevation(pThetaPlus, u_baseLevel).x;
    float elevPhi = computeElevation(pPhiPlus, u_baseLevel).x;

    float dx = (elevTheta - elevation) / dTheta;
    float dy = (elevPhi - elevation) / dPhi;

    float normalScale = 0.004;
    vec3 normal = normalize(vec3(-dx * normalScale * waterMask, -dy * normalScale * waterMask, 1.0));
    fragNormal = vec4(normal.x * 0.5 + 0.5, normal.y * 0.5 + 0.5, normal.z, 1.0);
    #endif // normalMapFlag

    // Emission (procedural, from noise)
    #ifdef emissiveMapFlag
    float emissiveMask = cityMask(p, elevation, waterMask);
    float emi = noise(p, BLOCKY, 0.5, 5.0, 1.5, false, false, vec3(8.0), 4, u_seed + 0.1325);
    float city = emi * emissiveMask;

    // Some color.
    float r = gln_rand(xy + city) * 0.1 + 0.9;
    float g = gln_rand(xy - city) * 0.1 + 0.8;
    float b = gln_rand(xy + city * 1.5) * 0.2 + 0.5;
    fragEmission = vec4(city * r, city * g, city * b, 1.0);

    if (emissiveMask > 0.0) {
        float d = city;

        fragDiffuse.rgb = mix(fragDiffuse.rgb, vec3(d), emissiveMask * 1.7);

    }
    #else
    fragEmission = vec4(0.0, 0.0, 0.0, 1.0);
    #endif // emissiveMapFlag
}
