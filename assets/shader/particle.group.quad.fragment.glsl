#version 330 core

#include <shader/lib/logdepthbuff.glsl>

// UNIFORMS
uniform float u_falloff;
uniform float u_zfar;
uniform float u_k;
uniform sampler2DArray u_textures;
uniform float u_ambientLight;
uniform float u_lightIntensity;
// 0-off, 1-billboard lighting, 2-spherical lighting
uniform int u_shadingType = 0;
uniform float u_sphericalPower;

// INPUT
in vec4 v_col;
in vec2 v_uv;
in float v_textureIndex;
in vec3 v_lightDir;
in vec3 v_viewDir;
in vec3 v_billboardRight;
in vec3 v_billboardUp;


// OUTPUT
layout (location = 0) out vec4 fragColor;
layout (location = 1) out vec4 layerBuffer;

#define PI 3.1415927

#ifdef ssrFlag
#include <shader/lib/ssr.frag.glsl>
#endif // ssrFlag

// Calculate fake sphere normal from billboard UV coordinates.
// Returns a normal vector pointing outward from sphere center IN WORLD SPACE.
// Returns vec3(0) if outside the sphere boundary.
vec3 getSphereNormal(vec2 uv) {
    // Convert UV (0-1) to centered coordinates (-1 to 1).
    vec2 centered = (uv - 0.5) * 2.0;
    float dist2 = dot(centered, centered);

    // If outside sphere, return zero vector.
    if (dist2 > 1.0) {
        return vec3(0.0);
    }

    // Calculate z component using sphere equation: x² + y² + z² = 1.
    float z = sqrt(1.0 - dist2);

    // This gives us a normal in billboard space (where the billboard lies in XY plane)
    // The billboard coordinate system is defined by the vectors passed from vertex shader:
    // - X axis (right): v_billboardRight
    // - Y axis (up): v_billboardUp
    // - Z axis (forward toward camera): v_viewDir
    vec3 billboardNormal = vec3(centered.x, centered.y, z);

    // Transform the billboard-space normal to world space using the billboard frame
    vec3 worldNormal = billboardNormal.x * v_billboardRight +
    billboardNormal.y * v_billboardUp +
    billboardNormal.z * v_viewDir;

    return normalize(worldNormal);
}

//  Calculates a pseudo-spherical normal for the entire UV quad.
//  Unlike a standard sphere, this ensures that pixels in the 'corners' of the
//  UV (dist > 1.0) still receive a valid outward-facing normal.
//  * This prevents 'black holes' or unlit regions on textured particles
//  like asteroids that have irregular, non-circular silhouettes.
vec3 getBulgingQuadNormal(vec2 uv) {
    // Transform UV [0,1] to centered coordinates [-1,1].
    vec2 centered = (uv - 0.5) * 2.0;

    // Determine how far this pixel is from the billboard center.
    float distSq = dot(centered, centered);

    // Calculate Z (depth). We clamp distSq to 0.999 to avoid
    // imaginary numbers/NaNs at the corners of the quad.
    float z = sqrt(1.0 - min(distSq, 0.999));

    // For pixels inside the 1.0 radius, we use the standard sphere math.
    // For pixels in the corners (distSq > 1.0), we normalize the XY
    // to push the normal to the extreme 'horizon' edge.
    vec3 billboardNormal;
    if (distSq > 1.0) {
        billboardNormal = vec3(normalize(centered), z);
    } else {
        billboardNormal = vec3(centered.x, centered.y, z);
    }

    // Transform from Billboard/Tangent space to World Space
    vec3 worldNormal = billboardNormal.x * v_billboardRight +
    billboardNormal.y * v_billboardUp +
    billboardNormal.z * v_viewDir;

    return normalize(worldNormal);
}

