#version 330 core

#include <shader/lib/logdepthbuff.glsl>

// Color buffer
uniform sampler2D u_texture0;
// Depth buffer (log)
uniform sampler2D u_texture1;
// Noise buffer
uniform sampler2D u_texture2;
// Time in seconds
uniform float u_time;
// Floating position
uniform vec3 u_pos;
// Zfar
uniform vec2 u_zfark;
// Viewport
uniform vec2 u_viewport;
// Use additional.x for bend scaling
uniform vec4 u_additional;

in vec2 v_texCoords;
in vec3 v_ray;
layout (location = 0) out vec4 fragColor;


/**
 GLSL implementation of volumetric cloud rendering as
 described in http://www.blog.sirenix.net/blog/realtime-volumetric-clouds-in-unity
**/
#define PI 3.14159265359
#define iterations 100.0
#define octaves 3
#define cloudintensity 3.0

float cloudDensity = 0.3;
float viewDistance = 6.0;
vec3 cloudColor = vec3(0.6, 0.8, 1.0);

float noise(in vec3 x) {
    vec3 p = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);

    vec2 uv = (p.xy + vec2(37.0, 17.0) * p.z) + f.xy;
    vec2 rg = texture(u_texture2, (uv + 0.5) / 256.0).yx;
    return mix(rg.x, rg.y, f.z);
}


float fbm(vec3 pos) {
    float f = 0.;
    for (int i = 0; i < octaves; i++) {
        f += noise(pos) / pow(2.0, float(i + 1));
        pos *= 2.01;
    }
    f = f / (1.0 - 1.0 / pow(2.0, float(octaves + 1)));
    return f;
}

vec4 raymarch(in vec3 ray, in vec3 pos, in float s) {
    // So now we have a position, and a ray defined for our current fragment, and we know from earlier in this article that it matches the field of view and aspect ratio of the camera. And we can now start iterating and creating our clouds.
    // We will not be ray-marching twoards any distance field in this example. So the following code should be much easier to understand.
    // pos is our original position, and p is our current position which we are going to be using later on.
    vec3 p = pos;
    // For each iteration, we read from our noise function the density of our current position, and adds it to this density variable.
    float density = 0.0;

    for (float i = 0.0; i < iterations; i++) {
        // f gives a number between 0 and 1.
        // We use that to fade our clouds in and out depending on how far and close from our camera we are.
        float f = i / iterations;
        // And here we do just that:
        float alpha = smoothstep(0.0, iterations * 0.2, i) * (1.0 - f) * (1.0 - f);
        // Note that smoothstep here doesn't do the same as Mathf.SmoothStep() in Unity C# - which is frustrating btw. Get a grip Unity!
        // Smoothstep in shader languages interpolates between two values, given t, and returns a value between 0 and 1.
        // To get a bit of variety in our clouds we collect two different samples for each iteration.
        float denseClouds = smoothstep(cloudDensity, 0.75, fbm(p));
        float lightClouds = (smoothstep(-0.2, 1.2, fbm(p * 2.0)) - 0.5) * 0.5;
        // Note that I smoothstep again to tell which range of the noise we should consider clouds.
        // Here we add our result to our density variable
        density += (lightClouds + denseClouds) * alpha;
        // And then we move one step further away from the camera.
        p = pos + ray * f * viewDistance;
    }
    // And here I just melted all our variables together with random numbers until I had something that looked good.
    // You can try playing around with them too.
    float l = (density / iterations) * cloudintensity;
    vec3 color = cloudColor.rgb * l;

    return vec4(color, 1.0);
}

float luma(vec4 color) {
    return dot(color.rgb, vec3(0.299, 0.587, 0.114));
}

void main() {
    // ray direction
    vec3 ray = normalize(v_ray);
    // floating position (camPos - pos)
    vec3 pos = u_pos * 1000.0;
    // depth buffer
    float depth = 1.0 / recoverWValue(texture(u_texture1, v_texCoords).r, u_zfark.x, u_zfark.y);
    depth *= length(ray);
    // color of pre-existing scene
    vec3 col = texture(u_texture0, v_texCoords).rgb;
    // ray marching
    vec4 rmcol = raymarch(ray, pos, depth);

    // return final color using alpha blending
    fragColor = vec4(col * (1.0 - rmcol.a) + rmcol.rgb * rmcol.a, 1.0);
    //fragColor = vec4(rmcol.rgb, 1.0);
}