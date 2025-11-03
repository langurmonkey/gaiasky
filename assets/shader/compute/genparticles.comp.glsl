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
#define D_BAR 3
#define D_ELLIPSE 4

// Base colors.
uniform vec3 u_baseColor0;
uniform vec3 u_baseColor1;
// Base particle size.
uniform float u_baseSize;
// Base radius for this distribution. 1 is default.
uniform float u_baseRadius;
// RNG base seed.
uniform uint u_seed;

// Spiral tightness control (angle twist factor).
uniform float u_spiralAngle; // e.g. 1.0 = normal, <1.0 = looser arms, >1.0 = tighter twist
// Number of arms for spiral galaxy.
uniform int u_spiralArms;

// Ellipticity
uniform float u_ellipticity; // 0 = circle, 0.5 = mildly elliptical, 0.9 = very elongated
// Aspect ratio of the bar
uniform float u_aspect;   // e.g. 0.3 = short bar, 1.0 = long bar

// Thickness (height scale)
uniform float u_heightScale;

// TRANSFORMATIONS
// The body size
uniform float u_bodySize;
// The body position
uniform vec3 u_bodyPos;
// The matrix transform
uniform mat4 u_transform;

// RNG in [0,1]
float rand(inout uint state) {
    state ^= state << 13;
    state ^= state >> 17;
    state ^= state << 5;
    return float(state & 0x00FFFFFFu) / 16777216.0;
}

float gaussian(inout uint state) {
    float u, v, s;

    do {
        u = rand(state) * 2.0 - 1.0; // [-1, 1]
        v = rand(state) * 2.0 - 1.0; // [-1, 1]
        s = u * u + v * v;
    } while (s >= 1.0 || s == 0.0);

    s = sqrt(-2.0 * log(s) / s);
    return u * s; // Returns ~N(0,1)
}

