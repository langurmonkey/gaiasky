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
    vec2 tc1 = v_texCoords.xy;
    vec2 tc2 = v_texCoords.yx;
    float t = u_time * 0.001;
    vec3 grain = vec3(rand(tc1 + t), rand(tc2 + t), rand(-tc1 + t)) * u_intensity - u_intensity * 0.5;

    // Mix color with grain.
    fragColor.rgb = saturate(fragColor.rgb + grain);
}
