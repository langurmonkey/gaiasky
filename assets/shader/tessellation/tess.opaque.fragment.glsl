#version 410 core

in vec2 o_texCoords;

// Varyings computed in the vertex shader
in float o_opacity;

// Logarithmic depth
in float o_depth;


out vec4 fragColor;

void main() {
    fragColor = vec4(0.0, 0.0, 0.0, 1.0);
    // Logarithmic depth buffer
    gl_FragDepth = o_depth;
}

