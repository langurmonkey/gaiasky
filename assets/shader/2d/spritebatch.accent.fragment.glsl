#version 330 core

uniform sampler2D u_texture;
uniform vec3 u_accentColor;

in vec4 v_color;
in vec2 v_texCoords;

layout (location = 0) out vec4 fragColor;

#include <shader/lib/colors.glsl>

bool isPurple(vec3 color) {
    return color.r > 0.5 && color.b > 0.5 && color.g < 0.5 * min(color.r, color.b);
}

vec3 transformColor(vec3 color, vec3 target) {
    float weightPurpleSat = 0.3;
    float weightTargetSat = 0.7;
    float weightPurpleBr = 0.3;
    float weightTargetBr = 0.7;

    vec3 purpleHSV = rgb2hsv(color);
    vec3 targetHSV = rgb2hsv(target);

    bool isTargetGrayscale = (target.r == target.b) && (target.b == target.g);

    float finalSat = isTargetGrayscale ? 0.0 : purpleHSV.y * weightPurpleSat + targetHSV.y * weightTargetSat;
    float finalBr = purpleHSV.z * weightPurpleBr + targetHSV.z * weightTargetBr;

    vec3 newColor = hsv2rgb(vec3(targetHSV.x, finalSat, finalBr));

    return newColor;
}

void main() {
    vec4 renderColor = v_color * texture(u_texture, v_texCoords);
    if (isPurple(renderColor.rgb)) {
        renderColor = vec4(transformColor(renderColor.rgb, u_accentColor), renderColor.a);
    }
    fragColor = renderColor;
}
