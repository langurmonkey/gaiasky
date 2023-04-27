#version 330 core

uniform sampler2D u_texture0;
uniform float u_aberrationAmount = 0.05;

in vec2 v_texCoords;
layout (location = 0) out vec4 fragColor;

void main() {
    vec2 uv = v_texCoords.xy;
    // Vector from center to the pixel.
    vec2 vec_center_pixel = uv - 0.5;

    // Aberration is stronger near the edges, so we apply a power function.
    vec2 aberrated = u_aberrationAmount * pow(length(vec_center_pixel), 3.0) * sign(vec_center_pixel);

    fragColor = vec4(texture(u_texture0, uv - aberrated).r, texture(u_texture0, uv).g, texture(u_texture0, uv + aberrated).b, 1.0);
}