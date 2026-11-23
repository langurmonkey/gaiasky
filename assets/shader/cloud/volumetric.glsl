#ifndef GLSL_CLOUD_VOLUMETRIC
#define GLSL_CLOUD_VOLUMETRIC

/*
 * Volumetric clouds: raymarch of the layer between surface and shell, in the object's frame.
 * Coverage is sampled per step and a distance LOD cross-fades with the shell path.
 */

// Vertical optical depth of a fully covered column: the band fills VC_BAND_MEAN of the slab, so
// the extinction inside it is scaled up by 1 / VC_BAND_MEAN.
#define VC_TAU 7.6
// Dual-lobe Henyey-Greenstein.
#define VC_G_FORWARD 0.60
#define VC_G_BACK -0.25
// Single-scattering albedo of the droplets.
#define VC_ALBEDO 0.95
// Gain and anisotropy of the in-scattering term. See vcPhase.
#define VC_PHASE_GAIN 3.0
#define VC_PHASE_ANISO 0.30
// Cheap stand-in for multiple scattering: without it the thick parts read as grey smoke.
#define VC_MULTIPLE_SCATTERING 0.45
// Ambient and sky light, so the night side is dark but not black and the underside of the layer
// stays visible when the sun is on the other side.
#define VC_AMBIENT 0.14
#define VC_SKY_UNDERSIDE 0.32
// Bottom of the cloud band as a fraction of the slab thickness. Any lower and the deck sits on
// the ground, reading as fog.
#define VC_BASE 0.24
// Thickness of the band, as a fraction of the slab, before the variation below.
#define VC_BAND 0.52
// Mean fraction of the slab actually occupied by the band, used to keep the column's optical depth
// unchanged by the confinement (see VC_TAU).
#define VC_BAND_MEAN 0.42
// Vertical mass distribution: pow(1 - h, VC_TAPER_POW) times (VC_TAPER_POW + 1), so the mean over
// the column is one and its optical depth is untouched.
#define VC_TAPER_POW 1.5
#define VC_TAPER_GAIN (VC_TAPER_POW + 1.0)
// Marching budget.
#define VC_MAX_STEPS 24
#define VC_SUN_STEPS 4
// Upper bound on the optical depth a shadow ray is allowed to search for. Past about four, the
// two-stream transmission below is under a few per cent and the rest of the ray cannot be seen.
#define VC_SUN_OD_MAX 4.0
// Longest stretch of a ray that is marched, in layer thicknesses: a horizon view from the ground
// crosses dozens, and past the first few the column is already opaque.
#define VC_MAX_PATH 8.0
// Coverage contrast: below VC_COV_LO is clear sky, above VC_COV_HI is solid cloud. A fraction of a
// pixel that is cloudy is not a fraction of a cloud.
#define VC_COV_LO 0.12
#define VC_COV_HI 0.55
// Pedestal of the coverage map: subtracted, or the layer becomes a grey veil over clear ground.
#define VC_COVERAGE_BASE 0.10
// Width of the cloud map in texels, the equirectangular way: a 2048x2048 cubemap lands here too.
#define VC_MAP_WIDTH 8192.0

// (planetRadius, layerBaseRadius, layerTopRadius, thickness), in internal units, object frame.
uniform vec4 u_cloudShell;
uniform mat4 u_worldTrans;
// Camera in the camera-relative world frame. vec4 to match the vertex stage, or the link fails.
uniform vec4 u_cameraPosition;
uniform float u_time;
// 1 when this layer is marched, 0 when the shell shader draws it (FloatAttribute.CloudVolumetric).
uniform float u_cloudVolumetric;

// Value noise in [0, 1]. Local on purpose: lib/noise calls mod(genType, float), which NVIDIA
// resolves to math.glsl's scalar overload and then refuses.
float vcHash13(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

float vcNoise(vec3 p) {
    vec3 i = floor(p);
    vec3 f = p - i;
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(vcHash13(i), vcHash13(i + vec3(1.0, 0.0, 0.0)), f.x),
                   mix(vcHash13(i + vec3(0.0, 1.0, 0.0)), vcHash13(i + vec3(1.0, 1.0, 0.0)), f.x), f.y),
               mix(mix(vcHash13(i + vec3(0.0, 0.0, 1.0)), vcHash13(i + vec3(1.0, 0.0, 1.0)), f.x),
                   mix(vcHash13(i + vec3(0.0, 1.0, 1.0)), vcHash13(i + vec3(1.0, 1.0, 1.0)), f.x), f.y), f.z);
}

