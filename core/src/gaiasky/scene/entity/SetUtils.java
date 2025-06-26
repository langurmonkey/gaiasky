/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.entity;

import com.badlogic.ashley.core.Entity;
import gaiasky.GaiaSky;
import gaiasky.data.group.DatasetOptions;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.scene.Archetype;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.scene.camera.CameraManager;
import gaiasky.scene.camera.CameraManager.CameraMode;
import gaiasky.scene.component.ParticleSet;
import gaiasky.scene.component.StarSet;
import gaiasky.scene.system.initialize.BaseInitializer;
import gaiasky.scene.system.initialize.ParticleSetInitializer;
import gaiasky.util.Constants;
import uk.ac.starlink.table.ColumnInfo;

import java.util.List;

/**
 * Generic operations that act on star and particle sets and are not tied to a singular instance.
 */
public class SetUtils {

    /**
     * Creates a star set entity with some sane parameters, given the name and the data.
     *
     * @param scene              The scene object.
     * @param name               The name of the star set. Any occurrence of <code>%%SGID%%</code> in
     *                           the name is replaced with the id of the star set.
     * @param data               The data of the star set.
     * @param baseInitializer    The base initializer.
     * @param starSetInitializer The initializer to use for the star set initialization.
     * @param fullInit           Whether to run the <code>setUpEntity()</code> to fully initialize the star set.
     *
     * @return The new star set entity.
     */
    public static Entity createStarSet(Scene scene,
                                       String name,
                                       List<IParticleRecord> data,
                                       BaseInitializer baseInitializer,
                                       ParticleSetInitializer starSetInitializer,
                                       boolean fullInit) {
        Archetype archetype = scene.archetypes().get("StarGroup");
        Entity entity = archetype.createEntity();

        var base = Mapper.base.get(entity);
        base.id = ParticleSet.getNextSequence();
        base.setName(name.replace("%%SGID%%", Long.toString(base.id)));
        base.ct = new ComponentTypes(ComponentType.Stars);

        var graph = Mapper.graph.get(entity);
        graph.setParent(Scene.ROOT_NAME);

        var body = Mapper.body.get(entity);
        body.setLabelColor(new double[]{1.0, 1.0, 1.0, 1.0});
        body.setColor(new double[]{1.0, 1.0, 1.0, 0.25});
        body.setSize(6.0 * Constants.DISTANCE_SCALE_FACTOR);

        var label = Mapper.label.get(entity);
        label.setLabelPosition(new double[]{0.0, -5.0e7, -4e8});

        var set = Mapper.starSet.get(entity);
        set.setData(data, true);

        // Initialize.
        baseInitializer.initializeEntity(entity);
        starSetInitializer.initializeEntity(entity);

        // Set up.
        if (fullInit) {
            baseInitializer.setUpEntity(entity);
            starSetInitializer.setUpEntity(entity);
        }
        return entity;
    }

    /**
     * Creates a star set entity given some parameters.
     *
     * @param scene               The scene object.
     * @param name                The name of the star set. Any occurrence of <code>%%SGID%%</code> in
     *                            the name is replaced with the id of the star set.
     * @param file                The data file.
     * @param data                The data of the star set.
     * @param columnInfoList      The base initializer.
     * @param datasetOptions      Dataset options to use.
     * @param addToCatalogManager Whether to add the set to the catalog manager.
     *
     * @return The new star set entity.
     */
    public static Entity createStarSet(Scene scene,
                                       String name,
                                       String file,
                                       List<IParticleRecord> data,
                                       List<ColumnInfo> columnInfoList,
                                       DatasetOptions datasetOptions,
                                       boolean addToCatalogManager) {
        double[] fadeIn = datasetOptions == null || datasetOptions.fadeIn == null ? null : datasetOptions.fadeIn;
        double[] fadeOut = datasetOptions == null || datasetOptions.fadeOut == null ? null : datasetOptions.fadeOut;
        double[] labelColor = datasetOptions == null || datasetOptions.labelColor == null ? new double[]{1.0, 1.0, 1.0, 1.0} : datasetOptions.labelColor;
        boolean renderSetLabel = datasetOptions == null || datasetOptions.renderSetLabel;

        var archetype = scene.archetypes().get("StarGroup");
        Entity entity = archetype.createEntity();

        var base = Mapper.base.get(entity);
        base.id = ParticleSet.getNextSequence();
        base.setName(name.replace("%%SGID%%", Long.toString(base.id)));
        base.setComponentType(ComponentType.Stars);

        var body = Mapper.body.get(entity);
        body.setColor(new double[]{1.0, 1.0, 1.0, 0.25});
        body.setLabelColor(labelColor);
        body.setSize(6.0);

        var graph = Mapper.graph.get(entity);
        graph.setParent(Scene.ROOT_NAME);

        var fade = Mapper.fade.get(entity);
        fade.setFadeIn(fadeIn);
        fade.setFadeOut(fadeOut);

        var label = Mapper.label.get(entity);
        label.setLabelPosition(new double[]{0.0, -5.0e7, -4e8});

        var set = Mapper.starSet.get(entity);
        set.setGenerateIndex(true);
        set.setData(data);
        set.setDatafile(file);
        set.setColumnInfoList(columnInfoList);
        set.setRenderSetLabel(renderSetLabel);

        scene.initializeEntity(entity);
        if (addToCatalogManager) {
            scene.setUpEntity(entity);
        }

        return entity;
    }

