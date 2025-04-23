/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.coord;

import com.badlogic.ashley.core.Entity;
import gaiasky.data.util.PointCloudData;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Trajectory;
import gaiasky.scene.component.Verts;
import gaiasky.scene.record.OrbitComponent;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;
import net.jafama.FastMath;

import java.time.Instant;
import java.util.Map;

public class OrbitLintCoordinates extends AbstractOrbitCoordinates {
    OrbitComponent orbitalParams;
    PointCloudData data;
    Matrix4d transform;
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
            transform = new Matrix4d();
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
        boolean inRange = getData().loadPoint(out, date);
        if(!periodic && !inRange) {
            return null;
        }
        if (data == null) {
            return out;
        }
        long dateWrap = data.getWrapTimeMs(date);
        int baseIndex = data.getIndex(dateWrap);

        int nextIndex = (baseIndex + 1) % data.getNumPoints();
        double percent = (double) FastMath.abs(dateWrap - data.getDate(baseIndex).toEpochMilli()) / (double) FastMath.abs(data.getDate(nextIndex).toEpochMilli() - data.getDate(baseIndex).toEpochMilli());

        data.loadPoint(out, baseIndex);
        data.loadPoint(aux, nextIndex);

        double len = aux.sub(out).len();
        aux.nor().scl(percent * len);
        out.add(aux);

        Matrix4d transformFunction = getTransformFunction();
        Matrix4d parentOrientation = getParentOrientation();

        if (transformFunction == null && parentOrientation != null) {
            transform.set(parentOrientation);
        } else if (transformFunction != null) {
            transform.set(transformFunction);
        } else {
            transform.idt();
        }
        if (!isNewMethod()) {
            transform.rotate(0, 1, 0, orbitalParams.argOfPericenter);
            transform.rotate(0, 0, 1, orbitalParams.i);
            transform.rotate(0, 1, 0, orbitalParams.ascendingNode);
        } else {
            if (entity != null && Mapper.trajectory.get(entity).model.isExtrasolar()) {
                transform.rotate(0, 1, 0, 90);
            }
        }

        out.mul(transform).scl(scaling);

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

    @Override
    public void updateReferences(Map<String, Entity> index) {
        updateOwner(index);
    }

    @Override
    public IBodyCoordinates getCopy() {
        var copy = new OrbitLintCoordinates();
        copy.orbitalParams = this.orbitalParams;
        copy.transform = this.transform.cpy();
        copy.data = this.data;
        copyParameters(copy);
        return copy;
    }

}
