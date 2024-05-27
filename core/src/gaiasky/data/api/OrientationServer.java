/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.api;

import gaiasky.util.math.QuaternionDouble;

import java.time.Instant;
import java.util.Date;

public interface OrientationServer {
    QuaternionDouble updateOrientation(final Date date);
    QuaternionDouble updateOrientation(final Instant instant);
    QuaternionDouble getCurrentOrientation();
    boolean hasOrientation();
}
