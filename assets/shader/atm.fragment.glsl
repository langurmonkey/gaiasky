#version 330 core

#define exposure 0.65

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
// Height normalized
in float v_heightNormalized;
// Fade factor between hieght-driven opacity and luminosity-driven opacity
in float v_fadeFactor;

out vec4 fragColor;

float luminance(vec3 color){
    return dot(color, vec3(0.2126, 0.7152, 0.0722));
}

void main(void) {
    float fCos = dot (v3LightPos, v_direction) / length (v_direction);
    float fCos2 = fCos * fCos;
    float fRayleighPhase = 0.75 + 0.75 * fCos2;
    float fMiePhase = 1.5 * ((1.0 - g2) / (2.0 + g2)) * (1.0 + fCos2) / pow (1.0 + g2 - 2.0 * g * fCos, 1.5);

    fragColor.rgb = (fRayleighPhase * v_frontColor.rgb + fMiePhase * v_frontSecondaryColor.rgb);
    fragColor.rgb = vec3(1.0) - exp(-exposure * fragColor.rgb);
    fragColor.a = v_heightNormalized * (1.0 - v_fadeFactor) + luminance(fragColor.rgb) * v_fadeFactor;

    fragColor.rgb = fragColor.rgb;
    gl_FragDepth = v_depth;
}
