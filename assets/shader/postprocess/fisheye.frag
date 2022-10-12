// Simple fisheye projection shader by Toni Sagrista
// License: MPL2
#version 330 core

uniform sampler2D u_texture0;
uniform vec2 u_viewport;
uniform float u_fov;
// 0 - default
// 1 - accurate (with fov, no full coverage)
uniform int u_mode;

in vec2 v_texCoords;
layout (location = 0) out vec4 fragColor;

#define PI 3.1415926535

void main()
{
    vec2 vp = u_viewport;

    vec2 texCoords = v_texCoords;
    // tc in v_texCoords in [-1, 1]
    vec2 tc = (texCoords * 2.0 - 1.0);
    vec2 arv = vp.xy / min(vp.x, vp.y);
    // Coordinates of current fragment
    vec2 xy = tc * arv;
    // Distance from centre to current fragment
    float d = length(xy);
    if (d < 1.0) {

        float z = sqrt(1.0 - d * d);
        if (u_mode == 0) {
            float arx = min(1.0, vp.y / vp.x);
            float ary = min(1.0, vp.x / vp.y);

            float r = atan(d, z) / PI;
            float phi = atan(xy.y, xy.x);

            vec2 uv;
            uv.x = (r * cos(phi) + 0.5) * arx + (1.0 - arx) / 2.0;
            uv.y = (r * sin(phi) + 0.5) * ary + (1.0 - ary) / 2.0;
            fragColor = texture(u_texture0, uv);
        } else {
            float a = 1.0 / (z * tan(u_fov * 0.5));
            fragColor = texture(u_texture0, (tc * a) + 0.5);
        }

    } else {
        fragColor = vec4(0.0, 0.0, 0.0, 1.0);
    }

}