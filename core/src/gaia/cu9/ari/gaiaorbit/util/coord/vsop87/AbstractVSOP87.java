package gaia.cu9.ari.gaiaorbit.util.coord.vsop87;

import java.time.Instant;

import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.Nature;
import gaia.cu9.ari.gaiaorbit.util.coord.AbstractOrbitCoordinates;
import gaia.cu9.ari.gaiaorbit.util.coord.AstroUtils;
import gaia.cu9.ari.gaiaorbit.util.coord.Coordinates;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;

public abstract class AbstractVSOP87 extends AbstractOrbitCoordinates implements iVSOP87 {

    protected boolean highAccuracy;

    @Override
    public void doneLoading(Object... params) {
        super.doneLoading(params);
    }

    @Override
    public Vector3d getEclipticSphericalCoordinates(Instant date, Vector3d out) {
        if (!Constants.withinVSOPTime(date.toEpochMilli()))
            return null;

        double tau = AstroUtils.tau(AstroUtils.getJulianDateCache(date));

        double L = (L0(tau) + L1(tau) + L2(tau) + L3(tau) + L4(tau) + L5(tau));
        double B = (B0(tau) + B1(tau) + B2(tau) + B3(tau) + B4(tau) + B5(tau));
        double R = (R0(tau) + R1(tau) + R2(tau) + R3(tau) + R4(tau) + R5(tau));
        R = R * Nature.AU_TO_KM;

        out.set(L, B, R * Constants.KM_TO_U);
        return out;
    }

    @Override
    public Vector3d getEclipticCartesianCoordinates(Instant date, Vector3d out) {
        Vector3d v = getEclipticSphericalCoordinates(date, out);
        if (v == null)
            return null;
        Coordinates.sphericalToCartesian(out, out);
        return out;

    }

    @Override
    public Vector3d getEquatorialCartesianCoordinates(Instant date, Vector3d out) {
        Vector3d v = getEclipticSphericalCoordinates(date, out);
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
