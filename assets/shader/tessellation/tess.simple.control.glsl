#version 410 core

#define N_VERTICES 3

layout(vertices = N_VERTICES) out;

in vec2 v_texCoords[gl_MaxPatchVertices];
out vec2 l_texCoords[N_VERTICES];

in vec3 v_normal[gl_MaxPatchVertices];
out vec3 l_normal[N_VERTICES];


// Internal units per km, from Constants.KM_TO_U. It is not a constant of the shader:
// the distance scale factor changes it (1e-6 normally, 1e-2 with a scale of 1e4).
uniform float u_kmToU;

vec3 center(vec3 p1, vec3 p2){
    return (p1 + p2) / 2.0;
}

vec3 center(vec3 p1, vec3 p2, vec3 p3){
    return (p1 + p2 + p3) / 3.0;
}

float tessellationLevel(vec3 center){
    // Distance scaling variable
    const float tessellationFactor = 300;
    const float tessellationSlope = 1.0;

    // Distance to the camera in real km, so the level is the same at any distance scale.
    float uToKm = u_kmToU > 0.0 ? 1.0 / u_kmToU : 1.0e6;
    float d = length(center) * uToKm;
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
}