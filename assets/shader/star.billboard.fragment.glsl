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
in vec3 v_skyRight;
in vec3 v_skyUp;
in float v_cosLat;

layout (location = 0) out vec4 fragColor;
layout (location = 1) out vec4 layerBuffer;

#ifdef ssrFlag
#include <shader/lib/ssr.frag.glsl>
#endif // ssrFlag

// Star sprite picked in the preferences, the same texture the point stars and
// the star groups use. In the near field it is the unresolved star, and the
// quad is sized so that it subtends a sensible angle.
float starSprite(vec2 uv) {
    vec4 c = texture(u_texture0, uv);
    return (c.r + c.g + c.b) / 3.0;
}

// Depth (along the camera forward axis, which is what the log depth buffer
// encodes) of the photosphere surface the fragment is looking at. The fragment
// sits on the quad plane through the star centre; the surface is that same
// point moved towards the camera by the sphere's sagitta at this offset.
float starSurfaceDepth(vec2 q, float rho) {
    float cosDir = max(dot(-v_view, u_camDir), 1e-6);
    float wQuad = u_distance * cosDir
                  + u_radius * (q.x * dot(v_right, u_camDir) + q.y * dot(v_up, u_camDir));
    return wQuad - u_radius * sqrt(max(0.0, 1.0 - rho * rho)) * cosDir;
}

void main() {
    // Quad half extent in apparent radii: the photosphere limb is |q| == 1, and
    // the rest of the quad is corona. This is what makes the look independent of
    // the quad size, unlike the old UV-space formulation.
    float k = max(u_size / max(u_radius, 1e-9), 1.0);
    vec2 q = (v_uv - 0.5) * 2.0 * k;
    float rho = length(q);

    // Pixel size in apparent radii, straight from the screen-space derivative.
    float pxR = starPixelFootprint(q);
    float sprite = starSprite(v_uv);
    // 0 while the star is drawn with the sprite, 1 once the procedural
    // photosphere is big enough on screen to be worth drawing. Shared with the
    // depth pass so that both agree on which representation is on screen.
    float resolved = starResolved(pxR);

    #ifdef STAR_DEPTH_ONLY
    // Depth-only pass. Writes the depth of the solid part of the star, so that
    // lines, labels and every other late element is occluded by it, exactly as
    // the old sphere mesh did. Everything the colour pass draws as emission
    // (the corona, the chromosphere, the halo of the sprite) must not occlude
    // anything, so it is discarded. Same test as the colour pass, so the two
    // always agree on what is solid.
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
    // Anything derived from the world position would make the pattern swim as
    // the camera moves, so the seed stays zero and the world-space normal does
    // the job of making every star look different.
    p.seed = 0.0;
    p.activity = clamp((v_color.r - v_color.b) * 1.2, 0.0, 1.0);
    p.time = u_time;
    p.quadR = k;
    p.right = v_right;
    p.up = v_up;
    p.view = v_view;
    p.skyRight = v_skyRight;
    p.skyUp = v_skyUp;
    p.cosLat = v_cosLat;

    vec4 col = starRadiance(q, pxR, p, sprite);

    // Premultiplied alpha blending (GL_ONE, GL_ONE_MINUS_SRC_ALPHA): the
    // photosphere replaces the background, the corona is added to it.
    fragColor = vec4(starTonemap(col.rgb) * v_color.a, col.a * v_color.a);

    // Logarithmic depth buffer
    gl_FragDepth = getDepthValue(u_zfar, u_k);
    // Leave the layer buffer (lines, labels, grids) untouched. It must be left
    // at alpha 0: with the premultiplied blend this pass uses, writing the
    // vec4(0,0,0,1) that the additive shaders use would evaluate to
    // dst * (1 - 1) + 0, i.e. it would erase every line and label inside the
    // quad. With alpha 0 the destination is passed through unchanged.
    layerBuffer = vec4(0.0);

    #ifdef ssrFlag
    ssrBuffers();
    #endif // ssrFlag
}
