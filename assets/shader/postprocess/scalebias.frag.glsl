#version 330 core
// Simple lens flare implementation by Toni Sagrista

uniform sampler2D u_texture0;

uniform vec3 u_scale = vec3(1.0);
uniform vec3 u_bias = vec3(0.0);

in vec2 v_texCoords;
layout (location = 0) out vec4 fragColor;

void main() {
    fragColor = max(vec4(0.0), texture(u_texture0, v_texCoords) + vec4(uBias, 0.0)) * vec4(uScale, 1.0);
}

