/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Method;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import gaiasky.scene.Mapper;
import gaiasky.scene.system.render.draw.line.LineEntityRenderSystem;
import gaiasky.util.Logger;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.math.Matrix4D;
import gaiasky.util.math.Vector3D;

public class AxesInitializer extends AbstractInitSystem {

    public AxesInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
    }

    @Override
    public void initializeEntity(Entity entity) {
        var axis = Mapper.axis.get(entity);
        var line = Mapper.line.get(entity);

        // Lines.
        line.lineWidth = 1f;
        line.renderConsumer = LineEntityRenderSystem::renderAxes;

        // Base
        axis.b0 = new Vector3D(1, 0, 0);
        axis.b1 = new Vector3D(0, 1, 0);
        axis.b2 = new Vector3D(0, 0, 1);

        axis.o = new Vector3D();
        axis.x = new Vector3D();
        axis.y = new Vector3D();
        axis.z = new Vector3D();
    }

    @Override
    public void setUpEntity(Entity entity) {
        var axis = Mapper.axis.get(entity);
        var transform = Mapper.transform.get(entity);

        if (transform.transformName != null) {
            Class<Coordinates> c = Coordinates.class;
            try {
                Method m = ClassReflection.getMethod(c, transform.transformName);
                transform.matrix = (Matrix4D) m.invoke(null);
            } catch (ReflectionException e) {
                Logger.getLogger(this.getClass()).error("Error getting/invoking method Coordinates." + transform.transformName + "()");
            }
            axis.b0.mul(transform.matrix);
            axis.b1.mul(transform.matrix);
            axis.b2.mul(transform.matrix);
        }

        // Axes colors, RGB by default.
        if (axis.axesColors == null) {
            axis.axesColors = new float[][] { { 1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1 } };
        }
    }
}
