#ifndef GLSL_LIB_STARNEAR
#define GLSL_LIB_STARNEAR

// Near-field star shading.
// All distances are in apparent radii: q is the offset from the centre over the photosphere radius, so |q| == 1 at the limb.

#include <shader/lib/luma.glsl>
#include <shader/lib/noise/common.glsl>
#include <shader/lib/noise/curl.glsl>
#include <shader/lib/noise/noise4q.glsl>

// Quadratic limb-darkening coefficients (solar-type).
const float STAR_LD_U1 = 0.58;
const float STAR_LD_U2 = 0.12;
// Centre-to-limb reddening, 0 disables it.
const float STAR_LIMB_REDDEN = 0.55;
// Granules over the disc radius. Fixed frequency, never scaled by zoom (see starOctaveWeight).
const float STAR_GRANULE = 42.0;
// Smallest granules.
const float STAR_GRANULE_FINE = 96.0;
// Supergranulation cells, which carry the bright magnetic network.
const float STAR_SUPERGRANULE = 8.0;
// Spot field frequency.
const float STAR_SPOT_SCALE = 2.1;
// Corona brightness, 0 disables it.
const float STAR_CORONA = 0.15;
// Corona reach in stellar radii above the limb, clipped to the quad (see StarParams.quadR).
const float STAR_CORONA_REACH = 1.2;
// Weight of the smooth limb ring against the streamers.
const float STAR_CORONA_RING = 0.55;
// Spatial frequency of the streamer octaves, in cycles across the star.
const float STAR_CORONA_RAYS = 10.0;
const float STAR_CORONA_RAYS_FINE = 50.0;
// Evolution of the streamer field, in units of the 4th noise coordinate per second.
const float STAR_CORONA_RATE = 0.05;
// Brightness of the coronal holes, the open field at high latitude.
const float STAR_CORONA_HOLE = 0.55;
// Diffraction spike amplitude, 0 disables them.
const float STAR_SPIKES = 0.0;
// Photosphere rotation, radians per second about world Y (real Sun: 3e-6, i.e. ~25 days).
const float STAR_ROTATION = 0.02;
// Differential rotation: the equator turns faster than the poles by this fraction.
const float STAR_ROTATION_SHEAR = 0.45;
// Granulation evolution, in full cross-fades per second.
const float STAR_GRANULE_RATE = 0.30;
// Spot field evolution, cycles per second.
const float STAR_SPOT_RATE = 0.02;
// Disc diameter in pixels where the model starts to replace the sprite, and where it has fully replaced it.
const float STAR_MODEL_MIN_PX = 20.0;
const float STAR_MODEL_FULL_PX = 55.0;
// Cell size in pixels below which an octave is faded out, to stay clear of the Nyquist limit.
const float STAR_OCTAVE_MIN_PX = 2.0;
// Spin axis of the star, in world space. Only used as a dot product, so it has no singularities.
const vec3 STAR_SPIN_AXIS = vec3(0.0, 1.0, 0.0);

struct StarParams {
    // Linear RGB of the photosphere, peak 1.
    vec3 color;
    // Surface activity in [0,1]: 0 for an A/F star (no spots), 1 for an active K/M star.
    float activity;
    // Pattern seed, distinct per star so that neighbours do not look alike.
    float seed;
    // Animation time in seconds.
    float time;
    // Detail level [0,1], for the amplitude of secondary structures only, never a frequency.
    float lod;
    // Pixel size in apparent radii, only used to fade octaves by amplitude (starOctaveWeight).
    float pxR;
    // Half extent of the quad in apparent radii; the corona must fade before its edge.
    float quadR;
    // Quad axes and direction to the camera, in world space.
    vec3 right;
    vec3 up;
    vec3 view;
    // Star radius over camera distance: the perspective ratio that turns the quad plane into a 3D shell.
    float radiusOverDist;
};

// Limb darkening as a function of mu = cos(angle from the disc centre).
float starLimbDarkening(float mu) {
    float c = 1.0 - mu;
    return 1.0 - STAR_LD_U1 * c - STAR_LD_U2 * c * c;
}

// Centre-to-limb reddening: at grazing angles we see cooler layers, so the limb reddens.
vec3 starLimbTint(vec3 c, float mu) {
    float k = (1.0 - mu) * (1.0 - mu) * STAR_LIMB_REDDEN;
    return mix(c, c * vec3(1.0, 0.82, 0.60), k);
}

