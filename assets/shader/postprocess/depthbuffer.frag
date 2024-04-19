// Visualize the contents of the depth texture attachment
#version 330 core

// Color buffer texture
uniform sampler2D u_texture0;
// Depth buffer texture
uniform sampler2D u_texture1;
// Z-far and K values for depth buffer.
uniform vec2 u_zFarK;

in vec2 v_texCoords;
layout (location = 0) out vec4 fragColor;

#include <shader/lib/logdepthbuff.glsl>

void main() {
    // Main color.
    vec4 col = texture(u_texture0, v_texCoords);
    // Recover 'linear' depth value.
    float depth = 1.0 / recoverWValue(texture(u_texture1, v_texCoords).r, u_zFarK.x, u_zFarK.y);

    if(v_texCoords.x < 0.5){
        fragColor = col;
    } else {
        fragColor = vec4(depth, depth, depth, 1.0);
    }
}