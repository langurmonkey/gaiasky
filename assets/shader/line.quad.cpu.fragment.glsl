#version 330 core

#include <shader/lib/logdepthbuff.glsl>

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

#define PI 3.14159

void main() {
    // x is in [-1,1], where 0 is the center of the line
    float x = (v_uv.y - 0.5) * 2.0;

    float core = min(cos(PI * x / 2.0), 1.0 - abs(x));
    float alpha = pow(core, 1.8);
    float cplus = pow(core, 10.0);

    // Recover depth value using input w.
    gl_FragDepth = logarithmicDepth(v_w, u_zfar, u_k);

    layerBuffer = vec4(v_col.rgb + cplus, 1.0) * v_col.a * alpha;

    #ifdef ssrFlag
    ssrBuffers();
    #endif // ssrFlag
}