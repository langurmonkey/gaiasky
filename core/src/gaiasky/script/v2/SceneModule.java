/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2;

import com.badlogic.ashley.core.Entity;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.scene.Scene;

import java.util.Objects;

/**
 * The scene module contains methods and calls to manipulate and query objects in the main
 * scene of Gaia Sky.
 */
public class SceneModule extends APIModule implements IObserver {

    /** Reference to the main {@link Scene} object in Gaia Sky. **/
    private Scene scene;

    /**
     * Create a new module with the given attributes.
     *
     * @param api  Reference to the API class.
     * @param name Name of the module.
     */
    public SceneModule(EventManager em, APIv2 api, String name) {
        super(em, api, name);

        // Subscribe to events.
        em.subscribe(this, Event.SCENE_LOADED);
    }

    /**
     * Gets the reference to an entity given its name.
     *
     * @param name Entity name.
     **/
    Entity getEntity(String name) {
        return getEntity(name, 0);
    }

    /**
     * Gets the reference to an entity given its name and a timeout in seconds.
     * If the entity is not in the scene after the timeout has passed, ths method
     * returns null.
     *
     * @param name           Entity name.
     * @param timeOutSeconds Timeout time, in seconds.
     **/
    Entity getEntity(String name, double timeOutSeconds) {
        Entity obj = scene.getEntity(name);
        if (obj == null) {
            if (name.matches("[0-9]+")) {
                // Check with 'HIP '
                obj = scene.getEntity("hip " + name);
            } else if (name.matches("hip [0-9]+")) {
                obj = scene.getEntity(name.substring(4));
            }
        }

        // If negative, no limit in waiting
        if (timeOutSeconds < 0) timeOutSeconds = Double.MAX_VALUE;

        double startMs = System.currentTimeMillis();
        double elapsedSeconds = 0;
        while (obj == null && elapsedSeconds < timeOutSeconds) {
            api.base.sleep_frames(1);
            obj = scene.getEntity(name);
            elapsedSeconds = (System.currentTimeMillis() - startMs) / 1000d;
        }
        return obj;
    }

    /**
     * Gets a focus object from the scene given its name.
     *
     * @param name The name of the focus object.
     *
     * @return The reference to the object if it exists, null otherwise.
     */
    Entity getFocus(String name) {
        return scene.findFocus(name);
    }

    /**
     * Alias to {@link #getFocus(String)}.
     */
    Entity getFocusEntity(String name) {
        return scene.findFocus(name);
    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        if (Objects.requireNonNull(event) == Event.SCENE_LOADED) {
            this.scene = (Scene) data[0];
        }
    }
}
