#version 430 core

layout(local_size_x = 256) in;

struct Particle {
    vec3 position; // xyz = world position
    vec3 color;    // rgb floats [0..1]
    vec3 extra;    // x = size, y = type (as float), z = texLayer (as float)
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

// Base colors.
uniform vec3 u_baseColor0;
uniform vec3 u_baseColor1;
// Base particle size.
uniform float u_baseSize;
// Base radius for this distribution. 1 is default.
uniform float u_baseRadius;
// RNG base seed.
uniform uint u_seed;

// TRANSFORMATIONS
// The body size
uniform float u_bodySize;
// The body position
uniform vec3 u_bodyPos;
// The matrix transform
uniform mat4 u_transform;

float rand(inout uint state) {
    state ^= state << 13;
    state ^= state >> 17;
    state ^= state << 5;
    return float(state & 0x00FFFFFFu) / 16777216.0;
}

// Generate RGB color based on u_baseColor0 / u_baseColor1 with random deviation
vec3 generateColor(inout uint state) {
    bool valid0 = all(greaterThanEqual(u_baseColor0, vec3(0.0)));
    bool valid1 = all(greaterThanEqual(u_baseColor1, vec3(0.0)));

    vec3 baseColor;
    if (valid0 && valid1) {
        // Both valid → randomly pick one
        baseColor = (rand(state) < 0.5) ? u_baseColor0 : u_baseColor1;
    } else if (valid0) {
        baseColor = u_baseColor0;
    } else if (valid1) {
        baseColor = u_baseColor1;
    } else {
        // Both invalid → fallback mid-gray
        baseColor = vec3(1.0);
    }

    // Small random deviation in [-0.02, +0.02]
    vec3 noise = vec3(
    rand(state),
    rand(state),
    rand(state)
    ) - 0.5;

    return clamp(baseColor + noise * 0.1, 0.0, 1.0);
}

// Generates a new particle position in a spherical distribution.
vec3 positionSphere(inout uint state) {
    // The radius of the distribution times a random number.
    float r = u_baseRadius * rand(state);
    float theta = rand(state) * 6.2831853;
    float phi = acos(rand(state) * 2.0 - 1.0);

    vec3 pos = vec3(
    r * sin(phi) * cos(theta),
    r * sin(phi) * sin(theta),
    r * cos(phi)
    );
    return pos;
}

// Generates a new particle position in a disk distribution, with the given heightScale.
vec3 positionDisk(inout uint state, float heightScale) {
    // Random angle [0, 2π)
    float theta = rand(state) * 6.2831853;

    // The radius of the distribution is 1
    float r = u_baseRadius * sqrt(rand(state));

    // Cartesian coordinates in ZX plane
    float z = r * cos(theta);
    float x = r * sin(theta);

    // Vertical displacement in Y
    float y = (rand(state) - 0.5) * 2.0 * heightScale;
    // rand in [0,1) → y in [-heightScale, +heightScale]

    return vec3(x, y, z);
}

// Spiral arms with smooth background transition
vec3 positionSpiral(inout uint state, float heightScale, int numArms) {
    // Use continuous value instead of binary inArm/background
    float armStrength = rand(state);

    float r;
    float spiralTightness;
    float radialSpread;

    if (armStrength < 0.75) {
        // STRONG ARM PARTICLES (75%)
        float randVal = rand(state);
        float minRadius = 0.15 * u_baseRadius;
        r = minRadius + pow(randVal, 0.8) * (u_baseRadius - minRadius);

        spiralTightness = 5.0;

        // Tight arms
        float thickness = mix(0.04, 0.08, smoothstep(0.2, 0.6, r / u_baseRadius)) * u_baseRadius;
        radialSpread = (rand(state) - 0.5) * thickness;

    } else if (armStrength < 0.95) {
        // WEAK ARM/BACKGROUND TRANSITION (20%)
        r = pow(rand(state), 0.6) * u_baseRadius;
        spiralTightness = 3.0;
        radialSpread = (rand(state) - 0.5) * 0.12 * u_baseRadius;

    } else {
        // DIFFUSE BACKGROUND (5%) - extends far out
        r = pow(rand(state), 0.3) * u_baseRadius * 1.2;
        spiralTightness = 1.5;
        radialSpread = (rand(state) - 0.5) * 0.2 * u_baseRadius;
    }

    // Avoid extreme center concentration for all types
    if (r < 0.1 * u_baseRadius) {
        r = 0.1 * u_baseRadius + rand(state) * 0.2 * u_baseRadius;
    }

    // Apply spiral pattern with strength-based tightness
    int armIndex = int(rand(state) * float(numArms));
    float armOffset = float(armIndex) * 6.2831853 / float(numArms);

    float baseAngle = spiralTightness * log(r + 0.15) + armOffset;

    // Add randomness based on how background-like the particle is
    float randomness = (1.0 - clamp(armStrength * 1.4, 0.0, 1.0)) * 3.0;
    float spiralAngle = baseAngle + (rand(state) - 0.5) * randomness;

    r += radialSpread;
    r = max(r, 0.05 * u_baseRadius);

    // Convert to Cartesian
    float x = r * cos(spiralAngle);
    float z = r * sin(spiralAngle);
    float y = (rand(state) - 0.5) * 2.0 * heightScale;

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

void main() {
    uint i = gl_GlobalInvocationID.x;
    if (i >= u_count) return;

    uint state = (uint(gl_GlobalInvocationID.x) * 747796405u + 2891336453u) * u_seed;


    int type = int(u_type);
    int layer = pickLayer(state);
    float size = u_baseSize * (rand(state) * 0.6 + 0.4);
    vec3 color = generateColor(state);
    vec3 pos;
    if (type == 0) {
        // Dust in spiral arms.
        pos = positionSpiral(state, 0.01, 4);
    } else {
        // The rest in a disk.
        pos = positionDisk(state, 0.01);
    }

    // Transform position
    pos = (u_transform * vec4(pos, 1.0)).xyz * u_bodySize + u_bodyPos;


    particles[i].position = pos;
    particles[i].color = color;
    particles[i].extra = vec3(size, type, layer);
}