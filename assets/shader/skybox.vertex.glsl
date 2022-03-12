#version 330 core

// Attributes
layout (location = 0) in vec3 a_position;

// Varyings
out vec3 v_texCoords;

uniform mat4 u_projViewTrans;

void main()
{
    v_texCoords = a_position;
    gl_Position = u_projViewTrans * vec4(a_position, 1.0);
}
