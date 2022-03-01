// Screen Space Reflections shader by Toni Sagrista
// License: MPL2
#version 330 core

#include shader/lib_logdepthbuff.glsl

// Color buffer
uniform sampler2D u_texture0;
// Depth buffer
uniform sampler2D u_texture1;
// Normal buffer
uniform sampler2D u_texture2;
// Reflection mask
uniform sampler2D u_texture3;
// Position buffer
uniform sampler2D u_texture4;

// Camera projection matrix
uniform mat4 u_projection;
// Camera inverse projection matrix
uniform mat4 u_invProjection;
// Camera projection-view (combined) matrix
uniform mat4 u_modelView;
// Z-far and K values for depth buffer
uniform vec2 u_zfark;

// INPUTS
in vec2 v_texCoords;
in vec3 v_ray;

// OUTPUTS
layout (location = 0) out vec4 fragColor;

#define MIN_RAY_STEP 0.1
#define STEP 0.1
#define STEPS 30

#define getPosition(uv) texture(u_texture4, uv).xyz
#define getDepth(uv) texture(u_texture1, uv).xyz

vec3 positionFromDepth(in vec2 texCoord, in float depth) {
    vec3 rawPosition                = vec3(texCoord, depth);

    vec4 screenSpacePosition        = vec4(rawPosition * 2.0 - 1.0, 1.0);
    vec4 viewSpacePosition          = u_invProjection * screenSpacePosition;

    return viewSpacePosition.xyz / viewSpacePosition.w;
}

vec3 calcViewPosition(in vec2 texCoord) {
    // Combine UV & depth into XY & Z (NDC)
    float depth                     = 1.0 / recoverWValue(texture(u_texture1, texCoord).r, u_zfark.x, u_zfark.y); // Logarithmic depth buffer
    return positionFromDepth(texCoord, getDepth(texCoord).z);
}

vec2 raymarch(vec3 dir, inout vec3 hitCoord, out float dDepth) {
    dir *= STEP;

    for(int i = 0; i < STEPS; ++i) {
        hitCoord               += dir;

        vec4 projectedCoord     = vec4(hitCoord, 1.0);
        projectedCoord.xy      /= projectedCoord.w;
        projectedCoord.xy       = projectedCoord.xy * 0.5 + 0.5;

        float depth             = calcViewPosition(projectedCoord.xy).z;
        //float depth             = getPosition(projectedCoord.xy).z;
        dDepth                  = hitCoord.z - depth;

        if(dDepth < 0.0)
        return projectedCoord.xy;
    }
    return vec2(0.0f);
}

void main(void) {
    float mask                      = texture(u_texture3, v_texCoords).r;
    vec3 col                        = texture(u_texture0, v_texCoords).rgb;
    if (mask > 0.0) {
        // SSR
        vec3 viewNormal            = texture(u_texture2, v_texCoords).xyz;
        vec3 viewPos               = calcViewPosition(v_texCoords).xyz;

        // Reflection vector
        vec3 reflected              = normalize(reflect(normalize(viewPos.xyz), normalize(viewNormal)));

        // Ray cast
        vec3 hitPos                 = viewPos.xyz;
        float dDepth;
        vec2 coords                 = raymarch(reflected * max(MIN_RAY_STEP, viewPos.z), hitPos, dDepth);

        fragColor                   = vec4(col + textureLod(u_texture0, coords, 0).rgb * mask, 1.0);
    } else {
        // Non-reflective parts
        fragColor                   = vec4(col, 1.0);
    }
    // View normal buffer
    //fragColor = vec4(texture(u_texture2, v_texCoords).xyz, 1.0);
    // View reflection mask
    //fragColor = vec4(texture(u_texture3, v_texCoords).xyz, 1.0);
    // View position buffer
    //fragColor = vec4(texture(u_texture4, v_texCoords).xyz, 1.0);
}
