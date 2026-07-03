package gaiasky.util.noise;

public interface INoise {
    int MAX_FBM_ITERATIONS = 30;

    /**
     * Evaluates the noise.
     * @param p The point at which the noise must be evaluated.
     * @return The noise value.
     */
    double evaluate(Vec3 p);

    /**
     * Generates 3D Fractional Brownian motion (fBm) from the implementing noise.
     * Mirrors the GLSL gln_sfbm function from assets/shader/lib/noise/simplex.glsl
     * with the fbm.glsl include block.
     *
     * @param v    Point to sample fBm at.
     * @param opts Options for fBm generation (see {@link FbmOpts}).
     * @return Value of fBm at point "v".
     */
    default double fbm(Vec3 v, FbmOpts opts) {
        v = new Vec3(
            v.x + opts.seed * 100.0,
            v.y + opts.seed * 100.0,
            v.z + opts.seed * 100.0
        );
        double result = 0.0;
        double amplitude = opts.amplitude;
        double frequency = opts.frequency;
        double maximum = amplitude;

        for (int i = 0; i < MAX_FBM_ITERATIONS; i++) {
            if (i >= opts.octaves)
                break;

            Vec3 p = new Vec3(
                v.x * frequency * opts.scale.x,
                v.y * frequency * opts.scale.y,
                v.z * frequency * opts.scale.z
            );

            double noiseVal = evaluate(p);

            // #include <shader/lib/noise/fbm.glsl>
            if (opts.turbulence && !opts.ridge) {
                result += Math.abs(noiseVal) * amplitude;
            } else if (opts.ridge) {
                noiseVal = Math.pow(1.0 - Math.abs(noiseVal), 2.0);
                result += noiseVal * amplitude;
            } else {
                result += noiseVal * amplitude;
            }

            frequency *= opts.lacunarity;
            amplitude *= opts.persistence;
            maximum += amplitude;
        }

        return Math.pow(result / maximum, opts.power);
    }

    /**
     * Permutation function used by the Simplex noise implementation.
     * Equivalent to GLSL: mod(((x * 34.0) + 1.0) * x, 289.0)
     */
    default double permute(double x) {
        return ((x * 34.0 + 1.0) * x) % 289.0;
    }

    /**
     * Taylor inverse square root approximation used in GLSL.
     * Equivalent to: 1.79284291400159 - 0.85373472095314 * r
     */
    default double taylorInvSqrt(double r) {
        return 1.79284291400159 - 0.85373472095314 * r;
    }

    /**
     * Maps a value from one range to another.
     */
    default double map(double value, double min1, double max1, double min2, double max2) {
        return min2 + (value - min1) * (max2 - min2) / (max1 - min1);
    }

    /**
     * Normalises a value from [-1,1] to [0,1].
     */
    default double normalize(double v) {
        return map(v, -1.0, 1.0, 0.0, 1.0);
    }

    /**
     * 2‑D random hash.
     */
    default Vec2 rand2(Vec2 p) {
        double a = Math.sin(dot(p, new Vec2(127.1, 311.7)));
        double b = Math.sin(dot(p, new Vec2(269.5, 183.3)));
        return new Vec2(fract(a * 43758.5453), fract(b * 43758.5453));
    }

    /**
     * 3‑D random hash (used by GLSL rand3).
     */
    default Vec3 rand3(Vec3 p) {
        // GLSL: mod(((p * 34.0) + 1.0) * p, 289.0)
        double x = ((p.x * 34.0 + 1.0) * p.x) % 289.0;
        double y = ((p.y * 34.0 + 1.0) * p.y) % 289.0;
        double z = ((p.z * 34.0 + 1.0) * p.z) % 289.0;
        return new Vec3(x, y, z);
    }

    /**
     * 4‑D random hash (mirrors GLSL rand4).
     */
    default Vec4 rand4(Vec4 p) {
        double x = ((p.x * 34.0 + 1.0) * p.x) % 289.0;
        double y = ((p.y * 34.0 + 1.0) * p.y) % 289.0;
        double z = ((p.z * 34.0 + 1.0) * p.z) % 289.0;
        double w = ((p.w * 34.0 + 1.0) * p.w) % 289.0;
        return new Vec4(x, y, z, w);
    }

    /**
     * Single float random based on a seed.
     */
    default double rand(double n) {
        return fract(Math.sin(n) * 1e4);
    }

    /**
     * 2‑D random based on a vec2.
     */
    default double rand(Vec2 p) {
        double v = 1e4 * Math.sin(17.0 * p.x + p.y * 0.1) * (0.1 + Math.abs(Math.sin(p.y * 13.0 + p.x)));
        return fract(v);
    }

    /**
     * 3‑D random based on a vec3.
     */
    default Vec3 rand(Vec3 p) {
        double a = Math.sin(dot(p, new Vec3(1.0, 57.0, 113.0)));
        double b = Math.sin(dot(p, new Vec3(57.0, 113.0, 1.0)));
        double c = Math.sin(dot(p, new Vec3(113.0, 1.0, 57.0)));
        return new Vec3(fract(a * 43758.5453), fract(b * 43758.5453), fract(c * 43758.5453));
    }

    // Helper math functions ---------------------------------------------------
    private static double dot(Vec2 a, Vec2 b) {
        return a.x * b.x + a.y * b.y;
    }

    private static double dot(Vec3 a, Vec3 b) {
        return a.x * b.x + a.y * b.y + a.z * b.z;
    }

    private static double fract(double v) {
        return v - Math.floor(v);
    }
}
