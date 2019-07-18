#version 410 core

#define N_VERTICES 3

layout(vertices = N_VERTICES) out;

in vec2 v_texCoords[gl_MaxPatchVertices];
out vec2 l_texCoords[N_VERTICES];

in vec3 v_normal[gl_MaxPatchVertices];
out vec3 l_normal[N_VERTICES];

in vec3 v_lightDir[gl_MaxPatchVertices];
out vec3 l_lightDir[N_VERTICES];

in vec3 v_lightCol[gl_MaxPatchVertices];
out vec3 l_lightCol[N_VERTICES];

in vec3 v_viewDir[gl_MaxPatchVertices];
out vec3 l_viewDir[N_VERTICES];

in vec3 v_ambientLight[gl_MaxPatchVertices];
out vec3 l_ambientLight[N_VERTICES];

in float v_opacity[gl_MaxPatchVertices];
out float l_opacity[N_VERTICES];

in vec4 v_color[gl_MaxPatchVertices];
out vec4 l_color[N_VERTICES];

#ifdef atmosphereGround
in vec4 v_atmosphereColor[gl_MaxPatchVertices];
out vec4 l_atmosphereColor[N_VERTICES];

in float v_fadeFactor[gl_MaxPatchVertices];
out float l_fadeFactor[N_VERTICES];
#endif

#ifdef shadowMapFlag
in vec3 v_shadowMapUv[gl_MaxPatchVertices];
out vec3 l_shadowMapUv[N_VERTICES];
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
    const float tessellationFactor = 500;
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

    l_texCoords[id] = v_texCoords[id];
    l_normal[id] = v_normal[id];
    l_viewDir[id] = v_viewDir[id];
    l_lightDir[id] = v_lightDir[id];
    l_lightCol[id] = v_lightCol[id];
    l_ambientLight[id] = v_ambientLight[id];
    l_opacity[id] = v_opacity[id];
    l_color[id] = v_color[id];

    #ifdef atmosphereGround
    l_atmosphereColor[id] = v_atmosphereColor[id];
    l_fadeFactor[id] = v_fadeFactor[id];
    #endif

    #ifdef shadowMapFlag
    l_shadowMapUv[id] = v_shadowMapUv[id];
    #endif
}