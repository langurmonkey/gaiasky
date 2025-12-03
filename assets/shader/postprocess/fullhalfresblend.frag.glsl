#version 330 core

// Full resolution color buffer.
uniform sampler2D u_texture0;
// Half resolution color buffer.
uniform sampler2D u_texture1;
// Full resolution depth buffer.
uniform sampler2D u_texture2;
// Half resolution depth buffer.
uniform sampler2D u_texture3;

// Half accumulation buffer.
uniform sampler2D u_texture4;
// Half revealage buffer.
uniform sampler2D u_texture5;

// Z-far and K values for depth buffer.
uniform vec2 u_zFarK;

in vec2 v_texCoords;
layout(location = 0) out vec4 fragColor;

#include <shader/lib/logdepthbuff.glsl>

void main() {
    vec3 fullRes = texture(u_texture0, v_texCoords).rgb;
    vec3 halfRes = texture(u_texture1, v_texCoords).rgb;

    // WB-OIT in half buffer.
    // Read the accumulated values
    vec4 A = texture(u_texture4, v_texCoords);
    float R = texture(u_texture5, v_texCoords).r;

    // Read the opaque background color
    vec3 C_O = halfRes;

    const float epsilon = 0.0001;

    // 1. Calculate the final opacity of the transparent layer (alpha_T)
    // The R value is the accumulated weighted (1 - alpha)
    // We use the total accumulated weighted alpha (A.a) and total revealage (R)
    // to calculate the final transparency contribution.

    // The 5.0 factor here controls how opaque the layer appears. Lowering it makes it more opaque.
    float alpha_T = 1.0 - exp(-A.a / 2.0);

    // C_T is the weighted color average
    vec3 C_T = A.rgb / max(A.a, epsilon);

    // Final Compositing (Transparent over Opaque)
    halfRes = mix(C_O, C_T, alpha_T);

    // Recover 'linear' depth values.
    float depthFull = 1.0 / recoverWValue(texture(u_texture2, v_texCoords).r, u_zFarK.x, u_zFarK.y);
    // float depthHalf = 1.0 / recoverWValue(texture(u_texture3, v_texCoords).r, u_zFarK.x, u_zFarK.y);

    if (depthFull < 1e5) {
        fragColor = vec4(fullRes + halfRes, 1.0);
    } else {
        fragColor = vec4(clamp(fullRes + halfRes, 0.0, 1.0), 1.0);
    }

    // if (depthFull < depthHalf) {
    //     // Full-res pixel is closer - use it
    //     fragColor = vec4(fullRes, 1.0);
    // } else if (depthFull > depthHalf) {
    //     // Half-res pixel is closer - use it
    //     fragColor = vec4(halfRes, 1.0);
    // } else {
    //     // Depths are approximately equal - blend
    //     fragColor = vec4(clamp(fullRes + halfRes, 0.0, 1.0), 1.0);
    // }
}
