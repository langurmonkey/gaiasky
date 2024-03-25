/*
 * Copyright (c) 2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.orientation;

import gaiasky.util.math.QuaternionDouble;

/**
 * Orientation server that reads a list of times and quaternions from a file, and interpolates
 * them using {@link QuaternionDouble#nlerp(QuaternionDouble, double)}.
 */
public class QuaternionNlerpOrientationServer extends QuaternionInterpolationOrientationServer {

    public QuaternionNlerpOrientationServer(String dataFile) {
        super(dataFile);
    }

    @Override
    protected QuaternionDouble interpolate(QuaternionDouble q0, QuaternionDouble q1, double alpha) {
        lastOrientation.set(q0);
        lastOrientation.nlerp(q1, alpha);
        return lastOrientation;
    }
}