// Generate RGB color based on u_baseColor0 / u_baseColor1 with random noise.
vec3 generateColor(inout uint state, float colorNoise) {
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

    // Small random deviation
    vec3 noise = colorNoise
                * (vec3(
                    rand(state),
                    rand(state),
                    rand(state)
                ) * 2.0 - 1.0);

    return clamp(baseColor + noise, 0.0, 1.0);
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

vec3 positionEllipse(inout uint state, float ellipticity) {
    // Generate random direction on a unit sphere
    float theta = rand(state) * 6.2831853;
    float phi = acos(rand(state) * 2.0 - 1.0);

    // Radius scaled by cube root to get roughly uniform distribution
    float r = pow(rand(state), 1.0/3.0) * u_baseRadius;

    // Ellipticity flattening on x-z plane
    float a = u_baseRadius;                   // major axis
    float b = u_baseRadius * (1.0 - ellipticity); // minor axis
    float c = a * (0.8 - 0.3 * ellipticity); // vertical compression

    // Random orientation around Y
    float rot = rand(state) * 6.2831853;
    float cosr = cos(rot);
    float sinr = sin(rot);

    vec3 pos = vec3(
    r * sin(phi) * cos(theta),
    r * cos(phi),
    r * sin(phi) * sin(theta)
    );

    // Scale to ellipsoid
    pos.x *= a / u_baseRadius;
    pos.z *= b / u_baseRadius;
    pos.y *= c / u_baseRadius;

    // Random rotation for more natural scatter
    pos = vec3(
    pos.x * cosr - pos.z * sinr,
    pos.y,
    pos.x * sinr + pos.z * cosr
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

// Generates spiral arms and background particles
vec3 positionSpiral(inout uint state, float heightScale, float pitchAngleDeg, uint numArms) {
    // Internal parameters
    float r_min = 0.2 * u_baseRadius;  // Minimum radius to avoid particles in the very center
    float r_max = u_baseRadius * 1.3;  // Maximum radius for spiral arm
    float armWidth = 0.05 * u_baseRadius;  // Width of the arm (controls how "thick" the arm is)

    // Logarithmic spiral factor from pitch angle
    float pitchRad = radians(clamp(pitchAngleDeg, 1.0, 80.0));
    float b = 1.0 / tan(pitchRad);

    // Decide particle type (strong arm, weak arm, or diffuse background)
    float selector = rand(state);

    if (selector > 0.5) {  // Diffuse background
        // Spread the background particles across a wider range, not just along the arms
        float r = r_min + rand(state) * (r_max - r_min);  // Uniform distribution for background particles
        // Return early for background particles to avoid arm logic
        return vec3(r * cos(rand(state) * 6.2831853), (rand(state) - 0.5) * 2.0 * heightScale, r * sin(rand(state) * 6.2831853));
    } else {
        // Uniform distribution along the arm
        float r = r_min + rand(state) * (r_max - r_min);

        // Apply spiral arm offset for strong/weak arms
        int armIndex = int(rand(state) * float(numArms));
        float armOffset = float(armIndex) * 6.2831853 / float(numArms);

        // Logarithmic spiral formula for spiral angle
        float spiralAngle = b * log(r / (0.1 * u_baseRadius)) + armOffset;

        // Calculate the unit tangent vector (direction of the spiral arm at this point)
        vec2 radialDir = vec2(cos(spiralAngle), sin(spiralAngle));
        vec2 tangentDir = normalize(vec2(-radialDir.y - b * radialDir.x, radialDir.x - b * radialDir.y));

        // Generate two uniform random numbers for Gaussian distribution
        float sigma = 1.0;
        float gaussianSpread = gaussian(state) * sigma;
        // Apply Gaussian spread (multiply by armWidth to control the spread size)
        vec2 perpendicularDisplacement = gaussianSpread * armWidth * tangentDir; // Apply the displacement in the tangent direction

        // Convert to Cartesian coordinates
        float x = r * cos(spiralAngle) + perpendicularDisplacement.x;
        float z = r * sin(spiralAngle) + perpendicularDisplacement.y;
        float y = (rand(state) - 0.5) * 2.0 * heightScale;  // Random vertical displacement

        // Return the position of the particle
        return vec3(x, y, z);
    }
}


vec3 positionBar(inout uint state, float heightScale, float aspect) {
    // Random position along bar major axis [-1,1]
    float x = (rand(state) * 2.0 - 1.0) * u_baseRadius * aspect;

    // Cross-section: Gaussian falloff around the bar
    float y = (rand(state) - 0.5) * 2.0 * heightScale * u_baseRadius;
    float z = (rand(state) - 0.5) * 2.0 * heightScale * u_baseRadius;

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

void main() {
    uint i = gl_GlobalInvocationID.x;
    if (i >= u_count) return;

    uint state = (uint(gl_GlobalInvocationID.x) * 747796405u + 2891336453u) * u_seed;

    int type = int(u_type);
    int distribution = int(u_distribution);
    int layer = pickLayer(state);
    float size = u_baseSize * (rand(state) * 0.6 + 0.4);
    vec3 color = generateColor(state, 0.2);
    vec3 pos;
    if (distribution == D_SPIRAL) {
        pos = positionSpiral(state, u_heightScale, u_spiralAngle, u_spiralArms);
    } else if (distribution == D_DISK) {
        pos = positionDisk(state, u_heightScale);
    } else if (distribution == D_BAR) {
        pos = positionBar(state, u_heightScale, u_aspect);
    } else if (distribution == D_ELLIPSE) {
        pos = positionEllipse(state, u_ellipticity);
    } else if (distribution == D_SPHERE) {
        pos = positionSphere(state);
    }

    // Transform position
    pos = (u_transform * vec4(pos, 1.0)).xyz * u_bodySize + u_bodyPos;

    particles[i].position = pos;
    particles[i].color = color;
    particles[i].extra = vec3(size, distribution, layer);
}