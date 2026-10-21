#version 330 core
// Light glow implementation by Toni Sagrista.

#define MAX_LIGHTS 10

// Height, in pixels, the halo sizes below are expressed at.
#define REF_HEIGHT 1080.0
// Tangent of the reference fov (see AbstractCamera).
#define TAN_REF_FOV 0.36397023
// Pixels per unit of normalised view angle, at the reference height.
#define PX_PER_ANGLE (REF_HEIGHT * 0.5 / TAN_REF_FOV)
// Smallest halo radius, in pixels at the reference height.
#define MIN_HALO_PX 0.1
// Angular radius, in pixels at the reference height, of a full-size halo.
#define GLOW_ANGULAR_REF_PX 2.0e-3
// Exponent of the halo radius against the fov: 0 keeps it constant, -1 is physical.
#define GLOW_FOV_EXP 0.4
// Radius, in pixels at the reference height, and strength of the white core.
#define CORE_PX 6.0
#define CORE_STRENGTH 0.25
// Rotation speed of the angular mask of the halos. 0.0 freezes it.
#define GLOW_MASK_SPEED 1.0

// Current frame.
uniform sampler2D u_texture0;
// Glow texture.
uniform sampler2D u_texture1;
// Scene depth buffer, used to drop the glow of the lights that are hidden behind other objects.
uniform sampler2D u_texture2;

uniform vec2 u_viewport;
// Halo radius, in pixels at the reference height, of a full-size halo.
uniform float u_textureScale;
uniform vec2 u_lightPositions[MAX_LIGHTS];
// Apparent view angle of each light, normalised with the fov factor (see Body.solidAngleApparent).
uniform float u_lightViewAngles[MAX_LIGHTS];
// Distance from the camera to each light, in the same space as the depth buffer.
uniform float u_lightDepths[MAX_LIGHTS];
// Glow intensity of each light in [0,1], from the faintest glowing star (0) to the Sun at 1 AU (1).
uniform float u_lightIntensities[MAX_LIGHTS];
uniform vec3 u_lightColors[MAX_LIGHTS];
uniform int u_nLights;
uniform float u_backbufferScale;
// Z-far and K of the logarithmic depth buffer, to recover a distance from a depth value.
uniform vec2 u_zFarK;
// Size of one texel of the depth buffer, in uv, to average a small neighbourhood.
uniform vec2 u_depthTexel;
// 1 to occlude the glow with the geometry in front of each light, 0 otherwise.
uniform float u_occlusion;
// Field of view factor: tan(fov/2) over tan(reference fov/2). 1 at the reference fov.
uniform float u_fovFactor;
// Run time in seconds, drives the rotation of the angular mask.
uniform float u_time;

in vec2 v_texCoords;

#define saturate(x) clamp(x, 0.0, 1.0)

#include <shader/lib/logdepthbuff.glsl>

layout (location = 0) out vec4 fragColor;

float brightness(vec4 color) {
    return dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
}

vec4 starImage(vec2 tc) {
    return texture(u_texture1, tc);
}

// Deterministic pseudo-random value in [0,1) for the given seed.
float hash11(float p) {
    p = fract(p * 0.1031);
    p *= p + 33.33;
    p *= p + p;
    return fract(p);
}

// Angular mask of the halo, seeded per light so that no two halos share the same pattern.
float polarMask(vec2 uv, float seed) {
    // Center coordinates in [-1,1].
    vec2 p = uv * 2.0 - 1.0;
    float r = length(p);
    // Fixed direction at the center, where normalizing would yield NaNs.
    vec2 d = r > 0.0 ? p / r : vec2(0.0, 1.0);

    // Static phase of the light plus the rotation with time.
    float t = seed * 6.2831853 + u_time * GLOW_MASK_SPEED;

    // Angular modulation.
    float angularMask = 0.5
    + 0.25 * sin(d.x * 12.0 + t * 2.0)
    + 0.20 * cos(d.y * 37.0 - t * 1.3)
    + 0.10 * sin((d.x + d.y) * 59.0 + t * 1.6);

    // In [0,1].
    angularMask = (angularMask + 1.0) * 0.5;
    // In [minVal,1].
    float minVal = 0.55;
    angularMask = minVal + (1.0 - minVal) * angularMask;

    // Keep the center.
    float center = smoothstep(0.85, 1.0, 1.0 - r);

    // Combine radial decay with angular modulation.
    return clamp(angularMask + center, minVal, 1.0);
}

