#version 330 core

// Full resolution color buffer.
uniform sampler2D u_texture0;
// Half resolution color buffer.
uniform sampler2D u_texture1;
// Full resolution depth buffer.
uniform sampler2D u_texture2;
// Half resolution depth buffer.
uniform sampler2D u_texture3;
// Z-far and K values for depth buffer.
uniform vec2 u_zFarK;

in vec2 v_texCoords;
layout (location = 0) out vec4 fragColor;

#include <shader/lib/logdepthbuff.glsl>

void main() {
    vec3 fullRes = texture(u_texture0, v_texCoords).rgb;
    vec3 halfRes = texture(u_texture1, v_texCoords).rgb;

    // Recover 'linear' depth values.
    float depthFull = 1.0 / recoverWValue(texture(u_texture2, v_texCoords).r, u_zFarK.x, u_zFarK.y);
    float depthHalf = 1.0 / recoverWValue(texture(u_texture3, v_texCoords).r, u_zFarK.x, u_zFarK.y);

    if (depthFull < depthHalf) {
        // Full-res pixel is closer - use it
        fragColor = vec4(fullRes, 1.0);
    } else if (depthFull > depthHalf) {
        // Half-res pixel is closer - use it
        fragColor = vec4(halfRes, 1.0);
    } else {
        // Depths are approximately equal - blend
        fragColor = vec4(clamp(fullRes + halfRes, 0.0, 1.0), 1.0);
    }
}