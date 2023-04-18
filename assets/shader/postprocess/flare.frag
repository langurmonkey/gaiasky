// Lens flare implementation by Toni Sagrista
// From John Chapman's article http://john-chapman-graphics.blogspot.co.uk/2013/02/pseudo-lens-flare.html
#version 330 core

// Scene
uniform sampler2D u_texture0;
// Lens dirt
uniform sampler2D u_texture1;

uniform vec2 u_viewportInverse;
uniform int u_ghosts; // number of ghost samples
uniform float u_haloWidth;
float u_ghostDispersal = 0.4;
float u_aberrationAmount = 3.5;

in vec2 v_texCoords;
layout (location = 0) out vec4 fragColor;

/*----------------------------------------------------------------------------*/
vec4 textureDistorted(sampler2D tex, vec2 texcoord, vec2 direction,	vec3 distortion) {
	return vec4(
		texture(tex, texcoord + direction * distortion.r).r,
		texture(tex, texcoord + direction * distortion.g).g,
		texture(tex, texcoord + direction * distortion.b).b,
		1.0
	);
}

/*----------------------------------------------------------------------------*/
void main() {
    vec2 texcoord = -v_texCoords + vec2(1.0);
    vec2 texelSize = u_viewportInverse;
    
    // ghost vector to image centre:
    vec2 ghostVec = (vec2(0.5) - texcoord) * u_ghostDispersal;
    vec2 haloVec = normalize(ghostVec) * u_haloWidth;
    	
    vec3 distortion = vec3(-texelSize.x * u_aberrationAmount, 0.0, texelSize.x * u_aberrationAmount);
    
    vec4 result = vec4(0.0);
    for (int i = 0; i < u_ghosts; i = i + 1) { 
		vec2 offset = fract(texcoord + ghostVec * float(i));
		
		float weight = length(vec2(0.5) - offset) / length(vec2(0.5));
		weight = pow(1.0 - weight, 2.0);
		
		result += textureDistorted(
					u_texture0,
					offset,
					normalize(ghostVec),
					distortion
				) * weight;
    }
    result *= texture(u_texture1, vec2(length(vec2(0.5) - texcoord) / length(vec2(0.5))));
  
    // sample halo
    float weight = length(vec2(0.5) - fract(texcoord + haloVec)) / length(vec2(0.5));
    weight = pow(1.0 - weight, 3.0);
    result += textureDistorted(
	    u_texture0,
	    fract(texcoord + haloVec),
	    normalize(ghostVec),
	    distortion
    ) * weight;

    // Hack to prevent too strong halo
    fragColor = min(vec4(0.7), result);
}