// Zero-mean fBm of the given number of octaves, in [-1, 1].
float vcFbm(vec3 p, int octaves) {
    float sum = 0.0;
    float amp = 0.5;
    float norm = 0.0;
    for (int i = 0; i < 6; i++) {
        if (i >= octaves) {
            break;
        }
        sum += amp * (vcNoise(p) * 2.0 - 1.0);
        norm += amp;
        p *= 2.03;
        amp *= 0.5;
    }
    return sum / max(norm, 1e-4);
}

// Ray-sphere intersection, sphere at the origin. The near root is negative when the origin is inside.
vec2 vcSphereIntersect(vec3 ro, vec3 rd, float r) {
    float b = dot(ro, rd);
    float c = dot(ro, ro) - r * r;
    float h = b * b - c;
    if (h < 0.0) {
        return vec2(-1.0);
    }
    h = sqrt(h);
    return vec2(-b - h, -b + h);
}

// Equirectangular uv of a direction, matching the sphere's own vertex texcoords (SphereCreator).
vec2 vcEquirect(vec3 dir) {
    float v = acos(clamp(dir.y, -1.0, 1.0)) / PI;
    float u = 1.0 - atan(dir.z, dir.x) / (2.0 * PI);
    return vec2(u - floor(u), v);
}

// Coverage in [0, 1] from the cloud map at an explicit mip level, with only the pedestal removed.
float vcCoverage(vec3 dir, float lod) {
    vec4 c = fetchCloudColorLod(vcEquirect(dir), lod);
    float raw = saturate((c.a * luma(c.rgb) - VC_COVERAGE_BASE) / (1.0 - VC_COVERAGE_BASE));
    return smoothstep(VC_COV_LO, VC_COV_HI, raw);
}

// Mip level for the coverage lookups: computed once per fragment, from values every fragment
// shares. Derivatives taken inside the march loop are garbage.
float vcCoverageLod(vec3 ro, vec3 rd, float rTop) {
    float angPix = max(length(dFdx(rd)), length(dFdy(rd)));
    // Incidence where the ray meets the top of the layer, at worst grazing.
    vec2 ti = vcSphereIntersect(ro, rd, rTop);
    float tEntry = max(ti.x, 0.0);
    vec3 n = normalize(ro + rd * tEntry);
    float inc = max(abs(dot(n, rd)), 0.15);
    // Fraction of the map width covered by one pixel. The distance factor keeps the map from being
    // sampled one mip too sharp per factor of two of altitude.
    float uvSpan = angPix * max(tEntry, 1.0e-6) / rTop / inc / (2.0 * PI);
    return clamp(log2(max(uvSpan * VC_MAP_WIDTH, 1.0)), 0.0, 8.0);
}

// Distance from the camera to the top of the layer, in layer thicknesses. Negative inside. Drives
// how much fine detail the march can show and therefore how much it pays for.
float vcAlt = 0.0;
// The same distance remapped to 0 up close and 1 far away, where one pixel covers several texels of
// the map and a threshold on the shape field can only produce grain.
float vcFar = 0.0;

