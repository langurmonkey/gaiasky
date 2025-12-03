#version 330 core

// Full resolution color buffer.
uniform sampler2D u_texture0;
// Half resolution color buffer.
uniform sampler2D u_texture1;
// Full resolution depth buffer.
uniform sampler2D u_texture2;
// Half resolution depth buffer.
uniform sampler2D u_texture3;

// Half accumulation buffer.
uniform sampler2D u_texture4;
// Half revealage buffer.
uniform sampler2D u_texture5;

// Z-far and K values for depth buffer.
uniform vec2 u_zFarK;

in vec2 v_texCoords;
layout(location = 0) out vec4 fragColor;

#include <shader/lib/logdepthbuff.glsl>

void main() {
    vec3 fullRes = texture(u_texture0, v_texCoords).rgb;
    vec3 halfRes = texture(u_texture1, v_texCoords).rgb;

    // WB-OIT in half buffer.
    // Read the accumulated values
    vec4 A = texture(u_texture4, v_texCoords);
    float R = texture(u_texture5, v_texCoords).r;

    vec4 fullRes4 = vec4(fullRes, 0.0);
    vec4 halfRes4 = vec4(A.rgb / max(A.a, 1.0e-5), R);

    // Recover 'linear' depth values.
    float depthFull = 1.0 / recoverWValue(texture(u_texture2, v_texCoords).r, u_zFarK.x, u_zFarK.y);
    // float depthHalf = 1.0 / recoverWValue(texture(u_texture3, v_texCoords).r, u_zFarK.x, u_zFarK.y);

    if (depthFull < 1e5) {
        fragColor = clamp(fullRes4 + halfRes4, 0.0, 1.0);
    } else {
        fragColor = clamp(fullRes4 + halfRes4, 0.0, 1.0);
    }
    fragColor = vec4(fullRes + halfRes4.rgb * 10.0, clamp(1.0 + halfRes4.a, 0.0, 1.0));

    // if (depthFull < depthHalf) {
    //     // Full-res pixel is closer - use it
    //     fragColor = vec4(fullRes, 1.0);
    // } else if (depthFull > depthHalf) {
    //     // Half-res pixel is closer - use it
    //     fragColor = vec4(halfRes, 1.0);
    // } else {
    //     // Depths are approximately equal - blend
    //     fragColor = vec4(clamp(fullRes + halfRes, 0.0, 1.0), 1.0);
    // }
}
