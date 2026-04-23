#version 330 core

#include <shader/lib/logdepthbuff.glsl>
#include <shader/lib/lines.glsl>

uniform float u_zfar;
uniform float u_k;

in vec4 v_col;
in vec2 v_uv;
// Used for depth computation.
in float v_w;

// We use the location of the layer buffer (1).
layout (location = 1) out vec4 layerBuffer;

#ifdef ssrFlag
#include <shader/lib/ssr.frag.glsl>
#endif // ssrFlag

void main() {
    vec2 line = computeLine(v_uv);

    // Final color calculation.
    layerBuffer = vec4(v_col.rgb + line.y, 1.0) * v_col.a * line.x;

    // Recover depth
    gl_FragDepth = logarithmicDepth(v_w, u_zfar, u_k);

    #ifdef ssrFlag
    ssrBuffers();
    #endif // ssrFlag
}