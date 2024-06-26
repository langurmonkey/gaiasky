#version 330 core

#define exposure 0.2

#include <shader/lib/logdepthbuff.glsl>

uniform vec3 v3LightPos;
uniform float g;
uniform vec2 u_cameraNearFar;
uniform float u_cameraK;

// Direction from the vertex to the camera
in vec3 v_direction;
// Calculated colors
in vec4 v_frontColor;
in vec3 v_frontSecondaryColor;
// Height normalized
in float v_heightNormalized;
// Fade factor between hieght-driven opacity and luminosity-driven opacity
in float v_fadeFactor;
#ifdef eclipsingBodyFlag
in float v_eclipseFactor;
#endif // eclipsingBodyFlag

layout (location = 0) out vec4 fragColor;

#ifdef ssrFlag
#include <shader/lib/ssr.frag.glsl>
#endif // ssrFlag

#include <shader/lib/luma.glsl>

void main(void) {
    float g2 = g * g;
    float fCos = dot (-v3LightPos, v_direction) / length (v_direction);
    float fCos2 = fCos * fCos;
    float fRayleighPhase = 0.75 + 0.75 * fCos2;
    float fMiePhase = 1.5 * ((1.0 - g2) / (2.0 + g2)) * (1.0 + fCos2) / pow (1.0 + g2 - 2.0 * g * fCos, 1.5);

    fragColor.rgb = (fRayleighPhase * v_frontColor.rgb + fMiePhase * v_frontSecondaryColor);
    fragColor.rgb = vec3(1.0) - exp(-exposure * fragColor.rgb);

    float lma = luma(fragColor.rbg);
    float scl = smoothstep(0.05, 0.2, lma);
    fragColor.a = (v_heightNormalized * (1.0 - v_fadeFactor) + lma * v_fadeFactor) * scl * v_frontColor.a;
    #ifdef eclipsingBodyFlag
    fragColor *= v_eclipseFactor;
    #endif // eclipsingBodyFlag

    gl_FragDepth = getDepthValue(u_cameraNearFar.y, u_cameraK);

    #ifdef ssrFlag
    ssrBuffers();
    #endif // ssrFlag
}
