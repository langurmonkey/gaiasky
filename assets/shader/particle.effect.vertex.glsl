#version 330 core

#include shader/lib_geometry.glsl

in vec4 a_position;
in vec4 a_color;
// size
in float a_size;
// creation time
in float a_t;

uniform float u_alpha;

uniform mat4 u_projView;
uniform vec3 u_camPos;
uniform float u_sizeFactor;
uniform float u_t; // time in seconds
uniform float u_ttl; // time to live in seconds

#ifdef relativisticEffects
    #include shader/lib_relativity.glsl
#endif // relativisticEffects

#ifdef gravitationalWaves
    #include shader/lib_gravwaves.glsl
#endif // gravitationalWaves

#ifdef ssrFlag
#include shader/lib_ssr.vert.glsl
#endif // ssrFlag

out vec4 v_col;

#include shader/lib_math.glsl

void main() {
    vec3 pos = a_position.xyz - u_camPos;
    
    #ifdef relativisticEffects
        pos = computeRelativisticAberration(pos, length(pos), u_velDir, u_vc);
    #endif // relativisticEffects
    
    #ifdef gravitationalWaves
        pos = computeGravitationalWaves(pos, u_gw, u_gwmat3, u_ts, u_omgw, u_hterms);
    #endif // gravitationalWaves
    
    // Fade particles according to time to live (ttl)
    float live_time = u_t - a_t;
    float alpha = u_alpha * lint(live_time, 0.0, u_ttl, 1.0, 0.0);
    
    v_col = vec4(a_color.rgb, a_color.a * alpha);

    vec4 gpos =  u_projView * vec4(pos, 1.0);
    gl_Position = gpos;
    gl_PointSize = a_size * u_sizeFactor;

    #ifdef ssrFlag
    ssrData(gpos);
    #endif // ssrFlag
}
