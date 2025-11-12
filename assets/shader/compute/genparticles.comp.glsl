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
#define D_DENSITY 4
#define D_ELLIPSE 5
#define D_GAUSS 6

// RNG base seed.
uniform uint u_seed;
// Base colors.
uniform vec3 u_baseColors[4];
// Color noise.
uniform float u_colorNoise;
// Size factor to take body size into account and normalize particle sizes.
uniform float u_sizeFactor;
// Base radius for this distribution. 1 is default.
uniform float u_baseRadius;
// Minimum radius for the distribution (spiral, density).
uniform float u_minRadius;

// Spiral tightness control (angle twist factor).
uniform float u_spiralAngle;// e.g. 1.0 = normal, <1.0 = looser arms, >1.0 = tighter twist
// Number of arms for spiral galaxy.
uniform int u_spiralArms;
// Displacement.
uniform vec2 u_displacement;

// Eccentricity
uniform float u_eccentricity;
// Aspect ratio of the bar
uniform float u_aspect;// e.g. 0.3 = short bar, 1.0 = long bar

// Thickness (height scale)
uniform float u_heightScale;

// TRANSFORMATIONS
// The body size
uniform float u_bodySize;
// The body position
uniform vec3 u_bodyPos;
// The matrix transform
uniform mat4 u_transform;

#include <shader/lib/distributions.glsl>

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

// Density wave using discrete ellipses
vec3 positionDensityWave(inout uint state, float heightScale, float pitchAngleDeg, float eccentricity, vec2 displacement, float minRadius) {
    // Discrete ellipses parameters
    const uint numEllipses = 80u;

    // Select which ellipse to use
    uint ellipseIndex = uint(rand(state) * float(numEllipses));
    float t = (float(ellipseIndex) + 0.5) / float(numEllipses);// 0 to 1

    // Ellipse radius increases from center outward
    float min = minRadius;
    float max = 1.0 - min;
    float ellipse_r = u_baseRadius * (min + max * t);

    // Ellipse dimensions in XZ plane
    float a = ellipse_r;
    float b = ellipse_r * (1.0 - eccentricity);

    // Rotate ellipse around Y axis by pitch angle - each ellipse has different rotation!
    float pitchRad = radians(pitchAngleDeg) * t;// Tilt increases with radius
    float cosPitch = cos(pitchRad);
    float sinPitch = sin(pitchRad);

    // Random position on ellipse circumference in XZ plane
    float ellipse_angle = rand(state) * 6.2831853;

    // Position on unrotated ellipse in XZ plane
    float x_ellipse = a * cos(ellipse_angle);
    float z_ellipse = b * sin(ellipse_angle);

    // Rotate ellipse around Y axis - this is where the spiral comes from!
    float x = x_ellipse * cosPitch + z_ellipse * sinPitch;
    float z = -x_ellipse * sinPitch + z_ellipse * cosPitch;

    // Apply progressive displacement - smallest ellipse has none, largest has full disp
    x += displacement.x * t;
    z += displacement.y * t;

    x += gaussian(state) * (u_baseRadius * 0.01);
    z += gaussian(state) * (u_baseRadius * 0.01);

    // Add height (Y)
    float y = (rand(state) - 0.5) * 2.0 * heightScale;

    return vec3(x, y, z);
}

