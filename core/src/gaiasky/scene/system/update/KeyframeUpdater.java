/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.update;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.GaiaSky;
import gaiasky.data.util.PointCloudData;
import gaiasky.scene.Mapper;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.view.VertsView;
import gaiasky.util.math.Vector3d;
import net.jafama.FastMath;

public class KeyframeUpdater extends AbstractUpdateSystem {

    private final Vector3d D31 = new Vector3d();
    private final Vector3d D32 = new Vector3d();
    private final Vector3d D33 = new Vector3d();

    private GraphUpdater graphUpdater;
    private VertsView view;

    public KeyframeUpdater(Family family, int priority) {
        super(family, priority);
        graphUpdater = new GraphUpdater(null, 0, GaiaSky.instance.time);
        view = new VertsView();
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        updateEntity(entity, deltaTime);
    }

    @Override
    public void updateEntity(Entity entity, float deltaTime) {
        var base = Mapper.base.get(entity);
        var graph = Mapper.graph.get(entity);
        var kf = Mapper.keyframes.get(entity);

        ICamera camera = GaiaSky.instance.getICamera();
        graphUpdater.setCamera(camera);

        for (Entity object : kf.objects) {
            graphUpdater.update(object, GaiaSky.instance.time, graph.translation, base.opacity);
        }

        // Update length of orientations
        for (Entity vo : kf.orientations) {
            Vector3d p0 = D31;
            Vector3d p1 = D32;
            view.setEntity(vo);
            PointCloudData p = view.getPointCloud();
            p0.set(p.x.get(0), p.y.get(0), p.z.get(0));
            p1.set(p.x.get(1), p.y.get(1), p.z.get(1));

            Vector3d c = D33.set(camera.getPos());
            double len = FastMath.max(1e-9, FastMath.atan(0.03) * c.dst(p0));

            Vector3d v = c.set(p1).sub(p0).nor().scl(len);
            p.x.set(1, p0.x + v.x);
            p.y.set(1, p0.y + v.y);
            p.z.set(1, p0.z + v.z);

            view.markForUpdate();
        }

    }
}
