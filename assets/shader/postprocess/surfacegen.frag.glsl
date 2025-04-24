// This shader generates the surface data from the elevation and moisture textures.
// Using an MRT, this generates the diffuse, specular and normal textures.
#version 330 core

#include <shader/lib/colors.glsl>
#include <shader/lib/luma.glsl>

// Biome texture (elevation in x, moisture in y).
uniform sampler2D u_texture0;
// LUT.
uniform sampler2D u_texture1;
// Emissive texture.
#ifdef emissiveMapFlag
uniform sampler2D u_texture2;
#endif // emissiveMapFlag

// LUT hue shift.
uniform float u_lutHueShift;
// LUT saturation value.
uniform float u_lutSaturation;

in vec2 v_texCoords;
layout (location = 0) out vec4 diffuseColor;
layout (location = 1) out vec4 specularColor;
#ifdef normalMapFlag
layout (location = 2) out vec4 normalColor;
#endif // normalMapFlag

float random(vec2 st) {
    return fract(sin(dot(st.xy, vec2(12.9898,78.233))) * 43758.5453123);
}

void main() {
    // Get height and moisture.
    vec4 biome = texture(u_texture0, v_texCoords);
    float height = biome.r;
    float moisture = biome.g;
    #ifdef emissiveMapFlag
    float emissive = clamp(luma(texture(u_texture2, v_texCoords).rgb) * 2.0, 0.0, 1.0);
    #endif // emissiveMapFlag

    // Query LUT.
    vec4 rgba = texture(u_texture1, vec2(moisture, 1.0 - height));
    vec4 c = rgba;
    // Manipulate hue and saturation.
    vec3 hsv = rgb2hsv(rgba.rgb);
    // Hue.
    hsv.x = mod(hsv.x * 360.0 + u_lutHueShift, 360.0) / 360.0;
    // Saturation.
    hsv.y = hsv.y * u_lutSaturation;
    #ifdef emissiveMapFlag
    hsv.y = hsv.y * (1.0 - emissive);
    hsv.z = hsv.z * mix(1.0, ((random(v_texCoords.xy * 100.0) * 0.3 + 1.4) - emissive), emissive);
    #endif // emissiveMapFlag
    // Back to RGB.
    rgba.rgb = hsv2rgb(hsv);

    // Diffuse.
    diffuseColor = rgba;

    // Specular.
    bool water = height < 0.1 || (c.b / c.r > 3.4 && c.b / c.g > 3.4);
    bool snow = luma(diffuseColor.rgb) > 0.9;

    vec4 spec = vec4(0.0, 0.0, 0.0, 1.0);
    if (water) {
        spec.rgb = vec3(1.0, 1.0, 1.0);
    } else if (snow) {
        spec.rgb = vec3(0.5, 0.5, 0.5);
    }
    specularColor = spec;

    #ifdef normalMapFlag
    // Normal.
    float scale = 4.0;
    float dx = dFdx(height) * scale;
    float dy = dFdy(height) * scale;
    float dz = 1.0;
    vec3 normal = normalize(vec3(dx, dy, dz));
    normalColor = vec4(normal.x * 0.5 + 0.5, normal.y * 0.5 + 0.5, normal.z, 1.0);
    #endif // normalMapFlag
}
