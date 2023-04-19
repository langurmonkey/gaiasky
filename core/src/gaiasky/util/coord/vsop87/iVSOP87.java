/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.coord.vsop87;

import gaiasky.util.math.Vector3b;

import java.time.Instant;

public interface iVSOP87 {

    double L0(double t);

    double L1(double t);

    double L2(double t);

    double L3(double t);

    double L4(double t);

    double L5(double t);

    double B0(double t);

    double B1(double t);

    double B2(double t);

    double B3(double t);

    double B4(double t);

    double B5(double t);

    double R0(double t);

    double R1(double t);

    double R2(double t);

    double R3(double t);

    double R4(double t);

    double R5(double t);

    void setHighAccuracy(boolean highAccuracy);

    Vector3b getEclipticSphericalCoordinates(Instant date, Vector3b out);
}
