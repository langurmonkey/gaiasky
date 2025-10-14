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
    // Apply rotation directly without quaternions (major performance gain)
    vec3 rotated_offset = s_vert_pos.x * s_right * s_size
                        + s_vert_pos.y * s_up * s_size
                        + s_vert_pos.z * s_obj * s_size;

    // Translation and final position
    vec3 world_pos = s_obj_pos + rotated_offset;
    gpos = s_proj_view * vec4(world_pos, 1.0);
} else {
    // MOTION TRAILS BASED ON ANGULAR SPEED

    // Compute rotation quaternion for the quad
    vec4 s_quat = q_look_at(s_obj, s_up);
    vec4 s_quat_inv = q_conj(s_quat);
    vec3 local_pos = s_vert_pos.xyz;
    // Offset to center of quad
    vec3 centered_pos = local_pos;
    vec3 local_cam_vel = qrot(s_quat_inv, u_camVel);

    // Precompute screen movement direction
    vec3 stretch_dir = normalize(vec2(local_cam_vel.x, local_cam_vel.y).xyx);

    // Estimate movement of the object in screen space
    vec3 obj_world = s_obj_pos;
    vec3 obj_next = obj_world - u_camVel * u_dt;

    vec4 clip_now = s_proj_view * vec4(obj_world, 1.0);
    vec4 clip_next = s_proj_view * vec4(obj_next, 1.0);

    // Approximate screen-space velocity using NDC delta
    vec2 ndc_now = clip_now.xy / clip_now.w;
    vec2 ndc_next = clip_next.xy / clip_next.w;
    float screenVel = length(ndc_now - ndc_next);

    // Use screen velocity to drive stretch
    float stretch = pow(screenVel * 300.0, 1.5);
    stretch = clamp(stretch, 0.0, 6.0);

    // --- Distance-based fadeout ---
    float distMpc = length(s_obj_pos) * u_uToMpc;
    float fade = smoothstep(50.0, 30.0, distMpc); // fade = 1.0 below 30 Mpc, fades to 0 at 50

    // Apply fade
    stretch *= fade;

    // Threshold cutoff
    if (screenVel < 0.0001 || fade < 0.01) {
        stretch = 0.0;
    } else {
        // Apply directional stretch in local (quad) space
        float taper = dot(stretch_dir, centered_pos);
        centered_pos += stretch * taper * stretch_dir;

        // Brightness correction to conserve "energy"
        float brightnessScale = 2.0 / (1.0 + stretch);
        v_col.rgb *= clamp(brightnessScale, 0.0, 1.0);
    }

    // Recenter and scale
    local_pos = centered_pos * s_size;

    // Rotate into world space
    vec3 world_pos = s_obj_pos + qrot(s_quat, local_pos);

    // Final position
    gpos = s_proj_view * vec4(world_pos, 1.0);
}