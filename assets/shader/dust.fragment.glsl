#version 120

////////////////////////////////////////////////////////////////////////////////////
////////// POSITION ATTRIBUTE - FRAGMENT
////////////////////////////////////////////////////////////////////////////////////
#define nop() {}

varying vec4 v_position;
#define pullPosition() { return v_position;}

////////////////////////////////////////////////////////////////////////////////////
////////// COLOR ATTRIBUTE - FRAGMENT
///////////////////////////////////////////////////////////////////////////////////
varying vec4 v_color;

////////////////////////////////////////////////////////////////////////////////////
////////// NORMAL ATTRIBUTE - FRAGMENT
///////////////////////////////////////////////////////////////////////////////////
varying vec3 v_normal;
vec3 g_normal = vec3(0.0, 0.0, 1.0);
#define pullNormal() g_normal = v_normal

////////////////////////////////////////////////////////////////////////////////////
////////// BINORMAL ATTRIBUTE - FRAGMENT
///////////////////////////////////////////////////////////////////////////////////
varying vec3 v_binormal;
vec3 g_binormal = vec3(0.0, 0.0, 1.0);
#define pullBinormal() g_binormal = v_binormal

////////////////////////////////////////////////////////////////////////////////////
////////// TANGENT ATTRIBUTE - FRAGMENT
///////////////////////////////////////////////////////////////////////////////////
varying vec3 v_tangent;
vec3 g_tangent = vec3(1.0, 0.0, 0.0);
#define pullTangent() g_tangent = v_tangent

////////////////////////////////////////////////////////////////////////////////////
////////// TEXCOORD0 ATTRIBUTE - FRAGMENT
///////////////////////////////////////////////////////////////////////////////////
#define exposure 4.0

varying vec2 v_texCoord0;

// Uniforms which are always available
uniform mat4 u_projViewTrans;

uniform mat4 u_worldTrans;

uniform vec4 u_cameraPosition;

uniform mat3 u_normalMatrix;

// Varyings computed in the vertex shader
varying float v_opacity;
varying float v_alphaTest;

// Other uniforms
#ifdef shininessFlag
uniform float u_shininess;
#else
const float u_shininess = 20.0;
#endif

#ifdef diffuseColorFlag
uniform vec4 u_diffuseColor;
#endif

#if defined(specularFlag) || defined(fogFlag)
#define cameraPositionFlag
#endif

varying vec3 v_lightCol;
varying vec3 v_viewDir;

#define saturate(x) clamp(x, 0.0, 1.0)

#define PI 3.1415926535

varying float v_depth;

void main() {
    vec2 g_texCoord0 = v_texCoord0;

    vec4 diffuse = v_color;

	// Normal in pixel space
	vec3 N = vec3(0.0, 0.0, 1.0);

    // see http://http.developer.nvidia.com/CgTutorial/cg_tutorial_chapter05.html
    vec3 V = normalize(v_viewDir);

    vec3 baseColor = diffuse.rgb;
    float edge = pow(max(0.0, dot(N, V)), 3.0);

    gl_FragColor = vec4(baseColor * edge, edge);

    // Prevent saturation
    gl_FragColor = clamp(gl_FragColor, 0.0, 1.0);
    gl_FragColor.rgb *= 0.95;

    if(gl_FragColor.a == 0.0){
        discard;
    }

    // Normal depth buffer
    gl_FragDepth = gl_FragCoord.z;
    // Logarithmic depth buffer
    gl_FragDepth = v_depth;

    // Debug! - vectors
    //float theta = acos(L.z); // in [0..Pi]
    //float phi = atan(L.y/L.x); // in [0..2Pi]
    //vec4 debugcol = vec4(0.0, L.y, 0.0, 1.0);
    //gl_FragColor = debugcol;

    // Debug! - visualise depth buffer
    //gl_FragColor = vec4(vec3(gl_FragCoord.z), 1.0f);
}
