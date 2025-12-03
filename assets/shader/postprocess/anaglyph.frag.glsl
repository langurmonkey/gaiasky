// Anaglyphic 3D red-cyan implementation by Toni Sagrista
// License: MPL2
#version 330 core

uniform sampler2D u_texture0;
uniform sampler2D u_texture1;
uniform int u_anaglyphMode;// 0 - red/cyan, 1 - red/blue, 2 - amber/blue, 3 - ColorCode

in vec2 v_texCoords;
layout (location = 0) out vec4 fragColor;

void main() {
    vec2 texcoord = v_texCoords;

    vec4 rightFrag = texture(u_texture1, texcoord);
    vec4 leftFrag = texture(u_texture0, texcoord);

    if (u_anaglyphMode == 0){
        // red-cyan
        rightFrag = vec4(1.0, rightFrag.g, rightFrag.b, 1.0);
        leftFrag = vec4(leftFrag.r, 1.0, 1.0, 1.0);
        fragColor = vec4(leftFrag.rgb * rightFrag.rgb, 1.0);

    } else if (u_anaglyphMode == 1) {
        // red-blue
        leftFrag = vec4(leftFrag.r, 0.0, 0.0, 1.0);
        rightFrag = vec4(0.0, 0.0, rightFrag.b, 1.0);
        fragColor = vec4(leftFrag.rgb + rightFrag.rgb, 1.0);

    } else if (u_anaglyphMode == 2) {
        // amber-blue approximation
        // amber ≈ R + G
        vec3 amber = vec3(leftFrag.r,
        leftFrag.g * 0.7,
        0.0);
        vec3 blue  = vec3(0.0, 0.0, rightFrag.b);
        fragColor = vec4(amber + blue, 1.0);

    } else if (u_anaglyphMode == 3) {
        // ColorCode 3D (approximate matrix method)
        vec3 L = leftFrag.rgb;
        vec3 R = rightFrag.rgb;

        // Left eye (amber) matrix
        mat3 M_left = mat3(
        0.0, 0.0, 0.0,
        1.0, 0.0, 0.0,
        0.0, 0.0, 0.0
        );

        // Right eye (blue) matrix
        mat3 M_right = mat3(
        0.0, 0.0, 1.0,
        0.0, 0.0, 1.0,
        0.0, 1.0, 1.0
        );

        vec3 outColor = M_left * L + M_right * R;

        fragColor = vec4(outColor, 1.0);
    }


}
