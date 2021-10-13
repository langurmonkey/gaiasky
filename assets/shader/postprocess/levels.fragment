#version 330 core

/*
 * This shader contains levels for brightness, contrast, saturation, hue.
 * Additionally, it provides gamma correction and HDR tone mapping
 */

uniform sampler2D u_texture0;

uniform float u_brightness = 0.0;
uniform float u_contrast = 1.0;
uniform float u_saturation = 1.0;
uniform float u_hue = 1.0;
uniform float u_gamma = 2.2;

#ifdef toneMappingExposure
uniform float u_exposure = 2.5;
#endif //toneMappingExposure

#ifdef toneMappingAuto
uniform float u_avgLuma, u_maxLuma;

float luminance(vec3 color){
    return dot(color, vec3(0.2126, 0.7152, 0.0722));
}

/// <summary>
/// Convert an sRGB pixel into a CIE xyY (xy = chroma, Y = luminance).
/// <summary>
vec3 RGB2xyY (vec3 rgb)
{
    const mat3 RGB2XYZ = mat3(0.4124, 0.3576, 0.1805,
    0.2126, 0.7152, 0.0722,
    0.0193, 0.1192, 0.9505);
    vec3 XYZ = RGB2XYZ * rgb;

    // XYZ to xyY
    return vec3(XYZ.x / (XYZ.x + XYZ.y + XYZ.z),
    XYZ.y / (XYZ.x + XYZ.y + XYZ.z),
    XYZ.y);
}

/// <summary>
/// Convert a CIE xyY value into sRGB.
/// <summary>
vec3 xyY2RGB (vec3 xyY)
{
    // xyY to XYZ
    vec3 XYZ = vec3((xyY.z / xyY.y) * xyY.x,
    xyY.z,
    (xyY.z / xyY.y) * (1.0 - xyY.x - xyY.y));

    const mat3 XYZ2RGB = mat3(3.2406, -1.5372, -0.4986,
    -0.9689, 1.8758, 0.0415,
    0.0557, -0.2040, 1.0570);

    return XYZ2RGB * XYZ;
}

// Reinhard tone mapping using average and maximum luminosity of previous frame
vec3 reinhardToneMapping(vec3 pixelColor, float scale){
    float lwhite = u_maxLuma * u_maxLuma;
    float luma = luminance(pixelColor);

    float L = (scale / u_avgLuma) * luma;
    float Ld = (L * (1.0 + L / lwhite)) / (1.0 + L);

    // Ld is in luminance space, so apply the scale factor to the xyY converted
    // values from RGB space, then convert back from xyY to RGB.
    vec3 xyY = RGB2xyY(pixelColor);
    xyY.z *= Ld;
    return xyY2RGB(xyY);
}

// Automatic exposure compensation
vec3 autoExposureToneMapping(vec3 pixelColor){
    float exposure = clamp(0.4 / u_avgLuma, 0.1, 1000.0);
    return vec3(1.0 - exp2(-pixelColor * exposure));
}

#endif //toneMappingAuto

#ifdef toneMappingACES
vec3 ACESToneMapping(vec3 pixelColor){
    const float a = 2.51f;
    const float b = 0.03f;
    const float c = 2.43f;
    const float d = 0.59f;
    const float e = 0.14f;
    return clamp((pixelColor * (a * pixelColor + b))/(pixelColor * (c * pixelColor + d) + e), 0.0, 1.0);
}
#endif //toneMappingACES

#ifdef toneMappingFilmic
vec3 filmicToneMapping(vec3 pixelColor){
    pixelColor = max(vec3(0.0f), pixelColor - vec3(0.004f));
    return (pixelColor * (6.2f * pixelColor + 0.5f)) / (pixelColor * (6.2f * pixelColor + 1.7f) + 0.06f);
}
#endif //toneMappingFilmic

#ifdef toneMappingUncharted
vec3 unchartedToneMapping(vec3 pixelColor){
    const float A = 0.15f;
    const float B = 0.50f;
    const float C = 0.10f;
    const float D = 0.20f;
    const float E = 0.02f;
    const float F = 0.30f;
    const float W = 11.2f;
    return ((pixelColor * (A * pixelColor + C * B) + D * E) / (pixelColor * ( A * pixelColor + B) + D * F)) - E / F;
}
#endif //toneMappingUncharted

in vec2 v_texCoords;
layout (location = 0) out vec4 fragColor;


vec3 rgb2hsv(vec3 c)
{
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));

    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}

vec3 hsv2rgb(vec3 c)
{
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}


void main() {
    vec3 pixelColor = texture(u_texture0, v_texCoords).rgb;

    // Apply contrast
    pixelColor = ((pixelColor - 0.5) * max(u_contrast, 0.0)) + 0.5;

    vec3 hsv = rgb2hsv(pixelColor);
    // Apply saturation
    hsv.y *= u_saturation;
    // Apply hue
    hsv.x *= u_hue;

    pixelColor = hsv2rgb(hsv);

    // Apply brightness
    pixelColor += u_brightness;

    // HDR tone mapping
    #ifdef toneMappingAuto
    pixelColor = reinhardToneMapping(pixelColor, 0.2);
    //pixelColor = autoExposureToneMapping(pixelColor);
    #endif //toneMappingAuto

    #ifdef toneMappingExposure
    pixelColor = 1.0 - exp2(-pixelColor * u_exposure);
    #endif //toneMappingExposure

    #ifdef toneMappingACES
    pixelColor = ACESToneMapping(pixelColor);
    #endif //toneMappingACES

    #ifdef toneMappingFilmic
    pixelColor = filmicToneMapping(pixelColor);
    #endif //toneMappingFilmic

    #ifdef toneMappingUncharted
    pixelColor = unchartedToneMapping(pixelColor);
    #endif //toneMappingUncharted


    // Gamma correction
    pixelColor = pow(pixelColor, vec3(1.0 / u_gamma));

    // Final color
    fragColor = vec4(pixelColor, 1.0);

    // Mark edges of screen in yellow
    //if(v_texCoords.x > 0.999 || v_texCoords.x < 0.001 || v_texCoords.y > 0.999 || v_texCoords.y < 0.001) {
    //    fragColor = vec4(1.0, 1.0, 0.0, 1.0);
    //}

    // Saturation in red
    //if(brightness(fragColor) > 1.0){
    //    fragColor = vec4(1, 0, 0, 1);
    //}
}