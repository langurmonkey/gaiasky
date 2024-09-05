#version 330 core

#include <shader/lib/geometry.glsl>
#include <shader/lib/logdepthbuff.glsl>

uniform float u_alpha;
uniform float u_zfar;
uniform float u_k;

in vec4 v_col;

// We use the location of the layer buffer (1).
layout (location = 1) out vec4 layerBuffer;

#ifdef ssrFlag
#include <shader/lib/ssr.frag.glsl>
#endif // ssrFlag

void main() {
    layerBuffer = vec4(v_col.rgb, v_col.a * u_alpha);
    gl_FragDepth = getDepthValue(u_zfar, u_k);

    #ifdef ssrFlag
    ssrBuffers();
    #endif // ssrFlag
}
