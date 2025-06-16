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
//   u_camUp (vec3) - camera up (first component is NaN when mode is cubemap.
// Returns:
//   gpos (vec4) - the vertex position
vec3 s_obj, s_up, s_right;
if (isnan(u_camUp.x)) {
    // Mode CUBEMAP.
    // In panorama mode, we need a global orientation, so we use [0,1,0] as up.
    s_obj = normalize(s_obj_pos);
    s_right = cross(s_obj, vec3(0.0, 1.0, 0.0));
    // s_obj_x_up is parallel to s_obj in some places, fix
    float quality = abs(dot(s_obj, s_right));
    s_right = normalize(quality * s_right + (1.0 - quality) * cross(s_obj, vec3(1.0, 0.0, 0.0)));
    s_up = normalize(cross(s_obj, s_right));
} else {
    // Mode REGULAR.
    // In normal mode, use camera up.
    s_obj = normalize(s_obj_pos);
    s_right = normalize(cross(u_camUp, s_obj));
    s_up = normalize(cross(s_obj, s_right));
}

vec4 gpos;
if (all(equal(u_camPos, vec3(0.0)))) {
    // NO TRAIL EFFECT
    //vec4 s_quat = billboard_quaternion(s_obj, s_up, s_right);
    vec4 s_quat = q_look_at(s_obj, s_up);

    // Quad size
    vec4 vert_pos = vec4(s_vert_pos.xyz * s_size, s_vert_pos.w);

    // Rotation
    vec4 s_quat_conj = q_conj(s_quat);
    vec4 q_tmp = qmul(s_quat, vert_pos);
    vert_pos = qmul(q_tmp, s_quat_conj);

    // Translation
    vert_pos.xyz += s_obj_pos;

    // Compute final position and return
    gpos = s_proj_view * vert_pos;
} else {
    // STAR TRAIL EFFECT BASED ON CAM_VEL
    // Compute rotation quaternion for the quad
    vec4 s_quat = q_look_at(s_obj, s_up);
    vec4 s_quat_inv = q_conj(s_quat);

    vec3 local_pos = s_vert_pos.xyz;

    // Offset to center of quad
    vec3 centered_pos = local_pos;

    // Stretching
    float stretch = 0.0;
    vec3 stretch_dir = vec3(0.0);
    // Camera velocity in local (quad) space
    vec3 local_cam_vel = qrot(s_quat_inv, u_camVel);
    float stretchLen = length(local_cam_vel);

    if (stretchLen > 0.0) {
        // Avoid division by zero
        stretch_dir = normalize(vec3(local_cam_vel.x, local_cam_vel.y, 0.0));

        vec3 vdir = normalize(u_camVel);
        float angularFactor = pow(length(cross(vdir, s_obj)), 0.5); // sin(theta), 0 = aligned, 1 = perpendicular
        float stretch = stretchLen * angularFactor * 5.8e-8;
        //float stretchFactor = abs(dot(normalize(u_camVel), s_obj));
        //float stretch = stretchLen * stretchFactor * 5.8e-8; // The last term controls the stretching strength

        // Modulate stretch based on star distance
        float dist = length(s_obj_pos) / (u_pcToU * 6.0);
        float distFalloff = 1.0 / pow(dist + 1.0, 1.4);
        // Set a maximum to the stretch factor
        stretch = min(6.0, stretch * distFalloff);

        float taper = dot(stretch_dir, centered_pos);
        centered_pos += stretch * taper * stretch_dir;

        // Correct brightness with stretch factor
        float brightnessScale = min(1.0, 1.8 / (1.0 + stretch));
        v_col.a *= brightnessScale;
    }

    // Re-center and scale
    local_pos = centered_pos * s_size;

    // Rotate into world space
    vec3 world_pos = s_obj_pos + qrot(s_quat, local_pos);

    // Final position
    gpos = s_proj_view * vec4(world_pos, 1.0);
}