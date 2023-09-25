#version 330 core

// Attributes
in vec3 a_position;

// Varyings
out vec3 v_texCoords;

// Uniforms
uniform mat4 u_worldTrans;
uniform mat4 u_projViewTrans;
uniform float u_vrScale;

#if defined(velocityBufferFlag) || defined(relativisticEffects)
#include <shader/lib/geometry.glsl>
#endif

#ifdef relativisticEffects
#include <shader/lib/relativity.glsl>
#endif // relativisticEffects

#ifdef gravitationalWaves
#include <shader/lib/gravwaves.glsl>
#endif // gravitationalWaves

#ifdef velocityBufferFlag
#include <shader/lib/velbuffer.vert.glsl>
#endif // velocityBufferFlag

void main() {
    vec4 pos = u_worldTrans * vec4(a_position, 1.0);

    #ifdef relativisticEffects
    pos.xyz = computeRelativisticAberration(pos.xyz, length(pos.xyz), u_velDir, u_vc);
    #endif // relativisticEffects

    #ifdef gravitationalWaves
    pos.xyz = computeGravitationalWaves(pos.xyz, u_gw, u_gwmat3, u_ts, u_omgw, u_hterms);
    #endif // gravitationalWaves

    v_texCoords = vec3(-a_position.x, a_position.y, a_position.z);
    vec4 gpos = u_projViewTrans * pos;
    gl_Position = gpos;

    #ifdef velocityBufferFlag
    velocityBufferCam(gpos, pos, 0.0);
    #endif // velocityBufferFlag
}