// Distance to the geometry in front of the given pixel, averaged over a 2x2 neighbourhood to soften
// the crossing of a silhouette. Pixels with no geometry map to the far plane.
float sceneDepth(vec2 uv) {
    vec2 o = u_depthTexel;
    vec2 c = clamp(uv, o, vec2(1.0) - o);
    float d0 = texture(u_texture2, c + vec2(-o.x, -o.y)).r;
    float d1 = texture(u_texture2, c + vec2(o.x, -o.y)).r;
    float d2 = texture(u_texture2, c + vec2(-o.x, o.y)).r;
    float d3 = texture(u_texture2, c + vec2(o.x, o.y)).r;
    return 0.25 * (
    1.0 / recoverWValue(d0, u_zFarK.x, u_zFarK.y) +
    1.0 / recoverWValue(d1, u_zFarK.x, u_zFarK.y) +
    1.0 / recoverWValue(d2, u_zFarK.x, u_zFarK.y) +
    1.0 / recoverWValue(d3, u_zFarK.x, u_zFarK.y));
}

// Fraction of the light that is visible, 1.0 when nothing is in front of it. Tested once per light,
// at its position, so the sprite does not get cut along the silhouette of a passing object.
float lightVisibility(int li, float depth) {
    float lightDepth = u_lightDepths[li];
    if (u_occlusion < 0.5 || lightDepth <= 0.0) {
        return 1.0;
    }

    // Tolerance band on the near side, so the light does not occlude itself.
    float edge = lightDepth * 1.0e-2;
    return smoothstep(lightDepth - 2.0 * edge, lightDepth - edge, depth);
}

void main() {
    float ar = u_viewport.x / u_viewport.y;
    vec3 effectColor = vec3(0.0, 0.0, 0.0);

    for (int li = 0; li < u_nLights; li++) {
        // Glow strength in [0,1], from the apparent magnitude of the light.
        float strength = clamp(u_lightIntensities[li], 0.0, 1.0);
        if (strength <= 0.0) {
            continue;
        }

        // Angular radius of the light relative to the one that gets a full-size halo.
        float angNorm = u_lightViewAngles[li] * PX_PER_ANGLE / GLOW_ANGULAR_REF_PX;

        // A light with a tiny angular size is a faint dot and gets only a fraction of the glow its
        // magnitude alone would give it, so far stars do not glow as if they were close.
        float prominence = smoothstep(0.003, 0.20, angNorm);
        float intensity = strength * prominence;
        if (intensity <= 1.0e-4) {
            continue;
        }

        float visibility = lightVisibility(li, sceneDepth(clamp(u_lightPositions[li], 0.0, 1.0)));
        if (visibility <= 0.0) {
            continue;
        }

        // Halo radius proportional to the angular radius, so it grows as the camera approaches. The
        // second factor bends the 1/fovFactor of the view angles into the GLOW_FOV_EXP response.
        float radiusPx = u_textureScale * angNorm * pow(u_fovFactor, 1.0 + GLOW_FOV_EXP);
        // Never below the minimum, never above the maximum, never larger than the screen.
        radiusPx = min(max(radiusPx, MIN_HALO_PX), min(u_textureScale, REF_HEIGHT * 0.5));
        if (radiusPx <= 0.0) {
            continue;
        }

        // Halo radius as a fraction of the viewport height.
        float size = radiusPx / REF_HEIGHT;

        vec3 lightColor = u_lightColors[li];

        // Sprite coordinates in units of the halo radius: center at (0,0), edge at a length of one.
        vec2 p = vec2(v_texCoords * u_backbufferScale - u_lightPositions[li]);
        p.x *= ar;
        float r = length(p) / size;

        // Beyond the halo edge there is nothing to add.
        if (r >= 1.0) {
            continue;
        }

        vec2 tc = p / size * 0.5 + 0.5;
        float mask = polarMask(tc, hash11(float(li) + 1.0));
        vec4 glow = starImage(tc);
        glow.rgb *= mask;

        float glow_value = brightness(glow) * intensity * visibility;
        // Compact white core, capped by the halo so a minimum-size halo does not become a white blob.
        float corePx = min(CORE_PX, radiusPx * 0.5);
        float core = (1.0 - smoothstep(0.0, corePx, r * radiusPx)) * CORE_STRENGTH;

        effectColor += glow_value * (lightColor + vec3(core));
    }
    fragColor.rgb = saturate(effectColor + texture(u_texture0, v_texCoords).rgb);
    fragColor.a = 1.0;
}
