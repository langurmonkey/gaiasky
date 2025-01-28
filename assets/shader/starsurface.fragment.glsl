#version 330 core
// UNIFORMS

uniform vec2 u_cameraNearFar;
uniform float u_cameraK;

// VARYINGS

// Time in seconds
in float v_time;
// Ambient color (star color in this case)
in vec3 v_lightDiffuse;
// The normal
in vec3 v_normal;
// Coordinate of the texture
in vec2 v_texCoords0;
// Opacity
in float v_opacity;
// View vector
in vec3 v_viewVec;

#include <shader/lib/logdepthbuff.glsl>

layout(location = 0) out vec4 fragColor;
layout(location = 1) out vec4 layerBuffer;

#define time v_time * 0.001

#include <shader/lib/luma.glsl>
#include <shader/lib/noise/common.glsl>
#include <shader/lib/noise/curl.glsl>

#ifdef ssrFlag
#include <shader/lib/ssr.frag.glsl>
#endif // ssrFlag

// Produces a triangle wave between 0 and 1.
float triangle_wave(float x) {
    return 2.0 * abs(x - floor(x + 0.5));
}

void main() {
    // Perimeter is 1 when normal faces camera, 0 when normal is 90 degrees from view.
    float perimeter = dot(normalize(v_normal), vec3(v_viewVec)) * 0.6;
    vec3 percolor = v_lightDiffuse * min(1.0, perimeter + 0.5);

    // Star surface
    gln_tFBMOpts opts = gln_tFBMOpts(0.3245, // seed
            0.1, // amplitude
            1.4, // persistence
            80.0, // frequency
            2.0, // lacunarity
            vec3(4.0, 4.0, 4.0), // scale
            1.0, // power
            1, // octaves
            false,
            false);
    // Sample point.
    vec2 viewport = vec2(1500.0, 750.0);
    vec2 xy = v_texCoords0 * viewport;
    // Sample on the surface of a sphere.
    float phiStep = gln_PI / (viewport.y - 1);
    float phi = (-gln_PI / 2.0) + xy.y * phiStep;
    float r = triangle_wave(time) * 0.2 + 1.0;
    float thetaStep = gln_PI * 2.0 / viewport.x;
    float theta = xy.x * thetaStep;
    float cosPhi = cos(phi);
    // P is a point in the sphere.
    vec3 p = vec3(
            cosPhi * cos(theta) + r,
            cosPhi * sin(theta),
            sin(phi)
        );

    float n = (gln_cfbm(p, opts) + 1.6) * 0.12;

    // Sunspots
    float s = 0.47;
    float un_radius = 2.3;
    vec3 spos = p * un_radius;
    float frequency = 1.6;
    float f1 = gln_simplex_curl(spos * frequency);
    float f2 = gln_simplex_curl((spos + un_radius) * frequency);
    float t1 = f1 - s;
    float t2 = f2 - s;
    float ss = (max(t1, 0.0) * max(t2, 0.0)) * 4.0;

    float ss2 = pow(ss * 1.5, 5.0);

    // Accumulate total noise
    float total = n * (1.0 - ss) - ss2;
    // float total = n - ss2;

    vec3 color = vec3(total, total, total);
    fragColor = vec4(min(vec3(0.9), color * 6.0 * v_lightDiffuse * percolor), v_opacity);

    gl_FragDepth = getDepthValue(u_cameraNearFar.y, u_cameraK);
    layerBuffer = vec4(0.0, 0.0, 0.0, 1.0);

    #ifdef ssrFlag
    ssrBuffers();
    #endif // ssrFlag
}
