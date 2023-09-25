#version 330 core

// OUTPUT
layout (location = 0) out vec4 fragColor;

#ifdef ssrFlag
#include <shader/lib/ssr.frag.glsl>
#endif // ssrFlag

// Renders all black for the occlusion testing
void main() {
    fragColor = vec4(0.0, 0.0, 0.0, 1.0);

    #ifdef ssrFlag
    ssrBuffers();
    #endif // ssrFlag
}