vec3 starRotateY(vec3 n, float a) {
    float c = cos(a);
    float s = sin(a);
    return vec3(c * n.x + s * n.z, n.y, -s * n.x + c * n.z);
}

// Rotation about the spin axis with differential shear, faster for active stars.
vec3 starSpin(vec3 n, StarParams p) {
    float omega = STAR_ROTATION * (0.4 + 0.6 * p.activity);
    float shear = 1.0 - STAR_ROTATION_SHEAR * n.y * n.y;
    return starRotateY(n, p.time * omega * shear);
}

// Cross-faded noise: sampled at two consecutive integer steps and blended, so the pattern morphs in place.
float starField(vec3 n, float scale, float rate, float time, float seed) {
    float t = time * rate;
    float t0 = floor(t);
    float f = smoothstep(0.0, 1.0, t - t0);
    vec3 c = n * scale + seed;
    float v0 = gln_simplex_curl(c + vec3(0.0, 0.0, t0 * 6.0));
    float v1 = gln_simplex_curl(c + vec3(0.0, 0.0, (t0 + 1.0) * 6.0));
    return mix(v0, v1, f);
}

// Pixels spanned by one cell of an octave with `cells` cells across the disc radius.
float starCellPx(float cells, StarParams p) {
    return 1.0 / max(cells * max(p.pxR, 1e-9), 1e-9);
}

// Weight of an octave: 0 below a couple of pixels per cell, 1 once the screen resolves them.
float starOctaveWeight(float cells, StarParams p) {
    return smoothstep(STAR_OCTAVE_MIN_PX, STAR_OCTAVE_MIN_PX * 2.0, starCellPx(cells, p));
}

// Granulation in [-1,1] at the world normal n, and the magnetic network in [0,1] through `network`.
float starGranulation(vec3 n, StarParams p, out float network) {
    float sup = starField(n, STAR_SUPERGRANULE, STAR_GRANULE_RATE * 0.12, p.time, p.seed);
    float gran = starField(n, STAR_GRANULE, STAR_GRANULE_RATE, p.time, p.seed);
    float fine = starField(n, STAR_GRANULE_FINE, STAR_GRANULE_RATE * 1.7, p.time, p.seed + 3.1);

    // Fade whole octaves by amplitude and redistribute the freed weight, so contrast stays constant.
    float aSup = 0.16;
    float aGran = 0.62 * starOctaveWeight(STAR_GRANULE, p);
    float aFine = 0.22 * starOctaveWeight(STAR_GRANULE_FINE, p);
    float asum = max(aSup + aGran + aFine, 1e-4);

    // Network: the lanes between supergranules, where the flux collects. Fades with the granule octave.
    network = (1.0 - smoothstep(0.0, 0.25, abs(sup))) * starOctaveWeight(STAR_GRANULE, p);

    return (sup * aSup + gran * aGran + fine * aFine) / asum;
}

// Spot coverage in [0,1] (0 = clean photosphere, 1 = umbra); `faculae` gets the bright rim.
float starSpotCoverage(vec3 n, StarParams p, out float faculae) {
    vec3 s = n * STAR_SPOT_SCALE + p.seed * 3.7 + vec3(0.0, 0.0, p.time * STAR_SPOT_RATE);
    float f1 = gln_simplex_curl(s);
    float f2 = gln_simplex_curl(s + vec3(5.3, 2.1, 7.9));
    // The threshold is the activity knob: a quiet star needs a much stronger field to make a spot.
    float th = mix(0.70, 0.28, p.activity);
    float sp = max(f1 - th, 0.0) * max(f2 - th, 0.0);
    float umbra = smoothstep(0.055, 0.180, sp);
    float penumbra = smoothstep(0.0, 0.055, sp) * (1.0 - umbra);
    // Faculae sit just outside the spot, where the field is strong but not yet cool enough to darken.
    faculae = smoothstep(0.0, 0.020, sp) * (1.0 - smoothstep(0.020, 0.055, sp));
    // Umbral dots and light bridges pierce the umbra.
    float bridge = smoothstep(0.55, 0.90, starField(n, 22.0, 0.0, 0.0, p.seed + 7.7)) * p.lod;
    umbra *= 1.0 - 0.55 * bridge;
    return umbra + 0.55 * penumbra;
}

