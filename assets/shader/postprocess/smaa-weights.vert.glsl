/**
 * @license
 * SMAA (Subpixel Morphological Antialiasing) - blending weights pass, vertex stage.
 * Copyright (C) 2013 Jorge Jimenez, Jose I. Echevarria, Belen Masia,
 * Fernando Navarro, Diego Gutierrez. Copyright (C) 2018 Damien Seguin.
 * Adapted for Gaia Sky (GLSL 330 core). See smaa-LICENSE.md for the full license.
 */
#version 330 core

#include <shader/lib/smaa-presets.glsl>

#ifndef SMAA_MAX_SEARCH_STEPS
#define SMAA_MAX_SEARCH_STEPS 16
#endif

in vec4 a_position;
in vec2 a_texCoord0;

uniform vec2 u_texelSize;

out vec2 v_texCoords;
out vec2 vPixCoord;
out vec4 vOffset[3];

void main() {
    v_texCoords = a_texCoord0;

    // Pixel coordinates, used to compute distances in the fragment stage.
    vPixCoord = v_texCoords / u_texelSize;

    // These offsets are used for the searches later on (see PSEUDO_GATHER4 in
    // the fragment shader).
    vOffset[0] = vec4(v_texCoords + u_texelSize * vec2(-0.25, -0.125),
                      v_texCoords + u_texelSize * vec2(1.25, -0.125));
    vOffset[1] = vec4(v_texCoords + u_texelSize * vec2(-0.125, -0.25),
                      v_texCoords + u_texelSize * vec2(-0.125, 1.25));

    // And these indicate the ends of the search loops.
    vOffset[2] = vec4(vOffset[0].x - 2.0 * float(SMAA_MAX_SEARCH_STEPS) * u_texelSize.x,
                      vOffset[0].x + 2.0 * float(SMAA_MAX_SEARCH_STEPS) * u_texelSize.x,
                      vOffset[1].y - 2.0 * float(SMAA_MAX_SEARCH_STEPS) * u_texelSize.y,
                      vOffset[1].y + 2.0 * float(SMAA_MAX_SEARCH_STEPS) * u_texelSize.y);

    gl_Position = a_position;
}
