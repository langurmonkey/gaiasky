#define u_to_pc 3.240779289444362E-8

// Projection-View matrix in previous frame
uniform mat4 u_prevProjView;
// Camera position in previous frame
uniform vec3 u_prevCamPos;
// Vector from current to previous camera position
uniform vec3 u_dCamPos;
// VR scale factor
uniform float u_vrScale;

out vec2 v_vel;

// This version accepts all parameters
void velocityBuffer(vec4 gpos, float dist, vec3 pm, mat4 prevTrans, vec2 fadepc, float fadeScale){
    vec3 prevPos = a_position.xyz - u_prevCamPos;
    prevPos = prevPos + pm;
    vec4 gprevpos = prevTrans * vec4(prevPos, 1.0);

    float distpc = dist * u_vrScale * u_to_pc;
    float fac = 1.0;
    if(fadeScale > 0.0){
        fac = (1.0 - smoothstep(fadepc.x, fadepc.y, distpc)) * fadeScale;
    }
    v_vel = (gpos.xy / gpos.w - gprevpos.xy / gprevpos.w) * fac;
}
// This version accepts a proper motion to the position
void velocityBuffer(vec4 gpos, float dist, vec3 pm){
    vec3 prevPos = a_position.xyz - u_prevCamPos;
    prevPos = prevPos + pm;
    vec4 gprevpos = u_prevProjView * vec4(prevPos, 1.0);

    v_vel = (gpos.xy / gpos.w - gprevpos.xy / gprevpos.w);
}

// This version accepts a proper motion to the position plus the fading parameters
void velocityBuffer(vec4 gpos, float dist, vec3 pm, vec2 fadepc, float fadeScale){
    velocityBuffer(gpos, dist, pm, u_prevProjView, fadepc, fadeScale);
}

// This version accepts the fading parameters
void velocityBuffer(vec4 gpos, float dist, vec2 fadepc, float fadeScale){
    velocityBuffer(gpos, dist, vec3(0.0), fadepc, fadeScale);
}

// This is the simplest version
void velocityBuffer(vec4 gpos, float dist){
    velocityBuffer(gpos, dist, vec3(0.0));
}
