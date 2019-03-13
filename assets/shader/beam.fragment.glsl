// UNIFORMS

// Diffuse base texture
uniform sampler2D u_diffuseTexture;
// Grayscale lookup table
uniform sampler2D u_normalTexture;
// Diffuse color
uniform vec4 u_diffuseColor;

// VARYINGS

// Time in seconds
varying float v_time;
// Ambient color (star color in this case)
varying vec3 v_lightDiffuse;
// The normal
varying vec3 v_normal;
// Coordinate of the texture
varying vec2 v_texCoords0;
// Opacity
varying float v_opacity;
// View vector
varying vec3 v_viewVec;
// Color
varying vec4 v_color;


#define time v_time * 0.003


void main() {
    float softedge = pow(dot(normalize(v_normal), normalize(vec3(v_viewVec))), 2.0) * 1.0;
    softedge = clamp(softedge, 0.0, 1.0);
    gl_FragColor = vec4(u_diffuseColor.rgb, u_diffuseColor.a * (1.0 - v_texCoords0.y) * softedge);
}