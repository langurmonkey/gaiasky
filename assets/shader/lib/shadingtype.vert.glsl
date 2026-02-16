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

// Set billboard right and up vectors for spherical lighting.
void setBillboardDirectons(vec3 pos, vec3 s_up, vec3 s_right) {
    if (u_shadingType == 2) {
        v_billboardRight = s_right;
        v_billboardUp = s_up;
    }
}

// Computes the lighting vectors and the billboard directoins for the
// fragment stage of the fake lighting.
void computeShadingTypeOutputs(vec3 pos, vec3 s_up, vec3 s_right) {
    computeLightingVectors(pos);
    setBillboardDirectons(pos, s_up, s_right);
}

// Rotate UV coordinates around center by the given angle in radians.
vec2 rotateUV(vec2 uv, float angle) {
    float s = sin(angle);
    float c = cos(angle);
    mat2 r = mat2(c, -s, s, c);
    return r * (uv - 0.5) + 0.5;
}

#endif //GLSL_LIB_SHADINGTYPE_VERT