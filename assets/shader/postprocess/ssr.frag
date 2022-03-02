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
// r: packed diffuse and reflection (rgb)
// g: 1
// b: roughness
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

bool isBinarySearchEnabled = true;
bool isAdaptiveStepEnabled = true;
bool isExponentialStepEnabled = true;
bool isSamplingEnabled = true;

#define M_TO_U 1.0e-9

#define RAY_STEP M_TO_U * 0.15
#define DISTANCE_BIAS M_TO_U * 0.05

#define ITERATION_COUNT 60
#define SAMPLE_COUNT 6

#define getViewDepth(uv) 1.0 / recoverWValue(texture(u_texture1, uv).r, u_zfark.x, u_zfark.y)
#define getNDCDepth(uv) texture(u_texture1, uv).r

#include shader/lib_pack.glsl

float random (vec2 uv) {
    return fract(sin(dot(uv, vec2(12.9898, 78.233))) * 43758.5453123); //simple random function
}

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

vec2 screenFromView(vec3 viewCoords) {
    vec4 screenCoords = u_projection * vec4(viewCoords, 1.0);
    screenCoords.xy = (screenCoords.xy / screenCoords.w) * 0.5 + 0.5;
    return screenCoords.xy;
}

vec2 raymarch(vec3 P, vec3 R) {
    vec3 step = RAY_STEP * R;
    vec3 marchingPosition = P + step;
    float delta;
    float depthFromScreen;
    vec2 screenPosition;

    int i = 0;
    for (; i < ITERATION_COUNT; ++i) {
        // Project
        screenPosition = screenFromView(marchingPosition);
        depthFromScreen = abs(viewFromDepth(screenPosition).z);
        delta = abs(marchingPosition.z) - depthFromScreen;

        if (abs(delta) < DISTANCE_BIAS) {
            return screenPosition;
        }
        if (isBinarySearchEnabled && delta > 0) {
            break;
        }
        if (isAdaptiveStepEnabled){
            float directionSign = sign(abs(marchingPosition.z) - depthFromScreen);
            //this is sort of adapting step, should prevent lining reflection by doing sort of iterative converging
            //some implementation doing it by binary search, but I found this idea more cheaty and way easier to implement
            step = step * (1.0 - RAY_STEP * max(directionSign, 0.0));
            marchingPosition += step * (-directionSign);
        } else {
            marchingPosition += step;
        }
        if (isExponentialStepEnabled){
            step *= 1.05;
        }
    }
    if(isBinarySearchEnabled){
        for(; i < ITERATION_COUNT; i++){
            step *= 0.5;
            marchingPosition = marchingPosition - step * sign(delta);

            screenPosition = screenFromView(marchingPosition);
            depthFromScreen = abs(viewFromDepth(screenPosition).z);
            delta = abs(marchingPosition.z) - depthFromScreen;

            if (abs(delta) < DISTANCE_BIAS) {
                return screenPosition;
            }
        }
    }
    return screenPosition;
}

void main(void) {
    vec4 mask = texture(u_texture3, v_texCoords);
    vec3 maskColor = vec3(mask.r, unpack2(mask.g));
    float roughness = clamp(mask.b * 0.4, 0.0, 0.4);
    vec3 col = texture(u_texture0, v_texCoords).rgb;
    if (mask.r > 0.0) {
        // SSR
        vec3 N = (u_view * normalize(texture(u_texture2, v_texCoords))).xyz;
        vec3 P = viewFromDepth(v_texCoords).xyz;

        // Reflection vector
        vec3 R = normalize(reflect(normalize(P), normalize(N)));

        if(isSamplingEnabled) {
            vec3 firstBasis = normalize(cross(vec3(0.0, 0.0, 1.0), R));
            vec3 secondBasis = normalize(cross(R, firstBasis));
            vec4 resultingColor = vec4(0.0);
            for (int i = 0; i < SAMPLE_COUNT; i++) {
                vec2 coeffs = vec2(random(v_texCoords + vec2(0, i)) + random(v_texCoords + vec2(i, 0))) * roughness;
                vec3 reflectionDirectionRandomized = R + firstBasis * coeffs.x + secondBasis * coeffs.y;
                vec2 coords = raymarch(P, normalize(reflectionDirectionRandomized));
                vec3 reflection = texture(u_texture0, coords).rgb;
                if (reflection != vec3(0.0)) {
                    resultingColor += vec4(reflection, 1.0);
                }
            }
            if (resultingColor.w == 0.0){
                fragColor = vec4(col + maskColor * 0.15, 1.0);
            } else {
                resultingColor /= resultingColor.w;
                fragColor = vec4(col + mix(resultingColor.rgb, maskColor, 0.15), 1.0);
            }
        } else {
            // Ray cast
            vec2 coords = raymarch(P, R);
            if (coords.x >= 0.0 && coords.y >= 0.0){
                vec3 reflection = texture(u_texture0, coords).rgb;
                fragColor = vec4(col + mix(reflection, maskColor, 0.15), 1.0);
            } else {
                fragColor = vec4(col + maskColor * 0.15, 1.0);
            }
        }

    } else {
        // Non-reflective parts
        fragColor = vec4(col, 1.0);
    }
    // View normal buffer
    //fragColor = vec4(texture(u_texture2, v_texCoords).xyz, 1.0);
    // View reflection mask (roughness)
    //fragColor = vec4(vec3(roughness), 1.0);
}