// Generates particles in an elliptical distribution.
vec3 positionEllipse(inout uint state, float eccentricity) {
    // Generate random direction on a unit sphere
    float theta = rand(state) * 6.2831853;
    float phi = acos(rand(state) * 2.0 - 1.0);

    // Radius scaled by cube root to get roughly uniform distribution
    float r = pow(rand(state), 1.0/3.0) * u_baseRadius;

    // Ellipticity flattening on x-z plane
    float a = u_baseRadius;// major axis
    float b = u_baseRadius * (1.0 - eccentricity);// minor axis
    float c = a * (0.8 - 0.3 * eccentricity);// vertical compression

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

// Lays out positions in a gaussian manner, with a very dense center and a falloff.
vec3 positionGauss(inout uint state, float heightScale) {
    // Random angle [0, 2π)
    float theta = rand(state) * 6.2831853;

    // The radius of the distribution is 1
    float r = u_baseRadius * sqrt(ggaussian(state, 1.0) * 0.15);

    // Cartesian coordinates in ZX plane
    float z = r * cos(theta);
    float x = r * sin(theta);

    // Vertical displacement in Y
    float y = (rand(state) - 0.5) * 2.0 * heightScale;
    // rand in [0,1) → y in [-heightScale, +heightScale]

    return vec3(x, y, z);

}

// Generates spiral arms and background particles
vec3 positionSpiral(inout uint state, float heightScale, float pitchAngleDeg, uint numArms, float minRadius) {
    // Internal parameters
    float r_min = minRadius * u_baseRadius;// Minimum radius to avoid particles in the very center
    float r_max = u_baseRadius * 1.0;// Maximum radius for spiral arm

    // Decide particle type (strong arm, weak arm, or diffuse background)
    float selector = rand(state);

    if (selector > 0.85) { // Diffuse background
        // Spread the background particles across a wider range, not just along the arms
        float r = r_min + rand(state) * (r_max - r_min);// Uniform distribution for background particles
        // Return early for background particles to avoid arm logic
        return vec3(r * cos(rand(state) * 6.2831853), (rand(state) - 0.5) * 2.0 * heightScale, r * sin(rand(state) * 6.2831853));
    } else {
        // Logarithmic spiral factor from pitch angle
        float pitchRad = radians(clamp(pitchAngleDeg, 1.0, 80.0));
        float b = 1.0 / tan(pitchRad);

        float armWidth = 0.1 * u_baseRadius;// Width of the arm (controls how "thick" the arm is)

        // CLUMPING: Try multiple times to find a valid position
        const int MAX_ATTEMPTS = 10;
        bool found_valid_position = false;
        float r, spiralAngle, armOffset;
        int armIndex;

        for (int attempt = 0; attempt < MAX_ATTEMPTS && !found_valid_position; attempt++) {
            // Generate candidate position
            r = r_min + rand(state) * (r_max - r_min);
            // Density falloff with radius
            float radius_factor = 1.0 - 0.3 * (r - r_min) / (r_max - r_min);// 30% reduction at outer edge
            if (rand(state) > radius_factor) {
                // Try again or use alternative position
                r = r_min + rand(state) * (r_max - r_min);// Simple retry
            }

            r = r_min + (r_max - r_min) * pow(rand(state), mix(1.0, 2.0, (r - r_min) / (r_max - r_min)));
            armIndex = int(rand(state) * float(numArms));
            armOffset = float(armIndex) * 6.2831853 / float(numArms);
            spiralAngle = b * log(r / (0.1 * u_baseRadius)) + armOffset;

            // CLUMPING: Check if we should place a particle here
            const float clump_freq1 = 13.0;
            const float clump_freq2 = 9.0;
            const float clump_bias = 0.05;
            float arm_phase = float(armIndex) * 1.618;

            // Create clump pattern
            float base_pattern = (sin((spiralAngle + arm_phase) * clump_freq1) +
            cos((spiralAngle + arm_phase) * clump_freq2) + 1.0) / 2.0;

            // Apply bias and convert to probability
            float probability = clamp(base_pattern + clump_bias, 0.0, 1.0);

            // Check if this position should be kept
            if (rand(state) <= probability) {
                found_valid_position = true;
            }
        }

        // If we didn't find a valid position after max attempts, use the last one
        // This ensures we always return a position

        // Compute tangent and normal directions.
        vec2 radialDir = vec2(cos(spiralAngle), sin(spiralAngle));
        vec2 tangentDir = normalize(vec2(-radialDir.y - b * radialDir.x, radialDir.x - b * radialDir.y));
        vec2 normalDir = vec2(-tangentDir.y, tangentDir.x);

        // Stdev for arm spread.
        float sigma = 0.2;

        // Add perpendicular wobble to break the perfect line
        float wobbleWidth = 0.2 * u_baseRadius;
        const float wobbleFreq1 = 23.1;
        const float wobbleFreq2 = 14.25;
        const float wobbleAmp1 = 0.477;
        const float wobbleAmp2 = 1.233;
        float wobble1 = sin(r * wobbleFreq1 + armOffset) * wobbleAmp1;
        float wobble2 = cos(r * wobbleFreq2 + armOffset * 1.5) * wobbleAmp2;
        float totalWobble = wobble1 + wobble2;

        vec2 wobbleDisplacement = totalWobble * wobbleWidth * normalDir;

        // Your existing Gaussian spread
        float gaussianSpread = gaussian(state) * sigma;
        vec2 armSpread = gaussianSpread * armWidth * tangentDir;

        // Combine both displacements
        vec2 totalDisplacement = armSpread + wobbleDisplacement;

        // Convert to Cartesian coordinates
        float x = r * cos(spiralAngle) + totalDisplacement.x;
        float z = r * sin(spiralAngle) + totalDisplacement.y;
        float y = (rand(state) - 0.5) * 2.0 * heightScale;

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
    float size = 1.0 * (rand(state) + 0.3) * u_sizeFactor;
    vec3 color = generateColor(state, u_colorNoise);
    vec3 pos;
    if (distribution == D_SPIRAL) {
        pos = positionSpiral(state, u_heightScale, u_spiralAngle, u_spiralArms, u_minRadius);
    } else if (distribution == D_DENSITY) {
        pos = positionDensityWave(state, u_heightScale, u_spiralAngle, u_eccentricity, u_displacement, u_minRadius);
    } else if (distribution == D_DISK) {
        pos = positionDisk(state, u_heightScale);
    } else if (distribution == D_BAR) {
        pos = positionBar(state, u_heightScale, u_aspect);
    } else if (distribution == D_ELLIPSE) {
        pos = positionEllipse(state, u_eccentricity);
    } else if (distribution == D_SPHERE) {
        pos = positionSphere(state);
    } else if (distribution == D_GAUSS) {
        pos = positionGauss(state, u_heightScale);
    }

    // Transform position
    pos = (u_transform * vec4(pos, 1.0)).xyz * u_bodySize + u_bodyPos;

    particles[i].position = pos;
    particles[i].color = color;
    particles[i].extra = vec3(size, distribution, layer);
}