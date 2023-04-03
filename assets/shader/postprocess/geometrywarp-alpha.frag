// Geometry warping and blending post-processor effect for
// the warp mesh format (spherical mirror projection).
#version 330 core

// Scene
uniform sampler2D u_texture0;

uniform vec2 u_viewport;

in vec2 v_texCoords;
in float v_intensity;
layout (location = 0) out vec4 fragColor;

void main() {
    vec4 col;
    vec2 uv = v_texCoords;
    float ar = u_viewport.y / u_viewport.x;
    uv.x = uv.x * ar + (1.0 - ar) / 2.0;
    // Warp is done by vertex mesh.
    col = texture(u_texture0, uv);
    // Multiply intensity.
    fragColor = vec4(col.rgb * v_intensity, 1.0);
}
