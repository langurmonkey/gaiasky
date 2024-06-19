// This shader generates the surface data from the elevation and moisture textures.
// Using an MRT, this generates the diffuse, specular and normal textures.
#version 330 core

#include <shader/lib/colors.glsl>
#include <shader/lib/luma.glsl>

// Height texture.
uniform sampler2D u_texture0;
// Moisture texture.
uniform sampler2D u_texture1;
// LUT.
uniform sampler2D u_texture2;

// LUT hue shift.
uniform float u_lutHueShift;

in vec2 v_texCoords;
layout (location = 0) out vec4 diffuseColor;
layout (location = 1) out vec4 specularColor;
layout (location = 2) out vec4 normalColor;


void main() {
    // Get height.
    float height = texture(u_texture0, v_texCoords).x;
    // Get moisture.
    float moisture = texture(u_texture1, v_texCoords).x;

    // Query LUT.
    vec4 rgba = texture(u_texture2, vec2(moisture, 1.0 - height));
    // Shift hue if needed.
    if (u_lutHueShift != 0.0) {
        vec3 hsv = rgb2hsv(rgba.rgb);
        hsv.x = mod(hsv.x * 360.0 + u_lutHueShift, 360.0) / 360.0;
        rgba.rgb = hsv2rgb(hsv);
    }

    // Diffuse.
    diffuseColor = rgba;

    // Specular.
    bool water = height < 0.1;
    bool snow = luma(diffuseColor.rgb) > 0.9;

    vec4 spec = vec4(0.0, 0.0, 0.0, 1.0);
    if (water) {
       spec.rgb = vec3(1.0, 1.0, 1.0);
    } else if (snow) {
        spec.rgb = vec3(0.5, 0.5, 0.5);
    }
    specularColor = spec;

    // Normal.
    float scale = 0.9;
    float dx = dFdx(height) * scale;
    float dy = dFdy(height) * scale;
    float dz = 1.0;
    vec3 normal = normalize(vec3(dx, dy, dz));
    normalColor = vec4(normal.x * 0.5 + 0.5, normal.y * 0.5 + 0.5, normal.z, 1.0);
}
