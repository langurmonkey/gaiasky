// Normal filtered anti-aliasing.
// See http://blenderartists.org/forum/showthread.php?209574-Full-Screen-Anti-Aliasing-(NFAA-DLAA-SSAA)
// and http://www.gamedev.net/topic/580517-nfaa---a-post-process-anti-aliasing-filter-results-implementation-details/
// Copyright Styves, Martinsh
// Modified by Sagrista, Toni
#version 330 core

#include shader/lib_luma.glsl
#include shader/lib_normal.glsl

uniform sampler2D u_texture0;
// The inverse of the viewport dimensions along X and Y
uniform vec2 u_viewportInverse;

in vec2 v_texCoords;
layout (location = 0) out vec4 fragColor;

vec4 nfaa(sampler2D tex, vec2 texCoords, vec2 viewportInverse){
    vec2 normal = normalMap(tex, texCoords) * viewportInverse;

    // Color
    vec4 scene0 = texture(tex, texCoords.xy);
    vec4 scene1 = texture(tex, texCoords.xy + normal.xy);
    vec4 scene2 = texture(tex, texCoords.xy - normal.xy);
    vec4 scene3 = texture(tex, texCoords.xy + vec2(normal.x, -normal.y) * 0.5);
    vec4 scene4 = texture(tex, texCoords.xy - vec2(normal.x, -normal.y) * 0.5);
    
    // Final color
    return vec4((scene0.rgb + scene1.rgb + scene2.rgb + scene3.rgb + scene4.rgb) * 0.2, 1.0);
    
    // Debug
    //return vec4(normalize(vec3(normal.x / viewportInverse.x, normal.y / viewportInverse.y , 1.0) * 0.5 + 0.5), 1.0);
	    
}

float GetColorLuminance(vec3 i_vColor){
    return dot(i_vColor, vec3(0.2126, 0.7152, 0.0722));
}

void main(){  
    fragColor = nfaa(u_texture0, v_texCoords, u_viewportInverse);
}
