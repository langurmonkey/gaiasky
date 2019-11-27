#version 330 core
// Light scattering implementation by Toni Sagrista

#define N 30

// Current frame
uniform sampler2D u_texture0;
// Glow texture
uniform sampler2D u_texture1;

uniform vec2 u_viewport;
uniform float u_textureScale;
uniform vec2 u_lightPositions[N];
uniform float u_lightViewAngles[N];
uniform vec3 u_lightColors[N];
uniform int u_nLights;
uniform float u_orientation;
uniform float u_backbufferScale;

in float v_lums[N];
in vec2 v_texCoords;

layout (location = 0) out vec4 fragColor;

float len(vec2 vect, float ar) {
    return sqrt(vect.x * vect.x * ar * ar + vect.y * vect.y);
}

float brightness(vec4 color) {
    return dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
}

vec4 starImage(vec2 tc) {
    //float cos_factor = cos(u_orientation);
    //float sin_factor = sin(u_orientation);
    // Rotate (around center)
    //tc -= 0.5;
    //tc *= mat2(cos_factor, sin_factor, -sin_factor, cos_factor);
    //tc += 0.5;
    return texture(u_texture1, tc);
}


void main() {
    float ar = u_viewport.x / u_viewport.y;
    fragColor = vec4(0.0, 0.0, 0.0, 1.0);

    for (int li = 0; li < u_nLights; li++){
        float lum = v_lums[li];

        vec3 lightColor = u_lightColors[li];

        float viewAngle = min(0.0001, u_lightViewAngles[li]);
        float size = u_textureScale * min(1.6, viewAngle * 5.0e5) * lum;

        vec2 glow_tc = (v_texCoords * u_backbufferScale - u_lightPositions[li]);
        glow_tc.x *= ar;
        float dist_center = length(glow_tc);
        glow_tc /= size;
        glow_tc += 0.5;

        float color_glow = brightness(starImage(glow_tc)) * 1.2;
        float core_inc = (0.1 - min(0.1, dist_center)) * color_glow;
        fragColor += vec4(color_glow * lightColor.r + core_inc, color_glow * lightColor.g + core_inc, color_glow * lightColor.b + core_inc, 1.0);
    }
    fragColor.rgb += texture(u_texture0, v_texCoords).rgb;
    fragColor.a = 1.0;
}
