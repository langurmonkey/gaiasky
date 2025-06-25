/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2.impl;

import gaiasky.event.EventManager;
import gaiasky.script.v2.api.GeomAPI;
import gaiasky.util.math.Vector2D;
import gaiasky.util.math.Vector3D;

import java.util.List;

/**
 * The geometry module provides calls and methods to carry out geometrical operations directly
 * within the scripting system.
 */
public class GeomModule extends APIModule implements GeomAPI {
    private final Vector3D aux3d1 = new Vector3D();
    private final Vector3D aux3d2 = new Vector3D();
    private final Vector2D aux2d1 = new Vector2D();

    /**
     * Create a new module with the given attributes.
     *
     * @param api  Reference to the API class.
     * @param name Name of the module.
     */
    public GeomModule(EventManager em, APIv2 api, String name) {
        super(em, api, name);
    }

    @Override
    public double[] rotate3(double[] vector, double[] axis, double angle) {
        Vector3D v = aux3d1.set(vector);
        Vector3D a = aux3d2.set(axis);
        return v.rotate(a, angle).values();
    }

    public double[] rotate3(double[] vector, double[] axis, long angle) {
        return rotate3(vector, axis, (double) angle);
    }

    public double[] rotate3(List<?> vector, List<?> axis, double angle) {
        return rotate3(api.dArray(vector), api.dArray(axis), angle);
    }

    public double[] rotate3(List<?> vector, List<?> axis, long angle) {
        return rotate3(vector, axis, (double) angle);
    }

    @Override
    public double[] rotate2(double[] vector, double angle) {
        Vector2D v = aux2d1.set(vector);
        return v.rotate(angle).values();
    }

    public double[] rotate2(double[] vector, long angle) {
        return rotate2(vector, (double) angle);
    }

    public double[] rotate2(List<?> vector, double angle) {
        return rotate2(api.dArray(vector), angle);
    }

    public double[] rotate2(List<?> vector, long angle) {
        return rotate2(vector, (double) angle);
    }

    @Override
    public double[] cross3(double[] vec1, double[] vec2) {
        return aux3d1.set(vec1).crs(aux3d2.set(vec2)).values();
    }

    public double[] cross3(List<?> vec1, List<?> vec2) {
        return cross3(api.dArray(vec1), api.dArray(vec2));
    }

    @Override
    public double dot3(double[] vec1, double[] vec2) {
        return aux3d1.set(vec1).dot(aux3d2.set(vec2));
    }

    public double dot3(List<?> vec1, List<?> vec2) {
        return dot3(api.dArray(vec1), api.dArray(vec2));
    }
}
