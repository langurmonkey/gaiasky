#version 330 core

// Diffuse color
uniform vec4 u_diffuseColor;
// Line width setting.
uniform float u_generic1;
// Fov factor.
uniform float u_generic2;

// VARYINGS

// Coordinate of the texture
in vec2 v_texCoords0;
// Opacity
in float v_opacity;
// Color
in vec4 v_color;

layout (location = 0) out vec4 fragColor;

#ifdef ssrFlag
#include <shader/lib/ssr.frag.glsl>
#endif // ssrFlag

#define PI 3.141592
#define N 10.0
#define BASE_LINE_WIDTH 5.0

vec4 circle(vec2 tc) {
    // Line width.
    float lw = 1.0 - abs(dFdx(tc.x)) * BASE_LINE_WIDTH * u_generic1 * u_generic2;

    // in [-1..1]
    tc = (tc - 0.5) * 2.0;
    float alpha = 1.3 - length(tc);
    
    if(length(tc) > 1.01){
        discard;
    }
    
    vec2 coord = tc * (N - 1.0) * 2.0;
    float dist = length(coord);
    
    // the grid in itself
    float func = cos(PI*dist);

    vec4 result = u_diffuseColor * smoothstep(lw, 1.0, func);
    result.a *= alpha;
    return result;
}

vec4 square(vec2 tc) {

    float fov = u_generic2;
    float lod = 1.0;
    if (fov < 0.05) {
        lod = 16.0;
    } else if (fov < 0.1) {
       lod = 8.0;
    } else if (fov < 0.5) {
        lod = 4.0;
    } else if (fov < 0.75) {
        lod = 2.0;
    }

    vec2 coord = tc;
    coord.x *= 36.0 * 2.0;
    coord.y *= 18.0 * 2.0;
    coord *= lod;

    // Line width.
    float lw = 1.0 - abs(dFdx(tc.x)) * BASE_LINE_WIDTH * u_generic1 * lod;

    // Normalize in 1..0..1
    vec2 norm = abs((tc - 0.5) * 2.0);
    
    float highlight = (smoothstep(0.001, 0.0, norm.x) + smoothstep(lw, 1.0, norm.x) + smoothstep(0.001, 0.0, norm.y)) * 0.35;
    
    coord = cos(PI * coord);
    vec4 result = u_diffuseColor * smoothstep(lw, 1.0, max(coord.x, coord.y));
    result = clamp(result + highlight, 0.0, 1.0);
    result.a *= pow(1.0 - abs((v_texCoords0.y * 2.0) - 1.0), 0.25) * v_opacity;
    return result;
}

void main() {
    fragColor = square(v_texCoords0);

    #ifdef ssrFlag
    ssrBuffers();
    #endif // ssrFlag
}