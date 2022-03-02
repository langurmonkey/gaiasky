// Visualize the contents of the depth texture attachment
#version 330 core

// Color buffer texture
uniform sampler2D u_texture0;
// Depth buffer texture
uniform sampler2D u_texture1;

in vec2 v_texCoords;
layout (location = 0) out vec4 fragColor;

void main() {
    vec4 col = texture(u_texture0, v_texCoords);
    vec4 depth = texture(u_texture1, v_texCoords);

    if(v_texCoords.x < 0.5){
        fragColor = col;
    } else {
        float v = depth.x;
        fragColor = vec4(v, v, v, 1.0);
    }
}