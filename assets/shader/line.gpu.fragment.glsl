#version 330 core

#include shader/lib_logdepthbuff.glsl

uniform float u_alpha;
uniform float u_zfar;
uniform float u_k;
uniform float u_coordEnabled;
uniform float u_trailMap;
uniform float u_coordPos;
uniform float u_period;
uniform float u_lineWidth;
uniform float u_blendFactor;

in vec4 v_col;
in float v_coord;
in vec2 v_lineCenter;

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
    trail = (1.0 / (1.0 - u_trailMap)) * (trail - u_trailMap);

    vec4 col = v_col;
    float a = 1.0;
    if (u_lineWidth > 0.0) {
        // We do aliasing here!
        float d = length(v_lineCenter - gl_FragCoord.xy);
        float w = u_lineWidth;
        if (d > w) {
            col *= 0.0;
            a = 0.0;
        } else {
            col.rgb *= pow((w - d) / w, u_blendFactor);
        }
    }

    fragColor = vec4(col.rgb * u_alpha * trail, a);
    gl_FragDepth = getDepthValue(u_zfar, u_k);

    #ifdef ssrFlag
    ssrBuffers();
    #endif // ssrFlag

    #ifdef velocityBufferFlag
    velocityBuffer();
    #endif
}
