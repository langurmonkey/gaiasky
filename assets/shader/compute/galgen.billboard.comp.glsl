#version 430 core

layout(local_size_x = 256) in;

struct Particle {
    vec3 position;// xyz = world position
    vec3 color;// rgb floats [0..1]
    vec3 extra;// x = size, y = type (as float), z = texLayer (as float)
};

layout(std430, binding = 0) buffer Particles {
    Particle particles[];
};

// UNIFORMS
// Number of particles
uniform uint u_count;
// Layers
uniform int u_layers[15];
// The billboard type
uniform uint u_type;
#define TYPE_DUST 0
#define TYPE_BULGE 1
#define TYPE_STAR 2
#define TYPE_GAS 3
#define TYPE_HII 4
#define TYPE_GALAXY 5
#define TYPE_POINT 6
#define TYPE_OTHER 7

// The probability distribution to spawn particles
uniform uint u_distribution;
#define D_SPHERE 0
#define D_DISK 1
#define D_SPIRAL 2
#define D_SPIRAL_LOG 3
#define D_BAR 4
#define D_ELLIPSOID 5
#define D_DISK_GAUSS 6
#define D_SPHERE_GAUSS 7
#define D_CONE 8
#define D_IRREGULAR 9

// RNG base seed.
uniform uint u_seed;
// Base colors.
uniform vec3 u_baseColors[4];
// Color noise (if positive), noise size (negative).
uniform float u_colorNoise;
// Size factor to take the full body size into account and normalize particle sizes.
uniform float u_sizeFactor;
// Base radius for this distribution. 1 is default.
uniform float u_baseRadius;
// Minimum radius for the distribution (spiral, density).
uniform float u_minRadius;

// Noise in the particle size generation, in [0..1].
uniform float u_sizeNoise;

// Base angle
// Spirals: tightness control (angle twist factor).
// Cone: cone angle
uniform float u_baseAngle;
// Number of arms for spiral galaxy.
uniform int u_numArms;
// Sigma across arm.
uniform float u_armSigma;
// No spiral delta: the field declares it a no-op, so the sampler does not use it and the uniform
// must not be declared here either - an unused uniform is stripped by the compiler and setting it
// then logs "uniform not found".

// Disks and co: warp.
uniform float u_warpStrength;

// Eccentricity in x, y and z
uniform vec2 u_eccentricity;
// Aspect ratio of the bar
uniform float u_aspect;// e.g. 0.3 = short bar, 1.0 = long bar

// Thickness (height scale)
uniform float u_heightScale;
// Height profile with radius
uniform uint u_heightProfile;
#define H_CONSTANT 0
#define H_LINEAR_INC 1
#define H_LINEAR_DEC 2
#define H_SMOOTH_INC 3
#define H_SMOOTH_DEC 4

// TRANSFORMATION
uniform mat4 u_baseTransform;

#include <shader/lib/distributions.glsl>
// The analytic galaxy field. The spiral sampler below samples it instead of approximating it, so
// the particles of a channel land where that channel's volume has material.
#include <shader/lib/galaxy.glsl>

float computeY(inout uint state, vec2 xz) {
    float profileFactor;
    float r = length(xz);
    if (u_heightProfile == H_CONSTANT) {
        profileFactor = 1.0;
    } else {
        if (u_heightProfile == H_SMOOTH_INC) {
            profileFactor = (smoothstep(0.0, u_baseRadius, r));
        } else if (u_heightProfile == H_SMOOTH_DEC) {
            profileFactor = (smoothstep(u_baseRadius, 0.0, r));
        } else if (u_heightProfile == H_LINEAR_INC) {
            profileFactor = (r / u_baseRadius);
        } else if (u_heightProfile == H_LINEAR_DEC) {
            profileFactor = (1.0 - r / u_baseRadius);
        }
    }
    // Compute height from profile and scale.
    float height = (rand(state) - 0.5) * 2.0 * profileFactor * u_heightScale;

    if (u_warpStrength == 0.0) {
        return height;
    } else {
        // Calculate warp component
        float warpFactor = 0.0;
        float normalizedR = r / u_baseRadius;

        // Only apply warp in the outer regions of the disk, when r > warpStartRadius
        float warpStartRadius = 0.7;
        float warpStrength = u_warpStrength;
        if (normalizedR > warpStartRadius) {
            float warpIntensity = (normalizedR - warpStartRadius) / (1.0 - warpStartRadius);
            //warpIntensity = smoothstep(0.0, 1.0, warpIntensity);// Smooth radial transition

            // Smooth angular transition: use normalized Z coordinate for smooth blending
            // xz.y is the Z coordinate, normalize it by radius for smooth transition
            float angularBlend = xz.y / max(r, 0.001);// Avoid division by zero

            warpFactor = angularBlend * warpIntensity * warpStrength;
        }

        return height + warpFactor;
    }
}

