// Screen Space Reflections shader by Toni Sagrista
// Implementation based on:
// https://github.com/RoundedGlint585/ScreenSpaceReflection (License: MIT)
// License: MPL2
#version 330 core

// Color buffer
uniform sampler2D u_texture0;
// Depth buffer (logarithmic)
uniform sampler2D u_texture1;
// Normal buffer (world space)
uniform sampler2D u_texture2;
// Reflection mask
// r: diffuse & reflection red channel
// g: diffuse & reflection green and blue channels (packed)
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

// Do a binary search pass in case no hit is found
bool isBinarySearchEnabled = true;
// Use adaptive iterative convergence
bool isAdaptiveStepEnabled = true;
// Use exponential steps
bool isExponentialStepEnabled = true;
// Take multiple samples with slightly randomized directions
// depending on material roughness
bool isSamplingEnabled = true;

// Ray-march iterations
int iterationCount = 50;
// Number of samples if sampling is enabled
int sampleCount = 4;

#define M_TO_U 1.0e-9
#define PC_TO_U 3.08567758149137e7

#define RAY_STEP M_TO_U * 0.06
#define DIST_BIAS M_TO_U * 0.005

#include shader/lib_logdepthbuff.glsl
#include shader/lib_pack.glsl

#define getViewDepth(uv) 1.0 / recoverWValue(texture(u_texture1, uv).r, u_zfark.x, u_zfark.y)
#define getNDCDepth(uv) texture(u_texture1, uv).r

float random(vec2 co) {
    float a = 12.9898;
    float b = 78.233;
    float c = 43758.5453;
    float dt = dot(co.xy, vec2(a, b));
    float sn = mod(dt, 3.14);
    return fract(sin(sn) * c);
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
    for (; i < iterationCount; ++i) {
        // Project
        screenPosition = screenFromView(marchingPosition);
        depthFromScreen = abs(viewFromDepth(screenPosition).z);
        delta = abs(marchingPosition.z) - depthFromScreen;

        if (abs(delta) < DIST_BIAS) {
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
        for(; i < iterationCount; i++){
            step *= 0.5;
            marchingPosition = marchingPosition - step * sign(delta);

            screenPosition = screenFromView(marchingPosition);
            depthFromScreen = abs(viewFromDepth(screenPosition).z);
            delta = abs(marchingPosition.z) - depthFromScreen;

            if (abs(delta) < DIST_BIAS) {
                return screenPosition;
            }
        }
    }
    // This is specific to Gaia Sky
    // Stars do not populate the depth buffer, so we only
    // reflect things that are far away (> 1e6 km)
    float z = getViewDepth(screenPosition);
    if(z < 1.0e9 * M_TO_U) {
        return vec2(-1.0);
    } else {
        return screenPosition;
    }
}

void main(void) {
    vec4 mask = texture(u_texture3, v_texCoords);
    vec3 maskColor = vec3(mask.r, unpack2(mask.g));
    float roughness = clamp(mask.b * 0.3, 0.0, 0.3);
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
            for (int i = 0; i < sampleCount; i++) {
                vec2 coeffs = vec2(random(v_texCoords + vec2(0, i)) + random(v_texCoords + vec2(i, 0))) * roughness;
                vec3 reflectionDirectionRandomized = R + firstBasis * coeffs.x + secondBasis * coeffs.y;
                vec2 coords = raymarch(P, normalize(reflectionDirectionRandomized));
                if(all(greaterThanEqual(coords, vec2(0.0)))) {
                    vec3 reflection = texture(u_texture0, coords).rgb;
                    if (reflection != vec3(0.0)) {
                        resultingColor += vec4(reflection, 1.0);
                    }
                }
            }
            if (resultingColor.w == 0.0) {
                fragColor = vec4(col + maskColor * 0.15, 1.0);
            } else {
                resultingColor /= resultingColor.w;
                fragColor = vec4(col + mix(resultingColor.rgb, maskColor, 0.15), 1.0);
            }
        } else {
            // Ray cast
            vec2 coords = raymarch(P, R);
            if (all(greaterThanEqual(coords, vec2(0.0)))){
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
