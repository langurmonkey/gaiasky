#version 330 core

#define TEXTURE_LOD_BIAS 0.0

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

#ifdef normalTextureFlag
uniform sampler2D u_normalTexture;
#endif


// AMBIENT LIGHT
in vec3 v_ambientLight;


float luma(vec3 color){
    return dot(color, vec3(0.2126, 0.7152, 0.0722));
}

// CLOUD TEXTURE
#if defined(diffuseTextureFlag) && defined(normalTextureFlag)
// We have clouds and transparency
vec4 fetchCloudColor(vec2 texCoord, vec4 defaultValue) {
    vec4 cloud = texture(u_diffuseTexture, texCoord, TEXTURE_LOD_BIAS);
    vec4 trans = texture(u_normalTexture, texCoord, TEXTURE_LOD_BIAS);
    return vec4(cloud.rgb, 1.0 - pow(luma(trans.rgb), 0.7));
}
#elif defined(diffuseTextureFlag)
// Only clouds, we use value as transp
vec4 fetchCloudColor(vec2 texCoord, vec4 defaultValue) {
    vec4 cloud = texture(u_diffuseTexture, texCoord, TEXTURE_LOD_BIAS);
    // Smooth towards the poles
    float smoothing = smoothstep(0.01, 0.07, texCoord.y);
    return vec4(2.0 * cloud.rgb, smoothing * pow(luma(cloud.rgb), 0.7));
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
    float NL = max(0.0, dot(N, L));

    vec3 cloudColor = clamp(v_lightCol * cloud.rgb, 0.0, 0.9);
    float opacity = v_opacity * clamp(NL + ambient_val, 0.0, 1.0);
    fragColor = vec4(cloudColor * opacity, cloud.a * opacity);

    fragColor = clamp(fragColor, 0.0, 0.9);

    gl_FragDepth = getDepthValue(u_cameraNearFar.y, u_cameraK);

    #ifdef velocityBufferFlag
    velocityBuffer();
    #endif
}