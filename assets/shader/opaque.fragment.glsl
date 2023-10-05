#version 330 core

// Uniforms which are always available
uniform vec2 u_cameraNearFar;
uniform float u_cameraK;

// OUTPUT
layout (location = 0) out vec4 fragColor;

#include <shader/lib/logdepthbuff.glsl>

#ifdef ssrFlag
#include <shader/lib/ssr.frag.glsl>
#endif // ssrFlag

// Renders all black for the occlusion testing.
void main() {
    fragColor = vec4(0.0, 0.0, 0.0, 1.0);

    // Logarithmic depth buffer.
    gl_FragDepth = getDepthValue(u_cameraNearFar.y, u_cameraK);

    #ifdef ssrFlag
    ssrBuffers();
    #endif // ssrFlag
}
