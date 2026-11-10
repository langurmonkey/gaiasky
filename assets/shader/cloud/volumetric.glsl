#ifndef GLSL_CLOUD_VOLUMETRIC
#define GLSL_CLOUD_VOLUMETRIC

/*
 * Volumetric clouds (M1). See notes/gaiasky-volumetric-clouds-brief.md.
 *
 * Marches the layer between the planet surface and the top of the cloud volume, in the object's own
 * frame. The horizontal placement comes from the cloud map the shell already uses, sampled *at every
 * step of the march*, and the three-dimensional shape from fBm stretched to the layer's dimensions.
 * No new data is needed and nothing outside this shader changes: bodies that do not opt in never get
 * here.
 *
 * The coverage must be sampled per step. Sampling it once per pixel - at the middle of the ray, say
 * - makes the map a vertical extrusion: the column either has cloud or it does not, from the ground
 * to the top of the layer, and what comes out are flat discs seen from above and pillars with sharp
 * vertical sides seen from inside. That was the first implementation.
 *
 * Geometry. The drawn proxy is a *flipped* sphere enclosing the top of the layer, with back faces
 * culled, so the fragment that reaches this shader sits on the far wall of the volume - which is in
 * front of the camera whether the camera is outside the layer or has flown into it. That is the
 * aurora's arrangement, and it is what makes the clouds survive the camera entering the shell. The
 * depth test is off for this material and no depth is written: the far wall is behind whatever the
 * cloud covers. Occlusion is analytic instead - the ray is clipped against the planet sphere.
 *
 * Missing from M1, by design: the ray is clipped by the planet *sphere*, not by the scene depth, so
 * clouds still draw in front of terrain that stands above the layer base (M1.5); and the density
 * comes from a coverage channel plus noise rather than a baked four-channel map (M2).
 */

// Vertical optical depth of a fully covered column, relative to the layer thickness.
#define VC_TAU 3.2
// Dual-lobe Henyey-Greenstein.
#define VC_G_FORWARD 0.60
#define VC_G_BACK -0.25
// Single-scattering albedo of the droplets.
#define VC_ALBEDO 0.95
// Cheap stand-in for multiple scattering: without it the thick parts read as grey smoke.
#define VC_MULTIPLE_SCATTERING 0.45
// Ambient and sky light, so the night side is dark but not black and the underside of the layer
// stays visible when the sun is on the other side.
#define VC_AMBIENT 0.14
#define VC_SKY_UNDERSIDE 0.32
// Marching budget.
#define VC_MAX_STEPS 24
#define VC_SUN_STEPS 4
// Upper bound on the optical depth a shadow ray is allowed to search for. Past about four, the
// two-stream transmission below is under a few per cent and the rest of the ray cannot be seen.
#define VC_SUN_OD_MAX 4.0
// Longest stretch of a ray that is marched, in layer thicknesses: a horizon view from the ground
// crosses dozens, and past the first few the column is already opaque.
#define VC_MAX_PATH 8.0
// Pedestal of the coverage map: cloud maps are drawn to be composited, so their background sits
// above zero and their clouds nowhere near one. The Earth cubemap bottoms out at 0 and its clouds
// peak at about 0.8. Leaving the pedestal in matters more than it looks: the march multiplies
// coverage by the optical depth of a *full* column, so a 3 % pedestal over 95 km is already a tenth
// of a unit of optical depth everywhere, and the whole layer becomes a grey veil over ground the map
// says is clear.
#define VC_COVERAGE_BASE 0.10
// Width of the cloud map, in texels, expressed the equirectangular way: how many texels it takes to
// go all the way round. A cubemap of six 2048x2048 faces resolves 2048 texels per 90 degrees, which
// is the same angular resolution as an 8192x4096 equirectangular map, so both cases land here.
// Getting this wrong by a factor of two costs one mip level, which close up is the difference
// between a cloud and a smudge.
#define VC_MAP_WIDTH 8192.0

