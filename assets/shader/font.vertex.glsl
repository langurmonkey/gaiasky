#version 330 core

in vec4 a_position;
in vec4 a_color;
in vec2 a_texCoord0;

uniform mat4 u_projTrans;
uniform float u_viewAngle;
uniform float u_viewAnglePow;
uniform float u_thLabel;
uniform float u_componentAlpha;
uniform vec4 u_color;
uniform vec3 u_pos;
uniform float u_opacity;

out vec4 v_color;
out vec2 v_texCoords;
out float v_opacity;

void main()
{
   v_opacity = u_opacity * clamp((pow(u_viewAngle, u_viewAnglePow) - u_thLabel) / u_thLabel, 0.0, 0.95) * u_componentAlpha;
   v_color = u_color;
   v_texCoords = a_texCoord0;
   
   gl_Position =  u_projTrans * a_position;
}
