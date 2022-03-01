#version 330 core

in vec4 v_col;

layout (location = 0) out vec4 fragColor;

#ifdef ssrFlag
#include shader/lib_ssr.frag.glsl
#endif // ssrFlag

void main() {
    fragColor = v_col;

    #ifdef ssrFlag
    ssrBuffers();
    #endif // ssrFlag
}