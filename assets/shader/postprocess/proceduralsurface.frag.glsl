// Merged procedural surface shader.
// Combines noise generation (biome) + surface generation (surfacegen) into a single pass.
// Computes elevation, moisture and temperature from noise, then uses them to
// generate diffuse, specular, normal and emission outputs procedurally.
#version 330 core
precision highp float;

#include <shader/lib/colors.glsl>
#include <shader/lib/luma.glsl>
#include <shader/lib/noise/common.glsl>
#include <shader/lib/noise/simplex.glsl>
#include <shader/lib/noise/perlin.glsl>
#include <shader/lib/noise/voronoi.glsl>

// LUT texture
uniform sampler2D u_texture1;
// LUT hue shift.
uniform float u_lutHueShift;
// LUT saturation value.
uniform float u_lutSaturation;

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

in vec2 v_texCoords;

layout (location = 0) out vec4 fragBiome;
layout (location = 1) out vec4 fragDiffuse;
layout (location = 2) out vec4 fragSpecular;
layout (location = 3) out vec4 fragEmission;
#ifdef normalMapFlag
layout (location = 4) out vec4 fragNormal;
#endif // normalMapFlag

// Noise types
#define PERLIN 0
#define SIMPLEX 1
#define VORONOI 2

float random(vec2 st) {
    return fract(sin(dot(st.xy, vec2(12.9898, 78.233))) * 43758.5453123);
}

