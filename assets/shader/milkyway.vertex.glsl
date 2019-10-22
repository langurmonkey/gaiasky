#version 330 core

#include shader/lib_math.glsl
#include shader/lib_geometry.glsl

uniform float u_pointAlphaMin;
uniform float u_pointAlphaMax;
uniform float u_starBrightness;
uniform mat4 u_projModelView;
uniform vec3 u_camPos;
uniform float u_sizeFactor;
uniform float u_intensity;
uniform float u_ar;
uniform vec2 u_edges;
uniform float u_vrScale;

uniform mat4 u_view;

#ifdef relativisticEffects
    #include shader/lib_relativity.glsl
#endif // relativisticEffects

#ifdef gravitationalWaves
    #include shader/lib_gravwaves.glsl
#endif // gravitationalWaves

in vec4 a_position;
in vec4 a_color;
// x - size, y - 0: star, 1: dust
in vec2 a_additional;

out vec4 v_col;
out float v_dust;

#ifdef velocityBufferFlag
#include shader/lib_velbuffer.vert.glsl
#endif

void main() {
    vec3 pos = a_position.xyz - u_camPos;
    float dist = length(pos);

    #ifdef relativisticEffects
        pos = computeRelativisticAberration(pos, dist, u_velDir, u_vc);
    #endif // relativisticEffects
    
    #ifdef gravitationalWaves
        pos = computeGravitationalWaves(pos, u_gw, u_gwmat3, u_ts, u_omgw, u_hterms);
    #endif // gravitationalWaves
    

    float dscale = smoothstep(u_edges.y, u_edges.x, dist);
    float viewAngle = a_additional.x / dist;

    v_col = vec4(a_color.rgb, a_color.a * u_intensity * dscale);
    v_dust = a_additional.y;

    gl_PointSize = viewAngle * u_sizeFactor * u_ar;

    vec4 gpos = u_projModelView * vec4(pos, 1.0);
    gl_Position = gpos;

    #ifdef velocityBufferFlag
    velocityBuffer(gpos, a_position.xyz, dist);
    #endif

}
