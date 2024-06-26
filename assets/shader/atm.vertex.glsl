#version 330 core

uniform mat4 u_projViewTrans;
uniform mat4 u_worldTrans;

in vec3 a_position;

#include <shader/lib/atmscattering.glsl>

////////////////////////////////////////////////////////////////////////////////////
//////////RELATIVISTIC EFFECTS - VERTEX
////////////////////////////////////////////////////////////////////////////////////
#ifdef relativisticEffects
#include <shader/lib/relativity.glsl>
#endif// relativisticEffects


////////////////////////////////////////////////////////////////////////////////////
//////////GRAVITATIONAL WAVES - VERTEX
////////////////////////////////////////////////////////////////////////////////////
#ifdef gravitationalWaves
#include <shader/lib/gravwaves.glsl>
#endif// gravitationalWaves

#ifdef eclipsingBodyFlag
uniform vec3 u_eclipsingBodyPos;
uniform float u_eclipsingBodyRadius;

out float v_eclipseFactor;

#include <shader/lib/math.glsl>
#endif// eclipsingBodyFlag

void main(void) {
    computeAtmosphericScattering();
    vec4 pos = u_worldTrans * vec4(a_position, 1.0);

    #ifdef relativisticEffects
    pos.xyz = computeRelativisticAberration(pos.xyz, length(pos.xyz), u_velDir, u_vc);
    #endif// relativisticEffects

    #ifdef gravitationalWaves
    pos.xyz = computeGravitationalWaves(pos.xyz, u_gw, u_gwmat3, u_ts, u_omgw, u_hterms);
    #endif// gravitationalWaves

    #ifdef eclipsingBodyFlag
    v_eclipseFactor = 1.0;
    if (fCameraHeight < fOuterRadius) {
        // Camera position.
        vec3 c = vec3(0.0, 0.0, 0.0);
        // Moon.
        vec3 m = u_eclipsingBodyPos;
        // Star.
        vec3 l = v3LightPos + v3PlanetPos;
        float dist = dist_segment_point(c, l, m);
        if (dist < u_eclipsingBodyRadius) {
            v_eclipseFactor = dist / u_eclipsingBodyRadius;
        }
    }
    #endif// eclipsingBodyFlag


    vec4 gpos = u_projViewTrans * pos;
    gl_Position = gpos;
}
