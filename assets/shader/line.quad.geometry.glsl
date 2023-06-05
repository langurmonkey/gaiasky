#version 400 core

// Subdivide the line if the camera is closer to any point in the
// line other than the two ends. This makes sure the width stays
// constant regardless of the camera location.
#define SUBDIVIDE false

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

// Computes the closest distance of the segment p0-p1 to the point p.
double dist_point_segment(dvec3 p1, dvec3 p2, dvec3 p) {
    dvec3 v = p1;
    dvec3 w = p2;

    dvec3 aux3 = p - v;
    dvec3 aux4 = w - v;

    // Return minimum distance between line segment vw and point p.
    double l2 = length(aux4);
    if (l2 == 0.0)
    return length(aux3);// v == w case
    // Consider the line extending the segment, parameterized as v + t (w - v).
    // We find projection of point p onto the line.
    // It falls where t = [(p-v) . (w-v)] / |w-v|^2
    double t = dot(aux3, aux4) / l2;
    if (t < 0.0){
        return length(v - p);// Beyond the 'v' end of the segment.
    } else if (t > 1.0){
        return length(w - p);// Beyond the 'w' end of the segment.
    }
    dvec3 projection = v + aux4 * t;// Projection falls on the segment
    return length(projection - p);
}


dvec3 closest_point(dvec3 p1, dvec3 p2, dvec3 p) {
    //           (P2-P1)dot(v)
    //Pr = P1 +  ------------- * v.
    //           (v)dot(v)
    dvec3 v = p2 - p1;

    double nomin = dot(p - p1, v);
    double denom = dot(v, v);
    dvec3 frac = v * (nomin / denom);

    return p1 + frac;
}

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
    EmitVertex();

    gl_Position = u_projView * vec4(v1.xyz - c1, v1.w);
    v_w = gl_Position.w;
    v_uv = vec2(0.0, 1.0);
    EmitVertex();

    if (SUBDIVIDE) {
        // ## Middle vertex, sometimes.
        // Closest distance to segment.
        // If the camera position (0,0,0) is closest to the
        // line than any of the two ends, we split the line at the closest point
        // to the camera.
        double dist = dist_point_segment(v1.xyz, v2.xyz, dvec3(0.0));
        if (dist < d1 && dist < d2) {
            // Location of middle vertex.
            dvec3 vm = closest_point(v1.xyz, v2.xyz, dvec3(0.0));
            // Distance.
            double dm = length(vm.xyz);
            // Width.
            double wm = u_lineWidthTan * dm;
            // Cross.
            dvec3 cm = normalize(cross(vm.xyz, v12)) * wm;
            // W value.
            double w_val = mix(v1.w, v2.w, 0.5);

            v_col = vec4(mix(col1, col2, 0.5));

            gl_Position = u_projView * vec4(vm.xyz + cm, w_val);
            v_w = gl_Position.w;
            v_uv = vec2(0.5, 0.0);
            EmitVertex();

            gl_Position = u_projView * vec4(vm.xyz - cm, w_val);
            v_w = gl_Position.w;
            v_uv = vec2(0.5, 1.0);
            EmitVertex();
        }
    }

    // ## Second vertex.
    v_col = vec4(col2);
    v_w = (u_projView * vec4(v2bak)).w;

    gl_Position = u_projView * vec4(v2.xyz + c2, v2.w);
    v_uv = vec2(1.0, 0.0);
    EmitVertex();

    gl_Position = u_projView * vec4(v2.xyz - c2, v2.w);
    v_uv = vec2(1.0, 1.0);
    EmitVertex();

    EndPrimitive();
}
