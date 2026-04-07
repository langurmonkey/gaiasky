#ifndef GLSL_LIB_SHADINGTYPE_VERT
#define GLSL_LIB_SHADINGTYPE_VERT

// Uniforms
uniform int u_shadingType = 0;
uniform vec3 u_lightPos;
uniform int u_occlusion = 0;
uniform float u_planetRadius = 0.0;
uniform float u_ambientLight;

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

void computeShadingTypeColor(vec3 pos, vec3 datasetPos, inout vec4 col) {
    if (u_shadingType != 0 && u_occlusion != 0) {
        // Light Direction (Directional light for parallel rays)
        // If u_lightPos is a position, normalize the vector from planet to light
        vec3 L = normalize(u_lightPos - datasetPos);

        // Vector from planet center to the current particle
        vec3 P = pos - datasetPos;

        // Project P onto the light vector L
        // This tells us how far "forward" or "backward" the particle is along the light axis
        float distAlongRay = dot(P, L);

        // Calculate the squared perpendicular distance from the particle to the ray
        // Using Pythagoras: perpendicular^2 = hypotenuse^2 - base^2
        float distSq = dot(P, P) - (distAlongRay * distAlongRay);

        // Shadow logic
        float rSq = u_planetRadius * u_planetRadius;
        // Is it inside the cylinder (distSq < rSq) and behind the planet (distAlongRay < 0)?
        // step returns 0.0 if x < edge, 1.0 otherwise.
        float outsideCylinder = step(rSq, distSq);
        float inFrontOfPlanet = step(0.0, distAlongRay);

        // The particle is LIT if it is outside the cylinder OR in front of the planet
        float lightIntensity = clamp(max(outsideCylinder, inFrontOfPlanet) + u_ambientLight, 0.0, 1.0);

        col.rgb *= lightIntensity;
    }
}

#endif //GLSL_LIB_SHADINGTYPE_VERT