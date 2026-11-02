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
layout(location = 0) out vec4 fragColor;

#include <shader/lib/logdepthbuff.glsl>

void main() {
    vec3 fullRes = texture(u_texture0, v_texCoords).rgb;
    vec3 halfRes = texture(u_texture1, v_texCoords).rgb;

    // Blend using depth buffers.

    // Recover 'linear' depth values.
    float depthFull = 1.0 / recoverWValue(texture(u_texture2, v_texCoords).r, u_zFarK.x, u_zFarK.y);
    float depthHalf = 1.0 / recoverWValue(texture(u_texture3, v_texCoords).r, u_zFarK.x, u_zFarK.y);

    // The atmosphere writes a constant depth at the radius of its shell for the motion blur, which is
    // not geometry. The half-resolution pass runs before it, so depthHalf holds real geometry only:
    // where the full-resolution depth is close but the half-resolution one is not, the only thing in
    // front is an atmosphere, and the half-resolution content behind it belongs in the result.
    if (depthFull < 1e5 && depthHalf < 1e5) {
        fragColor = vec4(fullRes, 1.0);
    } else {
        fragColor = vec4(clamp(fullRes + halfRes, 0.0, 1.0), 1.0);
    }
}