// Calculate lighting with both directional sun and ambient.
float calculateLighting(vec3 normal, bool isBillboard) {
    float diffuse;

    if (isBillboard) {
        // For billboards, we need to check if we're viewing the sun-facing side.
        // The billboard always faces the camera, so we check the angle between
        // the view direction and the light direction.
        // If they're aligned (dot > 0), we see the lit side.
        // If they're opposite (dot < 0), we see the shadow side.
        diffuse = max(dot(v_viewDir, v_lightDir), 0.0);
    } else {
        // For spherical shading, use the actual surface normal.
        diffuse = max(dot(normal, v_lightDir), 0.0);
    }

    // Combine ambient and diffuse.
    float lighting = u_ambientLight + u_lightIntensity * diffuse;

    // Clamp to prevent over-brightening.
    return min(lighting, 1.0);
}

vec4 programmatic(float dist) {
    float profile = 1.0 - pow(abs(sin(PI * dist / 2.0)), u_falloff);
    vec3 baseColor = v_col.rgb * profile;

    float lighting = 1.0;

    if (u_shadingType == 2 && dist <= 1.0) {
        // Get sphere normal for this fragment.
        vec3 sphereNormal = getSphereNormal(v_uv);

        if (length(sphereNormal) > 0.0) {
            // Apply lighting to sphere.
            lighting = calculateLighting(sphereNormal, false);

            // Add edge darkening for more spherical appearance.
            float edgeFactor = 1.0 - pow(dist, u_sphericalPower);
            lighting *= edgeFactor;
        }
    } else if (u_shadingType == 1){
        // Simple billboard lighting.
        lighting = calculateLighting(vec3(0.0), true);
    }
    // If u_shadingType == 0, lighting stays at 1 (no lighting).

    if (u_shadingType == 0) {
        return vec4(baseColor * lighting, 1.0) * v_col.a;
    } else {
        return vec4(baseColor * lighting, v_col.a * profile);
    }

}

vec4 textured() {
    vec4 c = texture(u_textures, vec3(v_uv, v_textureIndex));
    vec3 baseColor = c.rgb * v_col.rgb;
    // Discard transparent fragments.
    if (c.a <= 0.05) {
        discard;
    }

    float lighting = 1.0;

    if (u_shadingType == 2) {
        vec3 normal = getBulgingQuadNormal(v_uv);

        // Core Lighting (Sunlight).
        lighting = calculateLighting(normal, false);

        // Edge Darkening (Spherical roundness).
        float d = length((v_uv - 0.5) * 2.0);
        float edgeFactor = 1.0 - (0.2 * pow(min(d, 1.0), u_sphericalPower));
        lighting *= edgeFactor;

        // Rim Light Effect.
        // v_viewDir points from fragment to camera.
        // Dot product is 1.0 at center, 0.0 at edges.
        float rim = 1.0 - max(dot(normal, v_viewDir), 0.0);
        rim = pow(rim, 3.0);

        // Add a bit of the rim glow to the lighting
        // (multiplied by u_lightIntensity to keep it consistent with scene brightness).
        lighting += rim * u_lightIntensity * 0.5;
    } else if (u_shadingType == 1) {
        // Simple billboard lighting.
        lighting = calculateLighting(vec3(0.0), true);
    }
    // If u_shadingType == 0, lighting stays at 1 (no lighting).

    if (u_shadingType == 0) {
        return vec4(baseColor * lighting, 1.0) * c.a * v_col.a;
    } else {
        return vec4(baseColor * lighting, v_col.a * c.a);
    }
}

void main() {
    if (v_textureIndex < 0.0) {
        float dist = distance(vec2(0.5), v_uv) * 2.0;
        if (dist > 1.0) {
            discard;
        }
        fragColor = programmatic(dist);
    } else {
        fragColor = textured();
    }
    gl_FragDepth = getDepthValue(u_zfar, u_k);
    layerBuffer = vec4(0.0, 0.0, 0.0, 1.0);

    // Add outline
    //if (v_uv.x > 0.99 || v_uv.x < 0.01 || v_uv.y > 0.99 || v_uv.y < 0.01) {
    //    fragColor = vec4(1.0, 0.0, 0.0, 1.0);
    //}

    #ifdef ssrFlag
    ssrBuffers();
    #endif // ssrFlag
}
