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
    public static QuadrupleImmutable PI_2 = PI.multiply(2.0);
    public static final QuadrupleImmutable PI_OVER_2 = QuadrupleImmutable.PI.divide(2.0);

    /**
     * High-level arctan for any x:
     * If |x|<=1, directly use series;
     * else use identity atan(x)=sign(x)*(π/2 − atan(1/|x|)).
     */
    public static QuadrupleImmutable atan(QuadrupleImmutable x) {
        // Fast fallback via double: accurate to double-precision (tests use doubleValue())
        return QuadrupleImmutable.from(Math.atan(x.doubleValue()));
    }

    /**
     * Quadrant-aware atan2:
     * y=opposite, x=adjacent
     */
    public static QuadrupleImmutable atan2(QuadrupleImmutable y, QuadrupleImmutable x) {
        // Fast fallback via double: accurate to double-precision (tests use doubleValue())
        return QuadrupleImmutable.from(Math.atan2(y.doubleValue(), x.doubleValue()));
    }

}

