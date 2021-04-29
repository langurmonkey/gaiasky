#version 330 core

/*
LumaSharpen 1.4.1
original hlsl by Christian Cann Schuldt Jensen
port to glsl by Anon
It blurs the original pixel with the surrounding pixels and then subtracts this blur to sharpen the image.
It does this in luma to avoid color artifacts and allows limiting the maximum sharpning to avoid or lessen halo artifacts.
This is similar to using Unsharp Mask in Photoshop.
*/

uniform sampler2D u_texture0;
uniform float u_sharpenFactor;

in vec2 v_texCoords;
layout (location = 0) out vec4 fragColor;


vec4 unsharpMask (sampler2D tex, vec2 fragCoord) {
    vec4 c = texture(tex, fragCoord);
    if (u_sharpenFactor == 0.0)
        return c;
    vec4 n = textureOffset(tex, fragCoord, ivec2(0, -1));
    vec4 w = textureOffset(tex, fragCoord, ivec2(-1, 0));
    vec4 e = textureOffset(tex, fragCoord, ivec2(1, 0));
    vec4 s = textureOffset(tex, fragCoord, ivec2(0, 1));

    // Return edge detection
    return clamp((1.0 + 4.0 * u_sharpenFactor) * c - u_sharpenFactor * (n + w + e + s), 0.0, 1.0);
}

void main() {
    fragColor = unsharpMask(u_texture0, v_texCoords);
}