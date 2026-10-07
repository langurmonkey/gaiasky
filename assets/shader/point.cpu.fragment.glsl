#version 330 core

#include <shader/lib/logdepthbuff.glsl>

uniform float u_zfar;
uniform float u_k;

in vec4 v_col;

layout (location = 0) out vec4 fragColor;
// We use the location of the layer buffer (1).
layout (location = 1) out vec4 layerBuffer;

#ifdef ssrFlag
#include <shader/lib/ssr.frag.glsl>
#endif // ssrFlag

void main() {
    fragColor = v_col;
    layerBuffer = vec4(0.0, 0.0, 0.0, 1.0);

    // Write logarithmic depth. Without this, the depth buffer contains the
    // fixed-pipeline depth, which saturates to 1.0 for anything beyond a few
    // thousand km. Post-processing effects that reconstruct the view distance
    // from the depth buffer (camera motion blur) then misinterpret the value
    // as a logarithmic depth and produce a view distance close to zero.
    gl_FragDepth = getDepthValue(u_zfar, u_k);

    #ifdef ssrFlag
    ssrBuffers();
    #endif // ssrFlag
}