// Generates a new particle position in a uniform spherical distribution.
vec3 positionSphere(inout uint state) {
    // The radius of the distribution times a random number, with some variation.
    float r_0 = u_baseRadius - u_minRadius;
    float r = u_minRadius + (r_0 + (rand(state) - 0.5) * 0.3) * sqrt(rand(state));
    // The variation must not push particles past baseRadius, which is the outer edge the volume
    // of the same galaxy tapers to.
    r = min(r, u_baseRadius);
    float theta = rand(state) * 6.2831853;
    float phi = acos(rand(state) * 2.0 - 1.0);

    vec3 pos = vec3(
    r * sin(phi) * cos(theta),
    r * sin(phi) * sin(theta),
    r * cos(phi)
    );
    return pos;
}
// Generates a new particle in a spherical gaussian distribution.
vec3 positionSphereGauss(inout uint state) {
    // The radius of the distribution follows a gaussian.
    // abs(): a gaussian draw is negative half the time and sqrt() of that is NaN.
    float r = u_minRadius + (u_baseRadius - u_minRadius) * sqrt(abs(ggaussian(state, 1.0)) * 0.15);
    float theta = rand(state) * 6.2831853;
    float phi = acos(rand(state) * 2.0 - 1.0);

    vec3 pos = vec3(
    r * sin(phi) * cos(theta),
    r * sin(phi) * sin(theta),
    r * cos(phi)
    );
    return pos;
}

// Generates sparticles in a cone.
vec3 positionCone(inout uint state, float coneAngleDeg) {
    float coneAngle = radians(coneAngleDeg);

    // Sample a random direction within the cone (uniformly distributed)
    float cosCone = cos(coneAngle);
    float cosPhi  = mix(cosCone, 1.0, rand(state));// Uniform within cone
    float phi     = acos(cosPhi);
    float theta   = rand(state) * 6.2831853;// [0, 2π)

    // Unit direction vector inside cone (axis = +Z)
    vec3 dir = vec3(
    sin(phi) * cos(theta),
    cos(phi),
    sin(phi) * sin(theta)
    );

    // Sample random distance along the cone (for uniform *volume* density)
    float r = u_minRadius + (u_baseRadius - u_minRadius) * pow(rand(state), 1.0 / 3.0);
    // (cube root ensures uniform density in 3D volume)

    // Add small random variation to radius if desired, without leaving the distribution's radius.
    r = min(r + (rand(state) - 0.5) * 0.1 * u_baseRadius, u_baseRadius);

    return r * dir - vec3(0.0, 0.5, 0.0);
}

// ---- Shared spiral sampler ----
//
// Both spiral distributions describe the same thing in the volume: galaxyDensity() treats
// GX_SPIRAL and GX_SPIRAL_LOG identically (see its flat branch), so the samplers must be one
// function, and that function must sample that field rather than an approximation of it. The field
// is, for a flat spiral,
//
//     rho(r, phi) = exp(-rn / GX_DISK_SCALE) * gxTaper(rn, 0.8) * (0.22 + 0.78 * gxArmFactor())
//
// and this samples it factor by factor:
//
//   - radius: the marginal of an exponential disk in r is rho(r) ∝ r exp(-r/h), whose CDF has a
//     closed form; it is inverted with Newton. The rim taper is applied as an acceptance test, so
//     the particles and the volume fade out at the same radius.
//   - azimuth: the 0.22 inter-arm veil and the 0.78 arm are split by a coin flip, and the in-arm
//     position is spread across the centreline with the same sigma and the same perpendicular
//     metric gxArmFactor() uses. The arm angle itself comes from gxArmTheta(), so the two sets of
//     arms are on the same curve.
//
// The vertical structure is left to computeY(), which is the particle side's own slab.
// The lane warp, the lane width taper, the lane substructure and the noise mask are volume-only:
// they are not mirrored here, so a field with those set no longer matches its particles exactly.

