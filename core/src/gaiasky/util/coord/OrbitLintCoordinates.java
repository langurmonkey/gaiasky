/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.coord;

import gaiasky.data.util.PointCloudData;
import gaiasky.scenegraph.component.OrbitComponent;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;

import java.time.Instant;

public class OrbitLintCoordinates extends AbstractOrbitCoordinates {
    OrbitComponent orbitalParams;
    PointCloudData data;
    Matrix4d transf;
    Vector3d aux = new Vector3d();

    public OrbitLintCoordinates() {
        super();
    }

    @Override
    public void doneLoading(Object... params) {
        if (params.length == 0) {
            logger.error(new RuntimeException("OrbitLintCoordinates need the scene graph"));
        } else {
            super.doneLoading(params);
            transf = new Matrix4d();
            orbitalParams = orbit.oc;
            data = orbit.getPointCloud();
        }
    }

    @Override
    public Vector3b getEclipticSphericalCoordinates(Instant date, Vector3b out) {
        getEclipticCartesianCoordinates(date, out);

        // To spherical
        Coordinates.cartesianToSpherical(out, out);
        return out;
    }

    @Override
    public Vector3b getEclipticCartesianCoordinates(Instant date, Vector3b out) {
        getEquatorialCartesianCoordinates(date, out);
        out.mul(Coordinates.eqToEcl());

        return out;
    }

    @Override
    public Vector3b getEquatorialCartesianCoordinates(Instant date, Vector3b out) {
        // Find out index

        long dateWrap = data.getWrapTimeMs(date);
        int basei = data.getIndex(dateWrap);

        int nexti = (basei + 1) % data.getNumPoints();
        double percent = (double) Math.abs(dateWrap - data.getDate(basei).toEpochMilli()) / (double) Math.abs(data.getDate(nexti).toEpochMilli() - data.getDate(basei).toEpochMilli());

        data.loadPoint(out, basei);
        data.loadPoint(aux, nexti);

        double len = aux.sub(out).len();
        aux.nor().scl(percent * len);
        out.add(aux);

        if (orbit.transformFunction == null && orbit.parent.orientation != null) {
            transf.set(orbit.parent.orientation);
        } else if (orbit.transformFunction != null) {
            transf.set(orbit.transformFunction);
        } else {
            transf.idt();
        }
        if (!orbit.newMethod) {
            transf.rotate(0, 1, 0, orbitalParams.argofpericenter);
            transf.rotate(0, 0, 1, orbitalParams.i);
            transf.rotate(0, 1, 0, orbitalParams.ascendingnode);
        } else if (orbit.model.isExtrasolar()) {
            transf.rotate(0, 1, 0, 90);
        }

        out.mul(transf).scl(scaling);

        // Move to center if needed
        if(center != null && !center.isZero())
            out.add(center);
        return out;
    }

}
