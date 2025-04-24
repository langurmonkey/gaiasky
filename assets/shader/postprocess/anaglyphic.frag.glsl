// Anaglyphic 3D red-cyan implementation by Toni Sagrista
// License: MPL2
#version 330 core

uniform sampler2D u_texture0;
uniform sampler2D u_texture1;
uniform int u_anaglyphMode;// 0 - red/blue, 1 - red/cyan

in vec2 v_texCoords;
layout (location = 0) out vec4 fragColor;

void main() {
    vec2 texcoord = v_texCoords;

    vec4 rightFrag = texture(u_texture1, texcoord);
    vec4 leftFrag = texture(u_texture0, texcoord);

    if (u_anaglyphMode == 1){
        // red-cyan
        rightFrag = vec4(1.0, rightFrag.g, rightFrag.b, 1.0);
        leftFrag = vec4(leftFrag.r, 1.0, 1.0, 1.0);
        fragColor = vec4(leftFrag.rgb * rightFrag.rgb, 1.0);
    } else {
        // red-blue
        leftFrag = vec4(leftFrag.r, 0.0, 0.0, 1.0);
        rightFrag = vec4(0.0, 0.0, rightFrag.b, 1.0);
        fragColor = vec4(leftFrag.rgb + rightFrag.rgb, 1.0);
    }


}
