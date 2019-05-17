#version 330 core

////////////////////////////////////////////////////////////////////////////////////
////////// POSITION ATTRIBUTE - FRAGMENT
////////////////////////////////////////////////////////////////////////////////////
#define nop() {}

uniform vec4 u_diffuseColor;

in vec4 v_position;
#define pullPosition() { return v_position;}

// Varyings computed in the vertex shader
in float v_opacity;

in vec3 v_viewDir;

in float v_depth;
out vec4 fragColor;

void main() {
    vec4 diffuse = u_diffuseColor;

	// Normal in pixel space
	vec3 N = vec3(0.0, 0.0, 1.0);

    // see http://http.developer.nvidia.com/CgTutorial/cg_tutorial_chapter05.html
    vec3 V = normalize(v_viewDir);

    vec3 baseColor = diffuse.rgb;
    float edge = pow(max(0.0, dot(N, V)), 3.0);

    fragColor = vec4(baseColor * edge, 1.0) * v_opacity;

    // Prevent saturation
    fragColor = clamp(fragColor, 0.0, 1.0);

    if(fragColor.a == 0.0){
        discard;
    }
    gl_FragDepth = v_depth;
}
