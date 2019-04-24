#define exposure 0.5

uniform vec3 v3LightPos;
uniform float g;
uniform float g2;

// Direction from the vertex to the camera
varying vec3 v3Direction;
// Calculated colors
varying vec4 frontColor;
varying vec3 frontSecondaryColor;
// Depth buffer value
varying float v_depth;

void main(void) {
    float fCos = dot (v3LightPos, v3Direction) / length (v3Direction);
    float fCos2 = fCos * fCos;
    float fRayleighPhase = 0.75 + 0.75 * fCos2;
    float fMiePhase = 1.5 * ((1.0 - g2) / (2.0 + g2)) * (1.0 + fCos2) / pow (1.0 + g2 - 2.0 * g * fCos, 1.5);

    gl_FragColor.rgb = (fRayleighPhase * frontColor.rgb + fMiePhase * frontSecondaryColor.rgb);
    gl_FragColor.rgb = vec3(1.0) - exp(-exposure * gl_FragColor.rgb);
    gl_FragColor.a = frontColor.a * length(gl_FragColor.rgb);

    // Prevent saturation
    gl_FragColor.rgb = clamp(gl_FragColor.rgb, 0.0, 0.95);

    // Normal depth buffer
    // gl_FragDepth = gl_FragCoord.z;
    // Logarithmic depth buffer
    gl_FragDepth = v_depth;
}
