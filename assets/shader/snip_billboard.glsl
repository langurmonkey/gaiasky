// Billboard snippet
// This snippet requires importing shader/lib_math.glsl
// It rotates the vertex using a billboard rotation
// using the camera-object vector and a computed up vector.

// Parameters:
//   s_vert_pos (vec4) - vertex position
//   s_obj_pos (vec3) - object position
//   s_cam_up (vec3) - camera up vector
//   s_proj_view (mat4) - camera view-projection matrix
//   s_size (float) - scaling size
// Returns:
//   gpos (vec4) - the vertex position

// Compute up vector
vec3 s_obj = normalize(vec3(s_obj_pos));
vec3 s_obj_x_up = normalize(cross(s_obj, s_cam_up));
// s_obj_x_up is parallel to s_obj in some places, fix
float quality = abs(dot(s_obj, s_obj_x_up));
s_obj_x_up = quality * s_obj_x_up + (1.0 - quality) * normalize(cross(s_obj, vec3(1.0, 0.0, 0.0)));
vec3 s_up = normalize(cross(s_obj, s_obj_x_up));

vec4 s_quat = billboard_quaternion(s_obj, s_up);

// Translate
mat4 translation = mat4(1.0, 0.0, 0.0, 0.0,  // 1. column
                        0.0, 1.0, 0.0, 0.0,  // 2. column
                        0.0, 0.0, 1.0, 0.0,  // 3. column
                        s_obj_pos.x, s_obj_pos.y, s_obj_pos.z, 1.0); // 4. column

// Rotate
mat4 rotation = mat4(1.0, 0.0, 0.0, 0.0,  // 1. column
                        0.0, 1.0, 0.0, 0.0,  // 2. column
                        0.0, 0.0, 1.0, 0.0,  // 3. column
                        0.0, 0.0, 0.0, 1.0); // 4. column
float xx = s_quat.x * s_quat.x;
float xy = s_quat.x * s_quat.y;
float xz = s_quat.x * s_quat.z;
float xw = s_quat.x * s_quat.w;
float yy = s_quat.y * s_quat.y;
float yz = s_quat.y * s_quat.z;
float yw = s_quat.y * s_quat.w;
float zz = s_quat.z * s_quat.z;
float zw = s_quat.z * s_quat.w;

rotation[0][0] = 1.0 - 2.0 * (yy + zz);
rotation[1][0] = 2.0 * (xy - zw);
rotation[2][0] = 2.0 * (xz + yw);
rotation[0][1] = 2.0 * (xy + zw);
rotation[1][1] = 1.0 - 2.0 * (xx + zz);
rotation[2][1] = 2.0 * (yz - xw);
rotation[3][1] = 0.0;
rotation[0][2] = 2.0 * (xz - yw);
rotation[1][2] = 2.0 * (yz + xw);
rotation[2][2] = 1.0 - 2.0 * (xx + yy);
rotation[3][3] = 1.0;

// Scale
mat4 scale = mat4(s_size, 0.0, 0.0, 0.0,  // 1. column
                    0.0, s_size, 0.0, 0.0,  // 2. column
                    0.0, 0.0, s_size, 0.0,  // 3. column
                    0.0, 0.0, 0.0, 1.0); // 4. column

// Compute final position and return
vec4 gpos = s_proj_view * translation * rotation * scale * s_vert_pos;
