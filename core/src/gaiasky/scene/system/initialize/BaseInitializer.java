/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.math.Matrix4;
import gaiasky.GaiaSky;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.util.math.Vector3b;

public class BaseInitializer extends AbstractInitSystem {

    private final Scene scene;

    public BaseInitializer(Scene scene, boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
        this.scene = scene;
    }

    @Override
    public void initializeEntity(Entity entity) {
        var base = Mapper.base.get(entity);
        var graph = Mapper.graph.get(entity);
        var render = Mapper.render.get(entity);

        // Initialize component type, if entity does not have one.
        if (base.ct == null) {
            base.ct = new ComponentTypes(ComponentType.Others.ordinal());
        }

        // Initialize base scene graph structures.
        if (graph != null) {
            graph.localTransform = new Matrix4();
            graph.translation = new Vector3b();
        }

        // Render reference.
        if (render != null) {
            render.entity = entity;
        }

    }

    @Override
    public void setUpEntity(Entity entity) {
        if (Mapper.coordinates.has(entity)) {
            var coord = Mapper.coordinates.get(entity);
            if (coord.coordinates != null) {
                var body = Mapper.body.get(entity);
                coord.coordinates.doneLoading(scene, entity);
                // Make sure the position is up-to-date.
                coord.coordinates.getEquatorialCartesianCoordinates(GaiaSky.instance.time.getTime(), body.pos);
            }
        }
    }
}
