#ifndef GLSL_LIB_STARNEAR
#define GLSL_LIB_STARNEAR

// Near-field star shading.
//
// Every distance here is in *apparent radii*: a sample point q is the offset
// from the star centre divided by the apparent radius of the photosphere, so
// |q| == 1 exactly at the limb and the corona lives in |q| > 1. Nothing depends
// on the distance to the star, on the size of the quad it is drawn on, or on the
// tessellation of a mesh, so the same functions serve the billboard and the
// sphere surface.
//
// The photosphere pattern is sampled with a normal given in WORLD space, never
// in screen space, so it stays anchored to the star when the camera moves. The
// chromosphere and the corona are anchored to the star direction and the spin
// axis only, so they stay fixed on the celestial sphere when the camera rolls.

#include <shader/lib/luma.glsl>
#include <shader/lib/noise/common.glsl>
#include <shader/lib/noise/curl.glsl>

// Quadratic limb-darkening coefficients (solar-type).
const float STAR_LD_U1 = 0.58;
const float STAR_LD_U2 = 0.12;
// Centre-to-limb reddening, 0 disables it.
const float STAR_LIMB_REDDEN = 0.55;
// Granules over the disc radius. Fixed: the frequency of a noise field must not
// depend on the zoom, see starOctaveWeight().
const float STAR_GRANULE = 42.0;
// Smallest granules.
const float STAR_GRANULE_FINE = 96.0;
// Supergranulation cells, which carry the bright magnetic network.
const float STAR_SUPERGRANULE = 8.0;
// Spot field frequency.
const float STAR_SPOT_SCALE = 2.1;
// Corona brightness, 0 disables it.
const float STAR_CORONA = 0.35;
// How far the corona reaches, in apparent radii from the centre. Clipped to the
// quad, so a small quad simply shows less of it (see StarParams.quadR).
const float STAR_CORONA_REACH = 2.4;
// Diffraction spike amplitude, 0 disables them.
const float STAR_SPIKES = 0.0;
// Rotation of the photosphere, radians per second at full activity, about the
// world Y axis. The Sun turns in ~25 days, i.e. 3e-6 rad/s: the value here is
// deliberately thousands of times faster, because at the real rate nothing on
// the disc visibly moves within a session.
const float STAR_ROTATION = 0.02;
// Differential rotation: the equator turns faster than the poles by this
// fraction. This is what keeps the star from reading as a rigid turntable.
const float STAR_ROTATION_SHEAR = 0.45;
// Granulation evolution, in full cross-fades per second.
const float STAR_GRANULE_RATE = 0.30;
// Spot field evolution, cycles per second.
const float STAR_SPOT_RATE = 0.02;
// Apparent diameter of the disc, in pixels, at which the procedural photosphere
// starts to replace the star sprite, and at which it has fully replaced it. The
// model only carries more information than the sprite once the disc is big
// enough to resolve granules, and it takes over from there: the old 2-6 px
// window left the sprite in charge well past the point where the model is the
// better picture, and made the two disagree in size during the fade. These are
// the two knobs that control when the model appears.
const float STAR_MODEL_MIN_PX = 20.0;
const float STAR_MODEL_FULL_PX = 55.0;
// Cell size, in pixels, below which an octave is faded out. Keeps every octave
// clear of the Nyquist limit of the screen without ever rescaling it.
const float STAR_OCTAVE_MIN_PX = 2.0;

struct StarParams {
    // Linear RGB of the photosphere, peak 1.
    vec3 color;
    // Surface activity in [0,1]: 0 for an A/F star (no spots), 1 for a very
    // active young K/M star.
    float activity;
    // Pattern seed, distinct per star so that neighbours do not look alike.
    float seed;
    // Animation time in seconds.
    float time;
    // Detail level: 1 when the disc is large on screen, 0 when it is a handful
    // of pixels. Drives the amplitude of the secondary structures only; it must
    // never rescale a noise field, or the pattern crawls while zooming.
    float lod;
    // Size of a pixel in apparent radii. Only ever used to fade octaves in and
    // out by amplitude (starOctaveWeight); never as a sampling frequency.
    float pxR;
    // Half extent of the quad in apparent radii: the corona cannot extend past
    // it, and has to fade out well before the quad edge is reached.
    float quadR;
    // Quad axes and direction to the camera, in world space.
    vec3 right;
    vec3 up;
    vec3 view;
    // World-anchored axes of the sky plane: they depend on the star direction
    // and the spin axis only, so patterns sampled with them do not rotate when
    // the camera rolls.
    vec3 skyRight;
    vec3 skyUp;
    // Cosine of the latitude of the star direction with respect to the spin
    // axis, which is what tells the corona where its equator is.
    float cosLat;
};

