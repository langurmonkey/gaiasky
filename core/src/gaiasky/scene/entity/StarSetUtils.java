/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.entity;

import com.badlogic.ashley.core.Entity;
import gaiasky.GaiaSky;
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

import java.util.List;

public class StarSetUtils {

    /** Reference to the scene. **/
    private final Scene scene;

    /** Constructs a star set utils with the given scene. **/
    public StarSetUtils(Scene scene) {
        this.scene = scene;
    }

    /**
     * Creates a default star set entity with some sane parameters, given the name and the data
     *
     * @param name               The name of the star group. Any occurrence of '%%SGID%%' in name will be replaced with the id of the star group.
     * @param data               The data of the star group.
     * @param baseInitializer    The base initializer.
     * @param starSetInitializer The initializer to use for the star set initialization.
     * @param fullInit           Whether to run the <code>setUpEntity()</code> to fully initialize the star set.
     *
     * @return A new star group with sane parameters
     */
    public Entity getDefaultStarSet(String name, List<IParticleRecord> data, BaseInitializer baseInitializer, ParticleSetInitializer starSetInitializer, boolean fullInit) {
        Archetype archetype = scene.archetypes().get("gaiasky.scenegraph.StarGroup");
        Entity entity = archetype.createEntity();

        var base = Mapper.base.get(entity);
        base.id = ParticleSet.getNextSequence();
        base.setName(name.replace("%%SGID%%", Long.toString(base.id)));
        base.ct = new ComponentTypes(ComponentType.Stars);

        var graph = Mapper.graph.get(entity);
        graph.setParent(Scene.ROOT_NAME);

        var body = Mapper.body.get(entity);
        body.setLabelColor(new double[] { 1.0, 1.0, 1.0, 1.0 });
        body.setColor(new double[] { 1.0, 1.0, 1.0, 0.25 });
        body.setSize(6.0 * Constants.DISTANCE_SCALE_FACTOR);

        var label = Mapper.label.get(entity);
        label.setLabelPosition(new double[] { 0.0, -5.0e7, -4e8 });

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

    public void dispose(Entity entity, StarSet set) {
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
            EventManager.publish(Event.CAMERA_MODE_CMD, this, CameraMode.FREE_MODE);
        }
    }
}
