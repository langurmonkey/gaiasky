float hash1(vec3 p) {
    return fract(sin(dot(p, vec3(41.3, 289.1, 158.7))) * 43758.5453123);
}
vec3 hash3(vec3 p) {
    vec3 q = vec3(dot(p, vec3(127.1, 311.7, 74.7)),
            dot(p, vec3(269.5, 183.3, 246.1)),
            dot(p, vec3(113.5, 271.9, 124.6)));
    return fract(sin(q) * 43758.5453123);
}

// gln_crater(p) takes ANY 3D point (does not need to be normalized —
// works for points on a sphere, a flat plane, or a full 3D volume) and
// returns a pure height value: negative inside bowls, positive on rims,
// zero on undisturbed ground. No lighting or color is baked in.
//
// Craters are exactly radially symmetric because craterProfile() is only
// ever evaluated as a function of Euclidean distance to a crater center,
// never distorted by a UV projection.

// t = distance from crater center / crater radius (0 at center, 1 at rim)
float crater_profile(float t) {
    float bowl = t * t - 1.0;                              // parabola: -1 -> 0
    float rim  = 0.35 * exp(-pow((t - 0.82) * 7.0, 2.0));   // raised ring near rim
    return bowl + rim;
}

#define CRATER_DENSITY 0.8
// One frequency/scale of crater field. One candidate crater is jittered per
// grid cell; only the 8 nearest cells are checked (cheap 2x2x2 neighbor
// selection) rather than the full 3x3x3 = 27, which is fine as long as
// crater radius stays comfortably under half a cell (see radius range below).
float gln_crater(vec3 p) {
    vec3 cell = floor(p);
    vec3 f = fract(p);
    vec3 sgn = step(0.5, f) * 2.0 - 1.0; // which side of the cell we're on, per axis

    float best = 0.0;
    for (int zi = 0; zi < 2; zi++)
    for (int yi = 0; yi < 2; yi++)
    for (int xi = 0; xi < 2; xi++) {
        vec3 off = vec3(float(xi), float(yi), float(zi)) * sgn;
        vec3 neighbor = cell + off;

        float exists = step(1.0 - CRATER_DENSITY, hash1(neighbor + 17.3));
        if (exists < 0.5) continue;

        vec3 jitter = hash3(neighbor);
        vec3 center = neighbor + 0.15 + jitter * 0.7;
        float radius = mix(0.18, 0.40, hash1(neighbor + 5.1));

        float d = length(p - center);
        float t = d / radius;
        if (t < 1.15) {
            best = min(best, crater_profile(min(t, 1.15)));
        }
    }
    return best;
}