// Limb darkening as a function of mu = cos(angle from the disc centre).
float starLimbDarkening(float mu) {
    float c = 1.0 - mu;
    return 1.0 - STAR_LD_U1 * c - STAR_LD_U2 * c * c;
}

// Centre-to-limb colour variation: at grazing angles we see higher, cooler
// layers, so the limb reddens as it darkens. Free realism, two lines.
vec3 starLimbTint(vec3 c, float mu) {
    float k = (1.0 - mu) * (1.0 - mu) * STAR_LIMB_REDDEN;
    return mix(c, c * vec3(1.0, 0.82, 0.60), k);
}

vec3 starRotateY(vec3 n, float a) {
    float c = cos(a);
    float s = sin(a);
    return vec3(c * n.x + s * n.z, n.y, -s * n.x + c * n.z);
}

// Rotation of the surface about the spin axis, with differential shear: the
// pattern is wound up rather than turned as a rigid body. Active stars rotate
// faster, which also gives the activity parameter a visible consequence.
vec3 starSpin(vec3 n, StarParams p) {
    float omega = STAR_ROTATION * (0.4 + 0.6 * p.activity);
    float shear = 1.0 - STAR_ROTATION_SHEAR * n.y * n.y;
    return starRotateY(n, p.time * omega * shear);
}

// Cross-faded noise: the field is sampled at two consecutive integer steps and
// blended by the fractional part, so the pattern morphs in place instead of
// drifting across the surface.
float starField(vec3 n, float scale, float rate, float time, float seed) {
    float t = time * rate;
    float t0 = floor(t);
    float f = smoothstep(0.0, 1.0, t - t0);
    vec3 c = n * scale + seed;
    float v0 = gln_simplex_curl(c + vec3(0.0, 0.0, t0 * 6.0));
    float v1 = gln_simplex_curl(c + vec3(0.0, 0.0, (t0 + 1.0) * 6.0));
    return mix(v0, v1, f);
}

// Pixels spanned by one cell of an octave with `cells` cells across the disc
// radius.
float starCellPx(float cells, StarParams p) {
    return 1.0 / max(cells * max(p.pxR, 1e-9), 1e-9);
}

// Weight of an octave: 0 while its cells are under a couple of pixels, 1 once
// the screen resolves them with room to spare.
//
// This scales the AMPLITUDE of a fixed-frequency field, and nothing else. Doing
// the opposite, i.e. turning the pixel footprint into a number of cells and then
// sampling the noise at n * cells, re-parameterises the field on every zoom step:
// the cell count moves by 25-50% per 10% of zoom, so the pattern slides and
// re-forms continuously while the camera dollies, which reads as violent jitter.
// A LOD has to change what is drawn, never where the noise is sampled. The hard
// threshold that used to be visible was simply the point where the cell count hit
// STAR_GRANULE and stopped moving; above it the field was frozen and looked
// pristine, below it every zoom step rescaled it.
float starOctaveWeight(float cells, StarParams p) {
    return smoothstep(STAR_OCTAVE_MIN_PX, STAR_OCTAVE_MIN_PX * 2.0, starCellPx(cells, p));
}

