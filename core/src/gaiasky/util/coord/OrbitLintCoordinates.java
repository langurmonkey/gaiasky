/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.coord;

import gaiasky.data.util.PointCloudData;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Trajectory;
import gaiasky.scene.component.Verts;
import gaiasky.scene.record.OrbitComponent;
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
            if (entity != null) {
                Trajectory trajectory = Mapper.trajectory.get(entity);
                Verts verts = Mapper.verts.get(entity);
                if (trajectory == null) {
                    throw new RuntimeException("Trajectory component does not exist for orbit object: " + Mapper.base.get(entity).getName());
                }
                if (verts == null) {
                    throw new RuntimeException("Verts component does not exist for orbit object: " + Mapper.base.get(entity).getName());
                }
                orbitalParams = trajectory.oc;
                data = verts.pointCloudData;
            } else {
                throw new RuntimeException("This " + this.getClass().getSimpleName() + " object does have neither an orbit nor an entity object.");
            }
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
        if (data == null) {
            return out;
        }
        long dateWrap = data.getWrapTimeMs(date);
        int basei = data.getIndex(dateWrap);

        int nexti = (basei + 1) % data.getNumPoints();
        double percent = (double) Math.abs(dateWrap - data.getDate(basei).toEpochMilli()) / (double) Math.abs(data.getDate(nexti).toEpochMilli() - data.getDate(basei).toEpochMilli());

        data.loadPoint(out, basei);
        data.loadPoint(aux, nexti);

        double len = aux.sub(out).len();
        aux.nor().scl(percent * len);
        out.add(aux);

        Matrix4d transformFunction = getTransformFunction();
        Matrix4d parentOrientation = getParentOrientation();

        if (transformFunction == null && parentOrientation != null) {
            transf.set(parentOrientation);
        } else if (transformFunction != null) {
            transf.set(transformFunction);
        } else {
            transf.idt();
        }
        if (!isNewMethod()) {
            transf.rotate(0, 1, 0, orbitalParams.argofpericenter);
            transf.rotate(0, 0, 1, orbitalParams.i);
            transf.rotate(0, 1, 0, orbitalParams.ascendingnode);
        } else {
            if (entity != null && Mapper.trajectory.get(entity).model.isExtrasolar()) {
                transf.rotate(0, 1, 0, 90);
            }
        }

        out.mul(transf).scl(scaling);

        // Move to center if needed
        if (center != null && !center.isZero())
            out.add(center);
        return out;
    }

    protected boolean isNewMethod() {
        if (entity != null) {
            return Mapper.trajectory.get(entity).newMethod;
        }
        return false;
    }

    protected Matrix4d getTransformFunction() {
        if (entity != null) {
            return Mapper.transform.get(entity).matrix;
        }
        return null;
    }

    protected Matrix4d getParentOrientation() {
        if (entity != null) {
            return Mapper.graph.get(Mapper.graph.get(entity).parent).orientation;
        }
        return null;

    }

}
