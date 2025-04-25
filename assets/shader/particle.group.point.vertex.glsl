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
uniform float u_proximityThreshold;
// App run time in seconds.
uniform float u_appTime;
// Shading style: 0: default, 1: twinkle.
uniform int u_shadingStyle;
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

#include <shader/lib/goldennoise.glsl>

void main() {
    vec3 particlePos = a_position.xyz;
    if (u_transformFlag) {
        vec4 aux = u_transform * vec4(particlePos, 1.0);
        particlePos.xyz = aux.xyz;
    }
    // Only apply VR scale to far away positions to prevent overflow in VR mode.
    float d = dot(particlePos, particlePos);
    float vrScale;
    if (d < 1.0e30) {
        vrScale = 1.0;
    } else {
        vrScale = u_vrScale;
    }

    vec3 pos = (particlePos.xyz - u_camPos) / vrScale;

    // Distance to point - watch out, if position contains large values, this produces overflow!
    // Downscale before computing length()
    float dist = length(pos * 1e-14) * 1e14;
    float solidAngle = a_additional.x / dist;

    float cubemapSizeFactor = 1.0;
    if (u_cubemap == 1) {
        // Cosine of angle between star position and camera direction
        // Correct point primitive size error due to perspective projection
        float cosphi = pow(dot(u_camDir, pos) / dist, 2.0);
        cubemapSizeFactor = 1.0 - cosphi * 0.65;
    }

    // Shading style
    float shadingStyleFactor = 1.0;
    if (u_shadingStyle == 1) {
        float noise = abs(gold_noise(vec2(float(gl_VertexID)), 2334.943));
        //float reflectionFactor = (1.0 + dot(normalize(pos), normalize(particlePos / vrScale))) * 0.5;
        shadingStyleFactor = clamp(pow(
                    (1.0 + sin(mod(u_appTime + noise * 6.0, 3.141597))) / 2.0, 5.0), 0.0, 1.0);
    }

    // Proximity.
    float fadeFactor = shadingStyleFactor;
    if (u_proximityThreshold > 0.0) {
        fadeFactor = smoothstep(u_proximityThreshold * 1.5, u_proximityThreshold * 0.5, solidAngle);
    }

    #ifdef relativisticEffects
    pos = computeRelativisticAberration(pos, dist, u_velDir, u_vc);
    #endif // relativisticEffects

    #ifdef gravitationalWaves
    pos = computeGravitationalWaves(pos, u_gw, u_gwmat3, u_ts, u_omgw, u_hterms);
    #endif // gravitationalWaves

    v_col = vec4(a_color.rgb, a_color.a * u_alpha * fadeFactor);
    v_textureIndex = a_textureIndex;

    vec4 gpos = u_projView * vec4(pos, 1.0);
    gl_Position = gpos * vrScale;
    gl_PointSize = min(max(solidAngle * u_sizeFactor * cubemapSizeFactor, u_sizeLimits.x), u_sizeLimits.y);
}
