#version 330 core

#include shader/lib_dither8x8.glsl
#include shader/lib_logdepthbuff.glsl

////////////////////////////////////////////////////////////////////////////////////
////////// POSITION ATTRIBUTE - FRAGMENT
////////////////////////////////////////////////////////////////////////////////////
#define nop() {}

uniform vec4 u_diffuseColor;
uniform vec2 u_cameraNearFar;
uniform float u_cameraK;

in vec4 v_position;
#define pullPosition() { return v_position;}
in float v_opacity;
in vec3 v_viewDir;

layout (location = 0) out vec4 fragColor;

#ifdef velocityBufferFlag
#include shader/lib_velbuffer.frag.glsl
#endif

void main() {
    vec4 diffuse = u_diffuseColor;

	// Normal in pixel space
	vec3 N = vec3(0.0, 0.0, 1.0);

    // see http://http.developer.nvidia.com/CgTutorial/cg_tutorial_chapter05.html
    vec3 V = normalize(v_viewDir);

    vec3 baseColor = diffuse.rgb;
    float edge = min(1.0, pow(max(0.0, abs(dot(N, V))), 1.0) * 1.2);

    fragColor = vec4(baseColor.rgb, edge) * v_opacity;


    // Prevent saturation
    fragColor = clamp(fragColor, 0.0, 1.0);

    if(fragColor.a == 0.0 || dither(gl_FragCoord.xy, fragColor.a) < 0.5){
        discard;
    } else {
        gl_FragDepth = getDepthValue(u_cameraNearFar.y, u_cameraK);

        #ifdef velocityBufferFlag
        velocityBuffer();
        #endif
    }
}
