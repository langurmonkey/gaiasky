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
import gaiasky.scene.view.FocusView;
import gaiasky.util.math.QuaternionDouble;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;

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
    protected FocusView view;
    protected final Vector3b a, b, c, lastPos;
    protected final Vector3d up, dir, side, lastDir;
    protected boolean initialized = false;

    public LVLHOrientationServer(String objectName) {
        super();
        lastOrientation = new QuaternionDouble();
        this.objectName = objectName.substring(objectName.lastIndexOf("/") + 1);
        this.view = new FocusView();
        a = new Vector3b();
        b = new Vector3b();
        c = new Vector3b();
        lastPos = new Vector3b();
        up = new Vector3d();
        dir = new Vector3d();
        side = new Vector3d();
        lastDir = new Vector3d();
    }

    private void lazyInitialize() {
        if (!initialized) {
            var scene = GaiaSky.instance.scene;
            object = scene.getEntity(objectName);
            if (object != null) {
                view.setEntity(object);
                view.setScene(scene);
                var graph = Mapper.graph.get(object);
                parent = graph.parent;
            }
            initialized = true;
        }
    }

    @Override
    public QuaternionDouble getOrientation(Date date) {
        return getOrientation(date.toInstant());
    }

    @Override
    public QuaternionDouble getOrientation(Instant instant) {
        lazyInitialize();
        if(object != null && parent != null) {
            // Position of object in local reference system.
            a.set(Mapper.body.get(object).pos);

            if(!lastPos.equals(a)) {
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
