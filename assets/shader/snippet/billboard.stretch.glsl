// Billboard snippet
// This snippet requires importing shader/lib/geometry.glsl like so:
// #include <shader/lib/geometry.glsl>
// It rotates the vertex using a billboard rotation
// using the camera-object vector and a computed up vector.
// This version stretches the billboards in the direction of the camera velocity,
// which is in the uniform u_camVel.

// Parameters:
//   s_vert_pos (vec4) - vertex position
//   s_obj_pos (vec3) - object position wrt camera
//   s_proj_view (mat4) - camera view-projection matrix
//   s_size (float) - quad size
//   u_camUp (vec3) - camera up (first component is NaN when mode is cubemap)
//   u_camVel (vec3) - camera velocity
//   u_dt (float) - delta time in seconds
//   u_uToMpc (float) - conversion factor from internal units to Mpc
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
    s_up = cross(s_obj, s_right); // Already normalized
} else {
    // Mode REGULAR - optimized
    s_obj = normalize(s_obj_pos);
    s_right = normalize(cross(u_camUp, s_obj));
    s_up = cross(s_obj, s_right); // Already normalized
}

vec4 gpos;
if (dot(u_camVel, u_camVel) == 0.0) {
    // NO TRAIL EFFECT
    vec3 rotated_offset = s_vert_pos.x * s_right * s_size
                        + s_vert_pos.y * s_up * s_size
                        + s_vert_pos.z * s_obj * s_size;

    // Translation and final position
    vec3 world_pos = s_obj_pos + rotated_offset;
    gpos = s_proj_view * vec4(world_pos, 1.0);
} else {
    // MOTION TRAILS FOR STAR FIELD
    vec4 s_quat = q_look_at(s_obj, s_up);
    vec4 s_quat_inv = q_conj(s_quat);
    vec3 local_pos = s_vert_pos.xyz;
    vec3 centered_pos = local_pos;

    // Transform camera velocity to local space
    vec3 local_cam_vel = qrot(s_quat_inv, u_camVel);

    // Precompute screen movement direction
    vec2 local_vel_xy = local_cam_vel.xy;
    float local_vel_len = length(local_vel_xy);
    vec3 stretch_dir = vec3(normalize(local_vel_xy), 0.0);

    // Estimate screen-space movement
    vec3 obj_next = s_obj_pos - u_camVel * u_dt;

    vec4 clip_now = s_proj_view * vec4(s_obj_pos, 1.0);
    vec4 clip_next = s_proj_view * vec4(obj_next, 1.0);

    // Faster screen velocity calculation using reciprocal
    vec2 ndc_now = clip_now.xy * (1.0 / clip_now.w);
    vec2 ndc_next = clip_next.xy * (1.0 / clip_next.w);
    float screenVel = length(ndc_now - ndc_next);

    // Compute stretch with optimized pow - use cheaper approximation for 1.5 exponent
    float stretch = screenVel * 300.0;
    stretch = stretch * sqrt(stretch); // pow(x, 1.5) = x * sqrt(x)
    stretch = min(stretch, 6.0);

    // Distance-based fadeout - optimized
    float dist = length(s_obj_pos);
    float distMpc = dist * u_uToMpc;
    float fade = clamp((50.0 - distMpc) * 0.05, 0.0, 1.0); // equivalent to smoothstep(50,30)

    stretch *= fade;

    // Early exit for no-stretch case
    if (screenVel < 0.0001 || fade < 0.01 || local_vel_len < 0.001) {
        // Fast path: no stretching needed
        vec4 vert_pos = vec4(local_pos * s_size, 1.0);
        vec4 q_tmp = qmul(s_quat, vert_pos);
        vert_pos = qmul(q_tmp, s_quat_inv);
        vec3 world_pos = s_obj_pos + vert_pos.xyz;
        gpos = s_proj_view * vec4(world_pos, 1.0);
    } else {
        // Apply directional stretch
        float taper = dot(stretch_dir, centered_pos);
        centered_pos += stretch * taper * stretch_dir;

        // Brightness correction - precompute
        float brightnessScale = 2.0 / (1.0 + stretch);
        v_col.rgb *= min(brightnessScale, 1.0);

        // Transform to world space using your existing quaternion method
        vec4 vert_pos = vec4(centered_pos * s_size, 1.0);
        vec4 q_tmp = qmul(s_quat, vert_pos);
        vert_pos = qmul(q_tmp, s_quat_inv);
        vec3 world_pos = s_obj_pos + vert_pos.xyz;
        gpos = s_proj_view * vec4(world_pos, 1.0);
    }
}