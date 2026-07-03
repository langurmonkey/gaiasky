package gaiasky.util.noise;

import org.jetbrains.annotations.NotNull;

/**
 * Simple 2D vector class mirroring GLSL's vec2.
 */
public class Vec2 {
    public final double x;
    public final double y;

    public Vec2(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vec2 add(Vec2 other) {
        return new Vec2(x + other.x, y + other.y);
    }

    public Vec2 sub(Vec2 other) {
        return new Vec2(x - other.x, y - other.y);
    }

    public Vec2 mul(double scalar) {
        return new Vec2(x * scalar, y * scalar);
    }

    public double dot(Vec2 other) {
        return x * other.x + y * other.y;
    }

    @Override
    public @NotNull String toString() {
        return String.format("Vec2(%.6f, %.6f)", x, y);
    }
}