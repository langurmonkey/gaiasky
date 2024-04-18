#version 330 core

#include <shader/lib/geometry.glsl>

// UNIFORMS
uniform mat4 u_projView;
uniform vec3 u_camPos;
uniform vec3 u_camDir;
uniform float u_alpha;
uniform float u_sizeFactor;
uniform int u_cubemap;
uniform vec2 u_sizeLimits;
uniform float u_vrScale;
// Arbitrary affine transformation(s)
uniform bool u_transformFlag = false;
uniform mat4 u_transform;

// INPUT
in vec4 a_position;
in vec4 a_color;
// Additional attributes:
// x - size
// y - colmap_attribute_value
in vec2 a_additional;
in float a_textureIndex;

// OUTPUT
out vec4 v_col;
out float v_textureIndex;

#ifdef relativisticEffects
#include <shader/lib/relativity.glsl>
#endif // relativisticEffects

#ifdef gravitationalWaves
 #include <shader/lib/gravwaves.glsl>
#endif // gravitationalWaves

void main() {
    vec3 particlePos = a_position.xyz;
    if (u_transformFlag) {
        vec4 aux = u_transform * vec4(particlePos, 1.0);
        particlePos.xyz = aux.xyz;
    }
    vec3 pos = particlePos.xyz - u_camPos;

    // Distance to point - watch out, if position contains large values, this produces overflow!
    // Downscale before computing length()
    float dist = length(pos * 1e-14) * 1e14;

    float cubemapSizeFactor = 1.0;
    if (u_cubemap == 1) {
        // Cosine of angle between star position and camera direction
        // Correct point primitive size error due to perspective projection
        float cosphi = pow(dot(u_camDir, pos) / dist, 2.0);
        cubemapSizeFactor = 1.0 - cosphi * 0.65;
    }

    #ifdef relativisticEffects
        pos = computeRelativisticAberration(pos, dist, u_velDir, u_vc);
    #endif // relativisticEffects
    
    #ifdef gravitationalWaves
        pos = computeGravitationalWaves(pos, u_gw, u_gwmat3, u_ts, u_omgw, u_hterms);
    #endif // gravitationalWaves
    
    v_col = vec4(a_color.rgb, a_color.a * u_alpha);
    v_textureIndex = a_textureIndex;

    float viewAngle = a_additional.x / dist;

    vec4 gpos = u_projView * vec4(pos, 1.0);
    gl_Position = gpos;
    gl_PointSize = min(max(viewAngle * u_sizeFactor * cubemapSizeFactor, u_sizeLimits.x), u_sizeLimits.y);
}