// The uniforms of this draw, as the shape the field functions take.
GalaxyShape gxFieldShape() {
    GalaxyShape s;
    s.distribution = u_distribution == D_SPIRAL_LOG ? GX_SPIRAL_LOG : GX_SPIRAL;
    s.baseRadius = u_baseRadius;
    s.minRadius = u_minRadius;
    s.numArms = u_numArms;
    s.armSigma = u_armSigma;
    s.baseAngle = u_baseAngle;
    s.eccentricity = u_eccentricity;
    // The field ignores the spiral delta for both spirals; leaving it out keeps the uniform live.
    s.spiralDeltaPos = vec2(0.0);
    s.aspect = u_aspect;
    s.warpStrength = u_warpStrength;
    s.heightScale = u_heightScale;
    s.heightProfile = int(u_heightProfile);
    s.colorNoise = 0.0;
    s.weight = 1.0;
    s.laneWarp = 0.0;
    s.laneNoiseScale = 0.0;
    s.laneWidthTaper = 0.0;
    s.armStructure = 0.0;
    s.noiseMask = 0.0;
    s.noiseMaskFreq = 0.0;
    s.noiseMaskOctaves = 0;
    // Same order as GX_CHANNEL_*, except POINT, which is 5 in the field and 6 here.
    s.channel = u_type == TYPE_DUST ? GX_CHANNEL_DUST
              : u_type == TYPE_BULGE ? GX_CHANNEL_BULGE
              : u_type == TYPE_GAS ? GX_CHANNEL_GAS
              : u_type == TYPE_HII ? GX_CHANNEL_HII
              : u_type == TYPE_POINT ? GX_CHANNEL_POINT
              : GX_CHANNEL_STAR;
    for (int i = 0; i < 4; i++) {
        s.baseColors[i] = vec3(0.0);
    }
    return s;
}

// Radius of an exponential disk of scale h = GX_DISK_SCALE * base, truncated at base and starting at
// minR, by Newton on the closed-form CDF F(x) = 1 - e^-x (1 + x), x = r/h.
float gxSampleDiskRadius(inout uint state, float R, float minR) {
    float h = GX_DISK_SCALE * R;
    float xMax = R / h;
    float fMax = 1.0 - exp(-xMax) * (1.0 + xMax);
    float xMin = clamp(minR / h, 0.0, xMax);
    float fMin = 1.0 - exp(-xMin) * (1.0 + xMin);
    float target = fMin + rand(state) * (fMax - fMin);
    // A Rayleigh draw is the same shape without the (1 + x) factor, so it starts within a few
    // percent of the answer.
    float x = sqrt(-2.0 * log(max(1.0 - target / fMax, 1e-6)));
    for (int i = 0; i < 3; i++) {
        float e = exp(-x);
        float f = 1.0 - e * (1.0 + x) - target;
        x = clamp(x - f / max(x * e, 1e-6), xMin, xMax);
    }
    return clamp(x * h, minR, R);
}

vec3 positionSpiralField(inout uint state) {
    GalaxyShape s = gxFieldShape();
    float R = max(u_baseRadius, 1e-6);
    // minRadius is an absolute fraction of the object radius, as in the field: the particles start
    // where the volume's inner edge is.
    float minR = clamp(u_minRadius, 0.0, R);

    // Radius: accepted against the field's rim taper. 16 tries leaves the fallback unused even at
    // the widest disk the sampler can be handed.
    float r = 0.0;
    for (int i = 0; i < 16; i++) {
        r = gxSampleDiskRadius(state, R, minR);
        if (rand(state) < gxTaper(r / R, 0.8)) {
            break;
        }
    }

    float rn = r / R;
    float arms = float(max(u_numArms, 1));
    float period = 6.2831853 / arms;
    float b = gxPitchB(s.baseAngle);
    float sigma = max(u_armSigma * GX_ARM_WIDTH * R, 1e-5);
    // No azimuthal structure left of the arm start radius, as in the field: an anti-aliasing term
    // that does not follow minRadius, which is 0 for the gas.
    float armWeight = smoothstep(0.0, 2.0 * max(minR / R, GX_ARM_START), rn);

    float phi;
    if (rand(state) < 0.78 * armWeight) {
        float armOffset = floor(rand(state) * arms) * period;
        // Angular spread that gives a perpendicular offset sigma: a log spiral of pitch b spends
        // r / sqrt(1 + b^2) of arc length per unit of angle.
        float dPhi = gaussian(state) * sigma * sqrt(1.0 + b * b) / max(r, 1e-5);
        phi = gxArmTheta(s, r) + armOffset + dPhi;
    } else {
        phi = rand(state) * 6.2831853;
    }

    float x = r * cos(phi);
    float z = r * sin(phi);
    float y = computeY(state, vec2(x, z));
    return vec3(x, y, z);
}

