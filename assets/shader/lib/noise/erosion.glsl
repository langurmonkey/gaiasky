// #name: Erosion noise
// Source: https://www.shadertoy.com/view/7X2SWW

vec2 hash22(vec2 p) {
    uvec2 q = uvec2(ivec2(round(p))); // lattice coords -> exact integers
    q = q * 1664525u + 1013904223u;
    q.x += q.y * 1664525u;
    q.y += q.x * 1664525u;
    q ^= (q >> 16u);
    q.x += q.y * 1664525u;
    q.y += q.x * 1664525u;
    q ^= (q >> 16u);
    return vec2(q) / float(0xffffffffu);
}


// From: https://iquilezles.org/articles/gradientnoise/
vec3 perlin12d(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);
    vec2 du = 30.0 * f * f * (f * (f - 2.0) + 1.0);
    vec2 ga = hash22(i + vec2(0, 0)) * 2.0 - 1.0;
    vec2 gb = hash22(i + vec2(1, 0)) * 2.0 - 1.0;
    vec2 gc = hash22(i + vec2(0, 1)) * 2.0 - 1.0;
    vec2 gd = hash22(i + vec2(1, 1)) * 2.0 - 1.0;
    float va = dot(ga, f - vec2(0, 0));
    float vb = dot(gb, f - vec2(1, 0));
    float vc = dot(gc, f - vec2(0, 1));
    float vd = dot(gd, f - vec2(1, 1));
    return vec3(va + u.x * (vb - va) + u.y * (vc - va) + u.x * u.y * (va - vb - vc + vd), ga + u.x * (gb - ga) + u.y * (gc - ga) + u.x * u.y * (ga - gb - gc + gd) + du * (u.yx * (va - vb - vc + vd) + vec2(vb, vc) - va));
}

vec3 gullies(vec2 p, vec2 slope) {
    vec2 side_dir = vec2(-slope.y, slope.x) * 3.14159265;
    vec2 id = floor(p);
    p -= id;
    vec2 height_slope = vec2(0);
    float w_sum = 0.0;
    for(int x = -1; x <= 2; x++) {
        for(int y = -1; y <= 2; y++) {
            vec2 off = vec2(x, y);
            vec2 c = p - off - hash22(id + off) + 0.5;
            float dist2 = dot(c, c);
            float w = max(0.0, exp(-dist2 * 2.0) - 0.01111);
            w_sum += w;
            float t = dot(c, side_dir);
            height_slope += vec2(cos(t), -sin(t)) * w;
        }
    }
    return vec3(height_slope.x, height_slope.y * side_dir) / w_sum;
}

// modified & simplified from: https://www.shadertoy.com/view/sf23W1
vec3 erosion12(vec2 p) {
    vec3 nd = perlin12d(p);
    float strength = 0.25, freq = 8.0, total = 1.0;
    for(int i = 0; i < 4; i++) {
        float len2 = dot(nd.yz, nd.yz);
        nd += gullies(p * freq, nd.yz * pow(len2, 0.5 * (0.5 - 1.0))) * strength * vec3(1, freq, freq);
        total += strength;
        strength *= 0.5;
        freq *= 2.0;
    }
    return nd / total;
}

vec3 erosion_triplanar(vec3 p, float sharpness) {
    vec3 n = abs(normalize(p));
    n = pow(n, vec3(sharpness)); // sharpen blend to reduce mixing/blurring
    n /= (n.x + n.y + n.z);

    // Each sample reuses your existing 2D erosion12, just fed different plane coords.
    vec3 sx = erosion12(p.yz); // project along X axis
    vec3 sy = erosion12(p.xz); // project along Y axis
    vec3 sz = erosion12(p.xy); // project along Z axis

    // sx, sy, sz are (height, dHeight/du, dHeight/dv) in their own local 2D frames,
    // so for now just blend the heights — see note below about derivatives.
    return n.x * sx + n.y * sy + n.z * sz;
}

float gln_erosion(vec2 p) {
    return erosion12(p).x;
}


/**
 * Generates 3D Fractional Brownian motion (fBm) from Erosion Noise.
 *
 * @name gln_efbm
 * @function
 * @param {vec2} p               Point to sample fBm at.
 * @param {gln_tFBMOpts} opts    Options for generating Erosion Noise.
 * @return {float}               Value of fBm at point "p".
 */
float gln_efbm(vec2 p, gln_tFBMOpts opts) {
    float result = 0.0;
    float amplitude = 1.0;
    float frequency = opts.frequency;
    float maximum = amplitude;

    for (int i = 0; i < MAX_FBM_ITERATIONS; i++) {
        if (i >= opts.octaves)
        break;

        vec2 p = p * frequency * opts.scale.xy;

        float noiseVal = gln_erosion(p);

        if (opts.turbulence && !opts.ridge) {
            result += abs(noiseVal) * amplitude;
        } else if (opts.ridge) {
            noiseVal = pow(1.0 - abs(noiseVal), 2.0);
            result += noiseVal * amplitude;
        } else {
            result += noiseVal * amplitude;
        }

        frequency *= opts.lacunarity;
        amplitude *= opts.persistence;
        maximum += amplitude;
    }

    #include <shader/lib/noise/fbm_end.glsl>
}
