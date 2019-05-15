#version 410 core

#define N_VERTICES 3

layout(vertices = N_VERTICES) out;

in vec2 v_texCoords[gl_MaxPatchVertices];
out vec2 l_texCoords[N_VERTICES];

in vec3 v_normal[gl_MaxPatchVertices];
out vec3 l_normal[N_VERTICES];


#define U_TO_KM 1.0E6

void main(){
    #define id gl_InvocationID
    if(id == 0){
        // OUTER
        gl_TessLevelOuter[2] = 30.0;
        gl_TessLevelOuter[0] = 30.0;
        gl_TessLevelOuter[1] = 30.0;

        // INNER
        gl_TessLevelInner[0] = 20.0;
    }

    // Plumbing
    gl_out[id].gl_Position = gl_in[id].gl_Position;
    l_texCoords[id] = v_texCoords[id];
    l_normal[id] = v_normal[id];
}