// Positions on an ellipsoid.
vec3 positionEllipsoid(inout uint state, vec2 ec) {
    // --- sample random direction on unit sphere ---
    float u1 = rand(state);
    float u2 = rand(state);
    float z = 2.0 * u1 - 1.0;
    float phi = 6.283185307179586 * u2;
    float rxy = sqrt(max(0.0, 1.0 - z*z));
    vec3 dir = vec3(rxy * cos(phi), z, rxy * sin(phi));

    // --- sample radius fraction uniformly inside shell ---
    float s = rand(state);
    float minFrac = clamp(u_minRadius / u_baseRadius, 0.0, 1.0);
    float centralDensity = rand(state) + 1.2;// The higher the denser
    float rFrac = mix(minFrac, 1.0, pow(s, centralDensity/3.0));

    // --- compute semi-axes ---
    float a = u_baseRadius;// X-axis (major)
    float b = u_baseRadius * clamp(1.0 - ec.y, 0.0, 1.0);// Z-axis (minor)
    float c = u_baseRadius * clamp(1.0 - ec.x, 0.0, 1.0);// Y-axis (vertical)

    // --- scale unit vector to ellipsoid shell ---
    vec3 pos = vec3(a * dir.x, c * dir.y, b * dir.z) * rFrac;

    return pos;
}

// Generates a new particle position in a disk distribution
vec3 positionDisk(inout uint state) {
    // Random angle [0, 2π)
    float theta = rand(state) * 6.2831853;

    // The radius of the distribution is baseRadius, with some variation that is clamped back into
    // it: the volume of the same galaxy tapers to zero at baseRadius, so a particle beyond it has
    // no gas to sit in.
    float r = u_minRadius +  (u_baseRadius - u_minRadius + (rand(state) - 0.5) * 0.3) * sqrt(rand(state));
    r = min(r, u_baseRadius);

    // Cartesian coordinates in ZX plane
    float z = r * cos(theta);
    float x = r * sin(theta);

    // Vertical displacement in Y
    float y = computeY(state, vec2(x, z));

    return vec3(x, y, z);
}

// Lays out positions in a disk with a gaussian distribution, with a very dense center and a falloff.
vec3 positionDiskGauss(inout uint state) {
    // Random angle [0, 2π)
    float theta = rand(state) * 6.2831853;

    // The radius of the distribution is 1
    // abs(): a gaussian draw is negative half the time and sqrt() of that is NaN.
    float r = u_minRadius + (u_baseRadius - u_minRadius) * sqrt(abs(ggaussian(state, 1.0)) * 0.15);

    // Cartesian coordinates in ZX plane
    float z = r * cos(theta);
    float x = r * sin(theta);

    // Vertical displacement in Y
    float y = computeY(state, vec2(x, z));

    return vec3(x, y, z);
}

vec3 positionBar(inout uint state, float aspect) {
    // Random position along bar major axis [-1,1]
    float x = (rand(state) * 2.0 - 1.0) * u_baseRadius * aspect;

    // Cross-section: Gaussian falloff around the bar
    float y = computeY(state, vec2(x, 0.0)) * u_baseRadius;
    float z = computeY(state, vec2(x, 0.0)) * u_baseRadius;

    // Optional: apply falloff to make it denser at center
    float falloff = exp(-0.5 * (x*x + z*z) / (u_baseRadius * u_baseRadius));
    x *= falloff;
    z *= falloff;

    return vec3(x, y, z);
}

