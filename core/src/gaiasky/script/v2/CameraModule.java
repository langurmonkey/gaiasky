/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2;

import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.scene.Scene;
import gaiasky.scene.view.FocusView;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The camera module contains methods and calls that manipulate and query the camera system.
 */
public class CameraModule extends APIModule implements IObserver {

    /** Scene reference. **/
    private Scene scene;
    /** Focus view. **/
    private final FocusView focusView;
    /** Currently active stop instances. **/
    private final Set<AtomicBoolean> stops;

    /**
     * Create a new module with the given attributes.
     *
     * @param em   Reference to the event manager.
     * @param api  Reference to the API class.
     * @param name Name of the module.
     */
    public CameraModule(EventManager em, APIv2 api, String name) {
        super(em, api, name);
        this.focusView = new FocusView();
        this.stops = new HashSet<>();

        em.subscribe(this, Event.SCENE_LOADED);
    }

    @Override
    public void dispose() {
        // Stop all ongoing processes.
        for (var stop : stops) {
            if (stop != null) stop.set(true);
        }

        super.dispose();
    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        if (Objects.requireNonNull(event) == Event.SCENE_LOADED) {
            this.scene = (Scene) data[0];
            this.focusView.setScene(this.scene);
        }
    }
}
