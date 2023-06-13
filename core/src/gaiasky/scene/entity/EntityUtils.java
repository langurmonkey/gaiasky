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
import gaiasky.scene.component.Body;
import gaiasky.scene.component.Celestial;
import gaiasky.scene.component.ParticleSet;
import gaiasky.scene.component.Verts;
import gaiasky.scene.view.FocusView;
import gaiasky.util.CatalogInfo;
import gaiasky.util.math.Vector3b;

import java.util.List;

public class EntityUtils {

    /**
     * Returns the absolute position of this entity in the internal reference system and internal units.
     *
     * @param entity The entity.
     * @param out    Auxiliary vector to put the result in.
     *
     * @return The vector with the position.
     */
    public static Vector3b getAbsolutePosition(final Entity entity,
                                               Vector3b out) {
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
     * Returns the absolute position of the entity identified by the given name
     * within this entity in the native coordinates (equatorial system) and internal units.
     *
     * @param entity The entity.
     * @param name   The name.
     * @param out    Auxiliary vector to put the result in.
     *
     * @return The vector with the position.
     */
    public static Vector3b getAbsolutePosition(Entity entity,
                                               String name,
                                               Vector3b out) {
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

    /**
     * Sets the pale color of this body.
     *
     * @param body      The body component.
     * @param celestial The celestial component.
     * @param plus      The addition.
     */
    public static void setColor2Data(final Body body,
                                     final Celestial celestial,
                                     final float plus) {
        celestial.colorPale = new float[] { Math.min(1, body.color[0] + plus), Math.min(1, body.color[1] + plus), Math.min(1, body.color[2] + plus) };
    }

    public static Entity getParticleSet(Scene scene,
                                        String name,
                                        String file,
                                        List<IParticleRecord> data,
                                        DatasetOptions datasetOptions,
                                        boolean addToCatalogManager) {
        double[] fadeIn = datasetOptions == null || datasetOptions.fadeIn == null ? null : datasetOptions.fadeIn;
        double[] fadeOut = datasetOptions == null || datasetOptions.fadeOut == null ? null : datasetOptions.fadeOut;
        double[] particleColor = datasetOptions == null || datasetOptions.particleColor == null ? new double[] { 1.0, 1.0, 1.0, 1.0 } : datasetOptions.particleColor;
        double colorNoise = datasetOptions == null ? 0 : datasetOptions.particleColorNoise;
        double[] labelColor = datasetOptions == null || datasetOptions.labelColor == null ? new double[] { 1.0, 1.0, 1.0, 1.0 } : datasetOptions.labelColor;
        double particleSize = datasetOptions == null ? 0 : datasetOptions.particleSize;
        double[] particleSizeLimits =
                datasetOptions == null || datasetOptions.particleSizeLimits == null ? new double[] { 0.1, 6.0 } : datasetOptions.particleSizeLimits;
        double profileDecay = datasetOptions == null ? 1 : datasetOptions.profileDecay;
        String modelType = datasetOptions == null ? "quad" : datasetOptions.modelType;
        String modelPrimitive = datasetOptions == null ? "GL_TRIANGLES" : datasetOptions.modelPrimitive;
        String ct = datasetOptions == null || datasetOptions.ct == null ? ComponentType.Galaxies.toString() : datasetOptions.ct.toString();
        boolean renderSetLabel = datasetOptions == null || datasetOptions.renderSetLabel;
        int numLabels = datasetOptions == null ? -1 : datasetOptions.numLabels;

        var archetype = scene.archetypes().get("ParticleGroup");
        Entity entity = archetype.createEntity();

        var base = Mapper.base.get(entity);
        base.id = ParticleSet.idSeq++;
        base.setName(name.replace("%%PGID%%", Long.toString(base.id)));
        base.setCt(ct);

        var body = Mapper.body.get(entity);
        body.setColor(particleColor);
        body.setLabelColor(labelColor);
        body.setSize(particleSize);

        var graph = Mapper.graph.get(entity);
        graph.setParent(Scene.ROOT_NAME);

        var fade = Mapper.fade.get(entity);
        fade.setFadeIn(fadeIn);
        fade.setFadeOut(fadeOut);

        var set = Mapper.particleSet.get(entity);
        set.setExtended(datasetOptions != null && datasetOptions.type == DatasetLoadType.PARTICLES_EXT);
        set.setData(data);
        set.setDatafile(file);
        set.setProfileDecay(profileDecay);
        set.setColorNoise(colorNoise);
        set.setParticleSizeLimits(particleSizeLimits);
        set.setModelType(modelType);
        set.setModelPrimitive(modelPrimitive);
        set.setRenderSetLabel(renderSetLabel);
        set.setNumLabels((long) numLabels);

        scene.initializeEntity(entity);
        if (addToCatalogManager) {
            scene.setUpEntity(entity);
        }

        return entity;
    }

    public static Entity getStarSet(Scene scene,
                                    String name,
                                    String file,
                                    List<IParticleRecord> data,
                                    DatasetOptions datasetOptions,
                                    boolean addToCatalogManager) {
        double[] fadeIn = datasetOptions == null || datasetOptions.fadeIn == null ? null : datasetOptions.fadeIn;
        double[] fadeOut = datasetOptions == null || datasetOptions.fadeOut == null ? null : datasetOptions.fadeOut;
        double[] labelColor = datasetOptions == null || datasetOptions.labelColor == null ? new double[] { 1.0, 1.0, 1.0, 1.0 } : datasetOptions.labelColor;
        boolean renderSetLabel = datasetOptions == null || datasetOptions.renderSetLabel;

        var archetype = scene.archetypes().get("StarGroup");
        Entity entity = archetype.createEntity();

        var base = Mapper.base.get(entity);
        base.id = ParticleSet.idSeq++;
        base.setName(name.replace("%%SGID%%", Long.toString(base.id)));
        base.setComponentType(ComponentType.Stars);

        var body = Mapper.body.get(entity);
        body.setColor(new double[] { 1.0, 1.0, 1.0, 0.25 });
        body.setLabelColor(labelColor);
        body.setSize(6.0);

        var graph = Mapper.graph.get(entity);
        graph.setParent(Scene.ROOT_NAME);

        var fade = Mapper.fade.get(entity);
        fade.setFadeIn(fadeIn);
        fade.setFadeOut(fadeOut);

        var label = Mapper.label.get(entity);
        label.setLabelPosition(new double[] { 0.0, -5.0e7, -4e8 });

        var set = Mapper.starSet.get(entity);
        set.setData(data);
        set.setDatafile(file);
        set.setRenderSetLabel(renderSetLabel);

        scene.initializeEntity(entity);
        if (addToCatalogManager) {
            scene.setUpEntity(entity);
        }

        return entity;
    }

    public static boolean isVisibilityOn(Entity entity) {
        return GaiaSky.instance.isOn(Mapper.base.get(entity).ct);
    }

    /**
     * Re-implementation of {@link FocusView#getRadius()} in a static context.
     *
     * @param entity The entity.
     *
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

    private static ParticleSet getSet(Entity entity) {
        if (Mapper.particleSet.has(entity)) {
            return Mapper.particleSet.get(entity);
        } else if (Mapper.starSet.has(entity)) {
            return Mapper.starSet.get(entity);
        }
        return null;
    }

    /**
     * Gets the first dataset ancestor of this entity.
     *
     * @param entity The entity.
     *
     * @return The catalog info.
     */
    public static CatalogInfo getDatasetAncestor(Entity entity) {
        return null;
    }

}
