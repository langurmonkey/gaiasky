#version 330 core

#ifdef diffuseColorFlag
uniform vec4 u_diffuseColor;
#endif

#ifdef diffuseTextureFlag
uniform sampler2D u_diffuseTexture;
#endif

// COLOR DIFFUSE
#if defined(diffuseTextureFlag) && defined(diffuseColorFlag)
#define fetchColorDiffuseTD(texCoord, defaultValue) texture(u_diffuseTexture, texCoord) * u_diffuseColor
#elif defined(diffuseTextureFlag)
#define fetchColorDiffuseTD(texCoord, defaultValue) texture(u_diffuseTexture, texCoord)
#elif defined(diffuseColorFlag)
#define fetchColorDiffuseTD(texCoord, defaultValue) u_diffuseColor
#else
#define fetchColorDiffuseTD(texCoord, defaultValue) defaultValue
#endif// diffuse

#if defined(diffuseTextureFlag) || defined(diffuseColorFlag)
#define fetchColorDiffuse(baseColor, texCoord, defaultValue) baseColor * fetchColorDiffuseTD(texCoord, defaultValue)
#else
#define fetchColorDiffuse(baseColor, texCoord, defaultValue) baseColor
#endif// diffuse

// INPUT
struct VertexData {
    vec2 texCoords;
    vec3 normal;
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
    mat3 tbn;
};
in VertexData v_data;

// OUTPUT
layout (location = 0) out vec4 fragColor;

// Renders a depth map for the shadow mapping algorithm
void main() {
    // Fetch opacity value from diffuse color.
    vec4 diffuse = fetchColorDiffuse(v_data.color, v_data.texCoords, vec4(1.0, 1.0, 1.0, 1.0));
    float opacity = diffuse.a;

    // Fill buffer with depth and transparency.
    fragColor = vec4(gl_FragCoord.z, opacity, 0.0, 1.0);
}
