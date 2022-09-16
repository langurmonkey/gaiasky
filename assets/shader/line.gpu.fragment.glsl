#version 330 core

#include shader/lib_logdepthbuff.glsl

uniform float u_alpha;
uniform float u_zfar;
uniform float u_k;
uniform float u_coordEnabled;
uniform float u_coordPos;
uniform float u_period;

in vec4 v_col;
in float v_coord;

layout (location = 0) out vec4 fragColor;

#ifdef ssrFlag
#include shader/lib_ssr.frag.glsl
#endif // ssrFlag

#ifdef velocityBufferFlag
#include shader/lib_velbuffer.frag.glsl
#endif

void main() {
    float trail;
    if (u_coordEnabled > 0.0) {
        trail = v_coord - u_coordPos;
        if (trail < 0.0) {
            trail += 1.0;
        }
        if (u_period <= 0.0 && v_coord > u_coordPos) {
            trail = 0.0;
        }
    } else {
        trail = 1.0;
    }
    fragColor = vec4(v_col.rgb * u_alpha * trail, 1.0);
    gl_FragDepth = getDepthValue(u_zfar, u_k);

    #ifdef ssrFlag
    ssrBuffers();
    #endif // ssrFlag

    #ifdef velocityBufferFlag
    velocityBuffer();
    #endif
}
