// Screen Space Reflections shader by Toni Sagrista
// License: MPL2
#version 330 core

#include shader/lib_logdepthbuff.glsl

// Color buffer
uniform sampler2D u_texture0;
// Depth buffer (logarithmic)
uniform sampler2D u_texture1;
// Normal buffer (world space)
uniform sampler2D u_texture2;
// Reflection mask
uniform sampler2D u_texture3;

// Camera projection matrix
uniform mat4 u_projection;
// Camera inverse projection matrix
uniform mat4 u_invProjection;
// Camera projection-view (combined) matrix
uniform mat4 u_combined;
// Camera view transform
uniform mat4 u_view;
// Camera inverse view transform
uniform mat4 u_invView;
// Z-far and K values for depth buffer
uniform vec2 u_zfark;

// INPUTS
in vec2 v_texCoords;
in vec3 v_ray;

// OUTPUTS
layout (location = 0) out vec4 fragColor;

#define M_TO_U 1.0e-9
#define STEP M_TO_U * 0.2
#define STEPS 60
#define DISTANCE_BIAS M_TO_U * 0.1

#define getViewDepth(uv) 1.0 / recoverWValue(texture(u_texture1, uv).r, u_zfark.x, u_zfark.y)
#define getNDCDepth(uv) texture(u_texture1, uv).r

vec3 viewFromDepth(vec2 texCoords) {
    // Convert depth to default non-linear function
    // to apply inverse projection
    float depth = getViewDepth(texCoords);
    depth = defaultDepth(depth, 0.5 * M_TO_U, u_zfark.x);
    vec4 ndc = vec4(vec3(texCoords, depth) * 2.0 - 1.0, 1.0);
    vec4 viewCoords = u_invProjection * ndc;// going back from projected
    viewCoords.xyz /= viewCoords.w;
    return viewCoords.xyz;
}

vec2 project(vec3 viewCoords) {
    vec4 screenCoords = u_projection * vec4(viewCoords, 1.0);
    screenCoords.xy = (screenCoords.xy / screenCoords.w) * 0.5 + 0.5;
    return screenCoords.xy;
}

vec2 raymarch(vec3 P, vec3 R) {
    vec3 reflect;
    vec3 marchingPosition;
    float delta;
    float depthFromScreen;
    vec2 screenPosition;

    for (int i = 1; i <= STEPS; ++i) {
        reflect = R * (STEP * i);
        marchingPosition = P + reflect;

        // Project
        screenPosition = project(marchingPosition).xy;
        depthFromScreen = abs(viewFromDepth(screenPosition).z);
        delta = abs(marchingPosition.z) - depthFromScreen;

        if (abs(delta) < DISTANCE_BIAS) {
            return screenPosition;
        }
    }
    return screenPosition;
}

void main(void) {
    float mask = texture(u_texture3, v_texCoords).r;
    vec3 col = texture(u_texture0, v_texCoords).rgb;
    if (mask > 0.0) {
        // SSR
        vec3 N = (u_view * normalize(texture(u_texture2, v_texCoords))).xyz;
        vec3 P = viewFromDepth(v_texCoords).xyz;

        // Reflection vector
        vec3 R = normalize(reflect(normalize(P), normalize(N)));

        // Ray cast
        vec2 coords = raymarch(P, R);

        if(coords.x >= 0.0 && coords.y >= 0.0){
            fragColor = vec4(col + textureLod(u_texture0, coords, 0).rgb * mask, 1.0);
        } else {
            fragColor = vec4(col, 1.0);
        }
    } else {
        // Non-reflective parts
        fragColor = vec4(col, 1.0);
    }
    // View normal buffer
    //fragColor = vec4(texture(u_texture2, v_texCoords).xyz, 1.0);
    // View reflection mask
    //fragColor = vec4(texture(u_texture3, v_texCoords).xyz, 1.0);
}
