#version 330 core

uniform sampler2D u_texture0;
uniform float u_aberrationAmount = 0.05;

in vec2 v_texCoords;
layout (location = 0) out vec4 fragColor;

void main() {
    vec2 uv = v_texCoords.xy;
    vec2 distFromCenter = uv - 0.5;

    // stronger aberration near the edges by raising to power 3.
    vec2 aberrated = u_aberrationAmount * pow(distFromCenter, vec2(3.0, 3.0));

    fragColor = vec4(texture(u_texture0, uv - aberrated).r, texture(u_texture0, uv).g, texture(u_texture0, uv + aberrated).b, 1.0);
}