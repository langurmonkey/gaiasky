/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.coord.vsop87;

import gaiasky.data.AssetBean;
import gaiasky.util.Constants;
import gaiasky.util.Settings;
import gaiasky.util.coord.AbstractOrbitCoordinates;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.math.Vector3b;

import java.time.Instant;

/**
 * VSOP87 coordinates using an implementation based on binary data files.
 */
@Deprecated
public abstract class AbstractVSOP87 extends AbstractOrbitCoordinates implements iVSOP87 {

    private static final String dataFile = "$data/default-data/vsop87/vsop87a.bin";
    protected static VSOP87Binary vsop87;
    private final boolean versionA;


    protected AbstractVSOP87() {
        super();
        if (vsop87 == null) {
            AssetBean.addAsset(dataFile,
                    VSOP87Binary.class,
                    new VSOP87Loader.VSOP87LoaderParameters(Settings.settings.data.highAccuracy ? 0 : 0.6));
        }
        versionA = dataFile.contains("vsop87a.bin");
    }

    @Override
    public void doneLoading(Object... params) {
        super.doneLoading(params);
        if (vsop87 == null && AssetBean.manager().isLoaded(dataFile)) {
            vsop87 = AssetBean.manager().get(dataFile, VSOP87Binary.class);
        }
    }

    public abstract double[] getData(double tau);

    @Override
    public Vector3b getEclipticSphericalCoordinates(Instant date, Vector3b out) {
        return versionA ? getEclipticSphericalCoordinatesA(date, out) : getEclipticSphericalCoordinatesB(date, out);
    }
    public Vector3b getEclipticSphericalCoordinatesA(Instant date, Vector3b out) {
        Vector3b v = getEclipticCartesianCoordinates(date, out);
        if (v == null)
            return null;
        Coordinates.cartesianToSpherical(out, out);
        return out;
    }

    public Vector3b getEclipticSphericalCoordinatesB(Instant date, Vector3b out) {
        if (Constants.notWithinVSOPTime(date.toEpochMilli()))
            return null;

        double tau = AstroUtils.tau(AstroUtils.getJulianDateCache(date));
        // For some reason, this returns BLR instead of the more common LBR.
        double[] BLR = getData(tau);

        if (BLR != null) {
            double B = BLR[0];
            double L = BLR[1];
            double R = BLR[2] * Constants.AU_TO_U * scaling;
            out.set(L, B, R);
            return out;
        } else {
            return null;
        }
    }

    @Override
    public Vector3b getEclipticCartesianCoordinates(Instant date, Vector3b out) {
        return versionA ? getEclipticCartesianCoordinatesA(date, out) : getEclipticSphericalCoordinatesB(date, out);
    }

    public Vector3b getEclipticCartesianCoordinatesA(Instant date, Vector3b out) {
        if (Constants.notWithinVSOPTime(date.toEpochMilli()))
            return null;

        double tau = AstroUtils.tau(AstroUtils.getJulianDateCache(date));
        // For some reason, this returns BLR instead of the more common LBR.
        double[] XYZ = getData(tau);

        if (XYZ != null) {
            double X = XYZ[0] * Constants.AU_TO_U * scaling;
            double Y = XYZ[1] * Constants.AU_TO_U * scaling;
            double Z = XYZ[2] * Constants.AU_TO_U * scaling;
            out.set(Y, Z, X);
            return out;
        } else {
            return null;
        }
    }

    public Vector3b getEclipticCartesianCoordinatesB(Instant date, Vector3b out) {
        Vector3b v = getEclipticSphericalCoordinates(date, out);
        if (v == null)
            return null;
        Coordinates.sphericalToCartesian(out, out);
        return out;
    }

    @Override
    public Vector3b getEquatorialCartesianCoordinates(Instant date, Vector3b out) {
        Vector3b v = getEclipticCartesianCoordinates(date, out);
        if (v == null)
            return null;
        out.mul(Coordinates.eclToEq());
        return out;
    }

}
