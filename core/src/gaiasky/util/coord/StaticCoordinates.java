/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.coord;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Method;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Nature;
import gaiasky.util.math.*;

import java.time.Instant;
import java.util.Map;

public class StaticCoordinates implements IBodyCoordinates {

    private Vector3Q position;
    private String transformName;
    private Matrix4D trf;

    @Override
    public void doneLoading(Object... params) {
        if (trf != null) {
            this.position.mul(trf);
        }
    }

    @Override
    public Vector3Q getEclipticSphericalCoordinates(Instant date, Vector3Q out) {
        return out.set(position);
    }

    @Override
    public Vector3Q getEclipticCartesianCoordinates(Instant date, Vector3Q out) {
        return out.set(position);
    }

    @Override
    public Vector3Q getEquatorialCartesianCoordinates(Instant date, Vector3Q out) {
        return out.set(position);
    }

    public void setTransformFunction(String transformName) {
        setTransformName(transformName);
    }

    public void setTransformName(String transformName) {
        this.transformName = transformName;
        if (transformName != null) {
            Class<Coordinates> c = Coordinates.class;
            try {
                Method m = ClassReflection.getMethod(c, transformName);
                Object obj = m.invoke(null);

                if (obj instanceof Matrix4) {
                    trf = new Matrix4D(((Matrix4) obj).val);
                } else if (obj instanceof Matrix4D) {
                    trf = new Matrix4D((Matrix4D) obj);
                }

            } catch (ReflectionException e) {
                Logger.getLogger(this.getClass()).error("Error getting/invoking method Coordinates." + transformName + "()");
            }
        }
    }

    public void setTransformMatrix(double[] transformMatrix) {
        trf = new Matrix4D(transformMatrix);
    }

    public Vector3Q getPosition() {
        return position;
    }

    public void setPosition(Vector3Q pos) {
        this.position = new Vector3Q(pos);
    }

    public void setPosition(double[] position) {
        setPositionKm(position);
    }

    public void setPositionKm(double[] position) {
        this.position = new Vector3Q(position[0] * Constants.KM_TO_U, position[1] * Constants.KM_TO_U, position[2] * Constants.KM_TO_U);
    }

    public void setPositionkm(double[] position) {
        setPositionKm(position);
    }

    public void setPositionEquatorial(double[] position) {
        this.position = new Vector3Q();
        this.position.set(Coordinates.sphericalToCartesian(position[0] * Nature.TO_RAD, position[1] * Nature.TO_RAD, position[2] * Constants.PC_TO_U, new Vector3D()));
    }

    public void setPositionGalactic(double[] position) {
        this.position = new Vector3Q();
        this.position.set(Coordinates.sphericalToCartesian(position[0] * Nature.TO_RAD, position[1] * Nature.TO_RAD, position[2] * Constants.PC_TO_U, new Vector3D()));
        this.position.mul(Coordinates.galacticToEquatorial());
    }

    public void setPositionEcliptic(double[] position) {
        this.position = new Vector3Q();
        this.position.set(Coordinates.sphericalToCartesian(position[0] * Nature.TO_RAD, position[1] * Nature.TO_RAD, position[2] * Constants.PC_TO_U, new Vector3D()));
        this.position.mul(Coordinates.eclipticToEquatorial());
    }

    public void setPositionAu(double[] position) {
        this.position = new Vector3Q(position[0] * Constants.AU_TO_U, position[1] * Constants.AU_TO_U, position[2] * Constants.AU_TO_U);
    }

    public void setPositionPc(double[] position) {
        this.position = new Vector3Q(position[0] * Constants.PC_TO_U, position[1] * Constants.PC_TO_U, position[2] * Constants.PC_TO_U);
    }

    public void setPositionpc(double[] position) {
        setPositionPc(position);
    }

    /**
     * Sets equatorial coordinates as a vector of [ra, de, distance]
     *
     * @param equatorial Vector with [ra, dec, distance] with angles in degrees and distance in parsecs
     */
    public void setEquatorial(double[] equatorial) {
        double ra = MathUtilsDouble.degRad * equatorial[0];
        double dec = MathUtilsDouble.degRad * equatorial[1];
        double dist = Constants.PC_TO_U * equatorial[2];
        this.position = new Vector3Q();
        Coordinates.sphericalToCartesian(ra, dec, Quadruple.from(dist), this.position);
    }

    @Override
    public String toString() {
        return "{" + "pos=" + position + ", trf='" + transformName + '\'' + '}';
    }

    @Override
    public void updateReferences(Map<String, Entity> index) {
    }

    @Override
    public IBodyCoordinates getCopy() {
        return this;
    }

}
