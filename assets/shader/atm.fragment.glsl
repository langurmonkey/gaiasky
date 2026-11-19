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

    // Write a constant depth instead of the per-fragment one, so that the motion blur reconstructs
    // one consistent world position for all atmosphere pixels.
    //
    // The value is the distance to the planet centre, in internal units. It used to be
    // `getDepthValue(fOuterRadius, ...)`, which passed 1.025 *planet radii* where a distance in
    // units belongs: 1.025 units is 1.03e6 km at distanceScaleDesktop 1 (well behind the disc, so
    // the depth test kept the shell behind the ground and it only showed past the limb) but 102 km
    // at 1e4 (well in front of everything, so the shell painted over the whole disc and rejected
    // the clouds, which are drawn after it). The crossover is at the scale that makes 1 unit equal
    // the eye-to-ground distance, i.e. a few times ten - which is why 1e1 looked right and 1e2 did
    // not, and why every larger scale looked the same.
    //
    // The centre is behind every point of the planet's visibile disc and of the cloud deck at any
    // altitude, and in front of the far plane, so this reproduces the scale-1 behaviour everywhere.
    // Still constant per body, and still a depth that is not geometry: the half-resolution pass copies
    // the scene depth into a snapshot of its own before this shader runs
    // (SceneRenderer#snapshotDepth) and tests its content against that copy.
    gl_FragDepth = getDepthValue(u_depthDist, u_cameraNearFar.y, u_cameraK);

    #ifdef ssrFlag
    ssrBuffers();
    #endif // ssrFlag
}
