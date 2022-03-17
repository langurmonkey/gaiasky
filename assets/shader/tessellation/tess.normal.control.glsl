#version 410 core

#define N_VERTICES 3

layout(vertices = N_VERTICES) out;

// Tessellation quality in [1,7]
uniform float u_tessQuality = 4.0;

#if defined(numDirectionalLights) && (numDirectionalLights > 0)
#define directionalLightsFlag
#endif // numDirectionalLights

#ifdef directionalLightsFlag
struct DirectionalLight {
    vec3 color;
    vec3 direction;
};
#endif // directionalLightsFlag

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
    #ifdef reflectionCubemapFlag
    vec3 reflect;
    #endif // reflectionCubemapFlag
};

// INPUT
in VertexData v_data[gl_MaxPatchVertices];
#ifdef atmosphereGround
in vec4 v_atmosphereColor[gl_MaxPatchVertices];
in float v_fadeFactor[gl_MaxPatchVertices];
#endif
// OUTPUT
out VertexData l_data[N_VERTICES];
#ifdef atmosphereGround
out vec4 l_atmosphereColor[N_VERTICES];
out float l_fadeFactor[N_VERTICES];
#endif

#define U_TO_KM 1.0E6

vec3 center(vec3 p1, vec3 p2){
    return (p1 + p2) / 2.0;
}

vec3 center(vec3 p1, vec3 p2, vec3 p3){
    return (p1 + p2 + p3) / 3.0;
}

float tessellationLevel(vec3 center){
    // Distance scaling variable
    float tessellationFactor = u_tessQuality * 100.0;
    const float tessellationSlope = 1.0;

    float d = length(center) * U_TO_KM;
    return mix(0.0, float(gl_MaxTessGenLevel), tessellationFactor / pow(d, tessellationSlope));
}

void main(){
    #define id gl_InvocationID
    if(id == 0){
        // OUTER
        gl_TessLevelOuter[2] = tessellationLevel(center(gl_in[0].gl_Position.xyz, gl_in[1].gl_Position.xyz));
        gl_TessLevelOuter[0] = tessellationLevel(center(gl_in[1].gl_Position.xyz, gl_in[2].gl_Position.xyz));
        gl_TessLevelOuter[1] = tessellationLevel(center(gl_in[2].gl_Position.xyz, gl_in[0].gl_Position.xyz));

        // INNER
        float level = tessellationLevel(center(gl_in[0].gl_Position.xyz, gl_in[1].gl_Position.xyz, gl_in[2].gl_Position.xyz));
        level = clamp(level * 0.5, 0.0, float(gl_MaxTessGenLevel));
        gl_TessLevelInner[0] = level;
    }

    // Plumbing
    gl_out[id].gl_Position = gl_in[id].gl_Position;

    l_data[id].texCoords = v_data[id].texCoords;
    l_data[id].normal = v_data[id].normal;
    l_data[id].viewDir = v_data[id].viewDir;
    #ifdef directionalLightsFlag
    for (int i = 0; i < numDirectionalLights; i++) {
        l_data[id].directionalLights[i].color = v_data[id].directionalLights[i].color;
        l_data[id].directionalLights[i].direction = v_data[id].directionalLights[i].direction;
    }
    #endif
    l_data[id].ambientLight = v_data[id].ambientLight;
    l_data[id].opacity = v_data[id].opacity;
    l_data[id].color = v_data[id].color;
    l_data[id].fragPosWorld = v_data[id].fragPosWorld;
    #ifdef reflectionCubemapFlag
    l_data[id].reflect = v_data[id].reflect;
    #endif // reflectionCubemapFlag

    #ifdef atmosphereGround
    l_atmosphereColor[id] = v_atmosphereColor[id];
    l_fadeFactor[id] = v_fadeFactor[id];
    #endif

    #ifdef shadowMapFlag
    l_data[id].shadowMapUv = v_data[id].shadowMapUv;
    #endif
}