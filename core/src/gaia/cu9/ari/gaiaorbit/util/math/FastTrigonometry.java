/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.util.math;

import net.jafama.FastMath;

/**
 * Uses jafama ({@link net.jafama.FastMath}) library. It is not super accurate,
 * but it's the fastest shot in the West.
 * 
 * @author tsagrista
 *
 */
public class FastTrigonometry implements ITrigonometry {

    @Override
    public double sin(double angle) {
        return FastMath.sin(angle);
    }

    @Override
    public double asin(double angle) {
        return FastMath.asin(angle);
    }

    @Override
    public double cos(double angle) {
        return FastMath.cos(angle);
    }

    @Override
    public double acos(double angle) {
        return FastMath.acos(angle);
    }

    @Override
    public double tan(double angle) {
        return FastMath.tan(angle);
    }

    @Override
    public double atan(double angle) {
        return FastMath.atan(angle);
    }

}
