#ifndef GLSL_LIB_DISTRIB
#define GLSL_LIB_DISTRIB
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
        u = rand(state) * 2.0 - 1.0;// [-1, 1]
        v = rand(state) * 2.0 - 1.0;// [-1, 1]
        s = u * u + v * v;
    } while (s >= 1.0 || s == 0.0);

    s = sqrt(-2.0 * log(s) / s);
    return u * s;// Returns ~N(0,1)
}

// Approximate gamma function using Lanczos approximation
// Good enough for our purposes in the range we need
float gamma(float x) {
    // Lanczos approximation coefficients
    const float g = 4.7421875;
    float series = 0.99999999999999709182;
    series += 57.156235665862923517 / (x + 0.0);
    series += -59.597960355475491248 / (x + 1.0);
    series += 14.136097974741747174 / (x + 2.0);
    series += -0.49191381609762019978 / (x + 3.0);
    series += 0.33994649984811888699e-4 / (x + 4.0);
    series += 0.46523628927048575665e-4 / (x + 5.0);
    series += -0.98374475304879564677e-4 / (x + 6.0);
    series += 0.15808870322491248884e-3 / (x + 7.0);
    series += -0.21026444172410488382e-3 / (x + 8.0);
    series += 0.21743961811521264320e-3 / (x + 9.0);
    series += -0.16431810653676389022e-3 / (x + 10.0);
    series += 0.84418223983852743293e-4 / (x + 11.0);
    series += -0.26190838401581408670e-4 / (x + 12.0);
    series += 0.36899182659531622704e-5 / (x + 13.0);

    float t = x + g - 0.5;
    return sqrt(2.0 * 3.14159265358979323846) * pow(t, x - 0.5) * exp(-t) * series;
}

// Generalized gaussian distribution (GGD) using power transformation
float ggaussian(inout uint state, float beta) {
    float x = gaussian(state);

    if (abs(beta - 2.0) < 1e-6) {
        return x;
    }

    // Simple power transformation - works reasonably well for beta > 0.5
    float sign = sign(x);
    float abs_x = abs(x);

    // Transform based on beta
    // When beta < 2, this compresses the tails and sharpens the peak
    // When beta > 2, this expands the tails and flattens the peak
    float transformed = sign * pow(abs_x, 2.0 / beta);

    // Adjust variance to maintain ~N(0,1) scale
    // This is an empirical correction
    float variance_correction = sqrt(gamma(3.0/beta) / gamma(1.0/beta));

    return transformed * variance_correction;
}

float random(in vec2 st) {
    return fract(sin(dot(st.xy,
    vec2(12.9898,78.233)))*
    43758.5453123);
}

// Perlin noise based on Morgan McGuire @morgan3d
// https://www.shadertoy.com/view/4dS3Wd
float noise(in vec2 st) {
    vec2 i = floor(st);
    vec2 f = fract(st);

    // Four corners in 2D of a tile
    float a = random(i);
    float b = random(i + vec2(1.0, 0.0));
    float c = random(i + vec2(0.0, 1.0));
    float d = random(i + vec2(1.0, 1.0));

    vec2 u = f * f * (3.0 - 2.0 * f);

    return mix(a, b, u.x) +
    (c - a)* u.y * (1.0 - u.x) +
    (d - b) * u.x * u.y;
}

#define OCTAVES 6
float fbm (in vec2 st) {
    // Initial values
    float value = 0.0;
    float amplitude = .5;
    float frequency = 0.;
    //
    // Loop of octaves
    for (int i = 0; i < OCTAVES; i++) {
        value += amplitude * noise(st);
        st *= 2.;
        amplitude *= .5;
    }
    return value;
}

#endif