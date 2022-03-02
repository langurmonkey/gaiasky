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
uniform mat4 u_combined;
// Camera view transform
uniform mat4 u_view;
// Z-far and K values for depth buffer
uniform vec2 u_zfark;

// INPUTS
in vec2 v_texCoords;
in vec3 v_ray;

// OUTPUTS
layout (location = 0) out vec4 fragColor;

#define STEP 0.066
#define STEPS 30

#define getPosition(uv) texture(u_texture4, uv).xyz
#define getDepth(uv) 1.0 / recoverWValue(texture(u_texture1, uv).r, u_zfark.x, u_zfark.y)
#define getDepthBuff(uv) texture(u_texture1, uv).r

vec3 generatePositionFromDepth(vec2 texturePos, float depth) {
    vec4 ndc = vec4((texturePos - 0.5) * 2.0, depth, 1.0);
    vec4 inversed = u_invProjection * ndc;// going back from projected
    inversed /= inversed.w;
    return inversed.xyz;
}

vec2 generateProjectedPosition(vec3 pos) {
    vec4 samplePosition = u_projection * vec4(pos, 1.0);
    samplePosition.xy = (samplePosition.xy / samplePosition.w) * 0.5 + 0.5;
    return samplePosition.xy;
}

vec3 prj(vec3 wc, mat4 transform){
    vec4 w = vec4(wc, 1.0);
    float lw = 1.0 / (w.x * transform[0][3] + w.y * transform[1][3] + w.z * transform[2][3] + transform[3][3]);
    vec4 res = (transform * w) * lw;
    //vec3 res = vec3((w.x * combined[0][0] + w.y * combined[1][0] + w.z * combined[2][0] + combined[3][0]) * lw,
    //                (w.x * combined[0][1] + w.y * combined[1][1] + w.z * combined[2][1] + combined[3][1]) * lw,
    //                (w.x * combined[0][2] + w.y * combined[1][2] + w.z * combined[2][2] + combined[3][2]) * lw);
    return res.xyz;
}

vec3 project(vec3 wc, mat4 transform){
    vec3 sc = prj(wc, transform);
    return (sc + 1.0) / 2.0;
}

vec2 raymarch(vec3 P, vec3 R) {
    vec2 screenCoord;
    for (int i = 1; i <= STEPS; ++i) {
        vec3 r = R * (STEP * i);
        vec3 h = P + r;

        // Project
        screenCoord = project(h, u_projection).xy;
        // Sample position and get depth
        float depth = length(getPosition(screenCoord));

        if (length(h) > depth) {
            return screenCoord;
        }
    }
    return screenCoord;
}

void main(void) {
    float mask = texture(u_texture3, v_texCoords).r;
    vec3 col = texture(u_texture0, v_texCoords).rgb;
    if (mask > 0.0) {
        // SSR
        vec3 N = (u_view * normalize(texture(u_texture2, v_texCoords))).xyz;
        vec3 P = normalize(v_ray) * getDepthBuff(v_texCoords);
        P = generatePositionFromDepth(v_texCoords, getDepthBuff(v_texCoords));

        // Reflection vector
        vec3 R = normalize(reflect(normalize(P), N));

        // Ray cast
        vec2 coords = raymarch(P, R);

        fragColor = vec4(col + textureLod(u_texture0, coords, 0).rgb * mask, 1.0);
    } else {
        // Non-reflective parts
        fragColor = vec4(col, 1.0);
    }
    // View normal buffer
    //fragColor = vec4(texture(u_texture2, v_texCoords).xyz, 1.0);
    // View reflection mask
    //fragColor = vec4(texture(u_texture3, v_texCoords).xyz, 1.0);
    // View position buffer
    //fragColor = vec4(texture(u_texture4, v_texCoords).xyz, 1.0);
}
