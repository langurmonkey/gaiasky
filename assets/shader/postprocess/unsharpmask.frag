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

#define CONTR  0.08
#define DETAILS   1.0

vec3 unsharpMask (sampler2D tex, vec2 fragCoord) {
    vec3 c11 = texture(tex, fragCoord).rgb;
    if (u_sharpenFactor == 0.0)
        return c11;
    vec3 c10 = textureOffset(tex, fragCoord, ivec2(0.3333, -1.0)).rgb;
    vec3 c01 = textureOffset(tex, fragCoord, ivec2(-1.0, -0.3333)).rgb;
    vec3 c21 = textureOffset(tex, fragCoord, ivec2(1.0, 0.3333)).rgb;
    vec3 c12 = textureOffset(tex, fragCoord, ivec2(-0.3333, 1.0)).rgb;
    vec3 b11 = (c10 + c01 + c12 + c21) * 0.25;

    float contrast = max(max(c11.r, c11.g),c11.b);
    contrast = mix(2.0 * CONTR, CONTR, contrast);

    vec3 mn1 = min(min(c10, c01), min(c12, c21)); mn1 = min(mn1, c11 * (1.0 - contrast));
    vec3 mx1 = max(max(c10, c01), max(c12, c21)); mx1 = max(mx1, c11 * (1.0 + contrast));

    vec3 dif = pow(mx1 - mn1 + 0.0001, vec3(0.75, 0.75, 0.75));
    vec3 sharpen = mix(vec3(u_sharpenFactor * DETAILS), vec3(u_sharpenFactor), dif);

    return clamp(mix(c11, b11, -sharpen), mn1, mx1);
}

void main() {
    fragColor = vec4(unsharpMask(u_texture0, v_texCoords), 1.0);
}