// (planetRadius, layerBaseRadius, layerTopRadius, thickness), in internal units, object frame.
uniform vec4 u_cloudShell;
uniform mat4 u_worldTrans;
// Camera in the camera-relative world frame. vec4 because the vertex stage of this program declares
// it that way, and the link fails on a type mismatch. u_kmToU is already declared by
// cloud.fragment.glsl.
uniform vec4 u_cameraPosition;
uniform float u_time;
// 1 when this layer is marched, 0 when the shell shader draws it (FloatAttribute.CloudVolumetric).
uniform float u_cloudVolumetric;

// Value noise in [0, 1]. Local on purpose: the shaders in lib/noise call mod(genType, float) and
// shader/lib/math.glsl, included earlier in cloud.fragment.glsl, defines a scalar mod(float, float);
// NVIDIA's front end resolves the call to the scalar overload and refuses the whole shader.
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

// Equirectangular uv of a direction, matching the sphere's own vertex texcoords (SphereCreator:
// u = 1 - angleU / 2pi with angleU = atan2(z, x), v = angleV / pi with angleV = acos(y)). A mismatch
// here shows up as the volume sliding against the map.
vec2 vcEquirect(vec3 dir) {
    float v = acos(clamp(dir.y, -1.0, 1.0)) / PI;
    float u = 1.0 - atan(dir.z, dir.x) / (2.0 * PI);
    return vec2(u - floor(u), v);
}

// Coverage in [0, 1] from the cloud map, at an explicit mip level. Linear in the map: the map
// decides where clouds are and how solid they are, and the march only integrates through them. Only
// the pedestal is removed.
float vcCoverage(vec3 dir, float lod) {
    vec4 c = fetchCloudColorLod(vcEquirect(dir), lod);
    return saturate((c.a * luma(c.rgb) - VC_COVERAGE_BASE) / (1.0 - VC_COVERAGE_BASE));
}

// Mip level for the coverage lookups. It has to be computed here, once per fragment, from values
// that every fragment of the quad has: `fetchCloudColor` picks a level from the derivatives of what
// it is given, and inside the march loop - where neighbouring fragments run different numbers of
// iterations - those derivatives are garbage, which shows up as a field that flickers between frames
// and breaks into white pillars.
float vcCoverageLod(vec3 ro, vec3 rd, float rTop) {
    float angPix = max(length(dFdx(rd)), length(dFdy(rd)));
    // Incidence where the ray meets the top of the layer, at worst grazing.
    vec2 ti = vcSphereIntersect(ro, rd, rTop);
    vec3 n = normalize(ro + rd * max(ti.x, 0.0));
    float inc = max(abs(dot(n, rd)), 0.15);
    // Fraction of the map width covered by one pixel.
    float uvSpan = angPix / inc / (2.0 * PI);
    return clamp(log2(max(uvSpan * VC_MAP_WIDTH, 1.0)), 0.0, 8.0);
}

// Distance from the camera to the top of the layer, in layer thicknesses. Negative inside. Drives
// how much fine detail the march can show and therefore how much it pays for.
float vcAlt = 0.0;
// The same distance remapped to 0 up close and 1 far away, where one pixel covers several texels of
// the map and a threshold on the shape field can only produce grain.
float vcFar = 0.0;

