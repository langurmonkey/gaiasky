#version 330 core

uniform mat4 u_projView;
uniform float u_lineWidthTan;

in VS_OUT {
    vec4 color;
} gs_in[];

layout(lines) in;
layout(triangle_strip, max_vertices = 6) out;

out vec4 v_col;
out vec2 v_uv;

// Computes the closest distance of the segment p0-p1 to the point p.
float dist_point_segment(vec3 p1, vec3 p2, vec3 p) {
    vec3 v = p1;
    vec3 w = p2;

    vec3 aux3 = p - v;
    vec3 aux4 = w - v;

    // Return minimum distance between line segment vw and point p.
    float l2 = length(aux4);
    if (l2 == 0.0)
    return length(aux3);// v == w case
    // Consider the line extending the segment, parameterized as v + t (w - v).
    // We find projection of point p onto the line.
    // It falls where t = [(p-v) . (w-v)] / |w-v|^2
    float t = dot(aux3, aux4) / l2;
    if (t < 0.0){
        return length(v - p);// Beyond the 'v' end of the segment.
    } else if (t > 1.0){
        return length(w - p);// Beyond the 'w' end of the segment.
    }
    vec3 projection = v + aux4 * t;// Projection falls on the segment
    return length(projection - p);
}


vec3 closest_point(vec3 p1, vec3 p2, vec3 p) {
    //           (P2-P1)dot(v)
    //Pr = P1 +  ------------- * v.
    //           (v)dot(v)
    vec3 v = p2 - p1;

    float nomin = dot(p - p1, v);
    float denom = dot(v, v);
    vec3 frac = v * (nomin / denom);

    return p1 + frac;
}

#define SUBDIVIDE false

void main() {
    // Original points.
    vec4 v1 = gl_in[0].gl_Position;
    vec4 v2 = gl_in[1].gl_Position;

    // Distance from each point.
    float d1 = length(v1.xyz);
    float d2 = length(v2.xyz);

    // Closest distance to segment.
    // If the camera position (0,0,0) is closest to the
    // line than any of the two ends, we split the line at the closest point
    // to the camera.
    float dist = dist_point_segment(v1.xyz, v2.xyz, vec3(0.0));

    // Compute width of each end.
    float w1 = u_lineWidthTan * d1;
    float w2 = u_lineWidthTan * d2;

    // Vector from v1 to v2.
    vec3 v12 = normalize(v2.xyz - v1.xyz);

    // Vector orthogonal to each end, with the right widths.
    vec3 c1 = normalize(cross(v1.xyz, v12)) * w1;
    vec3 c2 = normalize(cross(v2.xyz, v12)) * w2;

    vec4 col1 = gs_in[0].color;
    vec4 col2 = gs_in[1].color;

    // ## First vertex.
    v_col = col1;

    gl_Position = u_projView * vec4(v1.xyz + c1, v1.w);
    v_uv = vec2(0.0, 0.0);
    EmitVertex();

    gl_Position = u_projView * vec4(v1.xyz - c1, v1.w);
    v_uv = vec2(0.0, 1.0);
    EmitVertex();

    // ## Middle vertex, sometimes.
    if (SUBDIVIDE && dist < d1 && dist < d2) {
        // Location of middle vertex.
        vec3 vm = closest_point(v1.xyz, v2.xyz, vec3(0.0));
        // Distance.
        float dm = length(vm.xyz);
        // Width.
        float wm = u_lineWidthTan * dm;
        // Cross.
        vec3 cm = normalize(cross(vm.xyz, v12)) * wm;
        // W value.
        float w_val = mix(v1.w, v2.w, 0.5);

        v_col = mix(col1, col2, 0.5);

        gl_Position = u_projView * vec4(vm.xyz + cm, w_val);
        v_uv = vec2(0.5, 0.0);
        EmitVertex();

        gl_Position = u_projView * vec4(vm.xyz - cm, w_val);
        v_uv = vec2(0.5, 1.0);
        EmitVertex();
    }

    // ## Second vertex.
    v_col = col2;

    gl_Position = u_projView * vec4(v2.xyz + c2, v2.w);
    v_uv = vec2(1.0, 0.0);
    EmitVertex();

    gl_Position = u_projView * vec4(v2.xyz - c2, v2.w);
    v_uv = vec2(1.0, 1.0);
    EmitVertex();

    EndPrimitive();
}
