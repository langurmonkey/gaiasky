#version 330 core
// Simple mosaic implementation by Toni Sagrista

// TL
uniform sampler2D u_texture0;
// BL
uniform sampler2D u_texture1;
// TR
uniform sampler2D u_texture2;
// BR
uniform sampler2D u_texture3;

// Viewport dimensions along X and Y
uniform vec2 u_viewport;

in vec2 v_texCoords;
layout (location = 0) out vec4 fragColor;


void main(void)
{
    // Coordinates
    vec2 tc = vec2(0.0);
    if (v_texCoords.x <= 0.5) {
        // LEFT
        tc.x = v_texCoords.x * 2.0;
    } else {
        // RIGHT
        tc.x = (v_texCoords.x - 0.5) * 2.0;
    }
    if (v_texCoords.y <= 0.5) {
        // BOTTOM
        tc.y = v_texCoords.y * 2.0;
    } else {
        // TOP
        tc.y = (v_texCoords.y - 0.5) * 2.0;
    }

    // Sample
    if (v_texCoords.x <= 0.5) {
        if (v_texCoords.y <= 0.5) {
            fragColor = texture(u_texture1, tc);
        } else {
            fragColor = texture(u_texture0, tc);
        }
    } else {
        if (v_texCoords.y <= 0.5) {
            fragColor = texture(u_texture3, tc);
        } else {
            fragColor = texture(u_texture2, tc);
        }
    }

}