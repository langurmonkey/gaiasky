#version 330 core

/*
 * Computes the luminance for each pixel
 */
uniform sampler2D u_texture0;
uniform vec2 u_texelSize;
uniform vec2 u_imageSize;
uniform float u_lodLevel;

in vec2 v_texCoords;
layout (location = 0) out vec4 fragColor;

float luminance(vec3 color){
    return dot(color, vec3(0.2126, 0.7152, 0.0722));
}

vec3 bilinearFilter (vec2 texCoords, float lodLevel){
    vec2 fUv = fract(texCoords * u_imageSize);
    texCoords = floor(texCoords * u_imageSize) / u_imageSize;

    vec3 tl = textureLod(u_texture0, texCoords, lodLevel).rgb;
    vec3 tr = textureLod(u_texture0, texCoords + vec2(u_texelSize.x, 0.0), lodLevel).rgb;
    vec3 bl = textureLod(u_texture0, texCoords + vec2(0.0, u_texelSize.y), lodLevel).rgb;
    vec3 br = textureLod(u_texture0, texCoords + vec2(u_texelSize.x, u_texelSize.y), lodLevel).rgb;

    vec3 a = mix(tl, tr, fUv.x);
    vec3 b = mix(bl, br, fUv.x);
    return mix(a, b, fUv.y);
}

vec3 maxFilter (vec2 texCoords, float lodLevel){
    // Readjust the UV to map on the point
    texCoords = floor(texCoords * u_imageSize) / u_imageSize;

    // Get rgbe values
    vec3 tl = textureLod(u_texture0, texCoords, lodLevel).rgb;
    vec3 tr = textureLod(u_texture0, texCoords + vec2(u_texelSize.x, 0.0), lodLevel).rgb;
    vec3 bl = textureLod(u_texture0, texCoords + vec2(0.0, u_texelSize.y), lodLevel).rgb;
    vec3 br = textureLod(u_texture0, texCoords + vec2(u_texelSize.x, u_texelSize.y), lodLevel).rgb;

    // Get luminance
    float ltl = luminance(tl);
    float ltr = luminance(tr);
    float lbl = luminance(bl);
    float lbr = luminance(br);

    // Compare luminance and return the brightest one
    float maxLuminance = max(max(max(ltl, ltr), lbl), lbr);
    if (ltl == maxLuminance)
        return tl;
    else if (ltr == maxLuminance)
        return tr;
    else if (lbl == maxLuminance)
        return bl;
    else
        return br;
}

void main() {
    #ifdef LUMA
    vec3 pixelColor = texture(u_texture0, v_texCoords).rgb;
    fragColor = vec4(vec3((luminance(pixelColor))), 1.0);
    #endif //LUMA

    #ifdef AVERAGE
    fragColor = vec4(bilinearFilter(v_texCoords, u_lodLevel), 1.0);
    #endif //AVERAGE

    #ifdef MAX
    fragColor = vec4(maxFilter(v_texCoords, u_lodLevel), 1.0);
    #endif //MAX
}