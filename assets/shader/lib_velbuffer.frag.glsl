layout (location = 1) out vec4 velMap;
in vec2 v_vel;

void velocityBufferLen(float maxLen) {
    // Clamp length
    vec2 vel = v_vel;
    if (abs(length(vel)) > maxLen) {
        vel = normalize(vel) * maxLen;
    }
    velMap = vec4(vel.x, vel.y, 0.0, 1.0);
}

void velocityBuffer(float scl) {
    velMap = vec4(v_vel.x * scl, v_vel.y * scl, 0.0, 1.0);
}

void velocityBuffer() {
    velMap = vec4(v_vel.x, v_vel.y, 0.0, 1.0);
}
