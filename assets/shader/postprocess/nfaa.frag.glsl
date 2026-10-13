/**
 * @license
 * Normal Filtered Anti-Aliasing (NFAA) - Optimized for Gaia Sky (GLSL 330 core)
 * Copyright Styves, Martinsh
 * Modified by Sagrista, Toni
 *
 * See http://blenderartists.org/forum/showthread.php?209574-Full-Screen-Anti-Aliasing-(NFAA-DLAA-SSAA)
 * and http://www.gamedev.net/topic/580517-nfaa---a-post-process-anti-aliasing-filter-results-implementation-details/
 */

#version 330 core

#include <shader/lib/luma.glsl>
#include <shader/lib/normal.glsl>

uniform sampler2D u_texture0;
uniform vec2 u_viewportInverse;

in vec2 v_texCoords;
layout (location = 0) out vec4 fragColor;

vec4 nfaa(sampler2D tex, vec2 texCoords, vec2 viewportInverse) {
    vec2 normal = normalMap(tex, texCoords) * viewportInverse;

    // Sample color center and 4 directional quincunx offsets
    vec4 scene0 = texture(tex, texCoords);
    vec4 scene1 = texture(tex, texCoords + normal);
    vec4 scene2 = texture(tex, texCoords - normal);
    vec4 scene3 = texture(tex, texCoords + vec2(normal.x, -normal.y) * 0.5);
    vec4 scene4 = texture(tex, texCoords - vec2(normal.x, -normal.y) * 0.5);

    // Weighted blend favoring the center pixel to prevent over-softening stars/text
    return (scene0 * 0.4) + ((scene1 + scene2 + scene3 + scene4) * 0.15);
}

void main() {
    fragColor = nfaa(u_texture0, v_texCoords, u_viewportInverse);
}