// Photosphere radiance: `mu` is the cosine to the camera, `n` the world normal used for the pattern.
vec3 starPhotosphere(float mu, vec3 n, StarParams p) {
    mu = clamp(mu, 0.0, 1.0);
    float ld = starLimbDarkening(mu);

    float faculae;
    float cov = starSpotCoverage(n, p, faculae);

    float network;
    // Granulation contrast is largest at the disc centre and vanishes at the limb.
    float g = 0.5 + 0.5 * starGranulation(n, p, network);
    float contrast = mix(0.06, 0.42, mu) * (1.0 - cov);

    float I = ld * (1.0 + contrast * (g - 0.5) * 2.0);
    I *= 1.0 - cov;
    // Magnetic network: brighter lanes, most visible towards the limb.
    I *= 1.0 + network * 0.12 * (1.0 - 0.4 * mu);
    // Faculae are a limb phenomenon.
    I += faculae * 0.45 * ld * (1.0 - 0.6 * mu);
    return starLimbTint(p.color, mu) * max(I, 0.0);
}

// World direction of the point of the corona this ray grazes: the closest point of the ray to the star centre.
// This makes the halo a 3D shell that parallaxes with the camera, instead of a decal on the quad.
vec3 starTangentDir(vec2 q, StarParams p) {
    vec3 off = q.x * p.right + q.y * p.up;
    float rho = length(off);
    // The tangent point is on the camera side of the centre plane; p.view points towards the camera.
    return normalize(off + (rho * rho * p.radiusOverDist) * p.view);
}

// Sine of the latitude of a world direction, in [0,1].
float starLatitude(vec3 m) {
    return abs(dot(m, STAR_SPIN_AXIS));
}

// Impact parameter of the ray through q, in stellar radii: rho for a distant star, less as the camera closes in.
float starImpact(float rho, StarParams p) {
    float t = rho * p.radiusOverDist;
    return rho * inversesqrt(1.0 + t * t);
}

// Impact parameter of the ray grazing the drawn limb (rho == 1); below 1 at any finite distance.
float starLimbImpact(StarParams p) {
    return inversesqrt(1.0 + p.radiusOverDist * p.radiusOverDist);
}

// Chromosphere: a ragged rim just outside the photosphere, spicules from 4D noise on the world direction.
vec3 starChromosphere(float h, vec3 m, StarParams p) {
    if (h < 0.0) {
        return vec3(0.0);
    }
    float spic = stnNoise4(vec4(m * 9.0, p.time * 0.15 + p.seed));
    float fall = exp(-h / mix(0.022, 0.012, p.lod));
    fall *= mix(0.65, 1.35, 0.5 + 0.4 * (spic * 2.0 - 1.0));
    // H-alpha tint, blended in so it still reads as the star's own colour.
    vec3 ha = mix(p.color, vec3(1.0, 0.45, 0.32), 0.55);
    return ha * fall * 0.35;
}

// Corona outside the limb: `c` impact parameter, `h` height above the drawn limb, `m` world direction.
// 4D noise on (world direction, impact parameter) stretches the structure radially and has no poles or rings.
vec3 starCorona(float rho, float c, float h, vec3 m, StarParams p) {
    if (STAR_CORONA <= 0.0) {
        return vec3(0.0);
    }
    if (h < 0.0 || h > STAR_CORONA_REACH) {
        return vec3(0.0);
    }

    // Radial profile: 1 at the limb, 0 at the reach.
    float s = 1.0 - h / STAR_CORONA_REACH;

    // The 4th coordinate drifts with time, which reads as the streamers boiling outward.
    float t4 = -p.time * STAR_CORONA_RATE + c;

    // Coarse field: the large structures, which also modulate the finer octaves.
    float nd = stnNoise4(vec4(m, t4)) * 2.0;
    nd *= nd;

    float n = stnNoise4(vec4(m * STAR_CORONA_RAYS, t4));
    // Fine octave, skipped on small discs; lod changes its amplitude only, never its frequency.
    float nf = 1.0;
    if (p.lod > 0.01) {
        nf = mix(1.0, stnNoise4(vec4(m * STAR_CORONA_RAYS_FINE,
                                    -p.time * STAR_CORONA_RATE * 2.5 + c * 2.0)) * 2.0, p.lod);
    }
    n = n * n * nd * nf;

    // Coronal holes: the field opens up at high latitude, so it is dark there.
    float hole = mix(1.0, STAR_CORONA_HOLE, smoothstep(0.55, 0.95, starLatitude(m)));

    vec3 col = p.color * (STAR_CORONA_RING * pow(s, 4.0) + s * s * n) * hole * STAR_CORONA;

    // Circular screen-space fade before the quad edge, the one term that must stay in the quad plane.
    float fadeEnd = p.quadR * 0.92;
    col *= 1.0 - smoothstep(fadeEnd * 0.75, fadeEnd, rho);
    return max(col, vec3(0.0));
}