    /**
     * Creates a particle set entity given some parameters.
     *
     * @param scene               The scene object.
     * @param name                The name of the particle set. Any occurrence of <code>%%PGID%%</code> in
     *                            the name is replaced with the id of the particle set.
     * @param file                The data file.
     * @param data                The data of the particle set.
     * @param columnInfoList      The base initializer.
     * @param datasetOptions      Dataset options to use.
     * @param addToCatalogManager Whether to add the set to the catalog manager.
     *
     * @return The new particle set entity.
     */
    public static Entity createParticleSet(Scene scene,
                                           String name,
                                           String file,
                                           List<IParticleRecord> data,
                                           List<ColumnInfo> columnInfoList,
                                           DatasetOptions datasetOptions,
                                           boolean addToCatalogManager) {
        double[] fadeIn = datasetOptions == null || datasetOptions.fadeIn == null ? null : datasetOptions.fadeIn;
        double[] fadeOut = datasetOptions == null || datasetOptions.fadeOut == null ? null : datasetOptions.fadeOut;
        double[] particleColor = datasetOptions == null || datasetOptions.particleColor == null ? new double[]{1.0, 1.0, 1.0, 1.0} : datasetOptions.particleColor;
        double colorNoise = datasetOptions == null ? 0 : datasetOptions.particleColorNoise;
        double[] labelColor = datasetOptions == null || datasetOptions.labelColor == null ? new double[]{1.0, 1.0, 1.0, 1.0} : datasetOptions.labelColor;
        double particleSize = datasetOptions == null ? 0 : datasetOptions.particleSize;
        double[] particleSizeLimits = datasetOptions == null || datasetOptions.particleSizeLimits == null ? new double[]{0.00474, 0.2047} : datasetOptions.particleSizeLimits;
        double profileDecay = datasetOptions == null ? 1 : datasetOptions.profileDecay;
        String modelType = datasetOptions == null ? "quad" : datasetOptions.modelType;
        String modelPrimitive = datasetOptions == null ? "GL_TRIANGLES" : datasetOptions.modelPrimitive;
        String ct = datasetOptions == null || datasetOptions.ct == null ? ComponentType.Galaxies.toString() : datasetOptions.ct.toString();
        boolean renderSetLabel = datasetOptions == null || datasetOptions.renderSetLabel;
        int numLabels = datasetOptions == null ? -1 : datasetOptions.numLabels;

        var archetype = scene.archetypes().get("ParticleGroup");
        Entity entity = archetype.createEntity();

        var base = Mapper.base.get(entity);
        base.id = ParticleSet.getNextSequence();
        base.setName(name.replace("%%PGID%%", Long.toString(base.id)));
        base.setCt(ct);

        var hl = Mapper.highlight.get(entity);
        hl.pointscaling = (float) particleSize;

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
        set.setExtended(datasetOptions != null && datasetOptions.type == DatasetOptions.DatasetLoadType.PARTICLES_EXT);
        set.setData(data);
        set.setDatafile(file);
        set.setColumnInfoList(columnInfoList);
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


    public static void dispose(Entity entity, StarSet set) {
        set.disposed = true;
        if (GaiaSky.instance.scene != null) {
            GaiaSky.instance.scene.remove(entity, true);
        }
        // Unsubscribe from all events
        EventManager.instance.removeRadioSubscriptions(entity);
        // Data to be gc'd
        set.pointData = null;
        // Remove focus if needed
        CameraManager cam = GaiaSky.instance.getCameraManager();
        if (cam != null && cam.hasFocus() && cam.isFocus(entity)) {
            set.setFocusIndex(-1);
            EventManager.publish(Event.CAMERA_MODE_CMD, set, CameraMode.FREE_MODE);
        }
    }
}
