#version 330 core
// Light scattering implementation by Toni Sagrista

#define MAX_LIGHTS 8

// Current frame.
uniform sampler2D u_texture0;
// Glow texture.
uniform sampler2D u_texture1;

uniform float u_time;
uniform vec2 u_viewport;
uniform float u_textureScale;
uniform vec2 u_lightPositions[MAX_LIGHTS];
uniform float u_lightViewAngles[MAX_LIGHTS];
uniform vec3 u_lightColors[MAX_LIGHTS];
uniform int u_nLights;
uniform float u_orientation;
uniform float u_backbufferScale;

in float v_lums[MAX_LIGHTS];
in vec2 v_texCoords;

#define saturate(x) clamp(x, 0.0, 1.0)

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

float polarMask(vec2 uv, float time) {
    // center coordinates [-1,1]
    vec2 p = uv * 2.0 - 1.0;
    float r = length(p);        // distance from center
    vec2 d = normalize(p);      // direction for angular mask

    // angular modulation
    float angularMask = 0.5
    + 0.25 * sin(d.x * 12.0 + time * 2.0)
    + 0.20 * cos(d.y * 37.0 - time * 1.3)
    + 0.10 * sin((d.x + d.y) * 59.0 + time * 1.6);

    // In [0,1]
    angularMask = (angularMask + 1.0) * 0.5;
    // In [minVal,1]
    float minVal = 0.55;
    angularMask = minVal + (1.0 - minVal) * angularMask;

    // Keep the center
    float center = smoothstep(0.85, 1.0, 1.0 - r);

    // Combine radial decay with angular modulation
    return clamp(angularMask + center, minVal, 1.0);
}


void main() {
    float ar = u_viewport.x / u_viewport.y;
    vec3 effectColor = vec3(0.0, 0.0, 0.0);

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

        float mask = polarMask(glow_tc, u_time);
        vec4 glow = starImage(glow_tc);
        glow.rgb *= mask;
        float glow_value = brightness(glow);
        float core_inc = (0.1 - min(0.1, dist_center)) * glow_value;
        effectColor += vec3(glow_value * lightColor.r + core_inc, glow_value * lightColor.g + core_inc, glow_value * lightColor.b + core_inc);
    }
    fragColor.rgb = saturate(effectColor + texture(u_texture0, v_texCoords).rgb);
    fragColor.a = 1.0;
}