// Density at p for a coverage of cov; `u` is the height fraction in the layer. Cheapest first:
// the shape field decides whether there is anything, and only then are the fields below evaluated.
float vcDensity(vec3 p, float u, float cov) {
    if (cov <= 0.002) {
        return 0.0;
    }
    // Everything is in kilometres, in the planet's own frame, so the shapes have a physical size
    // rather than one tied to the layer.
    vec3 q = p / u_kmToU;

    // Is the point inside the deck? Cheapest test first: two octaves of a low-frequency field,
    // squashed vertically so that it sets the height each column reaches.
    float lump = vcFbm(vec3(q.x, q.y * 0.30, q.z) / 60.0 + 21.0, 2) * 0.5 + 0.5;
    float base = VC_BASE + 0.10 * lump;
    float span = VC_BAND * (0.40 + 1.00 * lump);
    // Capped at the top of the slab, and the cap becomes the column's height: a column that wanted to
    // be taller than the volume would otherwise be cut off flat against the marching boundary.
    float top = min(base + span, 0.99);
    span = top - base;
    // Soft at both ends: a sixth of the column at the bottom and a fifth at the top.
    float profile = smoothstep(base, base + 0.16 * span, u) * (1.0 - smoothstep(top - 0.20 * span, top, u));
    if (profile <= 0.0) {
        return 0.0;
    }

    // Vertical mass distribution within the column, of mean one over its height, so the column's
    // optical depth is unchanged.
    float h = saturate((u - base) / max(span, 1e-3));
    float taper = VC_TAPER_GAIN * pow(1.0 - h, VC_TAPER_POW);

    // Filtered by distance like a texture: far out the field is coarse and its spread is stretched
    // back. Branch, not blend: `vcFar` is constant across the primitive.
    vec3 qs = vec3(q.x, q.y * 1.4, q.z);
    float shape;
    if (vcFar >= 0.999) {
        shape = saturate((vcFbm(qs / 140.0 + 3.7, 3) * 0.5 + 0.5 - 0.5) * 1.9 + 0.5);
    } else if (vcFar <= 0.001) {
        shape = vcFbm(qs / 35.0 + 3.7, 4) * 0.5 + 0.5;
    } else {
        shape = mix(vcFbm(qs / 35.0 + 3.7, 4) * 0.5 + 0.5,
                    saturate((vcFbm(qs / 140.0 + 3.7, 3) * 0.5 + 0.5 - 0.5) * 1.9 + 0.5),
                    vcFar);
    }

    // Two mappings of coverage to mass, blended by distance: up close, the shape field cut by a
    // floor the coverage lowers; far out, mass proportional to the coverage, which survives blurring.
    float floorv = 0.75 * pow(1.0 - cov, 1.6);
    // Gated by the coverage, or the top quarter of the noise field is cloud over clear ground.
    float gate = smoothstep(0.0, 0.20, cov);
    float dNear = gate * saturate((shape - floorv) / max(1.0 - floorv, 1e-3));
    // Far out, the column attenuates exactly as much as the coverage says: the optical depth is
    // -log(1 - cov), and the shape field is applied as a factor of mean one.
    float tauTarget = -log(max(1.0 - cov, 0.004));
    float dFar = (tauTarget / VC_TAU) * (0.50 + shape);
    float d = mix(dNear, dFar, vcFar);
    if (d <= 0.0) {
        return 0.0;
    }

    // Twelve kilometre detail, faded out with `vcFar`: finer than a pixel is grain by construction.
    float detail = 0.65 + 0.70 * (vcFbm(q / 12.0 + 11.2, 2) * 0.5 + 0.5);
    d *= mix(1.0, detail, 1.0 - vcFar);

    // Erosion at four and at one kilometre, each faded out at the altitude where it becomes a
    // sub-pixel shimmer.
    float nearA = 1.0 - smoothstep(1.2, 4.0, vcAlt);
    if (nearA > 0.0) {
        float f1 = vcFbm(q / 4.0 + 27.0, 3) * 0.5 + 0.5;
        d *= mix(1.0, saturate(1.9 * f1 - 0.25), nearA);
    }
    float nearB = 1.0 - smoothstep(0.30, 1.2, vcAlt);
    if (nearB > 0.0) {
        float f2 = vcFbm(q / 1.2 + 51.0, 2) * 0.5 + 0.5;
        d *= mix(1.0, saturate(1.9 * f2 - 0.25), nearB);
    }
    return saturate(d) * profile * taper;
}

// Density for a shadow ray: two octaves of the same field, so a sample that is empty for the view
// is empty here. Anything more would double the price of the march.
float vcDensityLight(vec3 p, float cov) {
    if (cov <= 0.002) {
        return 0.0;
    }
    // Same band as the view ray, on the mean column: without it samples above the deck would count
    // as occluding, and with a flat band the cloud tops come out too dark.
    float u = (length(p) - u_cloudShell.x) / max(u_cloudShell.w, 1e-9);
    float hm = saturate((u - VC_BASE) / VC_BAND_MEAN);
    float band = smoothstep(VC_BASE - 0.06, VC_BASE + 0.06, u) * VC_TAPER_GAIN * pow(1.0 - hm, VC_TAPER_POW);
    if (band <= 0.0) {
        return 0.0;
    }
    vec3 q = p / u_kmToU;
    float shape = vcFbm(vec3(q.x, q.y * 1.4, q.z) / 35.0 + 3.7, 2) * 0.5 + 0.5;
    float floorv = 0.75 * pow(1.0 - cov, 1.6);
    return band * saturate((shape - floorv) / max(1.0 - floorv, 1e-3));
}

