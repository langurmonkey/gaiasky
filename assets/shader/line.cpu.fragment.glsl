#version 330 core

#include <shader/lib/geometry.glsl>
#include <shader/lib/logdepthbuff.glsl>

uniform float u_alpha;
uniform float u_zfar;
uniform float u_k;

in vec4 v_col;

layout (location = 0) out vec4 fragColor;

#ifdef ssrFlag
#include <shader/lib/ssr.frag.glsl>
#endif // ssrFlag

void main() {
    fragColor = vec4(v_col.rgb, v_col.a * u_alpha);
    gl_FragDepth = getDepthValue(u_zfar, u_k);

    #ifdef ssrFlag
    ssrBuffers();
    #endif // ssrFlag
}