// Diffraction spikes, an aperture effect of the bright core.
vec3 starSpikes(vec2 q, StarParams p) {
    if (STAR_SPIKES <= 0.0) {
        return vec3(0.0);
    }
    vec2 a = abs(q);
    // Distance to the nearest of the two axes.
    float d = min(a.x, a.y);
    return p.color * exp(-d * d * 700.0) * exp(-length(q) * 2.0) * STAR_SPIKES;
}

// White core of the sprite, derived from the texture so it matches the sprite's own core at any quad size.
float starCore(float sprite) {
    return smoothstep(0.72, 0.98, sprite);
}

// Unresolved star: the sprite picked in the preferences, as the point stars and the star groups use it.
vec3 starPoint(float sprite, StarParams p) {
    return (p.color + vec3(2.0 * starCore(sprite))) * sprite;
}

// Opacity: full for the disc and the sprite core, 0 for halo, chromosphere and corona (pure emission).
float starAlpha(float rho, float pxR, float sprite, float resolved) {
    float a = smoothstep(0.55, 0.92, sprite) * (1.0 - resolved);
    if (rho < 1.0) {
        // Coverage of the outermost pixel, to anti-alias the limb.
        float aa = 1.0 - smoothstep(1.0 - pxR, 1.0 + pxR, rho);
        a = max(a, aa * resolved);
    }
    return clamp(a, 0.0, 1.0);
}

// 0 while the star is drawn with the sprite, 1 once the model carries more information than the sprite.
float starResolved(float pxR) {
    // Apparent diameter in pixels.
    float pxD = 2.0 / max(pxR, 1e-6);
    return smoothstep(STAR_MODEL_MIN_PX, STAR_MODEL_FULL_PX, pxD);
}

// Size of a pixel in apparent radii, from the screen-space derivative of q.
float starPixelFootprint(vec2 q) {
    return max(length(dFdx(q)), length(dFdy(q)));
}

// Full radiance at offset q: premultiplied colour in rgb, opacity in a.
vec4 starRadiance(vec2 q, float pxR, StarParams p, float sprite) {
    float rho = length(q);
    float resolved = starResolved(pxR);
    // The per-octave weights are derived from the pixel footprint.
    p.pxR = pxR;
    // Detail level for the secondary structures; the granulation is weighted per octave instead.
    p.lod = smoothstep(0.03, 0.01, pxR);

    vec3 glow = starPoint(sprite, p);

    vec3 col = glow * (1.0 - resolved);
    float alpha = starAlpha(rho, pxR, sprite, resolved);

    if (rho < 1.0) {
        // World-space surface normal, so the pattern stays put when the camera moves.
        float z = sqrt(max(0.0, 1.0 - rho * rho));
        vec3 n = normalize(q.x * p.right + q.y * p.up + z * p.view);
        // Coverage of the outermost pixel, to anti-alias the limb.
        float aa = 1.0 - smoothstep(1.0 - pxR, 1.0 + pxR, rho);
        col += starPhotosphere(z, starSpin(n, p), p) * aa * resolved;
    } else {
        // The halo is evaluated on the 3D shell the ray grazes, so it parallaxes like the photosphere.
        vec3 m = starTangentDir(q, p);
        float c = starImpact(rho, p);
        // Height above the drawn limb, measured along the ray.
        float h = c - starLimbImpact(p);
        col += starChromosphere(h, m, p) * resolved;
        col += starCorona(rho, c, h, m, p);
    }
    col += starSpikes(q, p) * resolved;
    return vec4(max(col, vec3(0.0)), alpha);
}

// Tone map with a soft shoulder: identity below 1, asymptotic to 2 above, so the peak matches the star-group path.
vec3 starTonemap(vec3 x) {
    vec3 a = min(x, vec3(1.0));
    vec3 b = max(x - vec3(1.0), vec3(0.0));
    return a + b / (b + vec3(1.0));
}

#endif // GLSL_LIB_STARNEAR
