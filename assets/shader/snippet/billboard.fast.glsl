// Optimized billboard snippet
// This snippet requires importing shader/lib/geometry.glsl like so:
// #include <shader/lib/geometry.glsl>
// It rotates the vertex using a billboard rotation
// using the camera-object vector and a computed up vector.

// Parameters:
//   s_vert_pos (vec4) - vertex position
//   s_obj_pos (vec3) - object position wrt camera
//   s_proj_view (mat4) - camera view-projection matrix
//   s_size (float) - quad size
//   u_camUp (vec3) - camera up (first component is NaN when mode is cubemap)
// Returns:
//   gpos (vec4) - the vertex position
vec3 s_obj, s_up, s_right;
if (isnan(u_camUp.x)) {
    // Mode CUBEMAP - optimized
    // In panorama mode, we need a global orientation, so we use [0,1,0] as up.
    s_obj = normalize(s_obj_pos);
    s_right = cross(s_obj, vec3(0.0, 1.0, 0.0));

    // Optimized singularity fix - use dot product directly
    float parallel_factor = dot(s_obj, vec3(0.0, 1.0, 0.0));
    if (abs(parallel_factor) > 0.999) {
        // Near parallel case - use alternative up vector
        s_right = normalize(cross(s_obj, vec3(1.0, 0.0, 0.0)));
    } else {
        s_right = normalize(s_right);
    }
    s_up = cross(s_obj, s_right);
} else {
    // Mode REGULAR - optimized
    s_obj = normalize(s_obj_pos);
    s_right = normalize(cross(u_camUp, s_obj));
    s_up = cross(s_obj, s_right);
}

// Apply rotation directly without quaternions (major performance gain)
vec3 rotated_offset = s_vert_pos.x * s_right * s_size
                    + s_vert_pos.y * s_up * s_size
                    + s_vert_pos.z * s_obj * s_size;

// Translation and final position
vec3 world_pos = s_obj_pos + rotated_offset;
vec4 gpos = s_proj_view * vec4(world_pos, 1.0);