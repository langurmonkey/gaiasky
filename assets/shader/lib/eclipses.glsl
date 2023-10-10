#ifndef GLSL_LIB_ECLIPSES
#define GLSL_LIB_ECLIPSES

#ifdef eclipsingBodyFlag
uniform float u_vrScale;
uniform int u_eclipseOutlines;
uniform float u_eclipsingBodyRadius;
uniform vec3 u_eclipsingBodyPos;

#include <shader/lib/math.glsl>

#define UMBRA0 0.04
#define UMBRA1 0.035
#define PENUMBRA0 1.7
#define PENUMBRA1 1.69

/**
Computes the color of the fragment (shadow and outline) for eclipses.
*/
vec4 eclipseColor(in vec3 fragPosWorld, in vec3 lightDirection, in vec3 normalVector, out float outline, inout float shdw) {
    outline = -1.0;
    vec4 outlineColor;
    vec3 f = fragPosWorld;
    vec3 m = u_eclipsingBodyPos;
    vec3 l = lightDirection * u_vrScale;
    vec3 fl = f + l;
    float dist = dist_segment_point(f, fl, m);
    float dot_NM = dot(normalize(normalVector), normalize(m - f));
    if (dot_NM > -0.15) {
        if (dist < u_eclipsingBodyRadius * 1.5) {
            float eclfac = dist / (u_eclipsingBodyRadius * 1.5);
            shdw *= eclfac;
            if (dist < u_eclipsingBodyRadius * UMBRA0) {
                shdw = 0.0;
            }
        }
        #ifdef eclipseOutlines
        if(dot_NM > 0.0) {
            if (dist < u_eclipsingBodyRadius * PENUMBRA0 && dist > u_eclipsingBodyRadius * PENUMBRA1) {
                // Penumbra.
                outline = 1.0;
                outlineColor = vec4(0.95, 0.625, 0.0, 1.0);
            } else if (dist < u_eclipsingBodyRadius * UMBRA0 && dist > u_eclipsingBodyRadius * UMBRA1) {
                // Umbra.
                outline = 1.0;
                outlineColor = vec4(0.85, 0.26, 0.21, 1.0);
            }
        }
        #endif // eclipseOutlines
    }
    return outlineColor;
}
#endif // eclipsingBodyFlag

#endif // GLSL_LIB_ECLIPSES