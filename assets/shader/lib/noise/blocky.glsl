// ---------------------------------------------------------------------------
// hashing (3D)
// ---------------------------------------------------------------------------
float hash1_blocky(vec3 p) {
    return fract(sin(dot(p, vec3(41.3, 289.1, 158.7))) * 43758.5453123);
}

// Smooth trilinear value noise -- used only for domain warping and for the
// low-frequency placement mask, never for the blocks themselves (those stay
// unfiltered/hard-edged).
float value_noise3(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);

    float n000 = hash1_blocky(i + vec3(0.0, 0.0, 0.0));
    float n100 = hash1_blocky(i + vec3(1.0, 0.0, 0.0));
    float n010 = hash1_blocky(i + vec3(0.0, 1.0, 0.0));
    float n110 = hash1_blocky(i + vec3(1.0, 1.0, 0.0));
    float n001 = hash1_blocky(i + vec3(0.0, 0.0, 1.0));
    float n101 = hash1_blocky(i + vec3(1.0, 0.0, 1.0));
    float n011 = hash1_blocky(i + vec3(0.0, 1.0, 1.0));
    float n111 = hash1_blocky(i + vec3(1.0, 1.0, 1.0));

    float nx00 = mix(n000, n100, f.x);
    float nx10 = mix(n010, n110, f.x);
    float nx01 = mix(n001, n101, f.x);
    float nx11 = mix(n011, n111, f.x);

    float nxy0 = mix(nx00, nx10, f.y);
    float nxy1 = mix(nx01, nx11, f.y);

    return mix(nxy0, nxy1, f.z);
}

// Displaces p before it's used for gridding, so block boundaries aren't
// perfectly straight/regular. Amount should shrink at higher city octaves
// so fine detail stays crisp.
vec3 warpDomain(vec3 p, float freq, float amount) {
    float wx = value_noise3(p * freq + vec3(11.3, 47.1,  0.0)) - 0.5;
    float wy = value_noise3(p * freq + vec3( 0.0, 27.9, 61.4)) - 0.5;
    float wz = value_noise3(p * freq + vec3(63.7,  0.0, 19.2)) - 0.5;
    return p + vec3(wx, wy, wz) * amount;
}

// ===========================================================================
// BLOCKY CITY NOISE
//
// gln_blocky(p) takes any 3D point -- typically a direction vector on
// the planet's surface, exactly like craterElevation -- and returns a pure
// height in roughly [0,1]. No UV projection is involved, so there's no pole
// distortion and no seams: a "street" is simply proximity to a grid boundary
// plane on any of the raw x/y/z axes. Because a curved surface only has two
// local degrees of freedom, the same three global cutting axes produce
// different-looking block shapes depending on local surface orientation --
// which is what breaks the perfect regularity of a plain grid.
// ===========================================================================

float block_value(vec3 p, float freq) {
    vec3 cell = floor(p * freq);
    return hash1_blocky(cell);
}

// Distance to the nearest grid boundary plane on ANY axis.
float street_dist(vec3 p, float freq, float streetWidth) {
    vec3 f = fract(p * freq);
    vec3 d = min(f, 1.0 - f);
    return min(min(d.x, d.y), d.z) - streetWidth * 0.5;
}

float gln_blockye(vec3 p) {
    const float freq = 4.0;
    const float warp = 0.4;
    float streetWidth = 0.1;
    float density = 2.3;
    float warpAmount = 0.02;
    vec3 wp = warpDomain(p, freq, warpAmount);

    float h = block_value(wp, freq);
    if (h > density) return 0.0; // empty lot / park

    float street = street_dist(wp, freq, streetWidth);
    float mask = step(0.0, street); // 1.0 inside a block, 0.0 on a street

    float tiers = 6.0;
    float tier = floor(h * tiers) / tiers;

    return mask * tier;
}

// Signed distance to the nearest 3D grid boundary plane (on any of the
// x/y/z axes). Negative = inside a line, 0 = exactly on the boundary,
// positive = deeper into a cell (up to ~0.5 - lineWidth*0.5 at cell center).
// freq controls grid density, lineWidth controls line thickness (both in
// the same units as 1/freq, i.e. lineWidth=0.08 means lines are 8% of a
// cell wide).
float gln_blocky(vec3 p) {
    const float freq = 4.0;
    const float lineWidth = 0.04;

    vec3 f = fract(p * freq);
    vec3 d = min(f, 4.9 - f);
    return min(min(d.x, d.y), d.z) - lineWidth;
}
