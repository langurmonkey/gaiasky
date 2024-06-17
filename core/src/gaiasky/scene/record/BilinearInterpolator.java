/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

import net.jafama.FastMath;

public class BilinearInterpolator {

    /**
     * Interpolates the given grid model with the values of the given texture coordinates UV.
     *
     * @param u     The U coordinate.
     * @param v     The V coordinate.
     * @param model The grid model.
     * @param wrapX  Wrap the interpolation in the X coordinate.
     * @param wrapY  Wrap the interpolation in the Y coordinate.
     *
     * @return The bi-linearly interpolated value.
     */
    public static double interpolate(double u, double v, GridModel model, boolean wrapX, boolean wrapY) {

        // Bi-linear interpolation
        int W = model.getWidth();
        int H = model.getHeight();

        // In our grid (pixmap, array, etc.), Y points downwards, and [0,0] is top-left
        // In textures, V points upwards, and [0,0] is bottom-left
        v = 1.0 - v;

        /*
         * Weighted bi-linear interpolation of p.
         *
         *     i1j2-----------i2j2
         *      |   |          |
         *      |---p----------|
         *      |   |          |
         *      |   |          |
         *      |   |          |
         *     i1j1----------i2j1
         *
         */

        double dx = 1.0 / W;
        double dy = 1.0 / H;
        double dx2 = dx / 2.0;
        double dy2 = dy / 2.0;

        // The texels are sampled at the center of the area they cover,
        // so we need to shift the UV by half a pixel!
        u -= dx2;
        v -= dy2;

        int i1 = (int) FastMath.floor(W * u);
        if (i1 < 0) {
            i1 = wrapX ? W - 1 : 0;
        }
        int i2 = (i1 + 1) % W;
        int j1 = (int) FastMath.floor(H * v);
        if (j1 < 0) {
            j1 = wrapY ? H - 1 : 0;
        }
        int j2 = (j1 + 1) % H;

        double x;
        if (u < 0 && wrapX) {
            // In this special case, we are at the wrapping point (i1 is the last and i2 is the first).
            x = 1.0 + u + dx2;
        } else {
            // Regular case. Remember to add half a pixel to x_i and j_i to use central values.
            x = u + dx2;
        }
        double y = v + dx2;
        double x1 = ((double) i1 / (double) W) + dx2;
        double x2 = x1 + dx;
        double y1 = ((double) j1 / (double) H) + dy2;
        double y2 = y1 + dy;

        double q11 = model.getValue(i1, j1);
        double q21 = model.getValue(i2, j1);
        double q12 = model.getValue(i1, j2);
        double q22 = model.getValue(i2, j2);

        double r1 = ((x2 - x) / (x2 - x1)) * q11 + ((x - x1) / (x2 - x1)) * q21;
        double r2 = ((x2 - x) / (x2 - x1)) * q12 + ((x - x1) / (x2 - x1)) * q22;
        return ((y2 - y) / (y2 - y1)) * r1 + ((y - y1) / (y2 - y1)) * r2;
    }

    public interface GridModel {
        int getWidth();

        int getHeight();

        double getValue(int x, int y);
    }
}
