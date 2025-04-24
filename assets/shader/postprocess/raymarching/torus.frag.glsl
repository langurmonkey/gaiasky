#version 330 core

#include <shader/lib/logdepthbuff.glsl>

// Color buffer
uniform sampler2D u_texture0;
// Depth buffer (log)
uniform sampler2D u_texture1;

uniform vec3 u_pos;
uniform vec3 u_camPos;
uniform vec2 u_zfark;

in vec2 v_texCoords;
in vec3 v_ray;
layout (location = 0) out vec4 fragColor;

// Torus
// t.x: diameter
// t.y: thickness
// Adapted from: http://iquilezles.org/www/articles/distfunctions/distfunctions.htm
float sdTorus(vec3 p, vec2 t) {
    vec2 q = vec2(length(p.xz) - t.x, p.y);
    return length(q) - t.y;
}

// This is the distance field function.  The distance field represents the closest distance to the surface
// of any object we put in the scene.  If the given point (point p) is inside of an object, we return a
// negative answer.
float map(vec3 p) {
    return sdTorus(p, vec2(10.0, 5.0));
}

vec3 calcNormal(in vec3 pos) {
    // epsilon - used to approximate dx when taking the derivative
    const vec2 eps = vec2(0.001, 0.0);

    // The idea here is to find the "gradient" of the distance field at pos
    // Remember, the distance field is not boolean - even if you are inside an object
    // the number is negative, so this calculation still works.
    // Essentially you are approximating the derivative of the distance field at this point.
    vec3 nor = vec3(
    map(pos + eps.xyy) - map(pos - eps.xyy),
    map(pos + eps.yxy) - map(pos - eps.yxy),
    map(pos + eps.yyx) - map(pos - eps.yyx));
    return normalize(nor);
}

// Raymarch along given ray
// ro: ray origin
// rd: ray directionColor
// s: depth buffer
vec4 raymarch(vec3 ro, vec3 rd, float s) {
    vec3 lightDir = vec3(0.0, 1.0, 0.2);
    vec4 ret = vec4(0.0, 0.0, 0.0, 0.0);

    const int maxstep = 128;
    float t = 0; // current distance traveled along ray
    for (int i = 0; i < maxstep; ++i) {
        // If we run past the depth buffer, stop and return nothing (transparent pixel)
        // this way raymarched objects and traditional meshes can coexist.
        if (t >= s) {
            ret = vec4(0.0, 0.0, 0.0, 0.0);
            break;
        }
        vec3 p = ro + rd * t; // World space position of sample
        float d = map(p);       // Sample of distance field (see map())

        // If the sample <= 0, we have hit something (see map()).
        if (d < 0.001) {
            // Lambertian Lighting
            vec3 n = calcNormal(p);
            float dt = dot(-lightDir, n);
            ret = clamp(vec4(dt * 0.2, dt, dt * 0.95, 0.7) + 0.3, 0.0, 1.0);
            break;
        }

        // If the sample > 0, we haven't hit anything yet so we should march forward
        // We step forward by distance d, because d is the minimum distance possible to intersect
        // an object (see map()).
        t += d;
    }
    return ret;
}

void main(){
    // ray direction
    vec3 ray = normalize(v_ray);
    // floating position (camPos - pos)
    vec3 pos = vec3(u_camPos);
    // depth buffer
    float depth = 1.0 / recoverWValue(texture(u_texture1, v_texCoords).r, u_zfark.x, u_zfark.y);
    depth *= length(v_ray);
    // color of pre-existing scene
    vec3 col = texture(u_texture0, v_texCoords).rgb;
    // ray marching
    vec4 rmcol = raymarch(ray, pos, depth);

    // return final color using alpha blending
    fragColor = vec4(col * (1.0 - rmcol.a) + rmcol.rgb * rmcol.a, 1.0);

    // debug ray direction
    //fragColor = clamp(vec4(v_ray.xyz, 1.0) * 0.4 + texture(u_texture0, v_texCoords) * 0.4, 0.0, 1.0);
}
