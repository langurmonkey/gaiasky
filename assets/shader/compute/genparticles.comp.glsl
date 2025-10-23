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

// Number of particles
uniform uint u_count;
// The billboard type
uniform uint u_type;
// RNG seed
uniform uint u_seed;
// The base radius, or size, of the particles
uniform float u_radius;

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

void main() {
    uint i = gl_GlobalInvocationID.x;
    if (i >= u_count) return;

    uint state = u_seed + i * 747796405u;

    float r = u_radius * rand(state) * 1.0e-2;
    float theta = rand(state) * 6.2831853;
    float phi = acos(rand(state) * 2.0 - 1.0);

    vec3 pos = vec3(
    r * sin(phi) * cos(theta),
    r * sin(phi) * sin(theta),
    r * cos(phi)
    );

    // Transform position
    pos = (u_transform * vec4(pos, 1.0)).xyz * u_bodySize + u_bodyPos;

    int type = int(u_type);
    int layer = 2;
    float size = u_radius;

    float brightness = clamp(1.0 - r / u_radius, 0.2, 1.0);
    vec3 color = vec3(brightness, brightness * 0.8, brightness * 0.6);

    particles[i].position = pos;
    particles[i].color = color;
    particles[i].extra = vec3(size, type, layer);
}