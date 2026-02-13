#ifndef GLSL_LIB_SHADINGTYPE_VERT
#define GLSL_LIB_SHADINGTYPE_VERT

// Uniforms
uniform int u_shadingType = 0;
uniform vec3 u_lightPos;

// Outputs
out vec3 v_lightDir;
out vec3 v_viewDir;
out vec3 v_billboardRight;
out vec3 v_billboardUp;

// Calculate lighting vectors.
void computeLightingVectors(vec3 pos) {
    if (u_shadingType != 0) {
        vec3 toLight = u_lightPos - pos;
        v_lightDir = normalize(toLight);
        // View direction for spherical shading.
        v_viewDir = -normalize(pos);
    }
}

// Compute billboard right and up vectors for spherical lighting.
void computeBillboardDirectons(vec3 pos) {
    if (u_shadingType == 2) {
        vec3 viewDirNorm = normalize(pos);
        vec3 right = normalize(cross(u_camUp, viewDirNorm));
        vec3 up = normalize(cross(viewDirNorm, right));
        v_billboardRight = -right;
        v_billboardUp = -up;
    }
}

// Computes the lighting vectors and the billboard directoins for the
// fragment stage of the fake lighting.
void computeShadingTypeOutputs(vec3 pos) {
    computeLightingVectors(pos);
    computeBillboardDirectons(pos);
}

#endif //GLSL_LIB_SHADINGTYPE_VERT