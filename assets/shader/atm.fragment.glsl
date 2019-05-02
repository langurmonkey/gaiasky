#version 330 core

#define exposure 0.5

uniform vec3 v3LightPos;
uniform float g;
uniform float g2;

// Direction from the vertex to the camera
in vec3 v_direction;
// Calculated colors
in vec4 v_frontColor;
in vec3 v_frontSecondaryColor;
// Depth buffer value
in float v_depth;

out vec4 fragColor;

void main(void) {
    float fCos = dot (v3LightPos, v_direction) / length (v_direction);
    float fCos2 = fCos * fCos;
    float fRayleighPhase = 0.75 + 0.75 * fCos2;
    float fMiePhase = 1.5 * ((1.0 - g2) / (2.0 + g2)) * (1.0 + fCos2) / pow (1.0 + g2 - 2.0 * g * fCos, 1.5);

    fragColor.rgb = (fRayleighPhase * v_frontColor.rgb + fMiePhase * v_frontSecondaryColor.rgb);
    fragColor.rgb = vec3(1.0) - exp(-exposure * fragColor.rgb);
    fragColor.a = v_frontColor.a * length(fragColor.rgb);

    // Prevent saturation
    fragColor.rgb = clamp(fragColor.rgb, 0.0, 0.95);

    // Normal depth buffer
    // gl_FragDepth = gl_FragCoord.z;
    // Logarithmic depth buffer
    gl_FragDepth = v_depth;
}
