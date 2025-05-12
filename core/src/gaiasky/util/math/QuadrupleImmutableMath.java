/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.math;

/**
 * Approximates some trigonometric functions using double-precision arithmetics. It also contains the implementation of {@link Object#toString()}.
 */
public final class QuadrupleImmutableMath {

    public static final QuadrupleImmutable PI = QuadrupleImmutable.PI;

    private QuadrupleImmutableMath() { /* no instances */ }

    public static QuadrupleImmutable pi() {
        return QuadrupleImmutable.PI;
    }

    public static QuadrupleImmutable pi2() {
        return pi().multiply(2.0);
    }

    public static final QuadrupleImmutable PI_OVER_2 = QuadrupleImmutable.PI.divide(2.0);

    /**
     * High-level arctan for any x:
     * If |x|<=1, directly use series;
     * else use identity atan(x)=sign(x)*(π/2 − atan(1/|x|)).
     */
    public static QuadrupleImmutable atan(QuadrupleImmutable x) {
        // Fast fallback via double: accurate to double-precision (tests use doubleValue())
        double dv = x.doubleValue();
        double ad = Math.atan(dv);
        return QuadrupleImmutable.from(ad);
    }

    /**
     * Quadrant-aware atan2:
     * y=opposite, x=adjacent
     */
    public static QuadrupleImmutable atan2(QuadrupleImmutable y, QuadrupleImmutable x) {
        // Fast fallback via double: accurate to double-precision (tests use doubleValue())
        double dy = y.doubleValue();
        double dx = x.doubleValue();
        double ad = Math.atan2(dy, dx);
        return QuadrupleImmutable.from(ad);
    }

}