// Optical depth from p towards the sun; `planetShadow` is 0 when the planet is in the way, which
// gives the terminator. The coverage is sampled along the ray, which is what self-shadows the deck.
float vcSunOpticalDepth(vec3 p, vec3 sunDir, float lod, out float planetShadow) {
    planetShadow = 1.0;
    if (vcSphereIntersect(p, sunDir, u_cloudShell.x).x > 0.0) {
        planetShadow = 0.0;
        return 1.0e6;
    }
    float thick = max(u_cloudShell.w, 1e-9);
    float tEnd = min(vcSphereIntersect(p, sunDir, u_cloudShell.z).y, thick * 4.0);
    float stepLen = max(tEnd, 1e-6) / float(VC_SUN_STEPS);
    float t = stepLen * 0.5;
    // Accumulated in kilometres and compared against a budget in kilometres.
    float odMax = VC_SUN_OD_MAX * thick / VC_TAU;
    float od = 0.0;
    for (int i = 0; i < VC_SUN_STEPS; i++) {
        vec3 q = p + sunDir * t;
        od += vcDensityLight(q, vcCoverage(normalize(q), lod)) * stepLen;
        if (od >= odMax) {
            return od;
        }
        t += stepLen;
    }
    return od;
}

float vcHg(float mu, float g) {
    float g2 = g * g;
    float d = max(1.0 + g2 - 2.0 * g * mu, 1e-4);
    return (1.0 - g2) / (4.0 * PI * d * sqrt(d));
}

// Dual-lobe Henyey-Greenstein: a single forward lobe is far too dark away from the sun, and the
// normalisation is a fudge because the orders this does not trace are what make clouds white.
float vcPhase(float mu) {
    // In-scattering against the angle between view and light: the lobe is kept for its shape and
    // flattened almost to a constant, so the forward cone is the only thing that varies.
    float dual = (2.0 * vcHg(mu, VC_G_FORWARD) + vcHg(mu, VC_G_BACK)) * (4.0 * PI / 3.0);
    return VC_PHASE_GAIN * mix(1.0, dual, VC_PHASE_ANISO);
}

// Whether the volumetric path is used for this fragment.
bool vcEnabled() {
    return u_cloudVolumetric >= 0.5;
}

// Scene depth, so real geometry in front of the layer clips the march. Same uniforms as the volume
// path (VolumeSceneDepth, VolumeDepthMaskScale); a scale of zero means nothing is bound.
uniform sampler2D u_volumeSceneDepth;
uniform vec2 u_volumeDepthMaskScale;

// Distance along the ray at which the scene occludes the layer, or 0. The buffer holds log depth,
// so getDepthValue rebuilds this fragment's own value from gl_FragCoord.w.
float vcOcclusionDistance() {
    if (u_volumeDepthMaskScale.x <= 0.0) {
        return 0.0;
    }
    ivec2 texel = ivec2(gl_FragCoord.xy * u_volumeDepthMaskScale);
    float scene = texelFetch(u_volumeSceneDepth, texel, 0).r;
    float mine = getDepthValue(u_cameraNearFar.y, u_cameraK);
    // 1e-5 is above the quantum of a 24-bit depth buffer.
    if (!(scene < mine - 1.0e-5)) {
        return 0.0;
    }
    // The buffer holds view depth; the march is parametrised by distance along the ray.
    float viewDist = 1.0 / recoverWValue(scene, u_cameraNearFar.y, u_cameraK);
    float rayScale = length(v_fragPosWorld - u_cameraPosition.xyz) * gl_FragCoord.w;
    return viewDist * rayScale;
}

// --- Far field: the shell path, evaluated for this ray. -------------------------------------

// Day factor of the shell path: world-space dot of the surface normal and the to-light direction.
float vcDayFactor(vec3 toLight, vec3 n) {
    float NL = dot(toLight, n);
    return 1.0 - linstep(-0.25, 0.12, -NL);
}

