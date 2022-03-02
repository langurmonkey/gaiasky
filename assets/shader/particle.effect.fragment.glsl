#version 330 core

uniform float u_ar;

in vec4 v_col;

layout (location = 0) out vec4 fragColor;
layout (location = 1) out vec4 velMap;

#ifdef ssrFlag
#include shader/lib_ssr.frag.glsl
#endif // ssrFlag

void main() {
    float alpha = v_col.a;
    fragColor = vec4(v_col.rgb * alpha, alpha);

    #ifdef ssrFlag
    ssrBuffers();
    #endif // ssrFlag

    velMap = vec4(0.0);
}
