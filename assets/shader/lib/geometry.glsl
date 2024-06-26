#ifndef GLSL_LIB_GEOMETRY
#define GLSL_LIB_GEOMETRY

#ifndef PI
#define PI 3.141592653589793238462643383
#endif // PI
#ifndef PI2
#define PI2 3.14159265358979323846264338 * 2.0
#endif // PI2

#ifndef QUATERNION_IDENTITY
#define QUATERNION_IDENTITY vec4(0.0, 0.0, 0.0, 1.0)
#endif // QUATERNION_IDENTITY

// Returns >=0 if visible, <0 if not visible 
float in_view(vec3 pos, vec3 dir, float dist, float angle_edge) {
    return angle_edge - acos(dot(pos, dir) / dist);
}

vec4 q_conj(vec4 q) {
    return vec4(-q.x, -q.y, -q.z, q.w);
}

vec4 q_look_at(vec3 forward, vec3 up) {
    vec3 right = normalize(cross(forward, up));
    up = normalize(cross(forward, right));

    float m00 = right.x;
    float m01 = right.y;
    float m02 = right.z;
    float m10 = up.x;
    float m11 = up.y;
    float m12 = up.z;
    float m20 = forward.x;
    float m21 = forward.y;
    float m22 = forward.z;

    float num8 = (m00 + m11) + m22;
    vec4 q = QUATERNION_IDENTITY;
    if (num8 > 0.0)
    {
        float num = sqrt(num8 + 1.0);
        q.w = num * 0.5;
        num = 0.5 / num;
        q.x = (m12 - m21) * num;
        q.y = (m20 - m02) * num;
        q.z = (m01 - m10) * num;
        return q;
    }

    if ((m00 >= m11) && (m00 >= m22))
    {
        float num7 = sqrt(((1.0 + m00) - m11) - m22);
        float num4 = 0.5 / num7;
        q.x = 0.5 * num7;
        q.y = (m01 + m10) * num4;
        q.z = (m02 + m20) * num4;
        q.w = (m12 - m21) * num4;
        return q;
    }

    if (m11 > m22)
    {
        float num6 = sqrt(((1.0 + m11) - m00) - m22);
        float num3 = 0.5 / num6;
        q.x = (m10 + m01) * num3;
        q.y = 0.5 * num6;
        q.z = (m21 + m12) * num3;
        q.w = (m20 - m02) * num3;
        return q;
    }

    float num5 = sqrt(((1.0 + m22) - m00) - m11);
    float num2 = 0.5 / num5;
    q.x = (m20 + m02) * num2;
    q.y = (m21 + m12) * num2;
    q.z = 0.5 * num5;
    q.w = (m01 - m10) * num2;
    return q;
}

/*
 * Gets the quaternion to rotate around the given axis by the given angle in radians
 */
vec4 get_quat_rotation(vec3 axis, float radians) {
    float d = 1.0 / length(axis);
    float l_ang;
    if(radians < 0.0) {
        l_ang = PI2 - (mod(-radians, PI2));
    }else {
        l_ang = mod(radians, PI2);
    }
    float l_sin = sin(l_ang / 2.0);
    float l_cos = cos(l_ang / 2.0);
    return  normalize(vec4(d * axis.x * l_sin, d * axis.y * l_sin, d * axis.z * l_sin, l_cos));
}

// Quaternion multiplication
// http://mathworld.wolfram.com/Quaternion.html
vec4 qmul(vec4 q1, vec4 q2) {
    return vec4(
    q2.xyz * q1.w + q1.xyz * q2.w + cross(q1.xyz, q2.xyz),
    q1.w * q2.w - dot(q1.xyz, q2.xyz)
    );
}

// Vector rotation with a quaternion
// http://mathworld.wolfram.com/Quaternion.html
vec3 rotate_vector(vec3 v, vec4 r) {
    vec4 r_c = r * vec4(-1, -1, -1, 1);
    return qmul(r, qmul(vec4(v, 0), r_c)).xyz;
}

vec3 rotate_vector_at(vec3 v, vec3 center, vec4 r) {
    vec3 dir = v - center;
    return center + rotate_vector(dir, r);
}

// A given angle of rotation about a given axis
vec4 rotate_angle_axis(float angle, vec3 axis) {
    float sn = sin(angle * 0.5);
    float cs = cos(angle * 0.5);
    return vec4(axis * sn, cs);
}

#ifndef GLSL_ROTATE_VERTEX_POS
#define GLSL_ROTATE_VERTEX_POS
vec3 rotate_vertex_position(vec3 position, vec3 axis, float angle) {
  vec4 q = get_quat_rotation(axis, angle);
  return position.xyz + 2.0 * cross(q.xyz, cross(q.xyz, position.xyz) + q.w * position.xyz);
}
#endif // GLSL_ROTATE_VERTEX_POS

// Get perpendicular vector
vec3 perpendicular_vec(vec3 vector) {
    // Find a vector that is not parallel to the input vector
    vec3 arbitrary_vec = vec3(1.0, 0.0, 0.0);

    // Calculate the cross product between the input vector and the arbitrary vector
    vec3 perpendicular_vec = cross(vector, arbitrary_vec);

    // If the cross product is nearly zero (indicating that the vectors are parallel),
    // choose a different arbitrary vector and recalculate
    if (length(perpendicular_vec) < 0.001) {
        arbitrary_vec = vec3(0.0, 1.0, 0.0);
        perpendicular_vec = cross(vector, arbitrary_vec);
    }

    return normalize(perpendicular_vec);
}
#endif