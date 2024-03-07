/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.coord.vsop87;

import gaiasky.util.Constants;
import gaiasky.util.coord.AbstractOrbitCoordinates;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.math.Vector3b;

import java.time.Instant;

public abstract class AbstractVSOP87 extends AbstractOrbitCoordinates implements iVSOP87 {

    protected boolean highAccuracy;

    protected AbstractVSOP87() {
        super();
    }

    @Override
    public void doneLoading(Object... params) {
        super.doneLoading(params);
    }

    @Override
    public Vector3b getEclipticSphericalCoordinates(Instant date, Vector3b out) {
        if (!Constants.withinVSOPTime(date.toEpochMilli()))
            return null;

        double tau = AstroUtils.tau(AstroUtils.getJulianDateCache(date));

        double L = (L0(tau) + L1(tau) + L2(tau) + L3(tau) + L4(tau) + L5(tau));
        double B = (B0(tau) + B1(tau) + B2(tau) + B3(tau) + B4(tau) + B5(tau));
        double R = (R0(tau) + R1(tau) + R2(tau) + R3(tau) + R4(tau) + R5(tau));
        R = R * Constants.AU_TO_U * scaling;

        out.set(L, B, R);
        return out;
    }

    @Override
    public Vector3b getEclipticCartesianCoordinates(Instant date, Vector3b out) {
        Vector3b v = getEclipticSphericalCoordinates(date, out);
        if (v == null)
            return null;
        Coordinates.sphericalToCartesian(out, out);
        return out;

    }

    @Override
    public Vector3b getEquatorialCartesianCoordinates(Instant date, Vector3b out) {
        Vector3b v = getEclipticSphericalCoordinates(date, out);
        if (v == null)
            return null;
        Coordinates.sphericalToCartesian(out, out);
        out.mul(Coordinates.eclToEq());
        return out;
    }

    @Override
    public void setHighAccuracy(boolean highAccuracy) {
        this.highAccuracy = highAccuracy;
    }

}
