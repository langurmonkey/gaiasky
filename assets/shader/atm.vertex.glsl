#version 330 core


uniform mat4 u_projViewTrans;
uniform mat4 u_worldTrans;

in vec3 a_position;

#include shader/lib_atmscattering.glsl

////////////////////////////////////////////////////////////////////////////////////
//////////RELATIVISTIC EFFECTS - VERTEX
////////////////////////////////////////////////////////////////////////////////////
#ifdef relativisticEffects
    #include shader/lib_geometry.glsl
    #include shader/lib_relativity.glsl
#endif // relativisticEffects


////////////////////////////////////////////////////////////////////////////////////
//////////GRAVITATIONAL WAVES - VERTEX
////////////////////////////////////////////////////////////////////////////////////
#ifdef gravitationalWaves
    #include shader/lib_gravwaves.glsl
#endif // gravitationalWaves

void main(void) {
    computeAtmosphericScattering();
    vec4 pos = u_worldTrans * vec4(a_position, 1.0);

    #ifdef relativisticEffects
        pos.xyz = computeRelativisticAberration(pos.xyz, length(pos.xyz), u_velDir, u_vc);
    #endif // relativisticEffects
    
    #ifdef gravitationalWaves
        pos.xyz = computeGravitationalWaves(pos.xyz, u_gw, u_gwmat3, u_ts, u_omgw, u_hterms);
    #endif // gravitationalWaves
    
    vec4 gpos = u_projViewTrans * pos;
    gl_Position = gpos;
}
