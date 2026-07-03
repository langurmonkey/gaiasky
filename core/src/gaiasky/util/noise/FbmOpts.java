package gaiasky.util.noise;

/**
 * Options for fBm generators.
 * Mirrors the GLSL struct gln_tFBMOpts from assets/shader/lib/noise/common.glsl.
 */
public class FbmOpts {
    /** Seed for PRNG generation. */
    public double seed;
    /** Initial amplitude. */
    public double amplitude;
    /** Factor by which successive layers of noise decrease in amplitude. */
    public double persistence;
    /** Initial frequency. */
    public double frequency;
    /** Factor by which successive layers of noise increase in frequency. */
    public double lacunarity;
    /** Noise scale in (x, y, z). */
    public Vec3 scale;
    /** Exponent to apply to the generated noise in a power function. */
    public double power;
    /** Number of layers of noise to stack. */
    public int octaves;
    /** Enable absolute value (turbulence). */
    public boolean turbulence;
    /** Convert the fBm to Ridge Noise. */
    public boolean ridge;

    public FbmOpts() {
        this.seed = 0.0;
        this.amplitude = 1.0;
        this.persistence = 0.5;
        this.frequency = 1.0;
        this.lacunarity = 2.0;
        this.scale = new Vec3(1.0, 1.0, 1.0);
        this.power = 1.0;
        this.octaves = 4;
        this.turbulence = false;
        this.ridge = false;
    }

    public FbmOpts(double seed, double amplitude, double persistence, double frequency,
                   double lacunarity, Vec3 scale, double power, int octaves,
                   boolean turbulence, boolean ridge) {
        this.seed = seed;
        this.amplitude = amplitude;
        this.persistence = persistence;
        this.frequency = frequency;
        this.lacunarity = lacunarity;
        this.scale = scale;
        this.power = power;
        this.octaves = octaves;
        this.turbulence = turbulence;
        this.ridge = ridge;
    }
}