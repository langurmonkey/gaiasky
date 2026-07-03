package gaiasky.util.noise;


/**
 * 4-component vector class mirroring GLSL's vec4.
 */
public class Vec4 {
    final double x, y, z, w;

    public Vec4(double val) {
        this(val, val, val, val);
    }

    public Vec4(double x, double y, double z, double w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    Vec4 add(Vec4 other) {
        return new Vec4(x + other.x, y + other.y, z + other.z, w + other.w);
    }

    Vec4 sub(Vec4 other) {
        return new Vec4(x - other.x, y - other.y, z - other.z, w - other.w);
    }

    Vec4 mul(Vec4 other) {
        return new Vec4(x * other.x, y * other.y, z * other.z, w * other.w);
    }

    Vec4 mul(double scalar) {
        return new Vec4(x * scalar, y * scalar, z * scalar, w * scalar);
    }

    Vec4 add(double scalar) {
        return new Vec4(x + scalar, y + scalar, z + scalar, w + scalar);
    }

    Vec4 floor() {
        return new Vec4(Math.floor(x), Math.floor(y), Math.floor(z), Math.floor(w));
    }

    Vec4 swizzleXYYY() { return new Vec4(x, y, y, y); }
    Vec4 swizzleZZWW() { return new Vec4(z, z, w, w); }
}
