/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2.impl;

import gaiasky.event.EventManager;
import gaiasky.script.v2.api.RefsysAPI;
import gaiasky.util.Constants;
import gaiasky.util.Nature;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.math.Vector3D;
import gaiasky.util.math.Vector3Q;

import java.util.List;

/**
 * The reference system module contains calls and methods to deal with reference system changes and other
 * useful utilities related to orientation.
 */
public class RefsysModule extends APIModule implements RefsysAPI {
    /**
     * Create a new module with the given attributes.
     *
     * @param api  Reference to the API class.
     * @param name Name of the module.
     */
    public RefsysModule(EventManager em, APIv2 api, String name) {
        super(em, api, name);
    }

    @Override
    public double[] galactic_to_cartesian(double l, double b, double r) {
        Vector3D pos = Coordinates.sphericalToCartesian(l * Nature.TO_RAD, b * Nature.TO_RAD, r * Constants.KM_TO_U, new Vector3D());
        pos.mul(Coordinates.galacticToEquatorial());
        return new double[]{pos.x, pos.y, pos.z};
    }

    @Override
    public double[] get_transform_matrix(String name) {
        var mat = Coordinates.getTransformD(name);
        if (mat != null) {
            return mat.val;
        } else {
            logger.error(name + ": no transformation found with the given name");
            return null;
        }
    }


    public double[] galactic_to_cartesian(int l, int b, int r) {
        return galactic_to_cartesian((double) l, (double) b, (double) r);
    }

    @Override
    public double[] ecliptic_to_cartesian(double l, double b, double r) {
        Vector3D pos = Coordinates.sphericalToCartesian(l * Nature.TO_RAD, b * Nature.TO_RAD, r * Constants.KM_TO_U, new Vector3D());
        pos.mul(Coordinates.eclipticToEquatorial());
        return new double[]{pos.x, pos.y, pos.z};
    }

    public double[] eclipticToInternalCartesian(int l, int b, int r) {
        return ecliptic_to_cartesian((double) l, (double) b, (double) r);
    }

    @Override
    public double[] equatorial_to_cartesian(double ra, double dec, double r) {
        Vector3D pos = Coordinates.sphericalToCartesian(ra * Nature.TO_RAD, dec * Nature.TO_RAD, r * Constants.KM_TO_U, new Vector3D());
        return new double[]{pos.x, pos.y, pos.z};
    }

    public double[] equatorial_to_cartesian(int ra, int dec, int r) {
        return equatorial_to_cartesian((double) ra, (double) dec, (double) r);
    }

    public double[] cartesian_to_equatorial(double x, double y, double z) {
        Vector3Q in = api.aux3b1.set(x, y, z);
        Vector3D out = api.aux3d6;
        Coordinates.cartesianToSpherical(in, out);
        return new double[]{out.x * Nature.TO_DEG, out.y * Nature.TO_DEG, in.lenDouble()};
    }

    public double[] cartesian_to_equatorial(int x, int y, int z) {
        return cartesian_to_equatorial((double) x, (double) y, (double) z);
    }

    @Override
    public double[] equatorial_cartesian_to_internal(double[] eq, double kmFactor) {
        var v = api.aux3d1.set(eq).scl(kmFactor).scl(Constants.KM_TO_U);
        return new double[]{v.y, v.z, v.x};
    }

    public double[] equatorial_cartesian_to_internal(final List<?> eq, double kmFactor) {
        return equatorial_cartesian_to_internal(api.dArray(eq), kmFactor);
    }

    @Override
    public double[] equatorial_to_galactic(double[] eq) {
        api.aux3d1.set(eq).mul(Coordinates.eqToGal());
        return api.aux3d1.values();
    }

    public double[] equatorial_to_galactic(List<?> eq) {
        return equatorial_to_galactic(api.dArray(eq));
    }

    @Override
    public double[] equatorial_to_ecliptic(double[] eq) {
        api.aux3d1.set(eq).mul(Coordinates.eqToEcl());
        return api.aux3d1.values();
    }

    public double[] equatorial_to_ecliptic(List<?> eq) {
        return equatorial_to_ecliptic(api.dArray(eq));
    }

    @Override
    public double[] galactic_to_equatorial(double[] gal) {
        api.aux3d1.set(gal).mul(Coordinates.galToEq());
        return api.aux3d1.values();
    }

    public double[] galactic_to_equatorial(List<?> gal) {
        return galactic_to_equatorial(api.dArray(gal));
    }

    @Override
    public double[] ecliptic_to_equatorial(double[] ecl) {
        api.aux3d1.set(ecl).mul(Coordinates.eclToEq());
        return api.aux3d1.values();
    }

    public double[] ecliptic_to_equatorial(List<?> ecl) {
        return ecliptic_to_equatorial(api.dArray(ecl));
    }
}
