#version 330 core

#include <shader/lib/geometry.glsl>

uniform float u_pointAlphaMin;
uniform float u_pointAlphaMax;
uniform float u_starBrightness;
uniform mat4 u_projView;
uniform vec3 u_camPos;
uniform vec3 u_camUp;
uniform float u_sizeFactor;
uniform float u_intensity;
uniform vec2 u_edges;
uniform float u_maxPointSize;
uniform float u_vrScale;

// Arbitrary affine transformation(s)
uniform bool u_transformFlag = false;
uniform mat4 u_transform;
// View matrix
uniform mat4 u_view;

#ifdef relativisticEffects
    #include <shader/lib/relativity.glsl>
#endif // relativisticEffects

#ifdef gravitationalWaves
    #include <shader/lib/gravwaves.glsl>
#endif // gravitationalWaves

// INPUT
in vec4 a_position;
in vec4 a_color;
in vec2 a_texCoord;
in vec3 a_particlePos;
// x - size, y - type, z - layer
in vec3 a_additional;

// OUTPUT
out vec4 v_col;
out vec2 v_uv;
// 0 - dust
// 1 - star
// 2 - bulge
// 3 - gas
// 4 - hii
flat out int v_type;
// Layer in the texture array
flat out int v_layer;

#ifdef velocityBufferFlag
#include <shader/lib/velbuffer.vert.glsl>
#endif // velocityBufferFlag

void main() {
    vec3 particlePos = a_particlePos;
    if (u_transformFlag) {
        vec4 aux = u_transform * vec4(particlePos, 1.0);
        particlePos.xyz = aux.xyz;
    }

    vec3 pos = (particlePos - u_camPos) / u_vrScale;
    float dist = length(pos * 1e-8) * 1e8;

    #ifdef relativisticEffects
        pos = computeRelativisticAberration(pos, dist, u_velDir, u_vc);
    #endif // relativisticEffects
    
    #ifdef gravitationalWaves
        pos = computeGravitationalWaves(pos, u_gw, u_gwmat3, u_ts, u_omgw, u_hterms);
    #endif // gravitationalWaves
    

    float dscale = smoothstep(u_edges.y, u_edges.x, dist);

    v_col = vec4(a_color.rgb, a_color.a * u_intensity * dscale);
    v_type = int(a_additional.y);
    v_layer = int(a_additional.z);
    v_uv = a_texCoord;

    float quadSize = min(a_additional.x * u_sizeFactor, u_maxPointSize * dist);

    // Use billboard snippet
    vec4 s_vert_pos = a_position;
    vec3 s_obj_pos = pos;
    mat4 s_proj_view = u_projView;
    float s_size = quadSize;
    #include <shader/snippet/billboard.glsl>

    gl_Position = gpos * u_vrScale;

    #ifdef velocityBufferFlag
    velocityBufferBillboard(gpos, pos, s_size, a_position, s_quat, s_quat_conj);
    #endif

}
