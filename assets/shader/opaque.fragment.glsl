#version 330 core

// OUTPUT
layout (location = 0) out vec4 fragColor;

// Renders all black for the occlusion testing
void main() {
    fragColor = vec4(0.0, 0.0, 0.0, 1.0);
}
