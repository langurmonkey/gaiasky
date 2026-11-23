#version 330 core

#include <shader/lib/logdepthbuff.glsl>

uniform vec2 u_cameraNearFar;
uniform float u_cameraK;
// Distance from the eye to the planet centre, in internal units (see AtmosphereComponent).
uniform float u_depthDist;

#ifdef eclipsingBodyFlag
in float v_eclipseFactor;
#endif // eclipsingBodyFlag

layout (location = 0) out vec4 fragColor;
layout (location = 1) out vec4 layerBuffer;

#ifdef ssrFlag
#include <shader/lib/ssr.frag.glsl>
#endif // ssrFlag

#include <shader/lib/atmscattering.frag.glsl>
in vec3 v_position;

void main(void) {
    vec4 atmosphereColor = computeAtmosphericScattering(v_position);
    //fragColor = vec4(atmosphereColor.rgb * atmosphereColor.a, atmosphereColor.a);
    fragColor = atmosphereColor;
    #ifdef eclipsingBodyFlag
    fragColor *= v_eclipseFactor;
    #endif // eclipsingBodyFlag

    layerBuffer = vec4(0.0, 0.0, 0.0, 1.0);

    // Constant depth: the distance to the planet centre in internal units, which reproduces the
    // scale-1 behaviour at every distance scale (see SceneRenderer#snapshotDepth).
    gl_FragDepth = getDepthValue(u_depthDist, u_cameraNearFar.y, u_cameraK);

    #ifdef ssrFlag
    ssrBuffers();
    #endif // ssrFlag
}