// Granulation value in [-1,1] at the surface normal n, and the bright magnetic
// network on the supergranule cell borders in [0,1] through `network`.
float starGranulation(vec3 n, StarParams p, out float network) {
    float sup = starField(n, STAR_SUPERGRANULE, STAR_GRANULE_RATE * 0.12, p.time, p.seed);
    float gran = starField(n, STAR_GRANULE, STAR_GRANULE_RATE, p.time, p.seed);
    float fine = starField(n, STAR_GRANULE_FINE, STAR_GRANULE_RATE * 1.7, p.time, p.seed + 3.1);

    // Fade whole octaves out as their cells shrink towards the pixel size, then
    // redistribute the freed weight over the ones that remain, so the overall
    // contrast stays constant while zooming instead of collapsing each time an
    // octave drops out.
    float aSup = 0.16;
    float aGran = 0.62 * starOctaveWeight(STAR_GRANULE, p);
    float aFine = 0.22 * starOctaveWeight(STAR_GRANULE_FINE, p);
    float asum = max(aSup + aGran + aFine, 1e-4);

    // The network sits where the supergranule field crosses zero: the lanes
    // between cells, where the magnetic flux collects (and where faculae live).
    // Tied to the granule octave, which is the scale it belongs to, so it fades
    // with that octave instead of with a threshold of its own.
    network = (1.0 - smoothstep(0.0, 0.25, abs(sup))) * starOctaveWeight(STAR_GRANULE, p);

    return (sup * aSup + gran * aGran + fine * aFine) / asum;
}

// Spot coverage in [0,1] (0 = clean photosphere, 1 = umbra) at the surface
// normal n. `faculae` receives the bright rim that surrounds a spot.
float starSpotCoverage(vec3 n, StarParams p, out float faculae) {
    vec3 s = n * STAR_SPOT_SCALE + p.seed * 3.7 + vec3(0.0, 0.0, p.time * STAR_SPOT_RATE);
    float f1 = gln_simplex_curl(s);
    float f2 = gln_simplex_curl(s + vec3(5.3, 2.1, 7.9));
    // The threshold is the activity knob: a quiet star needs a much stronger
    // field to produce a spot, an active one has them all over.
    float th = mix(0.70, 0.28, p.activity);
    float sp = max(f1 - th, 0.0) * max(f2 - th, 0.0);
    float umbra = smoothstep(0.055, 0.180, sp);
    float penumbra = smoothstep(0.0, 0.055, sp) * (1.0 - umbra);
    // Faculae sit right outside the spot, where the field is strong but not
    // yet cool enough to darken the photosphere.
    faculae = smoothstep(0.0, 0.020, sp) * (1.0 - smoothstep(0.020, 0.055, sp));
    // Umbral dots and light bridges: the umbra is not uniformly black, bright
    // structures pierce it.
    float bridge = smoothstep(0.55, 0.90, starField(n, 22.0, 0.0, 0.0, p.seed + 7.7)) * p.lod;
    umbra *= 1.0 - 0.55 * bridge;
    return umbra + 0.55 * penumbra;
}

// Photosphere radiance at the visible hemisphere. `mu` is the cosine between
// the surface normal and the direction to the camera, `n` the surface normal in
// world space (used to look up the surface pattern).
vec3 starPhotosphere(float mu, vec3 n, StarParams p) {
    mu = clamp(mu, 0.0, 1.0);
    float ld = starLimbDarkening(mu);

    float faculae;
    float cov = starSpotCoverage(n, p, faculae);

    float network;
    // Granulation contrast is largest at the disc centre and vanishes at the
    // limb, where the cells are seen edge-on and the dark lanes fill in.
    float g = 0.5 + 0.5 * starGranulation(n, p, network);
    float contrast = mix(0.06, 0.42, mu) * (1.0 - cov);

    float I = ld * (1.0 + contrast * (g - 0.5) * 2.0);
    I *= 1.0 - cov;
    // Magnetic network: brighter lanes between the supergranules, most visible
    // towards the limb, where the line of sight crosses them more obliquely.
    I *= 1.0 + network * 0.12 * (1.0 - 0.4 * mu);
    // Faculae are a limb phenomenon: they brighten the edge of the disc.
    I += faculae * 0.45 * ld * (1.0 - 0.6 * mu);
    return starLimbTint(p.color, mu) * max(I, 0.0);
}

// Angle of an offset q (in quad space) around the star, measured in the
// sky-anchored frame, so that patterns driven by it stay fixed on the sky.
float starSkyAngle(vec2 q, StarParams p) {
    vec3 off = q.x * p.right + q.y * p.up;
    return atan(dot(off, p.skyUp), dot(off, p.skyRight));
}