// Density at p for a coverage of cov, in [0, 1]. `u` is the height fraction in the layer.
//
// Ordered cheapest-first. The shape field is what decides whether there is anything here at all, so
// it goes first and the two fields below it - the top of the column and the fine detail - are only
// evaluated when the shape cut has already said there is cloud. In a thin layer most samples are
// empty, and for those this turns eight noise octaves into four.
float vcDensity(vec3 p, float u, float cov) {
    if (cov <= 0.002) {
        return 0.0;
    }
    // Everything is in kilometres, in the planet's own frame. It matters: the shapes have to have a
    // physical size, not a size tied to the layer. Stretched to the layer - about one thickness
    // across - a cloud mass comes out a hundred kilometres wide, and from a couple of hundred
    // kilometres up the whole deck is three or four blobs. Cu at 35 km across with 8 km detail is
    // what puts many clouds in view at that range and still holds up from orbit.
    vec3 q = p / u_kmToU;
    // The shape field is filtered by distance the way a texture is: 35 km features are half a pixel
    // across at the disc, and a threshold on a field finer than a pixel can only give grain. Far out
    // the field is coarse and its spread is stretched back, because a narrow field is a switch.
    // Branch, not blend: `vcFar` is a function of the camera only, so it is constant across the
    // primitive and the two branches almost never meet inside a warp. Evaluating both and mixing
    // costs seven octaves where three or four are needed.
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

    // Two mappings of coverage to mass, blended by distance.
    //
    // Up close: the shape field cut by a floor that the coverage lowers. The cut opens holes inside
    // the layer as well as between masses, and those holes are what a cloud deck has and a shell
    // does not - scaling the field by the coverage instead gives every covered column the same
    // vertical extent, and from inside the layer that is a uniform grey wash with no edges in it.
    //
    // Far out: mass proportional to the coverage, because that is linear in it and therefore
    // survives the blurring the map gets at distance. A threshold whose level moves with the
    // coverage does not: the blurred map has lost the high values that were doing the covering, and
    // the disc comes out either grainy or under-cast, never matching the map it came from.
    float floorv = 0.75 * pow(1.0 - cov, 1.6);
    // Gated by the coverage: the cut on its own is a property of the field, not of the weather, so
    // without the gate the top quarter of the noise field is cloud over ground the map says is clear,
    // which is a thin white wash over every desert close up. The gate only bites below a fifth of a
    // column, so a well covered column still keeps the cut's holes and its ragged edges.
    float gate = smoothstep(0.0, 0.20, cov);
    float dNear = gate * saturate((shape - floorv) / max(1.0 - floorv, 1e-3));
    // Far out the density is chosen so that the column comes out exactly as opaque as the coverage
    // says it is: a tenth-covered column attenuates a tenth of the light, a fully covered one is
    // opaque. The optical depth that does that is -log(1 - cov), so the mean density is that divided
    // by VC_TAU, and the shape field is applied as a factor of mean one rather than as the density
    // itself. Anything proportional to the coverage instead - cov times a constant - is too opaque at
    // low coverage and too translucent at high, which is why a 10 % pedestal of the map used to read
    // as a 30 % white veil over desert the map says is clear.
    float tauTarget = -log(max(1.0 - cov, 0.004));
    float dFar = (tauTarget / VC_TAU) * (0.50 + shape);
    float d = mix(dNear, dFar, vcFar);
    if (d <= 0.0) {
        return 0.0;
    }

    // Where this column's cloud ends, vertically. Low frequency, so a cloud mass has a lumpy top
    // rather than a ceiling at one altitude - from the side and from above, that is most of what
    // reads as volume.
    float top = 0.40 + 0.55 * (vcFbm(vec3(q.x, q.y * 2.0, q.z) / 90.0 + 21.0, 2) * 0.5 + 0.5);
    float profile = smoothstep(0.0, 0.10, u) * (1.0 - smoothstep(top - 0.30, top, u));
    if (profile <= 0.0) {
        return 0.0;
    }

    d *= 0.65 + 0.70 * (vcFbm(q / 12.0 + 11.2, 2) * 0.5 + 0.5);

    // Erosion, at four kilometres and at one, each faded out at the altitude where it stops being
    // more than a sub-pixel shimmer. Each on its own schedule: four kilometre detail is still worth
    // sampling from a couple of hundred kilometres up, one kilometre detail is not, and left on it
    // is what makes the deck look fibrous instead of cloudy.
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
    return saturate(d) * profile;
}

// Density for a shadow ray, in [0, 1]. Two octaves of the same shape field and the same cut, and
// nothing else: the shadow term goes through a transmission curve that is not sensitive to the
// fine structure, and a shadow ray that costs as much as a view sample doubles the price of the
// whole march. Same field, same threshold, so a sample that is empty for the view is empty here.
float vcDensityLight(vec3 p, float cov) {
    if (cov <= 0.002) {
        return 0.0;
    }
    vec3 q = p / u_kmToU;
    float shape = vcFbm(vec3(q.x, q.y * 1.4, q.z) / 35.0 + 3.7, 2) * 0.5 + 0.5;
    float floorv = 0.75 * pow(1.0 - cov, 1.6);
    return saturate((shape - floorv) / max(1.0 - floorv, 1e-3));
}

