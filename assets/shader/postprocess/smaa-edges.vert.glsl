/**
 * @license
 * SMAA (Subpixel Morphological Antialiasing) - edge detection pass, vertex stage.
 * Copyright (C) 2013 Jorge Jimenez, Jose I. Echevarria, Belen Masia,
 * Fernando Navarro, Diego Gutierrez. Copyright (C) 2018 Damien Seguin.
 * Adapted for Gaia Sky (GLSL 330 core). See smaa-LICENSE.md for the full license.
 */
#version 330 core

in vec4 a_position;
in vec2 a_texCoord0;

uniform vec2 u_texelSize;

out vec2 v_texCoords;
out vec4 vOffset[3];

void main() {
    v_texCoords = a_texCoord0;

    // Left, top, right, bottom, left-left and top-top neighbours.
    vOffset[0] = vec4(v_texCoords + u_texelSize * vec2(-1.0, 0.0),
                      v_texCoords + u_texelSize * vec2(0.0, -1.0));
    vOffset[1] = vec4(v_texCoords + u_texelSize * vec2(1.0, 0.0),
                      v_texCoords + u_texelSize * vec2(0.0, 1.0));
    vOffset[2] = vec4(v_texCoords + u_texelSize * vec2(-2.0, 0.0),
                      v_texCoords + u_texelSize * vec2(0.0, -2.0));

    gl_Position = a_position;
}
