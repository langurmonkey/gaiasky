// Anaglyphic 3D red-cyan implementation by Toni Sagrista
// License: MPL2
#version 330 core

uniform sampler2D u_texture0;
uniform sampler2D u_texture1;
uniform int u_anaglyphMode;
#define A_RED_CYAN 0
#define A_RED_CYAN_DUBOIS 1
#define A_AMBER_BLUE 2
#define A_AMBER_BLUE_DUBOIS 3
#define A_RED_BLUE 4


in vec2 v_texCoords;
layout (location = 0) out vec4 fragColor;

// From Peter Wimmer's "Optimized Anaglyphs"
// This separates gamma operations on each color channel, boosts red
// https://cybereality.com/rendepth-red-cyan-anaglyph-filter-optimized-for-stereoscopic-3d-on-lcd-monitors/
const vec3 gamma_map = vec3(1.6, 0.8, 1.0);
vec3 correctColor(vec3 original) {
    vec3 corrected;
    corrected.r = pow(original.r, 1.0 / gamma_map.r);
    corrected.g = pow(original.g, 1.0 / gamma_map.g);
    corrected.b = pow(original.b, 1.0 / gamma_map.b);
    return corrected;
}

void main() {
    vec2 texcoord = v_texCoords;

    vec4 rightFrag = texture(u_texture1, texcoord);
    vec4 leftFrag = texture(u_texture0, texcoord);

    if (u_anaglyphMode == A_RED_CYAN){
        // red-cyan
        vec3 l = vec3(leftFrag.r, 1.0, 1.0);
        vec3 r = vec3(1.0, rightFrag.g, rightFrag.b);
        fragColor = vec4(correctColor(l * r), 1.0);

    } else if (u_anaglyphMode == A_RED_CYAN_DUBOIS){
        // red-cyan Dubois-style
        // Values from https://cybereality.com/rendepth-red-cyan-anaglyph-filter-optimized-for-stereoscopic-3d-on-lcd-monitors/
        const mat3 left_filter = mat3(
        vec3(0.4561, 0.500484, 0.176381),
        vec3(-0.400822, -0.0378246, -0.0157589),
        vec3(-0.0152161, -0.0205971, -0.00546856));

        const mat3 right_filter = mat3(
        vec3(-0.0434706, -0.0879388, -0.00155529),
        vec3(0.378476, 0.73364, -0.0184503),
        vec3(-0.0721527, -0.112961, 1.2264));

        vec3 l = clamp(leftFrag.rgb * left_filter, vec3(0.0), vec3(1.0));
        vec3 r = clamp(rightFrag.rgb * right_filter, vec3(0.0), vec3(1.0));

        fragColor = vec4(correctColor(l + r), 1.0);

    } else if (u_anaglyphMode == A_AMBER_BLUE) {
        // amber-blue
        // amber ≈ R + G
        vec3 amber = vec3(leftFrag.r,
                            leftFrag.g * 0.7,
                            0.0);
        vec3 blue  = vec3(0.0, 0.0, rightFrag.b);
        fragColor = vec4(amber + blue, 1.0);

    } else if (u_anaglyphMode == A_AMBER_BLUE_DUBOIS) {
        // amber-blue Dubois-style
        // Values from https://php.mmc.school.nz/11/jacobbelt/Dolphin-x64/Sys/Shaders/Anaglyph/
        const mat3 left_filter = mat3(
        vec3( 1.062,-0.205, 0.299),
        vec3(-0.026, 0.908, 0.068),
        vec3(-0.038,-0.173, 0.022));

        const mat3 right_filter = mat3(
        vec3(-0.016,-0.123,-0.017),
        vec3( 0.006, 0.062,-0.017),
        vec3( 0.094, 0.185, 0.911));

        vec3 l = clamp(leftFrag.rgb * left_filter, vec3(0.0), vec3(1.0));
        vec3 r = clamp(rightFrag.rgb * right_filter, vec3(0.0), vec3(1.0));

        fragColor = vec4(l + r, 1.0);

    } else if (u_anaglyphMode == A_RED_BLUE) {
        // red-blue
        leftFrag = vec4(leftFrag.r, 0.0, 0.0, 1.0);
        rightFrag = vec4(0.0, 0.0, rightFrag.b, 1.0);
        fragColor = vec4(leftFrag.rgb + rightFrag.rgb, 1.0);

    }

}
