/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.entity;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import gaiasky.GaiaSky;
import gaiasky.data.group.DatasetOptions;
import gaiasky.data.group.DatasetOptions.DatasetLoadType;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.scene.component.ParticleSet;
import gaiasky.scene.component.Verts;
import gaiasky.scene.view.FocusView;
import gaiasky.util.math.Vector3Q;
import gaiasky.util.math.Vector3D;
import uk.ac.starlink.table.ColumnInfo;

import java.util.List;

public class EntityUtils {

    /**
     * Returns the absolute position of this entity in the internal reference system and internal units.
     *
     * @param entity The entity.
     * @param out    Auxiliary vector to put the result in.
     * @return The vector with the position.
     */
    public static Vector3Q getAbsolutePosition(final Entity entity, Vector3Q out) {
        if (entity != null) {
            var body = Mapper.body.get(entity);
            out.set(body.pos);

            var e = entity;
            var graph = Mapper.graph.get(e);
            while (graph.parent != null) {
                e = graph.parent;
                graph = Mapper.graph.get(e);
                out.add(Mapper.body.get(e).pos);
            }
        }
        return out;
    }

    /**
     * This is a faster version of {@link EntityUtils#getAbsolutePosition(Entity, Vector3Q)} that uses double-precision
     * vectors instead of arbitrary precision.
     *
     * @param entity The entity.
     * @param out    Auxiliary vector to put the result in.
     * @return The vector with the position.
     */
    public static Vector3D getAbsolutePosition(final Entity entity, Vector3D out) {
        if (entity != null) {
            var body = Mapper.body.get(entity);
            body.pos.put(out);

            var e = entity;
            var graph = Mapper.graph.get(e);
            while (graph.parent != null) {
                e = graph.parent;
                graph = Mapper.graph.get(e);
                out.add(Mapper.body.get(e).pos);
            }
        }
        return out;
    }

    /**
     * Returns the absolute position of the entity identified by the given name
     * within this entity in the native coordinates (equatorial system) and internal units.
     *
     * @param entity The entity.
     * @param name   The name.
     * @param out    Auxiliary vector to put the result in.
     * @return The vector with the position.
     */
    public static Vector3Q getAbsolutePosition(Entity entity, String name, Vector3Q out) {
        if (Mapper.particleSet.has(entity)) {
            return Mapper.particleSet.get(entity).getAbsolutePosition(name, out);
        } else if (Mapper.starSet.has(entity)) {
            return Mapper.starSet.get(entity).getAbsolutePosition(name, out);
        } else {
            return getAbsolutePosition(entity, out);
        }
    }

    /**
     * This is a faster version of {@link EntityUtils#getAbsolutePosition(Entity, String, Vector3Q)} that uses double-precision
     * vectors instead of arbitrary precision.
     *
     * @param entity The entity.
     * @param name   The name.
     * @param out    Auxiliary vector to put the result in.
     * @return The vector with the position.
     */
    public static Vector3D getAbsolutePosition(Entity entity, String name, Vector3D out) {
        if (Mapper.particleSet.has(entity)) {
            return Mapper.particleSet.get(entity).getAbsolutePosition(name, out);
        } else if (Mapper.starSet.has(entity)) {
            return Mapper.starSet.get(entity).getAbsolutePosition(name, out);
        } else {
            return getAbsolutePosition(entity, out);
        }
    }

    /**
     * Checks whether the given entity has a {@link gaiasky.scene.component.Coordinates} component,
     * and the current time is out of range.
     *
     * @return Whether the entity is in time overflow.
     */
    public static boolean isCoordinatesTimeOverflow(Entity entity) {
        return Mapper.coordinates.has(entity) && Mapper.coordinates.get(entity).timeOverflow;
    }

    /**
     * Prepares the blending OpenGL state given a {@link Verts} component.
     *
     * @param verts The verts component.
     */
    public static void blend(Verts verts) {
        if (verts.blend) {
            Gdx.gl20.glEnable(GL20.GL_BLEND);
            if (verts.additive) {
                Gdx.gl20.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE);
            } else {
                Gdx.gl20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            }
        } else {
            Gdx.gl20.glDisable(GL20.GL_BLEND);
        }
    }

    /**
     * Prepares the depth test OpenGL state given an {@link Verts} component.
     *
     * @param verts The verts component.
     */
    public static void depth(Verts verts) {
        Gdx.gl20.glDepthMask(verts.depth);
        if (verts.depth) {
            Gdx.gl20.glEnable(GL20.GL_DEPTH_TEST);
        } else {
            Gdx.gl20.glDisable(GL20.GL_DEPTH_TEST);
        }
    }


    public static boolean isVisibilityOn(Entity entity) {
        return GaiaSky.instance.isOn(Mapper.base.get(entity).ct);
    }

    /**
     * Re-implementation of {@link FocusView#getRadius()} in a static context.
     *
     * @param entity The entity.
     * @return The radius of the entity.
     */
    public static double getRadius(Entity entity) {
        var set = getSet(entity);
        if (set != null) {
            return set.getRadius();
        } else {
            var extra = Mapper.extra.get(entity);
            var body = Mapper.body.get(entity);
            return extra != null ? extra.radius : body.size / 2.0;
        }
    }

    /**
     * Gets the total span of the entity model after transformation, taking into account the original model span (length
     * of
     * the furthest vertex from the origin) plus the scaling in its local transform (size and sizeScaleFactor).
     *
     * @param entity The entity.
     * @return The total span of the entity, in internal units.
     */
    public static double getModelSpan(Entity entity) {
        var set = getSet(entity);
        if (set != null) {
            return set.getRadius() * (set.model != null ? set.model.span : 1);
        } else {
            var scaffolding = Mapper.modelScaffolding.get(entity);
            var body = Mapper.body.get(entity);
            var model = Mapper.model.get(entity);
            return (scaffolding != null ? body.size * scaffolding.sizeScaleFactor : body.size) * (model.model.instance != null ? model.model.instance.span : 1);
        }
    }

    private static ParticleSet getSet(Entity entity) {
        if (Mapper.particleSet.has(entity)) {
            return Mapper.particleSet.get(entity);
        } else if (Mapper.starSet.has(entity)) {
            return Mapper.starSet.get(entity);
        }
        return null;
    }

}