// Direction to the light the shell picks for its eclipse term: the negated first directional
// light, or the first point light.
vec3 vcShellLightDir(vec3 pWorld) {
    #ifdef directionalLightsFlag
    if (numDirectionalLights > 0 && any(notEqual(u_dirLights[0].color, vec3(0.0)))) {
        return -u_dirLights[0].direction;
    }
    #endif // directionalLightsFlag
    #ifdef pointLightsFlag
    if (numPointLights > 0) {
        return normalize(u_pointLights[0].position - pWorld);
    }
    #endif // pointLightsFlag
    return vec3(0.0, 1.0, 0.0);
}

// Limb clip of the textured layer: 1 where the planet is behind the clouds, 0 where the ray clears
// it. Keyed on `b`, the closest approach, which is smooth in pixels across a silhouette.
float vcShellLimb(vec3 ro, vec3 rd) {
    float rGround = u_cloudShell.x;
    float rTop = max(u_cloudShell.z, rGround * 1.0001);
    float b = length(ro - rd * dot(ro, rd));
    return 1.0 - smoothstep(rGround, rTop, b);
}

// What the shell shader would paint where this ray enters the layer. `nObj` is only for the map;
// the lights and the eclipse are world-space and have to be taken with `nWorld`.
vec4 vcShellLayer(vec3 ro, vec3 rd, vec3 nObj, vec3 nWorld, vec3 pWorld, float distU, float lod) {
    vec4 cloud = fetchCloudColorLod(vcEquirect(nObj), lod);

    // Lights, summed the way the shell path sums them.
    vec3 litColor = vec3(0.0);
    #ifdef directionalLightsFlag
    for (int i = 0; i < numDirectionalLights; i++) {
        litColor += u_dirLights[i].color * vcDayFactor(normalize(u_dirLights[i].direction), nWorld);
    }
    #endif // directionalLightsFlag
    #ifdef pointLightsFlag
    for (int i = 0; i < numPointLights; i++) {
        vec3 toLight = normalize(u_pointLights[i].position - pWorld);
        litColor += u_pointLights[i].color * u_pointLights[i].intensity * vcDayFactor(toLight, nWorld);
    }
    #endif // pointLightsFlag

    vec3 c = cloud.rgb * clamp(length(litColor + v_ambientLight), 0.03, 1.0);
    c *= v_opacity;

    // Eclipses, with the shell path's own choice of light.
    #ifdef eclipsingBodyFlag
    {
        float eclshdw;
        vec3 diffractionTint;
        int outline;
        eclipseColor(pWorld, vcShellLightDir(pWorld), nWorld, outline, diffractionTint, eclshdw);
        c = eclipseBlend(c, diffractionTint, eclshdw);
    }
    #endif // eclipsingBodyFlag

    // Limbal fade, same parameters as the shell path: at most 25 pixels, faded in with the fragment
    // distance - camera to fragment, not centre to fragment - and the radius of the body.
    float toKm = 1.0 / u_kmToU;
    float radiusKm = u_generic1 * toKm;
    float distKm = distU * toKm;
    float fadePixels = (1.0 - smoothstep(radiusKm / 6.0, radiusKm * 1.5, distKm)) * 25.0;
    c *= cloudLimbFade(-rd, nObj, fadePixels);

    // Limb clip: no clouds where there is no ground behind them. After the rim fade, before the
    // alpha is taken from the colour, so it fades the coverage and not just the brightness.
    c *= vcShellLimb(ro, rd);

    float a = clamp(luma(c), 0.0, 1.0);
    return a > 1.0e-4 ? vec4(c / a, a) : vec4(0.0);
}

