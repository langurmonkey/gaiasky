// Geometry warping and blending post-processor effect for slaves
// This implements part of the MPCDI specification to support multi-projector setups
#version 330 core

// Scene
uniform sampler2D u_texture0;
// Blend texture
uniform sampler2D u_texture1;

uniform vec2 u_viewport;
uniform int u_blend;

in vec2 v_texCoords;
layout (location = 0) out vec4 fragColor;

void main() {
    vec4 col;
    // Warp is done by vertex mesh
    col = texture(u_texture0, v_texCoords);

    if(u_blend == 1){
        // Blend
        vec4 blend = texture(u_texture1, vec2(v_texCoords.x, 1.0 - v_texCoords.y));
        col *= blend.r * blend.a;
    } else {
        // Nothing
    }

    fragColor = col;
}
