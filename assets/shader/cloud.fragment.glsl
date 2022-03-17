#version 330 core

////////////////////////////////////////////////////////////////////////////////////
////////// POSITION ATTRIBUTE - FRAGMENT
////////////////////////////////////////////////////////////////////////////////////
#define nop() { }

in vec4 v_position;
#define pullPosition() { return v_position; }


in vec2 v_texCoord0;

// Uniforms which are always available
uniform vec2 u_cameraNearFar;
uniform float u_cameraK;

// Varyings computed in the vertex shader
in float v_opacity;
in float v_alphaTest;

#ifdef diffuseTextureFlag
uniform sampler2D u_diffuseTexture;
#endif

#ifdef diffuseCubemapFlag
uniform samplerCube u_diffuseCubemap;
#endif

#ifdef normalTextureFlag
uniform sampler2D u_normalTexture;
#endif

// AMBIENT LIGHT
in vec3 v_ambientLight;

#include shader/lib_luma.glsl

// CLOUD TEXTURE
#if defined(diffuseCubemapFlag)
#include shader/lib_cubemap.glsl
vec4 fetchCloudColor(vec2 texCoord, vec4 defaultValue) {
    return texture(u_diffuseCubemap, UVtoXYZ(texCoord));
}
#elif defined(diffuseTextureFlag) && defined(normalTextureFlag)
// We have clouds and transparency
vec4 fetchCloudColor(vec2 texCoord, vec4 defaultValue) {
    vec4 cloud = texture(u_diffuseTexture, texCoord);
    vec4 trans = texture(u_normalTexture, texCoord);
    return vec4(cloud.rgb, 1.0 - luma(trans.rgb));
}
#elif defined(diffuseTextureFlag)
// Only clouds, we use value as transp
vec4 fetchCloudColor(vec2 texCoord, vec4 defaultValue) {
    return texture(u_diffuseTexture, texCoord);
}
#else
vec4 fetchCloudColor(vec2 texCoord, vec4 defaultValue) {
    return defaultValue;
}
#endif// diffuseTextureFlag && diffuseColorFlag

in vec3 v_lightDir;
in vec3 v_lightCol;
in vec3 v_viewDir;

layout (location = 0) out vec4 fragColor;

#include shader/lib_logdepthbuff.glsl

#ifdef ssrFlag
#include shader/lib_ssr.frag.glsl
#endif // ssrFlag

#ifdef velocityBufferFlag
#include shader/lib_velbuffer.frag.glsl
#endif

void main() {
    vec2 g_texCoord0 = v_texCoord0;

    vec4 cloud = fetchCloudColor(g_texCoord0, vec4(0.0, 0.0, 0.0, 0.0));
    vec3 ambient = v_ambientLight;
    float ambient_val = (ambient.r + ambient.g + ambient.b) / 3.0;

    // Normal in pixel space
    vec3 N = vec3(0.0, 0.0, 1.0);
    vec3 L = normalize(v_lightDir);
    float NL = clamp(dot(N, L) + 0.25, 0.0, 1.0);

    vec3 cloudColor = clamp(v_lightCol * cloud.rgb, 0.0, 0.9);
    float opacity = v_opacity * clamp(NL + ambient_val, 0.0, 1.0);
    fragColor = vec4(cloudColor, cloud.a) * opacity;

    fragColor = clamp(fragColor, 0.0, 0.9);

    gl_FragDepth = getDepthValue(u_cameraNearFar.y, u_cameraK);

    #ifdef ssrFlag
    ssrBuffers();
    #endif // ssrFlag

    #ifdef velocityBufferFlag
    velocityBuffer();
    #endif
}