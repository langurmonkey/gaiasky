#version 330 core

uniform sampler2D u_texture0;
uniform float u_intensity;
uniform float u_time;

in vec2 v_texCoords;
layout (location = 0) out vec4 fragColor;

#include <shader/lib/simple_noise.glsl>

#define saturate(x) clamp(x, 0.0, 1.0)

void main() {
    fragColor = texture(u_texture0, v_texCoords);

    // Compute grain.
    float t = mod(u_time * 0.1, 5.0);
    vec3 grain = vec3(rand(v_texCoords + t), rand(v_texCoords * 0.8 + t), rand(v_texCoords * 1.2 + t)) * u_intensity - u_intensity * 0.5;

    // Mix color with grain.
    fragColor.rgb = saturate(fragColor.rgb + grain);
}