// Picks one of the valid layers at random.
int pickLayer(inout uint state) {
    int validCount = 0;

    for (int i = 0; i < 15; i++) {
        if (u_layers[i] >= 0) {
            validCount++;
        }
    }

    // If there are valid layers, pick one randomly
    if (validCount > 0) {
        int randomIndex = int(rand(state) * float(validCount));
        int selectedLayer = u_layers[randomIndex];
        return selectedLayer;
    }
    return 0;
}

// Generate RGB color based on u_baseColors with random noise.
vec3 generateColor(inout uint state, float colorNoise) {
    vec3 baseColor = u_baseColors[int(rand(state) * 4.0)];

    // Noise based on colorNoise parameter.
    vec3 noise = colorNoise
    * (vec3(
    rand(state),
    rand(state),
    rand(state)
    ) * 2.0 - 1.0);

    return clamp(baseColor + noise, 0.0, 1.0);
}

// Generate a size
float generateSize(inout uint state, float sizeNoise, vec3 pos, int distrib, int type) {
    // Compute radial mask for dust in SPIRAL_LOG
    float radialMask = 1.0;
    if (distrib == D_SPIRAL_LOG && type == TYPE_DUST) {
        float r = length(pos.xz);
        // Up and down
        radialMask = smoothstep(u_minRadius, u_baseRadius * 0.3, r) * smoothstep(u_baseRadius, u_baseRadius * 0.8, r);
    } else if (distrib == D_IRREGULAR) {
        // For irregulars, do a 3D mask
        if (rand(state) > 0.5) {
            return fbm((pos + rand(state)) * (rand(state) * pow(abs(sizeNoise), 4.0))) * 2.0 * u_sizeFactor;
        } else {
            return fbm((pos.zxy + rand(state)) * (rand(state) * pow(abs(sizeNoise), 4.0))) * 2.0 * u_sizeFactor;
        }
    }
    // When sizeNoise = 0, the particle size is 1.
    // When sizeNoise = 1, size is random between ~0.1 and ~4.
    // When sizeNoise < 0, we use fbm as a mask.
    if (sizeNoise < 0.0) {
        return fbm(pos.xz * abs(sizeNoise)) * 2.0 * u_sizeFactor * radialMask;
    } else {
        return (u_sizeFactor + (rand(state) * 2.0 - 1.0) * u_sizeFactor * 2.0 * sizeNoise) * radialMask;
    }
}

// Main method.
void main() {
    uint i = gl_GlobalInvocationID.x;
    if (i >= u_count) return;

    uint state = (uint(gl_GlobalInvocationID.x) * 747796405u + 2891336453u) * u_seed;
    // Seed 0 makes every state 0, which is a fixed point of the xorshift (see rand()) and used to
    // hang the whole dispatch. Fall back to a zero-free hash of the index so the particles are
    // still distinct and the galaxy is still a galaxy.
    if (state == 0u) {
        state = uint(gl_GlobalInvocationID.x) * 2654435761u + 1u;
    }

    int distribution = int(u_distribution);
    vec3 pos;
    if (distribution == D_SPIRAL_LOG || distribution == D_SPIRAL) {
        // Both spirals are one field in the volume, so both are one sampler here.
        pos = positionSpiralField(state);
    } else if (distribution == D_DISK) {
        pos = positionDisk(state);
    } else if (distribution == D_BAR) {
        pos = positionBar(state, u_aspect);
    } else if (distribution == D_ELLIPSOID) {
        pos = positionEllipsoid(state, u_eccentricity);
    } else if (distribution == D_SPHERE || distribution == D_IRREGULAR) {
        pos = positionSphere(state);
    } else if (distribution == D_DISK_GAUSS) {
        pos = positionDiskGauss(state);
    } else if (distribution == D_SPHERE_GAUSS) {
        pos = positionSphereGauss(state);
    } else if (distribution == D_CONE) {
        pos = positionCone(state, u_baseAngle);
    }

    int layer = pickLayer(state);
    int type = int(u_type);
    float size = generateSize(state, u_sizeNoise, pos, distribution, type);
    vec3 color = generateColor(state, u_colorNoise);

    // Apply dataset transformation
    pos = (u_baseTransform * vec4(pos, 1.0)).xyz;

    particles[i].position = pos;
    particles[i].color = color;
    particles[i].extra = vec3(size, type, layer);
}
