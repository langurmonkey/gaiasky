/**
 * @license
 * SMAA (Subpixel Morphological Antialiasing) - neighbourhood blending pass, vertex stage.
 * Copyright (C) 2013 Jorge Jimenez, Jose I. Echevarria, Belen Masia,
 * Fernando Navarro, Diego Gutierrez. Copyright (C) 2018 Damien Seguin.
 * Adapted for Gaia Sky (GLSL 330 core). See smaa-LICENSE.md for the full license.
 */
#version 330 core

in vec4 a_position;
in vec2 a_texCoord0;

uniform vec2 u_texelSize;

out vec2 v_texCoords;
out vec4 vOffset;

void main() {
    v_texCoords = a_texCoord0;
    // Right and top neighbours.
    vOffset = vec4(v_texCoords + vec2(u_texelSize.x, 0.0),
                   v_texCoords + vec2(0.0, u_texelSize.y));

    gl_Position = a_position;
}
