#version 330 core

#include shader/lib_logdepthbuff.glsl

in vec4 v_col;
in vec2 v_uv;

out vec4 fragColor;

#define PI 3.14159

void main() {
    // x is in [-1,1], where 0 is the center of the line
    float x = (v_uv.y - 0.5) * 2.0;

    //float alpha = pow(cos(PI * (v_uv.y - 0.5)), 8.0);
    float core = min(cos(PI * x / 2.0), 1.0 - abs(x));
    float alpha = pow(core, 1.5);
    float cplus = pow(core, 10.0);

    fragColor = vec4(v_col.rgb + cplus, 1.0) * v_col.a * alpha ;
    gl_FragDepth = getDepthValue();
}