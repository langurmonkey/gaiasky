// Billboard snippet
// This snippet requires importing shader/lib/geometry.glsl like so:
// #include <shader/lib/geometry.glsl>
// It rotates the vertex using a billboard rotation
// using the camera-object vector and a computed up vector.

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

//vec4 s_quat = billboard_quaternion(s_obj, s_up, s_right);
vec4 s_quat = q_look_at(s_obj, s_up);



// --- Stretch now in rotated space ---

// Project camera velocity onto direction to star
float camSpeed = length(u_camVel);
float stretch = 1.0;
if (camSpeed != 0.0) {
    float stretchFactor = dot(normalize(u_camVel), s_obj);

    // Tune the strength and clamp
    float strength = 1.85; // tweak this
    stretch = length(u_camVel) * abs(dot(normalize(u_camVel), s_obj)) * strength;
    float sizeBoost = 1.0 + stretch * 0.01; // or some tuned mapping
    s_size *= sizeBoost;
}
// Stretch direction: along s_obj (camera to star)
vec3 stretchDir = s_obj;

// Apply scaling first (no stretch yet)
vec4 vert_pos = vec4(s_vert_pos.xyz * s_size, s_vert_pos.w);

// Rotate the quad into world space
vec4 s_quat_conj = q_conj(s_quat);
vec4 q_tmp = qmul(s_quat, vert_pos);
vert_pos = qmul(q_tmp, s_quat_conj);


// Use local Y coordinate to stretch differentially
float taper = max(s_vert_pos.y, 0.0); // only stretch top half if desired

// Apply per-vertex offset
vert_pos.xyz += stretchDir * (stretch * taper);


// Translation
vert_pos.xyz += s_obj_pos;

// Compute final position and return
vec4 gpos = s_proj_view * vert_pos;
