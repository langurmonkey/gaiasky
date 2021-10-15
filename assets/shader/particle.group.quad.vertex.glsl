#version 330 core

#include shader/lib_math.glsl
#include shader/lib_geometry.glsl

in vec4 a_position;
in vec3 a_particlePos;
in vec2 a_texCoord;
in vec4 a_color;
in float a_size;

uniform float u_alpha;
uniform mat4 u_projView;
uniform vec3 u_camPos;
uniform vec3 u_camUp;
uniform float u_sizeFactor;
uniform vec2 u_sizeLimits;
uniform float u_vrScale;

#ifdef relativisticEffects
    #include shader/lib_relativity.glsl
#endif // relativisticEffects

#ifdef gravitationalWaves
    #include shader/lib_gravwaves.glsl
#endif // gravitationalWaves

// OUTPUT
out vec4 v_col;
out vec2 v_uv;

#ifdef velocityBufferFlag
#include shader/lib_velbuffer.vert.glsl
#endif

void main() {
    vec3 pos = a_particlePos - u_camPos;

    // Distance to point - watch out, if position contains large values, this produces overflow!
    // Downscale before computing length()
    float dist = length(pos * 1e-14) * 1e14;

    #ifdef relativisticEffects
        pos = computeRelativisticAberration(pos, dist, u_velDir, u_vc);
    #endif // relativisticEffects
    
    #ifdef gravitationalWaves
        pos = computeGravitationalWaves(pos, u_gw, u_gwmat3, u_ts, u_omgw, u_hterms);
    #endif // gravitationalWaves
    
    v_col = vec4(a_color.rgb, a_color.a * u_alpha);

    float viewAngle = a_size / dist;
    float quadSize = clamp(viewAngle * u_sizeFactor * dist, u_sizeLimits.x * dist, u_sizeLimits.y * dist);

    // Use billboard snippet
    vec4 s_vert_pos = a_position;
    vec3 s_obj_pos = pos;
    vec3 s_cam_up = u_camUp;
    mat4 s_proj_view = u_projView;
    float s_size = quadSize;
    #include shader/snip_billboard.glsl

    gl_Position = gpos;

    v_uv = a_texCoord;

    #ifdef velocityBufferFlag
    velocityBuffer(gpos, a_particlePos, dist, vec2(1e10, 1e12), 1.0);
    #endif
}
