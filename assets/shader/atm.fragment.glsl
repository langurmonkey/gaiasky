#version 330 core


#include <shader/lib/logdepthbuff.glsl>

uniform vec3 v3LightPos;
uniform float fG;
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
layout (location = 1) out vec4 layerBuffer;

#ifdef ssrFlag
#include <shader/lib/ssr.frag.glsl>
#endif // ssrFlag

#include <shader/lib/luma.glsl>
#include <shader/lib/atmscattering.frag.glsl>

void main(void) {
    float fCos = dot(-v3LightPos, normalize(v_direction));
    float fCos2 = fCos * fCos;
    // Rayleigh phase
    float fRayleighPhase = rayleighPhase(fCos2);
    // Mie phase
    float fMiePhase = miePhase(fCos2, fCos2);

    vec3 atmosphereColor = fRayleighPhase * v_frontColor.rgb + fMiePhase * v_frontSecondaryColor;

    #define exposure 0.15
    fragColor.rgb = vec3(1.0) - exp(atmosphereColor * -exposure);

    float lma = luma(fragColor.rbg);
    float scl = smoothstep(0.05, 0.2, lma);
    fragColor.a = (v_heightNormalized * (1.0 - v_fadeFactor) + lma * v_fadeFactor) * scl * v_frontColor.a;
    #ifdef eclipsingBodyFlag
    fragColor *= v_eclipseFactor;
    #endif // eclipsingBodyFlag

    layerBuffer = vec4(0.0, 0.0, 0.0, 0.0);

    gl_FragDepth = getDepthValue(u_cameraNearFar.y, u_cameraK);

    #ifdef ssrFlag
    ssrBuffers();
    #endif // ssrFlag
}
