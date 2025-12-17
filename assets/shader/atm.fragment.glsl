#version 330 core


#include <shader/lib/logdepthbuff.glsl>

uniform vec2 u_cameraNearFar;
uniform float u_cameraK;

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
in vec3 v_position;

void main(void) {
    vec4 atmosphereColor = computeAtmosphericScattering(v_position);
    fragColor = atmosphereColor;
    #ifdef eclipsingBodyFlag
    fragColor *= v_eclipseFactor;
    #endif // eclipsingBodyFlag

    layerBuffer = vec4(0.0, 0.0, 0.0, 0.0);

    gl_FragDepth = getDepthValue(u_cameraNearFar.y, u_cameraK);

    #ifdef ssrFlag
    ssrBuffers();
    #endif // ssrFlag
}