// Optical depth from p towards the sun. `planetShadow` is 0 when the planet stands between the
// sample and the sun, which gives the terminator for free.
//
// The coverage is sampled along the sun ray, not taken from the sample being shaded. That is what
// produces the self-shadowing, and self-shadowing is most of what reads as volume: a tower shades
// its own base and the clouds beside it, so a top surface stops looking like a painted map. Reusing
// the viewing sample's coverage - an earlier version did, to save fetches - gives every column a
// uniform attenuation and a layer that looks flat no matter how bumpy its geometry is.
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
    // The optical depth is accumulated in kilometres and compared against a budget in kilometres:
    // VC_SUN_OD_MAX / (VC_TAU / thick) is the depth at which the transmission has fallen below the
    // point where the rest of the ray can be seen.
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

// Dual-lobe Henyey-Greenstein. A single forward lobe, used raw, is far too dark away from the
// forward direction, which is why a single-scattering cloud reads brown; the wide backward lobe is
// what makes clouds white when the sun is behind the camera, which is how they are seen from the
// ground most of the time.
//
// The normalisation is a fudge, and it has to be: a single forward lobe is a few hundredths of a
// steradian wide, so, normalised to unit energy, it makes a cloud almost black except within a few
// degrees of the sun. Real clouds are bright from everywhere because a photon scatters ten or twenty
// times before it leaves, which this shader models with the crude term below rather than by tracing
// it. The factor is set so that a lit top reads white against the planet, which is what the shell
// path gives with the same map: at 3pi the two lobes left the disc a grey veil, and the earlier
// experiment at 4pi looked washed out only because the sun direction was inverted.
float vcPhase(float mu) {
    return 5.0 * PI * (2.0 * vcHg(mu, VC_G_FORWARD) + vcHg(mu, VC_G_BACK));
}

// Whether the volumetric path is used for this fragment.
bool vcEnabled() {
    return u_cloudVolumetric >= 0.5;
}

// Scene depth, so that real geometry in front of the layer clips the march instead of being painted
// over: terrain that stands above the base of the layer, a nearer body, the surface itself when the
// ray would otherwise run past it. Same pair of uniforms as the volume path
// (TextureAttribute.VolumeSceneDepth and Vector2Attribute.VolumeDepthMaskScale), bound per frame
// from the full-resolution buffer by ModelEntityRenderSystem.renderClouds. A scale of zero means
// nothing is bound - the uniform is then never set - and no clipping happens.
uniform sampler2D u_volumeSceneDepth;
uniform vec2 u_volumeDepthMaskScale;

// Distance along this fragment's ray at which the scene occludes the layer, or 0 if nothing does.
//
// The buffer stores logarithmic depth (see shader/lib/logdepthbuff.glsl): getDepthValue rebuilds
// this fragment's own value from gl_FragCoord.w, exactly as the shell path writes it into
// gl_FragDepth, so the two numbers compare like with like. Anything nearer than the far wall of the
// proxy is nearer than everything the layer can cover, which is why the comparison is against the
// wall and not against the entry point.
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

