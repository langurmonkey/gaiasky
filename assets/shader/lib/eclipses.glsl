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
// Where diffraction starts
#define DIFFRACTION0 0.4
// Define width of penumbra outline
#define PENUMBRA0 1.7
#define PENUMBRA1 1.69

// Function to get orange-to-red spectrum only
vec3 getDiffractionSpectrum(float pos) {
    // Smooth transition from orange to red
    // Orange: vec3(1.0, 0.5, 0.0)
    // Red:    vec3(1.0, 0.0, 0.0)
    
    return mix(
        vec3(1.0, 0.5, 0.0),  // Orange at pos = 0
        vec3(1.0, 0.0, 0.0),  // Red at pos = 1
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
    float edgeFade = smoothstep(-0.1, 0.2, dot_NL); // Fade when approaching 90 degrees (dot_NL → 0)

    if (dot_NM > -0.15) {
        if (dist < u_eclipsingBodyRadius * 1.7) {
            float eclfac = dist / (u_eclipsingBodyRadius * 1.7);
            shdw *= eclfac;

            // Diffraction between DIFFRACTION0 and PENUMBRA0
            float diffractionStart = u_eclipsingBodyRadius * DIFFRACTION0;
            float diffractionEnd = u_eclipsingBodyRadius * PENUMBRA0;
            float diffractionRange = diffractionEnd - diffractionStart;

            if (dist > diffractionStart && dist < diffractionEnd) {
                float x = (dist - diffractionStart) / diffractionRange; // 0 to 1
                float diffractionIntensity = 4.0 * x * (1.0 - x); // Perfect parabola: 0 → 1 → 0
                diffractionIntensity *= 0.15; // Reduce intensity
                
                // Apply edge fade to diffraction
                diffractionIntensity *= edgeFade;
                
                // Diffraction spectrum colors based on position in the diffraction band
                float spectrumPos = x; // Use x to map across spectrum
                vec3 spectrumColor = getDiffractionSpectrum(spectrumPos);
                
                diffractionTint = spectrumColor * diffractionIntensity;
            }

            // Apply edge fade to shadow as well
            shdw = mix(1.0, shdw, edgeFade);
            if (dist < u_eclipsingBodyRadius * UMBRA0) {
                shdw = 0.0;
            }
        }
        #ifdef eclipseOutlines
        if(dot_NM > 0.0) {
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

#endif // eclipsingBodyFlag

#endif // GLSL_LIB_ECLIPSES