// Chromosphere: a thin, ragged rim just outside the photosphere. The angular
// noise gives the spicules that make the limb look like a flame rather than a
// smooth ring, and it is sampled in the sky-anchored frame so it does not swim
// when the camera rolls.
vec3 starChromosphere(float rho, vec2 q, StarParams p) {
    float x = rho - 1.0;
    if (x < 0.0) {
        return vec3(0.0);
    }
    float a = starSkyAngle(q, p);
    float spic = gln_simplex_curl(vec3(cos(a) * 9.0, sin(a) * 9.0, p.time * 0.15 + p.seed));
    float fall = exp(-x / mix(0.022, 0.012, p.lod));
    fall *= mix(0.65, 1.35, 0.5 + 0.4 * spic);
    // H-alpha tint, blended in so that it still reads as the star's own colour.
    vec3 ha = mix(p.color, vec3(1.0, 0.45, 0.32), 0.55);
    return ha * fall * 0.35;
}

// Corona in |q| > 1. `tex` is the radial profile texture picked in the
// preferences, used only as a shape modulation.
//
// Two layers: a steep, smooth inner K-corona and radial streamers that carry
// the structure far out.
vec3 starCorona(vec2 q, StarParams p, float tex) {
    vec3 col = vec3(0.0);
    if (STAR_CORONA <= 0.0) {
        return col;
    }
    float rho = length(q);
    float x = rho - 1.0;
    if (x <= 0.0) {
        return col;
    }
    // The corona cannot reach past the quad, and must fade out well before the
    // quad edge is reached, or the square is visible.
    float edge = min(STAR_CORONA_REACH, p.quadR * 0.92);
    if (x > edge) {
        return col;
    }

    float ang = starSkyAngle(q, p);
    // Latitude of the star direction: streamers concentrate near the equator,
    // coronal holes open at the poles.
    float lat = abs(p.cosLat);
    float eq = 1.0 - lat * lat;

    // Streamers: two angular octaves, stretched along the radius by sampling in
    // log(rho), so the structure runs outward instead of looking like a
    // screen-space blob. The fine octave is dropped on small discs.
    float loose = gln_simplex_curl(
            vec3(cos(ang) * 2.4, sin(ang) * 2.4, log(rho) * 3.0 + p.time * 0.03 + p.seed));
    float fine = gln_simplex_curl(
            vec3(cos(ang) * 7.0, sin(ang) * 7.0, log(rho) * 7.0 + p.time * 0.06 + p.seed + 4.1));
    float rays = mix(0.55, 1.0, 0.5 + 0.5 * loose);
    rays *= mix(1.0, mix(0.60, 1.0, 0.5 + 0.5 * fine), p.lod);

    // Inner corona: steep and smooth, much stronger along the equator.
    float k = 1.0 / (1.0 + 9.0 * x);
    float inner = 0.70 * k * k * k * mix(0.40, 1.0, eq);
    // Outer corona: shallow falloff, carries the streamer structure far out.
    // This is what makes the halo read as a corona instead of a glow.
    float outer = 0.40 * pow(rho, -2.4) * mix(0.20, 1.0, eq);

    float shape = mix(1.0, tex, 0.5);
    col = p.color * (inner + outer * rays) * shape * STAR_CORONA;

    // Coronal holes: open field at high latitude, an order of magnitude darker.
    col *= mix(1.0, 0.30, smoothstep(0.55, 0.95, lat));

    // Fade out towards the quad edge, circularly, so no square is ever seen.
    col *= 1.0 - smoothstep(edge * 0.70, edge, x);
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

// White core of the star: the brightest part of the chosen sprite. Deriving it
// from the texture instead of from a fixed fraction of the quad is what makes it
// coincide with the sprite's own core at any quad size. A fixed fraction (the
// 4 % of the quad the star group shader uses) becomes a bright dot of a few
// pixels sitting inside the sprite's much larger core as soon as the quad is
// several photosphere radii across, which is exactly the case here.
float starCore(float sprite) {
    return smoothstep(0.72, 0.98, sprite);
}

// Unresolved star: the pre-rendered sprite the user picked in the preferences,
// sampled in quad space, exactly as the point stars and the star groups do. The
// quad is sized by pointSize and glowFactor so that the sprite subtends the
// apparent size those settings ask for, and the sprite brings the bright core
// that a synthetic gaussian cannot provide.
vec3 starPoint(float sprite, StarParams p) {
    return (p.color + vec3(2.0 * starCore(sprite))) * sprite;
}

// Opacity of the star: full for the photosphere and for the core of the sprite,
// zero for the halo, the chromosphere and the corona, which are pure emission.
// This is what keeps the halo from veiling the background while the disc blocks
// it, in one premultiplied-alpha pass, and it is shared with the depth pass so
// that the two always agree on what is solid. A full alpha over the whole sprite
// would instead turn its halo into a dark veil over everything behind the star.
float starAlpha(float rho, float pxR, float sprite, float resolved) {
    float a = smoothstep(0.55, 0.92, sprite) * (1.0 - resolved);
    if (rho < 1.0) {
        // Coverage of the outermost pixel, to anti-alias the limb.
        float aa = 1.0 - smoothstep(1.0 - pxR, 1.0 + pxR, rho);
        a = max(a, aa * resolved);
    }
    return clamp(a, 0.0, 1.0);
}

// 0 while the star is drawn with the sprite, 1 once the procedural photosphere
// is large enough on screen to carry more information than the sprite.
float starResolved(float pxR) {
    // Apparent diameter in pixels.
    float pxD = 2.0 / max(pxR, 1e-6);
    return smoothstep(STAR_MODEL_MIN_PX, STAR_MODEL_FULL_PX, pxD);
}

// Size of a pixel in apparent radii, from the screen-space derivative of q.
// 1.0 means a single pixel spans the whole radius.
float starPixelFootprint(vec2 q) {
    return max(length(dFdx(q)), length(dFdy(q)));
}

// Full radiance of a near star at offset q.
//
// `sprite` is the value of the star texture picked in the preferences, sampled
// at the star's uv. Returns premultiplied colour in rgb and opacity in a: the
// photosphere and the core of the unresolved sprite are opaque (they replace
// whatever is behind them), while the corona, the chromosphere and the sprite
// halo are pure emission (a == 0, so they are simply added and never dim the
// background).
vec4 starRadiance(vec2 q, float pxR, StarParams p, float sprite) {
    float rho = length(q);
    float resolved = starResolved(pxR);
    // Passed to the surface functions: the per-octave weights are derived from it.
    p.pxR = pxR;
    // Detail level for the secondary structures (umbral dots, chromosphere width,
    // the finest corona octave). The granulation no longer uses it: its octaves
    // are weighted individually, see starOctaveWeight().
    p.lod = smoothstep(0.03, 0.01, pxR);

    vec3 glow = starPoint(sprite, p);

    vec3 col = glow * (1.0 - resolved);
    float alpha = starAlpha(rho, pxR, sprite, resolved);

    if (rho < 1.0) {
        // Surface normal of the visible hemisphere, in world space, so the
        // pattern stays put when the camera rotates or rolls.
        float z = sqrt(max(0.0, 1.0 - rho * rho));
        vec3 n = normalize(q.x * p.right + q.y * p.up + z * p.view);
        // Coverage of the outermost pixel, to anti-alias the limb.
        float aa = 1.0 - smoothstep(1.0 - pxR, 1.0 + pxR, rho);
        col += starPhotosphere(z, starSpin(n, p), p) * aa * resolved;
    } else {
        col += starChromosphere(rho, q, p) * resolved;
        col += starCorona(q, p, sprite);
    }
    col += starSpikes(q, p) * resolved;
    return vec4(max(col, vec3(0.0)), alpha);
}

// Tone map with a soft shoulder: identity below 1, asymptotic to 2 above, so
// the peak of the near field matches the star-group path and no step appears
// at the boundary, while the disc structure survives instead of clipping to
// flat white.
vec3 starTonemap(vec3 x) {
    vec3 a = min(x, vec3(1.0));
    vec3 b = max(x - vec3(1.0), vec3(0.0));
    return a + b / (b + vec3(1.0));
}

#endif // GLSL_LIB_STARNEAR
