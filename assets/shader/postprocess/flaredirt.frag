#version 330 core
// Lens flare implementation by Toni Sagrista
// License: MPL2
// From John Chapman's article http://john-chapman-graphics.blogspot.co.uk/2013/02/pseudo-lens-flare.html

// Scene
uniform sampler2D u_texture0;
// Lens dirt
uniform sampler2D u_texture1;
// Lens starburst
uniform sampler2D u_texture2;

// Starburst offset
uniform float u_starburstOffset;

in vec2 v_texCoords;
layout (location = 0) out vec4 fragColor;

/*----------------------------------------------------------------------------*/
void main() {
    vec2 texCoords = v_texCoords;
    
    vec4 base = texture(u_texture0, texCoords);
    vec4 dirt = texture(u_texture1, texCoords);

    vec2 centerVec = v_texCoords - 0.5;
    float d = length(centerVec);
    float radial = centerVec.x / d;
    float starburst = texture(u_texture2, vec2(mod(abs(radial - u_starburstOffset), 1.0), 0.0)).r
               * texture(u_texture2, vec2(mod(abs(-radial + u_starburstOffset), 1.0), 0.0)).r;

    starburst = clamp(starburst + (1.0 - smoothstep(0.0, 0.4, d)), 0.0, 1.0);

    fragColor = base * (dirt + starburst);
}
