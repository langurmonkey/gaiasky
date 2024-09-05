#version 330 core

// Uniforms
uniform mat4 u_worldTrans;
uniform samplerCube u_diffuseCubemap;
uniform float u_opacity;

// INPUT
in vec3 v_texCoords;

// OUTPUT
layout (location = 0) out vec4 fragColor;
layout (location = 1) out vec4 layerBuffer;

#ifdef ssrFlag
#include <shader/lib/ssr.frag.glsl>
#endif // ssrFlag

void main() {
    fragColor = texture(u_diffuseCubemap, v_texCoords) * u_opacity;
    layerBuffer = vec4(0.0, 0.0, 0.0, 1.0);

    #ifdef ssrFlag
    ssrBuffers();
    #endif // ssrFlag
}
