#version 330 core

#include <shader/lib/dither8x8.glsl>
#include <shader/lib/logdepthbuff.glsl>

uniform vec4 u_diffuseColor;
uniform vec2 u_cameraNearFar;
uniform float u_cameraK;

// INPUT
struct VertexData {
    vec2 texCoords;
    vec3 normal;
    vec3 viewDir;
    vec3 ambientLight;
    float opacity;
    vec4 color;
    #ifdef shadowMapFlag
    vec3 shadowMapUv;
    #ifdef numCSM
        vec3 csmUVs[numCSM];
    #endif // numCSM
    #endif// shadowMapFlag
    vec3 fragPosWorld;
    #ifdef metallicFlag
    vec3 reflect;
    #endif// metallicFlag
    mat3 tbn;
};
in VertexData v_data;

// OUTPUT
layout (location = 0) out vec4 fragColor;

#ifdef ssrFlag
#include <shader/lib/ssr.frag.glsl>
#endif // ssrFlag

void main() {
    vec4 diffuse = u_diffuseColor;

	// Normal in pixel space
	vec3 N = vec3(0.0, 0.0, 1.0);

    // see http://http.developer.nvidia.com/CgTutorial/cg_tutorial_chapter05.html
    vec3 V = normalize(v_data.viewDir);

    vec3 baseColor = diffuse.rgb;
    float edge = min(1.0, pow(max(0.0, abs(dot(N, V))), 1.0) * 1.2);

    fragColor = vec4(baseColor.rgb, edge) * v_data.opacity;


    // Prevent saturation
    fragColor = clamp(fragColor, 0.0, 1.0);

    if(fragColor.a == 0.0 || dither(gl_FragCoord.xy, fragColor.a) < 0.5){
        discard;
    } else {
        gl_FragDepth = getDepthValue(u_cameraNearFar.y, u_cameraK);

        #ifdef ssrFlag
        ssrBuffers();
        #endif // ssrFlag
    }
}
