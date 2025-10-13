#version 330 core

// Uniforms which are always available
uniform mat4 u_worldTrans;
uniform vec3 u_cameraPos;
uniform float u_bodySize;
uniform vec2 u_cameraNearFar;
uniform float u_cameraK;

#ifdef volume0Flag
// Volume texture 0
uniform sampler3D u_volume0;
uniform vec3 u_volume0BoundsMin;
uniform vec3 u_volume0BoundsMax;
#endif //volume0Flag

#ifdef volume1Flag
// Volume texture 1
uniform sampler3D u_volume1;
uniform vec3 u_volume1BoundsMin;
uniform vec3 u_volume1BoundsMax;
#endif //volume1Flag

#ifdef volume2Flag
// Volume texture 2
uniform sampler3D u_volume2;
uniform vec3 u_volume2BoundsMin;
uniform vec3 u_volume2BoundsMax;
#endif //volume2Flag

#ifdef volume3Flag
// Volume texture 3
uniform sampler3D u_volume3;
uniform vec3 u_volume3BoundsMin;
uniform vec3 u_volume3BoundsMax;
#endif //volume3Flag

#if defined(volume0Flag) || defined(volume1Flag) || defined(volume2Flag) || defined(volume3Flag)
#define volumeFlag
#endif // volume[0|1|2|3]Flag

// We use the diffuse channel for volume0.
#ifdef diffuseColorFlag
    uniform vec4 u_diffuseColor;
    #define fetchColorDiffuse(defaultValue) u_diffuseColor
#else
    #define fetchColorDiffuse(defaultValue) defaultValue
#endif // diffuseColorFlag

// INPUT
struct VertexData {
    vec2 texCoords;
    vec3 normal;
    vec3 viewDir;
    vec3 ambientLight;
    float opacity;
    vec4 color;
    vec3 fragPosWorld;
    mat3 tbn;
};
in VertexData v_data;

// OUTPUT
layout(location = 0) out vec4 fragColor;
layout(location = 1) out vec4 layerBuffer;

#include <shader/lib/logdepthbuff.glsl>

#define PI 3.14159265359
#define ITERATIONS 150

// Convert world position to volume texture coordinates
vec3 worldToVolumeUVW(vec3 worldPos, vec3 boundsMin, vec3 boundsMax) {
    // Transform world position to model space using inverse of u_worldTrans
    vec4 modelSpacePos = vec4(worldPos, 1.0);
    // Normalize model space position to [0,1] within volume bounds
    vec3 volumeSpace = (modelSpacePos.xyz - boundsMin) / (boundsMax - boundsMin);
    return volumeSpace;
}

// Sample volume density
float sampleVolume(vec3 uvw, sampler3D volume) {
    if (any(lessThan(uvw, vec3(0.0))) || any(greaterThan(uvw, vec3(1.0)))) {
        return 0.0;
    }
    return texture(volume, uvw).r;
}

// Ray-box intersection - returns (entryDistance, exitDistance)
vec2 boxIntersection(vec3 rayOrigin, vec3 rayDir, vec3 boundsMin, vec3 boundsMax) {
    vec3 invDir = 1.0 / rayDir;
    vec3 t0 = (boundsMin - rayOrigin) * invDir;
    vec3 t1 = (boundsMax - rayOrigin) * invDir;

    vec3 tmin = min(t0, t1);
    vec3 tmax = max(t0, t1);

    float entry = max(max(tmin.x, tmin.y), tmin.z);
    float exit = min(min(tmax.x, tmax.y), tmax.z);

    // Check if intersection is valid
    if (entry > exit || exit < 0.0) {
        return vec2(-1.0);
    }

    // If entry is negative, camera is inside the box
    entry = max(entry, 0.0);

    return vec2(entry, exit);
}

// Transfer function for additive blending - allows saturation to white
vec4 transferFunction(float density, vec3 baseColor, float stepSize, float transmittance) {
    // Start with base color but DON'T clamp it - let it accumulate
    vec3 color = baseColor;
    
    float intensity = density * (1.0 + density * 0.05);
    
    // Apply intensity (no clamping - let additive blending handle it)
    color *= intensity;
    
    // Opacity scales with density but doesn't clamp
    float alpha = density * stepSize * (1.0 + density * 0.2);
    
    return vec4(color, alpha);
}

vec4 raymarchVolume(sampler3D volume, vec3 boundsMin, vec3 boundsMax, vec3 color, inout float firstDepth) {
    vec3 rayOrigin = u_cameraPos;
    vec3 rayDir = normalize(v_data.fragPosWorld - u_cameraPos);

    vec2 intersection = boxIntersection(rayOrigin, rayDir, boundsMin, boundsMax);
    if (intersection.x < 0.0) {
        discard;
    }

    vec4 result = vec4(0.0);
    float entryDist = intersection.x;
    float exitDist = intersection.y;
    float rayLength = exitDist - entryDist;

    vec3 entryPoint = rayOrigin + rayDir * entryDist;
    float stepSize = rayLength / float(ITERATIONS);

    float transmittance = 1.0;

    vec3 baseColor = color;

    for (int i = 0; i < ITERATIONS; i++) {
        float currentDist = entryDist + float(i) * stepSize;
        vec3 samplePos = rayOrigin + rayDir * currentDist;
        vec3 uvw = worldToVolumeUVW(samplePos, boundsMin, boundsMax);

        float density = sampleVolume(uvw, volume);

        if (firstDepth < 0.0 && density > 0.01) {
            firstDepth = currentDist;
        }

        // Emission with enhanced transfer function
        if (density > 0.01 && transmittance > 0.001) {
            // Use accumulated density for saturation effects
            vec4 sampl = transferFunction(density, baseColor, stepSize, transmittance);

            vec3 emittedLight = sampl.rgb * transmittance;

            result.rgb += emittedLight;
            result.a += sampl.a * (1.0 - result.a); // Alpha still uses blending
        }

        // Stop if completely opaque or no light transmission
        if (transmittance < 0.01 || result.a > 0.99) {
            break;
        }
    }
    return result;
}

void main() {
    #ifdef volumeFlag
        vec4 result = vec4(0.0);
        float firstDepth = -1.0;

        #ifdef volume0Flag
            result += raymarchVolume(u_volume0, u_volume0BoundsMin, u_volume0BoundsMax, fetchColorDiffuse(vec4(0.3, 0.6, 1.0, 1.0)).rgb, firstDepth);
        #endif //volume0Flag
        #ifdef volume1Flag
            result += raymarchVolume(u_volume1, u_volume1BoundsMin, u_volume1BoundsMax, vec3(0.1, 0.8, 0.2), firstDepth);
        #endif //volume1Flag
        #ifdef volume2Flag
            result += raymarchVolume(u_volume2, u_volume2BoundsMin, u_volume2BoundsMax, vec3(0.0, 0.2, 0.8), firstDepth);
        #endif //volume2Flag
        #ifdef volume3Flag
            result += raymarchVolume(u_volume3, u_volume3BoundsMin, u_volume3BoundsMax, vec3(0.8, 0.2, 0.1), firstDepth);
        #endif //volume3Flag

        if (result.a > 0.001) {
            // Simple reinhard tone mapping to control brightness
            result.rgb = result.rgb / (1.0 + result.rgb);
            fragColor = vec4(result.rgb, result.a);
        } else {
            // See box
            fragColor = vec4(0.2, 0.1, 0.0, 1.0);
            //discard;
        }

        float depth = (firstDepth > 0.0) ? firstDepth : 1.0e10;
        gl_FragDepth = getDepthValue(depth, u_cameraNearFar.y, u_cameraK);
    #endif // volumeFlag
}
