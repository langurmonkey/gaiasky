#version 330 core

in vec4 a_position;
in vec4 a_color;
in vec2 a_texCoord0;

uniform mat4 u_projView;
uniform float u_viewAngle;
uniform float u_viewAnglePow;
uniform float u_thOverFactor;
uniform float u_thOverFactorScl;
uniform float u_componentAlpha;
uniform vec4 u_color;
uniform vec3 u_pos;

out vec4 v_color;
out vec2 v_texCoords;
out float v_opacity;

void main()
{
   float thOverFac = u_thOverFactor * u_thOverFactorScl;
   v_opacity = clamp((pow(u_viewAngle, u_viewAnglePow) - thOverFac) / thOverFac, 0.0, 0.95) * u_componentAlpha;
   v_color = u_color;
   v_texCoords = a_texCoord0;
   
   gl_Position =  u_projView * a_position;
}
