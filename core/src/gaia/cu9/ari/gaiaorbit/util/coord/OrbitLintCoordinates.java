package gaia.cu9.ari.gaiaorbit.util.coord;

import java.time.Instant;

import gaia.cu9.ari.gaiaorbit.data.orbit.PolylineData;
import gaia.cu9.ari.gaiaorbit.scenegraph.CelestialBody;
import gaia.cu9.ari.gaiaorbit.scenegraph.component.OrbitComponent;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.math.Matrix4d;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;

public class OrbitLintCoordinates extends AbstractOrbitCoordinates {
    OrbitComponent orbitalParams;
    PolylineData data;
    Matrix4d transf;
    Vector3d aux = new Vector3d();

    @Override
    public void doneLoading(Object... params) {
        if (params.length == 0) {
            Logger.error(new RuntimeException("OrbitLintCoordinates need the scene graph"));
        } else {
            super.doneLoading(params);
            transf = new Matrix4d();
            orbitalParams = orbit.oc;
            data = orbit.getPolyline();
        }
    }

    @Override
    public Vector3d getEclipticSphericalCoordinates(Instant date, Vector3d out) {
        getEquatorialCartesianCoordinates(date, out);
        out.mul(Coordinates.eqToEcl());

        // To spherical
        Coordinates.cartesianToSpherical(out, out);
        return out;
    }

    @Override
    public Vector3d getEclipticCartesianCoordinates(Instant date, Vector3d out) {
        getEquatorialCartesianCoordinates(date, out);
        out.mul(Coordinates.eqToEcl());
        return out;
    }

    @Override
    public Vector3d getEquatorialCartesianCoordinates(Instant date, Vector3d out) {
        // Find out index

        // Number of periods occurred
        double numPeriods = (AstroUtils.getJulianDateCache(date) - orbitalParams.epoch) / orbitalParams.period;
        // Current angle in degrees
        double angle = (orbitalParams.meananomaly + (numPeriods - Math.floor(numPeriods)) * 360d) % 360d;
        // Fraction in [0..numPoints]
        double fraction = (angle / 360d) * data.getNumPoints();

        int basei = (int) Math.floor(fraction);
        int nexti = (basei + 1) % data.getNumPoints();
        double percent = fraction - basei;

        data.loadPoint(out, basei);
        data.loadPoint(aux, nexti);

        double len = aux.sub(out).len();
        aux.nor().scl(percent * len);
        out.add(aux);

        if (orbit.transformFunction == null && ((CelestialBody) orbit.parent).orientation != null) {
            transf.set(((CelestialBody) orbit.parent).orientation);
        } else if (orbit.transformFunction != null) {
            transf.set(orbit.transformFunction);
        } else {
            transf.idt();
        }
        if (!orbit.newmethod) {
            transf.rotate(0, 1, 0, orbitalParams.argofpericenter);
            transf.rotate(0, 0, 1, orbitalParams.i);
            transf.rotate(0, 1, 0, orbitalParams.ascendingnode);
        } else {
            transf.rotate(0, 1, 0, 90);
        }

        out.mul(transf);
        return out;
    }

}
