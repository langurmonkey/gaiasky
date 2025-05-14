/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.coord.vsop87;

import gaiasky.util.math.Vector3Q;

import java.time.Instant;

public interface iVSOP87 {
    Vector3Q getEclipticSphericalCoordinates(Instant date, Vector3Q out);
    Vector3Q getEclipticCartesianCoordinates(Instant date, Vector3Q out);
}
