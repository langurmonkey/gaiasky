#version 330 core

#include shader/lib_logdepthbuff.glsl

uniform float u_alpha;
uniform float u_zfar;
uniform float u_k;
uniform float u_coordEnabled;
uniform float u_trailMap;
uniform float u_coordPos;
uniform float u_period;

in vec4 v_col;
in float v_coord;
in vec2 v_uv;

layout (location = 0) out vec4 fragColor;

#ifdef ssrFlag
#include shader/lib_ssr.frag.glsl
#endif// ssrFlag

#ifdef velocityBufferFlag
#include shader/lib_velbuffer.frag.glsl
#endif

#define PI 3.14159

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
    trail = (trail - u_trailMap) / (1.0 - u_trailMap);

    if (u_alpha <= 0.0 || trail <= 0.0) {
        discard;
    }

    // x is in [-1,1], where 0 is the center of the line
    float x = (v_uv.y - 0.5) * 2.0;

    float core = min(cos(PI * x / 2.0), 1.0 - abs(x));
    float alpha = pow(core, 1.8);
    float cplus = pow(core, 10.0);

    fragColor = vec4(v_col.rgb + cplus, 1.0) * v_col.a * alpha * trail * u_alpha;

    gl_FragDepth = getDepthValue(u_zfar, u_k);

    #ifdef ssrFlag
    ssrBuffers();
    #endif// ssrFlag

    #ifdef velocityBufferFlag
    velocityBuffer();
    #endif
}