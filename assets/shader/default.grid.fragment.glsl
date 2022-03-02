#version 330 core

// Diffuse color
uniform vec4 u_diffuseColor;

// VARYINGS

// Coordinate of the texture
in vec2 v_texCoords0;
// Opacity
in float v_opacity;
// Color
in vec4 v_color;

layout (location = 0) out vec4 fragColor;

#ifdef ssrFlag
#include shader/lib_ssr.frag.glsl
#endif // ssrFlag

#define PI 3.141592
#define N 10.0

vec4 circle(vec2 tc) {
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

    vec4 result = u_diffuseColor * smoothstep(0.999, 1.0, func);
    result.a *= alpha;
    return result;
}

vec4 square(vec2 tc) {
    vec2 coord = tc;
    coord.x *= 36.0 * 2.0;
    coord.y *= 18.0 * 2.0;
    
    // Normalize in 1..0..1
    vec2 norm = abs((tc - 0.5) * 2.0);
    
    float highlight = (smoothstep(0.001, 0.0, norm.x) + smoothstep(0.999, 1.0, norm.x) + smoothstep(0.001, 0.0, norm.y)) * 0.35;
    
    coord = cos(PI * coord);
    vec4 result = u_diffuseColor * smoothstep(0.998, 1.0, max(coord.x, coord.y));
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