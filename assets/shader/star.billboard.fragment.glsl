#version 330 core

#include <shader/lib/logdepthbuff.glsl>
#include <shader/lib/starnear.glsl>

uniform sampler2D u_texture0;
// Physical radius of the star.
uniform float u_radius;
// Size of the quad (its half extent, in the same units as u_radius).
uniform float u_size;
uniform float u_distance;
// Camera forward vector, in the same (camera-relative) frame as u_pos.
uniform vec3 u_camDir;
uniform float u_time;
uniform float u_zfar;
uniform float u_k;

// v_uv are UV coordinates in [0..1]
in vec2 v_uv;
in vec4 v_color;
in vec3 v_right;
in vec3 v_up;
in vec3 v_view;

layout (location = 0) out vec4 fragColor;
layout (location = 1) out vec4 layerBuffer;

#ifdef ssrFlag
#include <shader/lib/ssr.frag.glsl>
#endif // ssrFlag

// Star sprite from the preferences, the same texture the point stars and the star groups use.
float starSprite(vec2 uv) {
    vec4 c = texture(u_texture0, uv);
    return (c.r + c.g + c.b) / 3.0;
}

// Depth (along the camera forward axis) of the photosphere surface under this fragment.
float starSurfaceDepth(vec2 q, float rho) {
    float cosDir = max(dot(-v_view, u_camDir), 1e-6);
    float wQuad = u_distance * cosDir
                  + u_radius * (q.x * dot(v_right, u_camDir) + q.y * dot(v_up, u_camDir));
    return wQuad - u_radius * sqrt(max(0.0, 1.0 - rho * rho)) * cosDir;
}

void main() {
    // Quad half extent in apparent radii: |q| == 1 is the limb, the rest is corona.
    float k = max(u_size / max(u_radius, 1e-9), 1.0);
    vec2 q = (v_uv - 0.5) * 2.0 * k;
    float rho = length(q);

    // Pixel size in apparent radii, straight from the screen-space derivative.
    float pxR = starPixelFootprint(q);
    float sprite = starSprite(v_uv);
    // 0 while the star is drawn with the sprite, 1 once the model takes over. Shared with the depth pass.
    float resolved = starResolved(pxR);

    #ifdef STAR_DEPTH_ONLY
    // Depth-only pass: writes depth for the solid part only, so emission (corona, chromosphere, halo) occludes nothing.
    if (starAlpha(rho, pxR, sprite, resolved) < 0.5) {
        discard;
    }
    gl_FragDepth = getDepthValue(starSurfaceDepth(q, min(rho, 1.0)), u_zfar, u_k);
    fragColor = vec4(0.0);
    layerBuffer = vec4(0.0);
    return;
    #endif // STAR_DEPTH_ONLY

    StarParams p;
    p.color = v_color.rgb;
    // The seed stays zero: the world-space normal already makes every star look different.
    p.seed = 0.0;
    p.activity = clamp((v_color.r - v_color.b) * 1.2, 0.0, 1.0);
    p.time = u_time;
    p.quadR = k;
    p.right = v_right;
    p.up = v_up;
    p.view = v_view;
    // Star radius over camera distance: the perspective ratio behind the 3D halo.
    p.radiusOverDist = u_radius / max(u_distance, 1e-9);

    vec4 col = starRadiance(q, pxR, p, sprite);

    // Premultiplied alpha: the disc replaces the background, the corona adds to it.
    fragColor = vec4(starTonemap(col.rgb) * v_color.a, col.a * v_color.a);

    // Logarithmic depth buffer
    gl_FragDepth = getDepthValue(u_zfar, u_k);
    // Alpha 0 leaves the layer buffer (lines, labels, grids) untouched; alpha 1 would erase it.
    layerBuffer = vec4(0.0);

    #ifdef ssrFlag
    ssrBuffers();
    #endif // ssrFlag
}