// Marches the layer and writes the fragment outputs. Straight alpha: the average radiance of the
// column and how much of it the column covers.
void vcRender() {
    float R = u_cloudShell.x;
    float R1 = u_cloudShell.z;
    float thick = max(u_cloudShell.w, 1e-9);

    // Object frame. u_cameraPosition and v_fragPosWorld are both camera-relative world positions, so
    // subtracting the object's own translation is what puts the sphere at the origin here.
    mat4 W = u_worldTrans;
    mat3 rot = mat3(normalize(W[0].xyz), normalize(W[1].xyz), normalize(W[2].xyz));
    vec3 ro = transpose(rot) * (u_cameraPosition.xyz - W[3].xyz);
    vec3 rd = normalize(transpose(rot) * (v_fragPosWorld - u_cameraPosition.xyz));

    // Mip level for the map, before any discard: derivatives need the whole quad.
    float lod = vcCoverageLod(ro, rd, R1);
    // Camera altitude in layer thicknesses, from the ray origin in the object frame. Not from
    // u_cameraPosition.xyz: that uniform carries the camera's position in the frame in which the
    // camera is the origin, so it is zero for every fragment - which made every fragment read as
    // "just above the layer", paying for kilometre-scale erosion from orbit and never switching over
    // to the far-field shape field.
    vcAlt = (length(ro) - R1) / thick;
    vcFar = smoothstep(3.0, 10.0, vcAlt);

    // Entry and exit of the volume. Expressly not the surface of the proxy mesh: the analytic sphere
    // is what the march integrates, and it is right whether the fragment came from the near wall or
    // the far one. When the camera is inside the layer the entry is the camera itself.
    vec2 tOuter = vcSphereIntersect(ro, rd, R1);
    if (tOuter.y <= 0.0) {
        discard;
    }
    float t0 = max(tOuter.x, 0.0);
    float t1 = tOuter.y;

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
    t1 = min(t1, t0 + VC_MAX_PATH * thick);

    // Sun. M1 uses the first directional light, which is the planet case; the point-light fallback
    // covers scenes where the star is a point light.
    //
    // u_dirLights[].direction is the direction the light *travels* - from the star towards the scene,
    // as the PBR shader and the shell cloud shader both assume when they negate it. The vector the
    // march needs points the other way, towards the sun: used unnegated it sent every shadow ray into
    // the planet, so the whole lit hemisphere counted as eclipsed and only the ambient terms survived,
    // and it flipped the sign of mu, evaluating the phase function in the wrong hemisphere. The
    // point-light branch below is already a to-the-light vector and needs no negation.
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

    float rayLen = t1 - t0;
    int nSteps = clamp(int(ceil(rayLen / max(thick * 0.33, 1e-12))), 8, VC_MAX_STEPS);
    float stepLen = rayLen / float(nSteps);

    // Coverage, one fetch. Seen from orbit the layer is a sliver a couple of pixels thick, so the
    // column over a pixel is the column where the ray crosses the top of the layer, and taking it
    // there is not an approximation but the right answer - and a far smoother one than integrating it
    // from eight samples spaced farther apart than the features of the map, which is what turned the
    // disc into white sand. The single fetch is only wrong where the layer has depth on screen, and
    // there the per-sample path below takes over; `vcFar` is a function of the camera, so the branch
    // is coherent across the whole primitive.
    float lodRay = lod;
    float covRay = vcCoverage(normalize(ro + rd * t0), lodRay);

    // A grazing ray steps through the map every `stepLen`, which can be several texels: filter down
    // to the spacing of the samples, or the march aliases into grain along the limb.
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
            // The shadow ray reads the map one mip coarser than the view ray: the shadow term goes
            // through an exponential, so a 30 km wobble in the coverage along that ray turns into
            // per-pixel grain over the whole lit face.
            float od = vcSunOpticalDepth(p, sunDir, max(lod, lodRay) + 1.0, planetShadow);
            // Two-stream transmission, 1 / (1 + tau), rather than exp(-tau): a slab's diffuse
            // transmission falls steeply near a lit face and slowly deep inside, and the exponential
            // - which is all steepness - turns the same wobble into salt and pepper.
            float tsun = 1.0 / (1.0 + od * VC_TAU / thick);
            float ms = VC_MULTIPLE_SCATTERING * (1.0 - tsun);
            // Powder: the thin rim of a cloud facing the light is darker than its body, because
            // light scattered out of it is not replaced. Without it, every lit surface is equally
            // bright and the geometry disappears.
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
    if (alpha < 0.004) {
        discard;
    }

    // Straight alpha; opacity fades the coverage, not the colour, so a faded cloud goes transparent
    // instead of turning grey.
    fragColor = vec4(min(color / max(alpha, 1e-4), vec3(1.4)), alpha * v_opacity);
    layerBuffer = vec4(0.0, 0.0, 0.0, 1.0);
}

#endif // GLSL_CLOUD_VOLUMETRIC
