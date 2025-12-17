#version 410 core

#define N_VERTICES 3

layout(vertices = N_VERTICES) out;

// Tessellation quality factor in [1,10]
uniform float u_tessQuality = 4.0;
// Body size in kilometers.
uniform float u_bodySize;
// Km->unit
uniform float u_kmToU;

struct VertexData {
    vec2 texCoords;
    vec3 normal;
    vec3 viewDir;
    vec3 ambientLight;
    float opacity;
    vec4 color;
    #ifdef shadowMapFlag
    vec3 shadowMapUv;
    #ifdef shadowMapGlobalFlag
    vec3 shadowMapUvGlobal;
    #endif // shadowMapGlobalFlag
    #ifdef numCSM
    vec3 csmLightSpacePos[numCSM];
    #endif // numCSM
    #endif // shadowMapFlag
    vec3 fragPosWorld;
    #ifdef reflectionCubemapFlag
    vec3 reflect;
    #endif // reflectionCubemapFlag
    mat3 tbn;
};

// INPUT
in VertexData v_data[gl_MaxPatchVertices];
#ifdef atmosphereGround
in vec3 v_position[gl_MaxPatchVertices];
#endif // atmosphereGround
// OUTPUT
out VertexData l_data[N_VERTICES];
#ifdef atmosphereGround
out vec3 l_position[N_VERTICES];
#endif // atmosphereGround

#ifdef svtIndirectionHeightTextureFlag
#define QUALITY_SCALE 2.0
#else
#define QUALITY_SCALE 1.0
#endif

vec3 center(vec3 p1, vec3 p2){
    return (p1 + p2) / 2.0;
}

vec3 center(vec3 p1, vec3 p2, vec3 p3){
    return (p1 + p2 + p3) / 3.0;
}

float tessellationLevel(vec3 center){
    float distKm = length(center) / u_kmToU;

    // Tessellation based on screen-space coverage
    float screenCoverage = u_bodySize / max(distKm, 0.001);

    // Base level on desired detail
    float baseLevel = u_tessQuality * screenCoverage;

    return clamp(baseLevel, 1.0, float(gl_MaxTessGenLevel));
}

void main(){
    #define id gl_InvocationID
    float level = 0;
    if(id == 0){
        // OUTER
        gl_TessLevelOuter[2] = tessellationLevel(center(gl_in[0].gl_Position.xyz, gl_in[1].gl_Position.xyz));
        gl_TessLevelOuter[0] = tessellationLevel(center(gl_in[1].gl_Position.xyz, gl_in[2].gl_Position.xyz));
        gl_TessLevelOuter[1] = tessellationLevel(center(gl_in[2].gl_Position.xyz, gl_in[0].gl_Position.xyz));

        // INNER
        level = tessellationLevel(center(gl_in[0].gl_Position.xyz, gl_in[1].gl_Position.xyz, gl_in[2].gl_Position.xyz));
        level = clamp(level * 0.5, 0.0, float(gl_MaxTessGenLevel));
        gl_TessLevelInner[0] = level;
    }

    // Plumbing
    gl_out[id].gl_Position = gl_in[id].gl_Position;

    l_data[id].texCoords = v_data[id].texCoords;
    l_data[id].normal = v_data[id].normal;
    l_data[id].viewDir = v_data[id].viewDir;
    l_data[id].ambientLight = v_data[id].ambientLight;
    l_data[id].opacity = v_data[id].opacity;
    l_data[id].color = v_data[id].color;
    l_data[id].fragPosWorld = v_data[id].fragPosWorld;
    #ifdef reflectionCubemapFlag
    l_data[id].reflect = v_data[id].reflect;
    #endif // reflectionCubemapFlag

    #ifdef atmosphereGround
    l_position[id] = v_position[id];
    #endif // atmosphereGround

    #ifdef shadowMapFlag
    l_data[id].shadowMapUv = v_data[id].shadowMapUv;
    #ifdef shadowMapGlobalFlag
    l_data[id].shadowMapUvGlobal = v_data[id].shadowMapUvGlobal;
    #endif // shadowMapGlobalFlag
    #ifdef numCSM
    for (int i = 0; i < numCSM; i++) {
        l_data[id].csmLightSpacePos[i] = v_data[id].csmLightSpacePos[i];
    }
    #endif // numCSM
    #endif // shadowMapFlag

    l_data[id].tbn = v_data[id].tbn;
}