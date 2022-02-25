// Simple motion blur implementation by Toni Sagrista
// License: MPL2
#version 330 core

// Unprocessed image
uniform sampler2D u_texture0;
// Last frame
uniform sampler2D u_texture1;
// Last frame alpha
uniform float u_blurOpacity;

// Resolution
uniform vec2 u_resolution;
// Blur amount
uniform float u_blurRadius;

in vec2 v_texCoords;
layout (location = 0) out vec4 fragColor;

vec4 blur(vec2 dir, sampler2D tex, vec2 tc, float radius, float resolution){
    vec4 sum = vec4(0.0);

    //the amount to blur, i.e. how far off center to sample from
    //1.0 -> blur by one pixel
    //2.0 -> blur by two pixels, etc.
    float blur = radius / resolution;


    //the direction of our blur
    //(1.0, 0.0) -> x-axis blur
    //(0.0, 1.0) -> y-axis blur
    float hstep = dir.x;
    float vstep = dir.y;

    //apply blurring, using a 9-tap filter with predefined gaussian weights

    sum += texture(tex, vec2(tc.x - 4.0 * blur * hstep, tc.y - 4.0 * blur * vstep)) * 0.0162162162;
    sum += texture(tex, vec2(tc.x - 3.0 * blur * hstep, tc.y - 3.0 * blur * vstep)) * 0.0540540541;
    sum += texture(tex, vec2(tc.x - 2.0 * blur * hstep, tc.y - 2.0 * blur * vstep)) * 0.1216216216;
    sum += texture(tex, vec2(tc.x - 1.0 * blur * hstep, tc.y - 1.0 * blur * vstep)) * 0.1945945946;

    sum += texture(tex, vec2(tc.x, tc.y)) * 0.2270270270;

    sum += texture(tex, vec2(tc.x + 1.0 * blur * hstep, tc.y + 1.0 * blur * vstep)) * 0.1945945946;
    sum += texture(tex, vec2(tc.x + 2.0 * blur * hstep, tc.y + 2.0 * blur * vstep)) * 0.1216216216;
    sum += texture(tex, vec2(tc.x + 3.0 * blur * hstep, tc.y + 3.0 * blur * vstep)) * 0.0540540541;
    sum += texture(tex, vec2(tc.x + 4.0 * blur * hstep, tc.y + 4.0 * blur * vstep)) * 0.0162162162;

    return sum;
}

void main() {
    // Horizontal blur
    vec4 sampl = blur(vec2(1.0, 0.0), u_texture1, v_texCoords, u_blurRadius, u_resolution.x) * 0.5;
    // Vertical blur
    sampl += blur(vec2(0.0, 1.0), u_texture1, v_texCoords, u_blurRadius, u_resolution.y) * 0.5;

    fragColor = max(texture(u_texture0, v_texCoords), sampl * u_blurOpacity);
}