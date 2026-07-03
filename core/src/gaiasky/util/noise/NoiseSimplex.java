package gaiasky.util.noise;

/**
 * Java implementation of the Simplex noise algorithm.
 * This is a port of the GLSL gln_simplex function from assets/shader/lib/noise/simplex.glsl.
 * The algorithm produces identical noise values for the same inputs.
 */
public class NoiseSimplex implements INoise {
    // Constants from GLSL simplex.glsl
    private static final double C_X = 1.0 / 6.0;
    private static final double C_Y = 1.0 / 3.0;
    private static final double D_0_5 = 0.5;
    private static final double D_2 = 2.0;
    private static final double N_7 = 1.0 / 7.0; // N=7


    public NoiseSimplex() {}

    /**
     * Generates 3D Simplex Noise at the given point.
     * Equivalent to GLSL: gln_simplex(v)
     */
    public double evaluate(Vec3 v) {
        // First corner
        double dotV_C_yyy = dot(v, C_Y, C_Y, C_Y);
        Vec3 i = new Vec3(
                Math.floor(v.x + dotV_C_yyy),
                Math.floor(v.y + dotV_C_yyy),
                Math.floor(v.z + dotV_C_yyy)
        );
        // x0 = v - i + dot(i, C.xxx)  -- dot(...) is a SCALAR, broadcast to all lanes.
        double dotI_C_xxx = i.x * C_X + i.y * C_X + i.z * C_X;
        Vec3 x0 = v.sub(i).add(new Vec3(dotI_C_xxx, dotI_C_xxx, dotI_C_xxx));

        // Other corners
        // g = step(x0.yzx, x0.xyz)
        Vec3 x0_yzx = new Vec3(x0.y, x0.z, x0.x);
        Vec3 g = step(x0_yzx, x0);
        Vec3 l = new Vec3(1.0, 1.0, 1.0).sub(g);

        // i1 = min(g, l.zxy)
        Vec3 l_zxy = new Vec3(l.z, l.x, l.y);
        Vec3 i1 = min(g, l_zxy);

        // i2 = max(g, l.zxy)
        Vec3 i2 = max(g, l_zxy);

        // x1 = x0 - i1 + 1.0 * C.xxx
        Vec3 x1 = x0.sub(i1).add(new Vec3(C_X, C_X, C_X));
        Vec3 x2 = x0.sub(i2).add(new Vec3(2.0 * C_X, 2.0 * C_X, 2.0 * C_X));
        Vec3 x3 = x0.sub(new Vec3(1.0, 1.0, 1.0)).add(new Vec3(3.0 * C_X, 3.0 * C_X, 3.0 * C_X));

        // Permutations
        i = new Vec3(mod(i.x, 289.0), mod(i.y, 289.0), mod(i.z, 289.0));

        // p = permute(permute(permute(i.z + vec4(0,i1.z,i2.z,1)) + i.y + vec4(0,i1.y,i2.y,1)) + i.x + vec4(0,i1.x,i2.x,1))
        Vec4 vecZ = new Vec4(0.0, i1.z, i2.z, 1.0).add(i.z);
        Vec4 perm1 = permute(vecZ);
        Vec4 vecY = new Vec4(0.0, i1.y, i2.y, 1.0).add(i.y);
        Vec4 perm2 = permute(perm1.add(vecY));
        Vec4 vecX = new Vec4(0.0, i1.x, i2.x, 1.0).add(i.x);
        Vec4 p = permute(perm2.add(vecX));

        // Gradients
        // ( N*N points uniformly over a square, mapped onto an octahedron.)
        double n_ = N_7; // N=7
        // ns = n_ * D.wyz - D.xzx = (2*n_, 0.5*n_ - 1.0, n_)
        Vec3 ns = new Vec3(2.0 * n_, 0.5 * n_ - 1.0, n_);

        Vec4 j = p.sub(new Vec4(49.0).mul(p.mul(ns.z).mul(ns.z).floor())); // mod(p,N*N)

        Vec4 x_ = j.mul(ns.z).floor();
        Vec4 y_ = j.sub(x_.mul(7.0)).floor(); // mod(j,N)

        // x = x_ * ns.x + ns.yyyy ; y = y_ * ns.x + ns.yyyy
        Vec4 x = x_.mul(ns.x).add(ns.y);
        Vec4 y = y_.mul(ns.x).add(ns.y);
        // h = 1.0 - abs(x) - abs(y)
        Vec4 h = new Vec4(1.0, 1.0, 1.0, 1.0).sub(abs(x)).sub(abs(y));

        // b0 = vec4(x.xy, y.xy) ; b1 = vec4(x.zw, y.zw)
        Vec4 b0 = new Vec4(x.x, x.y, y.x, y.y);
        Vec4 b1 = new Vec4(x.z, x.w, y.z, y.w);

        Vec4 s0 = b0.floor().mul(2.0).add(1.0);
        Vec4 s1 = b1.floor().mul(2.0).add(1.0);
        // sh = -step(h, vec4(0.0))
        Vec4 sh = step(h, new Vec4(0.0, 0.0, 0.0, 0.0)).mul(-1.0);

        // a0 = b0.xzyw + s0.xzyw * sh.xxyy
        Vec4 b0xzyw = new Vec4(b0.x, b0.z, b0.y, b0.w);
        Vec4 s0xzyw = new Vec4(s0.x, s0.z, s0.y, s0.w);
        Vec4 shxxyy = new Vec4(sh.x, sh.x, sh.y, sh.y);
        Vec4 a0 = b0xzyw.add(s0xzyw.mul(shxxyy));

        // a1 = b1.xzyw + s1.xzyw * sh.zzww
        Vec4 b1xzyw = new Vec4(b1.x, b1.z, b1.y, b1.w);
        Vec4 s1xzyw = new Vec4(s1.x, s1.z, s1.y, s1.w);
        Vec4 shzzww = new Vec4(sh.z, sh.z, sh.w, sh.w);
        Vec4 a1 = b1xzyw.add(s1xzyw.mul(shzzww));

        Vec3 p0 = new Vec3(a0.x, a0.y, h.x);
        Vec3 p1 = new Vec3(a0.z, a0.w, h.y);
        Vec3 p2 = new Vec3(a1.x, a1.y, h.z);
        Vec3 p3 = new Vec3(a1.z, a1.w, h.w);

        // Normalise gradients
        Vec4 norm = new Vec4(
                taylorInvSqrt(p0.dot(p0)),
                taylorInvSqrt(p1.dot(p1)),
                taylorInvSqrt(p2.dot(p2)),
                taylorInvSqrt(p3.dot(p3))
        );
        p0 = p0.mul(norm.x);
        p1 = p1.mul(norm.y);
        p2 = p2.mul(norm.z);
        p3 = p3.mul(norm.w);

        // Mix final noise value
        Vec4 m = new Vec4(
                0.6 - x0.dot(x0),
                0.6 - x1.dot(x1),
                0.6 - x2.dot(x2),
                0.6 - x3.dot(x3)
        );
        m = max(m, new Vec4(0.0, 0.0, 0.0, 0.0));
        m = m.mul(m);           // matches GLSL "m = m * m"       -> m^2
        Vec4 mQuad = m.mul(m);  // matches GLSL "m * m" in return -> m^4

        return 42.0 * dot(mQuad, new Vec4(
                p0.dot(x0),
                p1.dot(x1),
                p2.dot(x2),
                p3.dot(x3)
        ));
    }

