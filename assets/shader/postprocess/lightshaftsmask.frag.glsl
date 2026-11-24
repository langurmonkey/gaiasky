#version 330 core
// Light shafts (god rays), pass 1: builds the light mask of the frame.

// Scene depth buffer, logarithmic.
uniform sampler2D u_texture0;
// Z-far and K of the logarithmic depth buffer, to recover a distance from a depth value.
uniform vec2 u_zFarK;
// Position of the light on screen, in [0,1].
uniform vec2 u_lightPos;
// Distance from the camera to the light, along the view direction.
uniform float u_lightDepth;
// Radius, in uv, of the lit halo around the light.
uniform float u_lightRadius;

in vec2 v_texCoords;
layout (location = 0) out vec4 fragColor;

#include <shader/lib/logdepthbuff.glsl>

void main() {
    // Distance along the view direction of the geometry at this pixel.
    float depth = 1.0 / recoverWValue(texture(u_texture0, v_texCoords).r, u_zFarK.x, u_zFarK.y);

    // Geometry in front of the light blocks it. The tolerance is relative, so it holds at any scale.
    float edge = u_lightDepth * 1.0e-2;
    float visible = smoothstep(u_lightDepth - 2.0 * edge, u_lightDepth - edge, depth);

    // Lit halo around the light: a Gaussian, so the radial blur of the next pass has something to spread.
    vec2 d = v_texCoords - u_lightPos;
    float r2 = dot(d, d) / (u_lightRadius * u_lightRadius);
    float halo = exp(-r2);

    fragColor = vec4(vec3(visible * halo), 1.0);
}