float noise(vec3 p,
        int type,
        float frequency,
        bool turbulence,
        bool ridge,
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

vec3 diffuseLUT(float elevation, float moisture, float temperature, float baseLevel) {
    float epsilon = 1.0 / 255.0; // One quantization step for 8-bit textures
    if (elevation <= baseLevel + epsilon) {
        elevation = 0.0;
    }

    vec4 rgba = texture(u_texture1, vec2(moisture, 1.0 - elevation));
    vec4 c = rgba;
    // Manipulate hue and saturation.
    vec3 hsv = rgb2hsv(rgba.rgb);
    // Hue.
    hsv.x = mod(hsv.x * 360.0 + u_lutHueShift, 360.0) / 360.0;
    // Saturation.
    hsv.y = hsv.y * u_lutSaturation;

    // Back to RGB, rotated.
    rgba.rgb = hsv2rgb(hsv);

    return rgba.rgb;
}

// Procedural diffuse color from elevation, moisture and temperature.
vec3 diffuseProcedural(float elevation, float moisture, float temperature, float baseLevel) {
    float epsilon = 1.0 / 255.0;

    vec3 waterColor = vec3(0.2, 0.4, 0.8);
    vec3 deepWaterColor = vec3(0.0, 0.1, 0.5);

    // Water.
    if (elevation <= baseLevel + epsilon) {
        float depth = 1.0 - (elevation / baseLevel);
        return mix(waterColor, deepWaterColor, clamp(depth * 2.0, 0.0, 1.0));
    }

    // Land: normalize height above water to [0, 1].
    float h = (elevation - baseLevel) / (1.0 - baseLevel);
    h = clamp(h, 0.0, 1.0);

    // Low elevation: vegetation / desert
    // Moisture controls the green vs. brown gradient.
    vec3 low = mix(
        vec3(0.76, 0.70, 0.50),  // dry: sandy/savanna
        vec3(0.12, 0.45, 0.10),  // wet: forest green
        moisture
    );

    // Mid elevation: rock / scrub
    vec3 mid = vec3(0.48, 0.40, 0.30);

    // High elevation: rock / snow
    vec3 highBase = vec3(0.55, 0.50, 0.42);
    vec3 snow = vec3(0.92, 0.93, 0.95);
    vec3 high = mix(highBase, snow, smoothstep(0.65, 0.95, h));

    // Blend by height.
    vec3 color;
    if (h < 0.35) {
        color = mix(low, mid, h / 0.35);
    } else if (h < 0.70) {
        color = mix(mid, high, (h - 0.35) / 0.35);
    } else {
        color = high;
    }

    // Temperature influence: cold → desaturate and darken.
    // temperature is in [0, 1], 0 = cold, 1 = warm.
    float coldness = 1.0 - temperature;
    color = mix(color * 0.5, color, temperature);

    // Very cold: add frost/ice tint.
    vec3 frost = vec3(0.85, 0.88, 0.92);
    color = mix(color, frost, smoothstep(0.0, 0.15, coldness) * (1.0 - h * 0.5));

    return color;
}

void main() {
    // Sample point on sphere surface.
    vec2 xy = v_texCoords * u_viewport;
    float phiStep = gln_PI / (u_viewport.y - 1);
    float phi = (-gln_PI / 2.0) + xy.y * phiStep;
    float thetaStep = gln_PI * 2.0 / u_viewport.x;
    float theta = xy.x * thetaStep;
    float cosPhi = cos(phi);
    vec3 p = vec3(
            cosPhi * cos(theta),
            cosPhi * sin(theta),
            sin(phi)
    );
    float baseLevel = u_baseLevel;

    ///
    /// BIOME GEN (elevation, moisture, temperature)
    ///
    fragBiome = vec4(0.0, 0.0, 0.0, 1.0);

    // Elevation (channel 1)
    float elevation_noise = noise(p, u_type, u_frequency, u_turbulence, u_ridge, u_scale, u_octaves, u_seed);
    if (u_smoothing) {
        elevation_noise = smoothstep(0.0, 1.0, elevation_noise);
    }

    float elevation;
    if (u_remap) {
        elevation = gln_map(elevation_noise, baseLevel, 1.0, 0.0, 1.0);
        // In remap mode, base level gets mapped to 0.
        baseLevel = 0.0;
    } else {
        elevation = max(baseLevel, elevation_noise);
    }
    fragBiome.r = elevation;

    // Moisture (channel 2)
    float moisture = 0.0;
    if (u_channels >= 2) {
        moisture = noise(p + vec3(0.1, -0.4, 0.2), SIMPLEX, 0.5, u_turbulence, u_ridge, u_scale, u_octaves, u_seed + 0.023);
        fragBiome.g = moisture;
    }

    // Temperature (channel 3)
    float temperature = 0.0;
    if (u_channels >= 3) {
        const float LATITUDE_INFLUENCE = 0.8;
        float latitudeFactor = 1.0 - abs(phi) / (gln_PI * 0.5); // 1 at equator, 0 at poles
        float noiseTemperature = noise(p, u_type, u_frequency, false, false, u_scale, u_octaves, u_seed + 0.4325);
        temperature = mix(noiseTemperature, latitudeFactor, LATITUDE_INFLUENCE);
        fragBiome.b = temperature;
    }

    ///
    /// TEXTURE GEN (diffuse, specular, normal, emission)
    ///

    // Diffuse (procedural or LUT)
    vec3 diffuse = diffuseLUT(elevation, moisture, temperature, baseLevel);
    fragDiffuse = vec4(diffuse, 1.0);

    // Specular
    float epsilon = 1.0 / 255.0;
    float waterFac = 1.0 - smoothstep(baseLevel - 0.05, baseLevel, elevation);
    float snowFac = smoothstep(0.9, 0.99, luma(fragDiffuse.rgb));
    fragSpecular = vec4(vec3(waterFac + snowFac), 1.0);

    // Normal
    #ifdef normalMapFlag
    float normalScale = 4.0;
    float dx = dFdx(elevation) * normalScale;
    float dy = dFdy(elevation) * normalScale;
    float dz = 1.0;
    vec3 normal = normalize(vec3(dx, dy, dz));
    fragNormal = vec4(normal.x * 0.5 + 0.5, normal.y * 0.5 + 0.5, normal.z, 1.0);
    #endif // normalMapFlag

    // Emission (procedural, from noise)
    #ifdef emissiveMapFlag
    float emi = noise(p, SIMPLEX, 0.16, false, false, vec3(8.0, 8.0, 8.0), 5, u_seed + 0.1325);
    emi = emi * smoothstep(0.55, 0.9, emi) * 2.0;
    emi = emi * noise(p + vec3(0.1, -0.1, 0.3), VORONOI, 1.6, true, true, vec3(14.0), 1, u_seed);
    // Not on water!
    emi = emi * step(baseLevel, elevation_noise);
    float r = gln_rand(xy + emi) * 0.2 + 0.8;
    float g = gln_rand(xy + emi) * 0.2 + 0.7;
    float b = gln_rand(xy + emi) * 0.2 + 0.5;
    fragEmission = vec4(emi * r, emi * g, emi * b, 1.0);
    #else
    fragEmission = vec4(0.0, 0.0, 0.0, 1.0);
    #endif // emissiveMapFlag
}