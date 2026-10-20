/**
 * @license
 * SMAA (Subpixel Morphological Antialiasing) - neighbourhood blending pass.
 * Copyright (C) 2013 Jorge Jimenez, Jose I. Echevarria, Belen Masia,
 * Fernando Navarro, Diego Gutierrez. Copyright (C) 2018 Damien Seguin.
 * Adapted for Gaia Sky (GLSL 330 core): no temporal reprojection.
 * See smaa-LICENSE.md for the full license.
 */
#version 330 core

uniform sampler2D u_texture0; // Color
uniform sampler2D u_texture1; // Blending weights
uniform vec2 u_texelSize;

in vec2 v_texCoords;
in vec4 vOffset;

layout(location = 0) out vec4 fragColor;

#define mad(a, b, c) (a * b + c)

#define uColorTexture u_texture0
#define uBlendTexture u_texture1

/**
 * Conditional move:
 */
void SMAAMovc(bvec2 cond, inout vec2 variable, vec2 value) {
    if (cond.x) variable.x = value.x;
    if (cond.y) variable.y = value.y;
}

void SMAAMovc(bvec4 cond, inout vec4 variable, vec4 value) {
    SMAAMovc(cond.xy, variable.xy, value.xy);
    SMAAMovc(cond.zw, variable.zw, value.zw);
}

void main() {
    // Fetch the blending weights for current pixel:
    vec4 a;
    a.x = texture(uBlendTexture, vOffset.xy).a; // Right
    a.y = texture(uBlendTexture, vOffset.zw).g; // Top
    a.wz = texture(uBlendTexture, v_texCoords).xz; // Bottom / Left

    // Is there any blending weight with a value greater than 0.0?
    if (dot(a, vec4(1.0, 1.0, 1.0, 1.0)) < 1e-5) {
        fragColor = texture(uColorTexture, v_texCoords);
    } else {
        bool h = max(a.x, a.z) > max(a.y, a.w); // max(horizontal) > max(vertical)

        // Calculate the blending offsets:
        vec4 blendingOffset = vec4(0.0, a.y, 0.0, a.w);
        vec2 blendingWeight = a.yw;
        SMAAMovc(bvec4(h, h, h, h), blendingOffset, vec4(a.x, 0.0, a.z, 0.0));
        SMAAMovc(bvec2(h, h), blendingWeight, a.xz);
        blendingWeight /= dot(blendingWeight, vec2(1.0, 1.0));

        // Calculate the texture coordinates:
        vec4 blendingCoord = mad(blendingOffset, vec4(u_texelSize, -u_texelSize), v_texCoords.xyxy);

        // We exploit bilinear filtering to mix current pixel with the chosen
        // neighbor:
        vec4 color = blendingWeight.x * texture(uColorTexture, blendingCoord.xy);
        color += blendingWeight.y * texture(uColorTexture, blendingCoord.zw);

        fragColor = color;
    }
}
