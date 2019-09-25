#define u_to_pc 3.240779289444362E-8
// VR scale factor
uniform float u_vrScale;

#ifdef velocityBufferFlag
// Projection-View matrix in previous frame
uniform mat4 u_prevProjView;
// Camera position in previous frame
uniform vec3 u_prevCamPos;
// Vector from current to previous camera position
uniform vec3 u_dCamPos;
// Velocity for next stage
out vec2 v_vel;
#endif// velocityBufferFlag

// This version accepts all parameters
void velocityBuffer(vec4 gpos, vec3 pos, float dist, vec3 pm, mat4 prevTrans, vec2 fadepc, float fadeScale){
    #ifdef velocityBufferFlag
    vec3 prevPos = pos - u_prevCamPos;
    prevPos = prevPos + pm;
    vec4 gprevpos = prevTrans * vec4(prevPos, 1.0);

    float distpc = dist * u_vrScale * u_to_pc;
    float fac = 1.0;
    if(fadeScale > 0.0){
        fac = (1.0 - smoothstep(fadepc.x, fadepc.y, distpc)) * fadeScale;
    }
    v_vel = (gpos.xy / gpos.w - gprevpos.xy / gprevpos.w) * fac;
    #endif// velocityBufferFlag
}
// This version accepts a proper motion to the position
void velocityBuffer(vec4 gpos, vec3 pos, float dist, vec3 pm){
    #ifdef velocityBufferFlag
    vec3 prevPos = pos - u_prevCamPos;
    prevPos = prevPos + pm;
    vec4 gprevpos = u_prevProjView * vec4(prevPos, 1.0);

    v_vel = (gpos.xy / gpos.w - gprevpos.xy / gprevpos.w);
    #endif// velocityBufferFlag
}

// This version accepts a proper motion to the position plus the fading parameters
void velocityBuffer(vec4 gpos, vec3 pos, float dist, vec3 pm, vec2 fadepc, float fadeScale){
    #ifdef velocityBufferFlag
    velocityBuffer(gpos, pos, dist, pm, u_prevProjView, fadepc, fadeScale);
    #endif// velocityBufferFlag
}

// This version accepts the fading parameters
void velocityBuffer(vec4 gpos, vec3 pos, float dist, vec2 fadepc, float fadeScale){
    #ifdef velocityBufferFlag
    velocityBuffer(gpos, pos, dist, vec3(0.0), fadepc, fadeScale);
    #endif// velocityBufferFlag
}

// This is the simplest version
void velocityBuffer(vec4 gpos, vec3 pos, float dist){
    #ifdef velocityBufferFlag
    velocityBuffer(gpos, pos, dist, vec3(0.0));
    #endif// velocityBufferFlag
}

// Uses the difference in camera position between frames
// This version is for vertices which have the camera
// position pre-subtracted
void velocityBufferCam(vec4 gpos, vec4 pos, float dCamPosFactor){
    #ifdef velocityBufferFlag
    vec4 prevPos = pos + vec4(u_dCamPos, 0.0) * dCamPosFactor;
    vec4 gprevpos = u_prevProjView * prevPos;
    v_vel = ((gpos.xy / gpos.w) - (gprevpos.xy / gprevpos.w));
    #endif// velocityBufferFlag
}

void velocityBufferCam(vec4 gpos, vec4 pos){
    #ifdef velocityBufferFlag
    velocityBufferCam(gpos, pos, 1.0);
    #endif// velocityBufferFlag
}
