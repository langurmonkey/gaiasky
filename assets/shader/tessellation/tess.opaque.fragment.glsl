#version 410 core

uniform vec2 u_cameraNearFar;
uniform float u_cameraK;

in vec2 o_texCoords;

in float o_opacity;

layout (location = 0) out vec4 fragColor;

#include <shader/lib/logdepthbuff.glsl>

void main() {
    fragColor = vec4(0.0, 0.0, 0.0, 1.0);
    // Logarithmic depth buffer
    gl_FragDepth = getDepthValue(u_cameraNearFar.y, u_cameraK);
}

