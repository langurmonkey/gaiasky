#version 330 core

/*
 * Stateless velocity streaks.
 *
 * The vertex buffer holds nothing but a fixed seed per particle. The position is generated here,
 * in a cell of side u_size wrapped around the camera, and the streak is the segment the particle
 * sweeps while the camera travels for u_tau seconds. Nothing is simulated, nothing is uploaded per
 * frame, and every coordinate in the shader is camera-relative, so no float ever has to hold an
 * absolute position.
 *
 * The quad is built in pixels: both ends are projected, the segment's direction on screen is taken
 * from them, and the corners are placed along it by u_widthPx and by the projected length. That is
 * what gives the streak anti-aliasing and a length that is capped in pixels rather than in world
 * units, which is what makes it behave the same over the whole range of distances of the scene.
 */

in vec3 a_seed;
in vec2 a_rand;

// Projection times the rotation of the view: the positions below are camera-relative.
uniform mat4 u_projView;
// -(camera position) mod u_size, in [0, u_size), so the particles stay put in the world.
uniform vec3 u_wrap;
// Side of the cell the particles are wrapped in, in internal units.
uniform float u_size;
// Velocity of the camera relative to the scene, in U/s.
uniform vec3 u_vel;
// Travel time that a streak represents, in seconds.
uniform float u_tau;
// Render size, in pixels.
uniform vec2 u_res;
// Below this the streak is faded out, above the other one it is capped. In pixels.
uniform float u_minPx;
uniform float u_maxPx;
// Width of the streak, in pixels.
uniform float u_widthPx;
// Near plane, in internal units. Anything closer cannot be projected.
uniform float u_near;
// Distance from the camera at which a particle fades in and out, as fractions of the half-cell:
// (inner low, inner high, outer low, outer high). The inner pair is what keeps the particles right
// next to the camera, which would cross the screen in a couple of frames, from being drawn.
uniform vec4 u_fade;
uniform float u_alpha;

#ifdef relativisticEffects
    #include <shader/lib/relativity.glsl>
#endif // relativisticEffects

#ifdef gravitationalWaves
    #include <shader/lib/gravwaves.glsl>
#endif // gravitationalWaves

// Distance behind the head of the streak, in widths: 0 at the head, v_len at the tail.
out float v_s;
// Length of the streak, in widths.
out float v_len;
// Across the streak, in [-1, 1].
out float v_x;
out float v_a;
out vec3 v_col;

void main() {
    // Two triangles. x across, y along: 0 is the head, 1 the tail.
    const vec2 corner[6] = vec2[6](
        vec2(-1.0, 0.0), vec2(1.0, 0.0), vec2(-1.0, 1.0),
        vec2(-1.0, 1.0), vec2(1.0, 0.0), vec2(1.0, 1.0)
    );
    vec2 c = corner[gl_VertexID % 6];

    // The head, in the world, relative to the camera.
    vec3 p = mod(a_seed * u_size + u_wrap, u_size) - 0.5 * u_size;
    // The tail, where that same point was u_tau seconds ago: a static point drifts through the
    // camera frame at -u_vel, so its trail lies along +u_vel.
    vec3 q = p + u_vel * u_tau;

#ifdef relativisticEffects
    p = computeRelativisticAberration(p, length(p), u_velDir, u_vc);
    q = computeRelativisticAberration(q, length(q), u_velDir, u_vc);
#endif // relativisticEffects

    vec4 hc = u_projView * vec4(p, 1.0);
    vec4 tc = u_projView * vec4(q, 1.0);

    // Behind the eye, or so close to it that the projection is not meaningful. Clip out: the quad
    // is degenerate and every interpolant is unused.
    if (hc.w <= u_near || tc.w <= u_near) {
        gl_Position = vec4(2.0, 2.0, 2.0, 1.0);
        v_s = 0.0;
        v_len = 0.0;
        v_x = 0.0;
        v_a = 0.0;
        v_col = vec3(1.0);
        return;
    }

    vec2 hs = (hc.xy / hc.w * 0.5 + 0.5) * u_res;
    vec2 ts = (tc.xy / tc.w * 0.5 + 0.5) * u_res;
    vec2 d = ts - hs;
    float len = length(d);

    vec2 dir = len > 1e-4 ? d / len : vec2(0.0, 1.0);
    vec2 nrm = vec2(-dir.y, dir.x);
    float l = min(len, u_maxPx);
    float w = u_widthPx * (0.55 + 0.9 * a_rand.x);

    // The quad starts half a width behind the head, so the round cap of the head is not cut off by
    // the quad's own edge, and runs to the tail. Distances along the quad are measured in widths,
    // which is the unit the fragment shader needs to keep the head round however long the streak is.
    float cap = 0.5 * w;
    float s = -cap + (l + cap) * c.y;

    // The head is a little wider than the trail, which is what turns the brighter end of the streak
    // into a bead instead of into one more bright pixel of the line.
    float hw = 1.0 + 0.75 * (1.0 - c.y);

    // Place the corner in pixels and go back to clip space at the head's depth. Using the head's
    // depth for the whole quad keeps it from being clipped by the near plane halfway along, and the
    // effect is drawn without depth testing anyway.
    vec2 sp = hs + nrm * (0.5 * w * hw * c.x) + dir * s;
    vec2 ndc = sp / u_res * 2.0 - 1.0;
    gl_Position = vec4(ndc * hc.w, hc.z, hc.w);

    // Fades: nothing right against the eye, nothing far away where the cell wraps, nothing so short
    // that it is a dot. The radial band also turns the cell into a ball, so the corners of the cube
    // and the faces where the particles wrap never show.
    float rh = length(p) / (0.5 * u_size);
    float fade = smoothstep(u_fade.x, u_fade.y, rh) * (1.0 - smoothstep(u_fade.z, u_fade.w, rh));
    float lenFade = smoothstep(0.0, 1.0, len / max(u_minPx, 1e-3));

    v_a = u_alpha * fade * mix(0.35, 1.0, a_rand.y) * lenFade;
    v_s = s / w;
    v_len = l / w;
    v_x = c.x;
    // Nearly white, with a hint of blue and a little variation per particle.
    v_col = mix(vec3(0.80, 0.88, 1.0), vec3(1.0, 0.98, 0.94), a_rand.y);
}
