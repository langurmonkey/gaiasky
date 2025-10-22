#version 430

layout(local_size_x = 256) in;

struct Particle {
    vec3 position; // xyz = world position
    vec4 color;    // rgba floats [0..1]
    vec3 extra;    // x = size, y = type (as float), z = texLayer (as float)
};

layout(std430, binding = 0) buffer Particles {
    Particle particles[];
};

uniform uint seed;
uniform uint count;
uniform float radius;

float rand(inout uint state) {
    state ^= state << 13;
    state ^= state >> 17;
    state ^= state << 5;
    return float(state & 0x00FFFFFFu) / 16777216.0;
}

void main() {
    uint i = gl_GlobalInvocationID.x;
    if (i >= count) return;

    uint state = seed + i * 747796405u;

    float r = radius * pow(rand(state), 1.5);
    float theta = rand(state) * 6.2831853;
    float phi = acos(rand(state) * 2.0 - 1.0);

    vec3 pos = vec3(
    r * sin(phi) * cos(theta),
    r * sin(phi) * sin(theta),
    r * cos(phi)
    );

    float size = 0.002 + rand(state) * 0.006;
    int type = 1;
    int layer = 3;

    float brightness = clamp(1.0 - r / radius, 0.2, 1.0);
    vec4 color = vec4(brightness, brightness * 0.8, brightness * 0.6, 1.0);

    particles[i].position = vec3(1.0, 2.0, 3.0);
    particles[i].color = color;
    particles[i].extra = vec3(size, type, layer);
}