// Shades the layer and writes the fragment outputs. Straight alpha out: the radiance of what is
// seen and how much of it is covered.
void vcRender() {
    float R = u_cloudShell.x;
    float R1 = u_cloudShell.z;
    float thick = max(u_cloudShell.w, 1e-9);

    // Object frame. u_cameraPosition and v_fragPosWorld are both camera-relative world positions, so
    // subtracting the object's own translation is what puts the sphere at the origin here.
    mat4 W = u_worldTrans;
    mat3 rot = mat3(normalize(W[0].xyz), normalize(W[1].xyz), normalize(W[2].xyz));
    vec3 ro = transpose(rot) * (u_cameraPosition.xyz - W[3].xyz);
    vec3 rdWorld = normalize(v_fragPosWorld - u_cameraPosition.xyz);
    vec3 rd = normalize(transpose(rot) * (v_fragPosWorld - u_cameraPosition.xyz));

    // Pixel size and mip level for the map, both from derivatives, and therefore before any discard.
    float lod = vcCoverageLod(ro, rd, R1);

    // Camera altitude in layer thicknesses, from the ray origin in the object frame. Not from
    // u_cameraPosition.xyz, which is zero for every fragment.
    vcAlt = (length(ro) - R1) / thick;
    vcFar = smoothstep(3.0, 10.0, vcAlt);

    // Entry and exit of the analytic sphere, not of the proxy mesh: inside the layer the entry is
    // the camera itself.
    vec2 tOuter = vcSphereIntersect(ro, rd, R1);
    if (tOuter.y <= 0.0) {
        discard;
    }
    float t0 = max(tOuter.x, 0.0);
    float t1 = tOuter.y;

    // Where the ray enters the layer: the point the shell path would have shaded. The normal and
    // the uv come from here, never from the antipode's v_normal or v_texCoord0.
    vec3 nObj = normalize(ro + rd * t0);
    vec3 nWorld = normalize(rot * nObj);
    vec3 pEntryWorld = v_fragPosWorld - rdWorld * max(tOuter.y - t0, 0.0);

    // Stop at the planet: clipping on the sphere rather than on the scene depth (see the header).
    vec2 tPlanet = vcSphereIntersect(ro, rd, R);
    if (tPlanet.x > 0.0) {
        t1 = min(t1, tPlanet.x);
    }
    // Stop at whatever the scene has already drawn nearer than the far wall, and give up entirely if
    // that is in front of the layer: terrain above the base, a nearer body, another cloud layer.
    float occl = vcOcclusionDistance();
    if (occl > 0.0) {
        if (occl <= t0) {
            discard;
        }
        t1 = min(t1, occl);
    }
    if (t1 <= t0) {
        discard;
    }

    // Far/near split, keyed on the camera's altitude above the layer: one number for the whole
    // body, so no boundary is drawn on the disc. Pinned to the same altitude that drives `vcFar`.
    const float VC_LOD_ALT_LO = 1.2; // below this altitude (in thicknesses): the march
    const float VC_LOD_ALT_HI = 4.5; // above it: the shell
    float fade = 1.0 - smoothstep(VC_LOD_ALT_LO, VC_LOD_ALT_HI, vcAlt);
    // Camera inside the layer: there is nothing to fall back to, because the map would be sampled
    // from behind the eye. The march is the only path that means anything here.
    if (length(ro) < R1) {
        fade = 1.0;
    }

    // Far end of the transition: the shell path. Skipped only when the fade has pushed it out.
    vec4 shellCol = vec4(0.0);
    if (fade < 0.999) {
        shellCol = vcShellLayer(ro, rd, nObj, nWorld, pEntryWorld, t0, lod);
    }

    // Near end: the march. Runs only when the fade asks for it, which is why the whole body is
    // inside a branch - and why none of it may discard, since the shell layer may still be visible.
    vec3 marchRGB = vec3(0.0);
    float marchA = 0.0;
    if (fade > 0.001) {
        t1 = min(t1, t0 + VC_MAX_PATH * thick);

        // Sun. u_dirLights[].direction is the direction the light travels, so it has to be negated:
        // the point-light branch is already a to-the-light vector.
        vec3 sunDir = vec3(0.0, 1.0, 0.0);
        vec3 sunCol = vec3(0.0);
        #ifdef directionalLightsFlag
        if (numDirectionalLights > 0) {
            sunDir = normalize(transpose(rot) * (-u_dirLights[0].direction));
            sunCol = u_dirLights[0].color;
        }
        #endif // directionalLightsFlag
        #ifdef pointLightsFlag
        if (dot(sunCol, sunCol) < 1e-6 && numPointLights > 0) {
            sunDir = normalize(transpose(rot) * (u_pointLights[0].position - v_fragPosWorld));
            sunCol = u_pointLights[0].color * u_pointLights[0].intensity;
        }
        #endif // pointLightsFlag

        // Eclipses, evaluated once where the ray enters the layer and applied to the whole column.
        float eclShadow = 1.0;
        vec3 eclTint = vec3(0.0);
        #ifdef eclipsingBodyFlag
        {
            float eclshdw;
            vec3 diffractionTint;
            int outline;
            eclipseColor(pEntryWorld, vcShellLightDir(pEntryWorld), nWorld, outline, diffractionTint, eclshdw);
            eclShadow = eclshdw;
            eclTint = diffractionTint;
        }
        #endif // eclipsingBodyFlag

        float rayLen = t1 - t0;
        int nSteps = clamp(int(ceil(rayLen / max(thick * 0.33, 1e-12))), 8, VC_MAX_STEPS);
        float stepLen = rayLen / float(nSteps);

        // Coverage, one fetch: from orbit the layer is a couple of pixels thick, so the column is
        // the one where the ray crosses the top. The per-sample path below takes over up close.
        float lodRay = lod;
        float covRay = vcCoverage(normalize(ro + rd * t0), lodRay);

        // A grazing ray steps through the map every `stepLen`, which can be several texels: filter
        // down to the spacing of the samples, or the march aliases into grain along the limb.
        lod = max(lod, log2(max(stepLen / (2.0 * PI * R / VC_MAP_WIDTH), 1.0)));
        // Stagger the first sample per pixel: the layer is tens of samples thick now, so the dither
        // hides the banding of a fixed step instead of turning into noise.
        float t = t0 + stepLen * vcHash13(vec3(gl_FragCoord.xy, u_time)) * (1.0 - vcFar);

        float mu = dot(rd, sunDir);
        float phase = VC_ALBEDO * vcPhase(mu);
        vec3 ambient = v_ambientLight * VC_AMBIENT;
        vec3 sky = v_ambientLight * VC_SKY_UNDERSIDE;

        float transp = 1.0;
        vec3 color = vec3(0.0);

        for (int i = 0; i < VC_MAX_STEPS; i++) {
            if (t >= t1 || transp < 0.02) {
                break;
            }
            vec3 p = ro + rd * t;
            // Coverage at the sample's own place on the sphere when the layer has depth on screen, and
            // the single entry fetch when it does not (see above).
            float cov = vcFar > 0.5 ? covRay : vcCoverage(normalize(p), lod);
            float d = vcDensity(p, (length(p) - R) / thick, cov);
            if (d > 0.001) {
                float sigma = VC_TAU / thick * d;
                float a = 1.0 - exp(-sigma * stepLen);

                float planetShadow;
                // The shadow ray reads the map one mip coarser, or the coverage wobble turns into
                // per-pixel grain over the whole lit face.
                float od = vcSunOpticalDepth(p, sunDir, max(lod, lodRay) + 1.0, planetShadow);
                // Two-stream transmission rather than exp(-tau): an exponential is all steepness,
                // and turns the same wobble into salt and pepper.
                float tsun = 1.0 / (1.0 + od * VC_TAU / thick);
                float ms = VC_MULTIPLE_SCATTERING * (1.0 - tsun);
                // Powder: the rim of a cloud facing the light is darker than its body.
                float powder = 1.0 - 0.55 * exp(-2.5 * d);
                vec3 light = sunCol * planetShadow * (tsun * phase + ms) * powder
                             + ambient
                             + sky * (1.0 - planetShadow * 0.5);

                color += transp * a * light;
                transp *= 1.0 - a;
            }
            t += stepLen;
        }

        float alpha = 1.0 - transp;
        if (alpha >= 0.004) {
            // Eclipse, applied to the accumulated column and not to each sample, weighted by the
            // coverage the column actually has.
            #ifdef eclipsingBodyFlag
            color = eclipseBlend(color, eclTint * alpha, eclShadow);
            #endif // eclipsingBodyFlag

            // Straight alpha; opacity fades the coverage, not the colour, so a faded cloud goes
            // transparent instead of turning grey.
            marchA = alpha * v_opacity;
            marchRGB = min(color / max(alpha, 1e-4), vec3(1.4));
        }
    }

    // Cross-fade of the two layers, premultiplied: the shell carries its coverage in the colour and
    // the march in its alpha.
    float shellW = (1.0 - fade) * shellCol.a;
    float marchW = fade * marchA;
    float alpha = shellW + marchW;
    if (alpha < 0.004) {
        discard;
    }

    // No depth is written: the atmosphere is drawn after this pass and writes the depth of its
    // shell, so a cloud depth here would blank out the sky above the deck.

    fragColor = vec4((shellCol.rgb * shellW + marchRGB * marchW) / alpha, alpha);
    layerBuffer = vec4(0.0, 0.0, 0.0, 1.0);
}

#endif // GLSL_CLOUD_VOLUMETRIC
