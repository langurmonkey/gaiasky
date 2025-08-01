/*
 * Copyright (c) 2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.orientation;

import com.badlogic.ashley.core.Entity;
import gaiasky.GaiaSky;
import gaiasky.data.api.OrientationServer;
import gaiasky.scene.Mapper;
import gaiasky.util.math.QuaternionDouble;
import gaiasky.util.math.Vector3D;
import gaiasky.util.math.Vector3Q;

import java.time.Instant;
import java.util.Date;

/**
 * This class implements the LVLH (Local Vertical Local Horizontal) attitude.
 */
public class LVLHOrientationServer implements OrientationServer {

    protected final QuaternionDouble lastOrientation;
    protected final String objectName;
    protected Entity object;
    protected Entity parent;
    protected final Vector3Q a, b, c, lastPos;
    protected final Vector3D up, dir, side, lastDir;
    protected boolean initialized = false;

    public LVLHOrientationServer(String objectName) {
        super();
        lastOrientation = new QuaternionDouble();
        this.objectName = objectName.substring(objectName.lastIndexOf("/") + 1);
        a = new Vector3Q();
        b = new Vector3Q();
        c = new Vector3Q();
        lastPos = new Vector3Q();
        up = new Vector3D();
        dir = new Vector3D();
        side = new Vector3D();
        lastDir = new Vector3D();
    }

    private void lazyInitialize() {
        if (!initialized) {
            var scene = GaiaSky.instance.scene;
            object = scene.getEntity(objectName);
            if (object != null) {
                // Find nearest celestial parent up the hierarchy.
                var obj = object;
                do {
                    var graph = Mapper.graph.get(obj);
                    obj = graph.parent;
                } while(!Mapper.celestial.has(obj));
                parent = obj;
            }
            initialized = true;
        }
    }

    @Override
    public QuaternionDouble updateOrientation(Date date) {
        return updateOrientation(date.toInstant());
    }

    @Override
    public QuaternionDouble updateOrientation(Instant instant) {
        lazyInitialize();
        if (object != null && parent != null) {
            // Position of object in local reference system.
            a.set(Mapper.body.get(object).pos);

            if (!lastPos.equals(a)) {
                // Compute direction using lastPos.
                lastPos.sub(a).nor().put(dir).scl(-1);
                lastDir.set(dir);
                // Store lastPos.
                lastPos.set(a);
            } else {
                // LastPos = a, use lastDir.
                dir.set(lastDir);
            }

            // Compute vertical vector, our up.
            a.put(up).nor();

            // Make sure it is consistent.
            side.set(up).crs(dir).nor();
            //dir.set(side).crs(up).nor();

            // Create quaternion from axes.
            lastOrientation.fromAxes(dir, side, up).nor();
        }
        return lastOrientation;
    }

    @Override
    public QuaternionDouble getCurrentOrientation() {
        return lastOrientation;
    }

    @Override
    public boolean hasOrientation() {
        return true;
    }
}
