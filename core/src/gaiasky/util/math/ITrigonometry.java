/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.math;

/**
 * Trigonometry interface to enable multiple implementations
 * 
 * @author tsagrista
 *
 */
public interface ITrigonometry {
    public double sin(double angle);

    public double asin(double angle);

    public double cos(double angle);

    public double acos(double angle);

    public double tan(double angle);

    public double atan(double angle);

}
