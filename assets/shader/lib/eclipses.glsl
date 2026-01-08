#ifndef GLSL_LIB_ECLIPSES
#define GLSL_LIB_ECLIPSES

#ifdef eclipsingBodyFlag
uniform float u_vrScale;
uniform int u_eclipseOutlines;
uniform float u_eclipsingBodyRadius;
uniform vec3 u_eclipsingBodyPos;

#include <shader/lib/math.glsl>

// Define width of umbra outline 
#define UMBRA0 0.04
#define UMBRA1 0.035
// Define width of penumbra outline
#define PENUMBRA0 1.7
#define PENUMBRA1 1.69
// Where diffraction starts
#define DIFFRACTION0 0.2
#define DIFFRACTION1 1.6

// Function to get diffraction spectrum
vec3 getDiffractionSpectrum(float pos) {
    return mix(
    vec3(0.41, 0.26, 0.013),
    vec3(0.88, 0.42, 0.063),
    pos
    );
}
/**
 * Computes the color of the fragment (shadow and outline) for eclipses.
**/
vec4 eclipseColor(in vec3 fragPosWorld, in vec3 lightDirection, in vec3 normalVector, out int outline, out vec3 diffractionTint, out float shdw) {
    shdw = 1.0;
    outline = -1;
    vec4 outlineColor = vec4(0.0);
    diffractionTint = vec3(0.0);
    vec3 f = fragPosWorld;
    vec3 m = u_eclipsingBodyPos;
    vec3 l = lightDirection * u_vrScale;
    vec3 fl = f + l;
    float dist = dist_segment_point(f, fl, m);
    float dot_NM = dot(normalize(normalVector), normalize(m - f));

    // Calculate angle-based fade at the edge of the Earth
    float dot_NL = dot(normalize(normalVector), normalize(lightDirection));
    float edgeFade = smoothstep(-0.1, 0.2, dot_NL);

    if (dot_NM > -0.15) {
        if (dist < u_eclipsingBodyRadius * 1.7) {
            shdw = dist / (u_eclipsingBodyRadius * 1.7);

            // Diffraction between DIFFRACTION0 and DIFFRACTION1
            float diffractionStart = u_eclipsingBodyRadius * DIFFRACTION0;
            float diffractionEnd = u_eclipsingBodyRadius * DIFFRACTION1;
            float diffractionRange = diffractionEnd - diffractionStart;

            if (dist > diffractionStart && dist < diffractionEnd) {
                float x = (dist - diffractionStart) / diffractionRange;
                float diffractionIntensity = 4.0 * x * (1.0 - x);
                diffractionIntensity *= 0.3;

                // Apply edge fade to diffraction
                diffractionIntensity *= edgeFade;

                // Diffraction spectrum colors based on position in the diffraction band
                float spectrumPos = x;
                vec3 spectrumColor = getDiffractionSpectrum(spectrumPos) * 0.5;

                diffractionTint = spectrumColor * diffractionIntensity;
            }

            // Apply edge fade to shadow as well
            shdw = mix(1.0, shdw, edgeFade);
            if (dist < u_eclipsingBodyRadius * UMBRA0) {
                shdw = 0.0;
            }
        }
        #ifdef eclipseOutlines
        if (dot_NM > 0.0) {
            if (dist < u_eclipsingBodyRadius * PENUMBRA0 && dist > u_eclipsingBodyRadius * PENUMBRA1) {
                // Penumbra.
                outline = 1;
                outlineColor = vec4(0.95, 0.625, 0.0, 1.0);
            } else if (dist < u_eclipsingBodyRadius * UMBRA0 && dist > u_eclipsingBodyRadius * UMBRA1) {
                // Umbra.
                outline = 1;
                outlineColor = vec4(0.85, 0.26, 0.21, 1.0);
            }
        }
        #endif // eclipseOutlines
    }
    return outlineColor;
}

// Simple additive blending.
vec3 eclipseBlendAdditive(vec3 base, vec3 tint, float shadow) {
    return clamp(base + tint, 0.0, 1.0) * shadow;
}

// A weighted mix function between base and tint based on the shadow value.
vec3 eclipseBlendWeightedMix(vec3 base, vec3 tint, float shadow) {
    return mix(base, tint, 1.0 - shadow);
}

// An energy-preserving blend.
vec3 eclipseBlendPreserveEnergy(vec3 base, vec3 tint, float shadow) {
    return base * shadow + tint * (1.0 - shadow);
}

// Emissive addition for a stronger glow.
vec3 eclipseBlendEmissiveAddition(vec3 base, vec3 tint, float shadow) {
    float alpha = 0.1;
    return base * shadow + tint * pow(1.0 - shadow, alpha);
}

// Perform blending of base color, tint, and eclipse shadow.
vec3 eclipseBlend(vec3 base, vec3 tint, float shadow) {
    return eclipseBlendWeightedMix(base, tint, shadow);
}
#endif // eclipsingBodyFlag

#endif // GLSL_LIB_ECLIPSES
