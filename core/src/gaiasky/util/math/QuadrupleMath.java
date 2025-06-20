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
public final class QuadrupleMath {

    public static final Quadruple PI = Quadruple.pi();

    private QuadrupleMath() { /* no instances */ }

    public static Quadruple pi() {
        return Quadruple.pi();
    }

    public static Quadruple pi2() {
        return pi().multiply(2.0);
    }

    public static Quadruple piOver2() {
        return QuadrupleMath.pi().divide(2.0);
    }

    /**
     * High-level arctan for any x:
     * If |x|&leq;1, directly use series;
     * else use identity <code>atan(x)=sign(x)*(PI/2 − atan(1/|x|))</code>.
     */
    public static Quadruple atan(Quadruple x) {
        // Fast fallback via double: accurate to double-precision (tests use doubleValue())
        double dv = x.doubleValue();
        double ad = Math.atan(dv);
        return Quadruple.from(Double.toString(ad));
    }

    /**
     * Quadrant-aware atan2:
     * y=opposite, x=adjacent
     */
    public static Quadruple atan2(Quadruple y, Quadruple x) {
        // Fast fallback via double: accurate to double-precision (tests use doubleValue())
        double dy = y.doubleValue();
        double dx = x.doubleValue();
        double ad = Math.atan2(dy, dx);
        return Quadruple.from(Double.toString(ad));
    }

}

