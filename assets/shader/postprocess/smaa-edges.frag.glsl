/**
 * @license
 * SMAA (Subpixel Morphological Antialiasing) - luma edge detection pass.
 * Copyright (C) 2013 Jorge Jimenez, Jose I. Echevarria, Belen Masia,
 * Fernando Navarro, Diego Gutierrez. Copyright (C) 2018 Damien Seguin.
 * Adapted for Gaia Sky (GLSL 330 core): luma edge detection only, no
 * depth/predication, and no discard so the edge buffer needs no clearing.
 * See smaa-LICENSE.md for the full license.
 *
 * Writes: r = edge on the west (left) side, g = edge on the north (top) side.
 */
#version 330 core

#include <shader/lib/smaa-presets.glsl>

#ifndef SMAA_THRESHOLD
#define SMAA_THRESHOLD 0.1
#endif

#ifndef SMAA_LOCAL_CONTRAST_ADAPTATION_FACTOR
#define SMAA_LOCAL_CONTRAST_ADAPTATION_FACTOR 2.0
#endif

uniform sampler2D u_texture0;

in vec2 v_texCoords;
in vec4 vOffset[3];

layout(location = 0) out vec4 fragColor;

/**
 * Edge detection expects gamma-encoded color: sampling an sRGB texture
 * decodes it to linear, so it has to be re-encoded before thresholding.
 */
vec3 smaaSampleColor(sampler2D tex, vec2 texcoord) {
    vec3 color = texture(tex, texcoord).rgb;
#ifdef SMAA_SRGB_INPUT
    return mix(color * 12.92,
               1.055 * pow(color, vec3(1.0 / 2.4)) - 0.055,
               step(vec3(0.0031308), color));
#else
    return color;
#endif
}

vec2 smaaLumaEdgeDetection(sampler2D colorTex) {
    vec2 threshold = vec2(SMAA_THRESHOLD);

    // Calculate lumas:
    const vec3 weights = vec3(0.2126, 0.7152, 0.0722);
    float L = dot(smaaSampleColor(colorTex, v_texCoords), weights);
    float Lleft = dot(smaaSampleColor(colorTex, vOffset[0].xy), weights);
    float Ltop = dot(smaaSampleColor(colorTex, vOffset[0].zw), weights);

    // We do the usual threshold:
    vec4 delta;
    delta.xy = abs(L - vec2(Lleft, Ltop));
    vec2 edges = step(threshold, delta.xy);

    // No edge: return zeros (the original discards here).
    if (dot(edges, vec2(1.0, 1.0)) == 0.0)
        return vec2(0.0);

    // Calculate right and bottom deltas:
    float Lright = dot(smaaSampleColor(colorTex, vOffset[1].xy), weights);
    float Lbottom = dot(smaaSampleColor(colorTex, vOffset[1].zw), weights);
    delta.zw = abs(L - vec2(Lright, Lbottom));

    // Calculate the maximum delta in the direct neighborhood:
    vec2 maxDelta = max(delta.xy, delta.zw);

    // Calculate left-left and top-top deltas:
    float Lleftleft = dot(smaaSampleColor(colorTex, vOffset[2].xy), weights);
    float Ltoptop = dot(smaaSampleColor(colorTex, vOffset[2].zw), weights);
    delta.zw = abs(vec2(Lleft, Ltop) - vec2(Lleftleft, Ltoptop));

    // Calculate the final maximum delta:
    maxDelta = max(maxDelta.xy, delta.zw);
    float finalDelta = max(maxDelta.x, maxDelta.y);

    // Local contrast adaptation:
    edges.xy *= step(finalDelta, SMAA_LOCAL_CONTRAST_ADAPTATION_FACTOR * delta.xy);

    return edges;
}

void main() {
    fragColor = vec4(smaaLumaEdgeDetection(u_texture0), 0.0, 1.0);
}
