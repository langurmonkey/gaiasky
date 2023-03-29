// Billboard snippet
// This snippet requires importing shader/lib_geometry.glsl like so:
// #include shader/lib_geometry.glsl
// It rotates the vertex using a billboard rotation
// using the camera-object vector and a computed up vector.

// Parameters:
//   s_vert_pos (vec4) - vertex position
//   s_obj_pos (vec3) - object position wrt camera
//   s_proj_view (mat4) - camera view-projection matrix
//   s_size (float) - scaling size
// Returns:
//   gpos (vec4) - the vertex position

// Compute up vector from X and Y
vec3 s_obj = normalize(s_obj_pos);
vec3 s_obj_x_up = cross(s_obj, vec3(0.0, 1.0, 0.0));
// s_obj_x_up is parallel to s_obj in some places, fix
float quality = abs(dot(s_obj, s_obj_x_up));
s_obj_x_up = quality * s_obj_x_up + (1.0 - quality) * cross(s_obj, vec3(1.0, 0.0, 0.0));
vec3 s_up = normalize(cross(s_obj, s_obj_x_up));

vec4 s_quat = billboard_quaternion(s_obj, s_up);

// Scaling
vec4 vert_pos = vec4(s_vert_pos.xyz * s_size, s_vert_pos.w);

// Rotation
vec4 s_quat_conj = quat_conj(s_quat);
vec4 q_tmp = quat_mult(s_quat, vert_pos);
vert_pos = quat_mult(q_tmp, s_quat_conj);

// Translation
vert_pos.xyz += s_obj_pos;

// Compute final position and return
vec4 gpos = s_proj_view * vert_pos;