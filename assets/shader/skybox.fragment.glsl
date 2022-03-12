#version 330 core

// Uniforms
uniform samplerCube u_environmentCubemap;
uniform float u_opacity;

// INPUT
in vec3 v_texCoords;

// OUTPUT
out vec4 fragColor;

void main() {
    fragColor = texture(u_environmentCubemap, v_texCoords) * u_opacity;
}
