/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.update;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import gaiasky.GaiaSky;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Body;
import gaiasky.scene.component.GraphNode;
import gaiasky.scene.component.Perimeter;
import gaiasky.util.math.MathUtilsDouble;
import net.jafama.FastMath;

public class PerimeterUpdater extends AbstractUpdateSystem {

    private final Vector3 aux3 = new Vector3();
    private final ModelUpdater updater;

    public PerimeterUpdater(Family family, int priority) {
        super(family, priority);
        updater = new ModelUpdater(null, 0);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        updateEntity(entity, deltaTime);
    }

    @Override
    public void updateEntity(Entity entity, float deltaTime) {
        var camera = GaiaSky.instance.getICamera();
        var graph = Mapper.graph.get(entity);
        var base = Mapper.base.get(entity);
        var body = Mapper.body.get(entity);
        var perimeter = Mapper.perimeter.get(entity);

        var parent = graph.parent;
        var parentSa = Mapper.sa.get(parent);
        var parentBody = Mapper.body.get(parent);

        float angleLow = (float) parentSa.thresholdQuad * camera.getFovFactor() * 30f;
        float angleHigh = angleLow * 3f;

        if (GaiaSky.instance.isOn(base.ct) && parentBody.solidAngleApparent > angleLow
                && parentBody.solidAngle < 1.09) {
            graph.localTransform.idt();
            toCartesian(perimeter.loc2d[0][0][0], perimeter.loc2d[0][0][1], perimeter.cart0, graph.localTransform);

            updateLocalValues(parent, parentBody, graph, perimeter);
            graph.translation.add(body.pos);

            base.opacity = (float) MathUtilsDouble.flint(parentBody.solidAngleApparent, angleLow, angleHigh, 0, 1);
            base.opacity *= base.getVisibilityOpacityFactor();

            body.distToCamera = (float) graph.translation.lenDouble();
            body.solidAngle = (float) FastMath.atan(body.size / body.distToCamera);
            body.solidAngleApparent = body.solidAngle / camera.getFovFactor();
        } else {
            base.opacity = 0;
        }
    }

    public void updateLocalValues(Entity papa, Body papaBody, GraphNode graph, Perimeter perimeter) {
        var papaGraph = Mapper.graph.get(papa);
        updater.setToLocalTransform(papa, papaBody, papaGraph, 1f, graph.localTransform, false);
        int lineIndex = 0;
        for (float[][] line : perimeter.loc2d) {
            int pointIndex = 0;
            for (float[] point : line) {
                toCartesian(point[0], point[1], aux3, graph.localTransform);

                perimeter.loc3d[lineIndex][pointIndex][0] = aux3.x;
                perimeter.loc3d[lineIndex][pointIndex][1] = aux3.y;
                perimeter.loc3d[lineIndex][pointIndex][2] = aux3.z;

                pointIndex++;
            }
            lineIndex++;
        }
    }

    private void toCartesian(float lon, float lat, Vector3 res, Matrix4 localTransform) {
        res.set(0, 0, -0.5015f);
        // Latitude [-90..90]
        res.rotate(lat, 1, 0, 0);
        // Longitude [0..360]
        res.rotate(lon + 90, 0, 1, 0);

        res.mul(localTransform);
    }
}
