#version 400 core

uniform mat4 u_projView;
uniform float u_lineWidthTan;

in VS_OUT {
    vec4 color;
} gs_in[];

layout(lines) in;
layout(triangle_strip, max_vertices = 6) out;

out vec4 v_col;
out vec2 v_uv;
out float v_w;

#ifdef velocityBufferFlag
#include shader/lib_velbuffer.vert.glsl
#endif

void main() {
    // Original points.
    dvec4 v1 = gl_in[0].gl_Position;
    dvec4 v2 = gl_in[1].gl_Position;

    // Distance from each point.
    double d1 = length(v1.xyz);
    double d2 = length(v2.xyz);
    // Trick! We set all points in the line at the same distance
    // to avoid UV distortion. Then, we pass to the fragment
    // shader the original position of v2 to use for the depth
    // computation.
    dvec4 v2bak = dvec4(v2);
    v2 = v2 * (d1 / d2);
    d2 = d1;

    // Vector from v1 to v2.
    dvec3 v12 = v2.xyz - v1.xyz;

    // Compute width of each end.
    double w1 = u_lineWidthTan * d1;
    double w2 = u_lineWidthTan * d2;

    // Vector orthogonal to each end, with the right widths.
    dvec3 c1 = normalize(cross(v1.xyz, v12)) * w1;
    dvec3 c2 = normalize(cross(v2.xyz, v12)) * w2;

    dvec4 col1 = gs_in[0].color;
    dvec4 col2 = gs_in[1].color;

    // ## First vertex.
    v_col = vec4(col1);

    gl_Position = u_projView * vec4(v1.xyz + c1, v1.w);
    v_w = gl_Position.w;
    v_uv = vec2(0.0, 0.0);
    #ifdef velocityBufferFlag
    velocityBufferCam(gl_Position, vec3(v1.xyz + c1));
    #endif
    EmitVertex();

    gl_Position = u_projView * vec4(v1.xyz - c1, v1.w);
    v_w = gl_Position.w;
    v_uv = vec2(0.0, 1.0);
    #ifdef velocityBufferFlag
    velocityBufferCam(gl_Position, vec3(v1.xyz - c1));
    #endif
    EmitVertex();

    // ## Second vertex.
    v_col = vec4(col2);
    v_w = (u_projView * vec4(v2bak)).w;

    gl_Position = u_projView * vec4(v2.xyz + c2, v2.w);
    v_uv = vec2(1.0, 0.0);
    #ifdef velocityBufferFlag
    velocityBufferCam(gl_Position, vec3(v2bak.xyz + c2));
    #endif
    EmitVertex();

    gl_Position = u_projView * vec4(v2.xyz - c2, v2.w);
    v_uv = vec2(1.0, 1.0);
    #ifdef velocityBufferFlag
    velocityBufferCam(gl_Position, vec3(v2bak.xyz - c2));
    #endif
    EmitVertex();

    EndPrimitive();
}
