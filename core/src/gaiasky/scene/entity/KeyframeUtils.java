/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.entity;

import com.badlogic.ashley.core.Entity;
import gaiasky.render.ComponentTypes;
import gaiasky.render.RenderGroup;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.component.tag.TagNoProcess;
import gaiasky.util.math.Vector3b;

public class KeyframeUtils {

    public KeyframeUtils(Scene scene) {
    }

    public Entity newVerts(Scene scene, String name, ComponentTypes ct, String className, float[] color, RenderGroup rg, boolean closedLoop, float primitiveSize, boolean arrowCaps) {
        var entity = scene.archetypes().get(className).createEntity();

        var base = Mapper.base.get(entity);
        base.setName(name);
        base.ct = ct;

        var body = Mapper.body.get(entity);
        body.setColor(color);

        var verts = Mapper.verts.get(entity);
        verts.renderGroup = rg;
        verts.primitiveSize = primitiveSize;

        var graph = Mapper.graph.get(entity);
        graph.translation = new Vector3b();

        // First, initialize it.
        scene.initializeEntity(entity);
        // Then, add no-process tag.
        entity.add(scene.engine.createComponent(TagNoProcess.class));

        var arrow = Mapper.arrow.get(entity);
        if (arrow != null) {
            arrow.arrowCap = arrowCaps;
        }

        return entity;
    }

    public Entity newVerts(Scene scene, String name, ComponentTypes ct, String className, float[] color, RenderGroup rg, boolean closedLoop, float primitiveSize) {
        return newVerts(scene, name, ct, className, color, rg, closedLoop, primitiveSize, false);
    }

    public Entity newVerts(Scene scene, String name, ComponentTypes ct, String className, float[] color, RenderGroup rg, boolean closedLoop, float primitiveSize, boolean blend, boolean depth) {
        var entity = newVerts(scene, name, ct, className, color, rg, closedLoop, primitiveSize);

        var verts = Mapper.verts.get(entity);
        verts.blend = blend;
        verts.depth = depth;

        return entity;
    }
}
