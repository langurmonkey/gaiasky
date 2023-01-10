#version 410 core

// UNIFORMS
uniform float u_svtId;
uniform float u_svtDepth;
uniform float u_svtTileSize;
uniform sampler2D u_diffuseTexture;

// INPUT
struct VertexData {
    vec2 texCoords;
    vec3 normal;
#ifdef directionalLightsFlag
    DirectionalLight directionalLights[numDirectionalLights];
#endif // directionalLightsFlag
    vec3 viewDir;
    vec3 ambientLight;
    float opacity;
    vec4 color;
#ifdef shadowMapFlag
    vec3 shadowMapUv;
#endif // shadowMapFlag
    vec3 fragPosWorld;
#ifdef metallicFlag
    vec3 reflect;
#endif // metallicFlag
};
in VertexData v_data;

// OUTPUT
layout (location = 0) out vec4 fragColor;

void main() {
    // RGBA: x, y, mip, id
    fragColor.xy = floor(v_data.texCoords.xy * u_svtTileSize);
    fragColor.z = textureQueryLod(u_diffuseTexture, v_data.texCoords).y;
    fragColor.w = u_svtId;
}
