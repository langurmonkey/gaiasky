#version 330 core

// Uniforms which are always available
uniform vec2 u_cameraNearFar;
uniform float u_cameraK;
uniform float u_time;
uniform float u_simuTime;

// We use the diffuse channel for the bottom part.
#ifdef diffuseColorFlag
uniform vec4 u_diffuseColor;
#define fetchColorDiffuse(defaultValue) u_diffuseColor
#else
#define fetchColorDiffuse(defaultValue) defaultValue
#endif // diffuseColorFlag

// We use the emissive channel for the top part.
#ifdef emissiveColorFlag
uniform vec4 u_emissiveColor;
#define fetchColorEmissive(defaultValue) u_emissiveColor
#else
#define fetchColorEmissive(defaultValue) defaultValue
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

#define PI 3.14159

#include <shader/lib/logdepthbuff.glsl>

// Noise functions
float hash(vec2 co) {
    return fract(sin(dot(co, vec2(12.9898, 78.233))) * 43758.5453);
}
float hash(float x, float y) {
    return hash(vec2(x, y));
}

float shash(vec2 co) {
    float x = co.x;
    float y = co.y;

    float corners = (hash(x - 1., y - 1.) + hash(x + 1., y - 1.) + hash(x - 1., y + 1.) + hash(x + 1., y + 1.)) / 16.;
    float sides = (hash(x - 1., y) + hash(x + 1., y) + hash(x, y - 1.) + hash(x, y + 1.)) / 8.;
    float center = hash(co) / 4.;

    return corners + sides + center;
}

float noise(vec2 co) {
    vec2 pos = floor(co);
    vec2 fpos = co - pos;

    fpos = (3.0 - 2.0 * fpos) * fpos * fpos;

    float c1 = shash(pos);
    float c2 = shash(pos + vec2(0.0, 1.0));
    float c3 = shash(pos + vec2(1.0, 0.0));
    float c4 = shash(pos + vec2(1.0, 1.0));

    float s1 = mix(c1, c3, fpos.x);
    float s2 = mix(c2, c4, fpos.x);

    return mix(s1, s2, fpos.y);
}

float pnoise(vec2 co, int oct) {
    float total = 0.0;
    float m = 0.0;

    for (int i = 0; i < oct; i++) {
        float freq = pow(2.0, float(i));
        float amp = pow(0.5, float(i));

        total += noise(freq * co) * amp;
        m += amp;
    }

    return total / m;
}

// FBM: repeatedly apply Perlin noise to position
vec2 fbm(vec2 p, int oct) {
    return vec2(pnoise(p + vec2(u_time, 0.0), oct), pnoise(p + vec2(-u_time, 0.0), oct));
}

float fbm2(vec2 p, int oct) {
    return pnoise(p + 10. * fbm(p, oct) + vec2(0.0, u_time), oct);
}

// Calculate the aurora lights given the top and bottom colors.
vec3 lights(vec2 co, vec3 colTop, vec3 colBottom) {
    float d, r, g, b, h;
    vec3 rc, gc, bc, hc;


    // Red (top)
    r = fbm2(co * vec2(1.0, 0.5), 1);
    d = pnoise(2.0 * co + vec2(0.3 * u_time), 2);
    rc = colTop * r * smoothstep(0.0, 2.5 + d * r, co.y) * smoothstep(-5.0, 1.0, 2.0 - co.y - 2.0 * d);

    // Green (bottom)
    g = fbm2(co * vec2(2., 0.5), 4);
    gc = 0.8 * colBottom * clamp(2. * pow((3. - 2. * g) * g * g, 2.5) - 0.5 * co.y, 0.0, 1.0) * smoothstep(-2. * d, 0.0, co.y) * smoothstep(0.0, 0.3, 1.1 + d - co.y);

    g = fbm2(co * vec2(1.0, 0.2), 2);
    gc += 0.5 * colBottom * clamp(2. * pow((3. - 2. * g) * g * g, 2.5) - 0.5 * co.y, 0.0, 1.0) * smoothstep(-2. * d, 0.0, co.y) * smoothstep(0.0, 0.3, 1.1 + d - co.y);

    // Blue (below)
    h = pnoise(vec2(5.0 * co.x, 5.0 * u_time), 1);
    hc = vec3(0.0, 0.6, 1.0) * pow(h + 0.1, 2.0) * smoothstep(-2. * d, 0.0, co.y + 0.2) * smoothstep(-h, 0.0, -co.y - 0.4);

    return rc + gc + hc;
}

// Renders all black for the occlusion testing.
void main() {
    vec2 uv = v_data.texCoords;
    float t = u_simuTime * 0.01;

    vec3 colTop = fetchColorEmissive(vec4(1.0, 0.2, 0.1, 1.0)).rgb;
    vec3 colBottom = fetchColorDiffuse(vec4(0.0, 1.0, 0.4, 1.0)).rgb;

    float phase = colBottom.b * PI;


    float f = 0.3 + 0.3 * pnoise(vec2(8.0 * uv.x, 0.01 * t), 4);
    vec2 aco = uv;
    aco.y -= f - 0.3;
    aco.y *= (25.0 + abs(sin( 0.1 - uv.x * 5.0 + phase)) * 32.0);
    aco.x *= 1000.0;
    // Fade at x == 0 and x == 1
    float fade = smoothstep(uv.x, 0.0, 0.01) * smoothstep(uv.x, 1.0, 0.99) * smoothstep(uv.y, 1.0, 0.9);

    //vec3 col = lights(aco);
    vec3 col = lights(aco, colTop, colBottom)
            * (smoothstep(0.3, 0.6, pnoise(vec2(10.0 * uv.x + phase, 0.3 * t), 1))
                + 0.5 * smoothstep(0.5, 0.7, pnoise(vec2(10.0 * uv.x, t), 1)))
            * pow(clamp(sin(PI + ((t * 0.1 + uv.x + phase) * PI / 2.0) * pnoise(vec2(10.0 * uv.x, 0.3 * t), 1)) * cos((t * 0.2 + uv.x + phase) * -PI / 3.0), 0.0, 2.0), 1.5);

    fragColor = vec4(clamp(pow(col * fade, vec3(2.0)), 0.0, 1.0), 1.0);
    layerBuffer = vec4(0.0, 0.0, 0.0, 1.0);

    // Logarithmic depth buffer.
    gl_FragDepth = getDepthValue(u_cameraNearFar.y, u_cameraK);
}
