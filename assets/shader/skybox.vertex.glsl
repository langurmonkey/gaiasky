#version 330 core

// Attributes
in vec3 a_position;

// Varyings
out vec3 v_texCoords;

// Uniforms
uniform mat4 u_worldTrans;
uniform mat4 u_projViewTrans;

void main() {
    vec4 pos = u_worldTrans * vec4(a_position, 1.0);
    v_texCoords = pos.xyz;
    gl_Position = u_projViewTrans * pos;
}
