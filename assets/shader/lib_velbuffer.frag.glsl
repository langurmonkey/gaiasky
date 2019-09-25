#ifdef velocityBufferFlag
in vec2 v_vel;
layout (location = 1) out vec4 velMap;
#endif


void velocityBufferLen(float maxLen){
    #ifdef velocityBufferFlag
    // Clamp length
    vec2 vel = v_vel;
    if(abs(length(vel)) > maxLen){
        vel = normalize(vel) * maxLen;
    }
    velMap = vec4(vel.x, vel.y, 0.0, 1.0);
    #endif
}

void velocityBuffer(float scl){
    #ifdef velocityBufferFlag
    velMap = vec4(v_vel.x * scl, v_vel.y * scl, 0.0, 1.0);
    #endif
}

void velocityBuffer(){
    #ifdef velocityBufferFlag
    velMap = vec4(v_vel.x, v_vel.y, 0.0, 1.0);
    #endif
}