    // Helper methods mirroring GLSL vector operations
    private double dot(Vec3 a, double b1, double b2, double b3) {
        return a.x * b1 + a.y * b2 + a.z * b3;
    }

    private double dot(Vec4 a, Vec4 b) {
        return a.x * b.x + a.y * b.y + a.z * b.z + a.w * b.w;
    }

    /**
     * Matches GLSL step(edge, x): returns 1.0 where x >= edge, else 0.0.
     */
    private Vec3 step(Vec3 edge, Vec3 x) {
        return new Vec3(
                x.x >= edge.x ? 1.0 : 0.0,
                x.y >= edge.y ? 1.0 : 0.0,
                x.z >= edge.z ? 1.0 : 0.0
        );
    }

    private Vec3 min(Vec3 a, Vec3 b) {
        return new Vec3(Math.min(a.x, b.x), Math.min(a.y, b.y), Math.min(a.z, b.z));
    }

    private Vec3 max(Vec3 a, Vec3 b) {
        return new Vec3(Math.max(a.x, b.x), Math.max(a.y, b.y), Math.max(a.z, b.z));
    }

    /**
     * Matches GLSL step(edge, x): returns 1.0 where x >= edge, else 0.0.
     */
    private Vec4 step(Vec4 edge, Vec4 x) {
        return new Vec4(
                x.x >= edge.x ? 1.0 : 0.0,
                x.y >= edge.y ? 1.0 : 0.0,
                x.z >= edge.z ? 1.0 : 0.0,
                x.w >= edge.w ? 1.0 : 0.0
        );
    }

    private Vec4 max(Vec4 a, Vec4 b) {
        return new Vec4(
                Math.max(a.x, b.x),
                Math.max(a.y, b.y),
                Math.max(a.z, b.z),
                Math.max(a.w, b.w)
        );
    }

    private double mod(double x, double y) {
        return x - Math.floor(x / y) * y;
    }

    private Vec4 permute(Vec4 v) {
        return new Vec4(
                permute(v.x),
                permute(v.y),
                permute(v.z),
                permute(v.w)
        );
    }

    private Vec4 abs(Vec4 v) {
        return new Vec4(Math.abs(v.x), Math.abs(v.y), Math.abs(v.z), Math.abs(v.w));
    }

}