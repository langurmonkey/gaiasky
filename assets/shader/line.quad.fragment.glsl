#version 330 core

#include shader/lib_logdepthbuff.glsl

uniform float u_zfar;
uniform float u_k;

in vec4 v_col;
in vec2 v_uv;

layout (location = 0) out vec4 fragColor;

#ifdef velocityBufferFlag
#include shader/lib_velbuffer.frag.glsl
#endif

#define PI 3.14159

void main() {
    // x is in [-1,1], where 0 is the center of the line
    float x = (v_uv.y - 0.5) * 2.0;

    float core = min(cos(PI * x / 2.0), 1.0 - abs(x));
    float alpha = pow(core, 1.8);
    float cplus = pow(core, 10.0);

    fragColor = vec4(v_col.rgb + cplus, 1.0) * v_col.a * alpha ;

    gl_FragDepth = getDepthValue(u_zfar, u_k);

    #ifdef velocityBufferFlag
    velocityBuffer(alpha);
    #endif
}