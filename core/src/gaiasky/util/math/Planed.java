
/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.math;

import java.io.Serializable;

public class Planed implements Serializable {
    private static final long serialVersionUID = -1240652082930747866L;
    public final Vector3d normal = new Vector3d();
    public double d = 0;
    /**
     * Constructs a new plane with all values set to 0
     */
    public Planed() {

    }

    /**
     * Constructs a new plane based on the normal and distance to the origin.
     *
     * @param normal The plane normal
     * @param d      The distance to the origin
     */
    public Planed(Vector3d normal, double d) {
        this.normal.set(normal).nor();
        this.d = d;
    }

    /**
     * Constructs a new plane based on the normal and a point on the plane.
     *
     * @param normal The normal
     * @param point  The point on the plane
     */
    public Planed(Vector3d normal, Vector3d point) {
        this.normal.set(normal).nor();
        this.d = -this.normal.dot(point);
    }

    /**
     * Constructs a new plane out of the three given points that are considered to be on the plane. The normal is calculated via a
     * cross product between (point1-point2)x(point2-point3)
     *
     * @param point1 The first point
     * @param point2 The second point
     * @param point3 The third point
     */
    public Planed(Vector3d point1, Vector3d point2, Vector3d point3) {
        set(point1, point2, point3);
    }

    /**
     * Sets the plane normal and distance to the origin based on the three given points which are considered to be on the plane.
     * The normal is calculated via a cross product between (point1-point2)x(point2-point3)
     *
     * @param point1
     * @param point2
     * @param point3
     */
    public void set(Vector3d point1, Vector3d point2, Vector3d point3) {
        normal.set(point1).sub(point2).crs(point2.x - point3.x, point2.y - point3.y, point2.z - point3.z).nor();
        d = -point1.dot(normal);
    }

    /**
     * Sets the plane normal and distance
     *
     * @param nx normal x-component
     * @param ny normal y-component
     * @param nz normal z-component
     * @param d  distance to origin
     */
    public void set(double nx, double ny, double nz, double d) {
        normal.set(nx, ny, nz);
        this.d = d;
    }

    /**
     * Calculates the shortest signed distance between the plane and the given point.
     *
     * @param point The point
     *
     * @return the shortest signed distance between the plane and the point
     */
    public double distance(Vector3d point) {
        return normal.dot(point) + d;
    }

    /**
     * Returns on which side the given point lies relative to the plane and its normal. PlaneSide.Front refers to the side the
     * plane normal points to.
     *
     * @param point The point
     *
     * @return The side the point lies relative to the plane
     */
    public PlaneSide testPoint(Vector3d point) {
        double dist = normal.dot(point) + d;

        if (dist == 0)
            return PlaneSide.OnPlane;
        else if (dist < 0)
            return PlaneSide.Back;
        else
            return PlaneSide.Front;
    }

    /**
     * Returns on which side the given point lies relative to the plane and its normal. PlaneSide.Front refers to the side the
     * plane normal points to.
     *
     * @param x
     * @param y
     * @param z
     *
     * @return The side the point lies relative to the plane
     */
    public PlaneSide testPoint(double x, double y, double z) {
        double dist = normal.dot(x, y, z) + d;

        if (dist == 0)
            return PlaneSide.OnPlane;
        else if (dist < 0)
            return PlaneSide.Back;
        else
            return PlaneSide.Front;
    }

    /**
     * Returns whether the plane is facing the direction vector. Think of the direction vector as the direction a camera looks in.
     * This method will return true if the front side of the plane determined by its normal faces the camera.
     *
     * @param direction the direction
     *
     * @return whether the plane is front facing
     */
    public boolean isFrontFacing(Vector3d direction) {
        double dot = normal.dot(direction);
        return dot <= 0;
    }

    /** @return The normal */
    public Vector3d getNormal() {
        return normal;
    }

    /** @return The distance to the origin */
    public double getD() {
        return d;
    }

    /**
     * Sets the plane to the given point and normal.
     *
     * @param point  the point on the plane
     * @param normal the normal of the plane
     */
    public void set(Vector3d point, Vector3d normal) {
        this.normal.set(normal);
        d = -point.dot(normal);
    }

    public void set(double pointX, double pointY, double pointZ, double norX, double norY, double norZ) {
        this.normal.set(norX, norY, norZ);
        d = -(pointX * norX + pointY * norY + pointZ * norZ);
    }

    /**
     * Sets this plane from the given plane
     *
     * @param plane the plane
     */
    public void set(Planed plane) {
        this.normal.set(plane.normal);
        this.d = plane.d;
    }

    public String toString() {
        return normal.toString() + ", " + d;
    }

    /**
     * Enum specifying on which side a point lies respective to the plane and it's normal. {@link PlaneSide#Front} is the side to
     * which the normal points.
     *
     * @author mzechner
     */
    public enum PlaneSide {
        OnPlane,
        Back,
        Front
    }
}
