#version 330 core

in vec4 a_position;
in vec2 a_texCoord0;
in float a_intensity;

out vec2 v_texCoords;
out float v_intensity;

void main()
{
	v_texCoords = a_texCoord0;
	v_intensity = a_intensity;
	gl_Position = a_position;
}