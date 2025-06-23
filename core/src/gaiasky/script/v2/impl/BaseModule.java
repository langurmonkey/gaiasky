/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2.impl;

import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.script.v2.api.BaseAPI;
import gaiasky.util.camera.rec.Camcorder;
import net.jafama.FastMath;

/**
 * The base module contains methods and calls that are of global nature.
 */
public class BaseModule extends APIModule implements BaseAPI {

    /**
     * Create a new module with the given attributes.
     *
     * @param em   Reference to the event manager.
     * @param api  Reference to the API class.
     * @param name Name of the module.
     */
    public BaseModule(EventManager em, APIv2 api, String name) {
        super(em, api, name);
    }

    @Override
    public void sleep(float seconds) {
        if (api.validator.checkNum(seconds, 0f, Float.MAX_VALUE, "seconds")) {
            if (seconds == 0f) return;

            if (api.output.is_frame_output_active()) {
                sleep_frames(Math.max(1, FastMath.round(api.output.get_frame_output_fps() * seconds)));
            } else if (Camcorder.instance.isRecording()) {
                sleep_frames(Math.max(1, FastMath.round(api.camcorder.get_camcorder_fps() * seconds)));
            } else {
                try {
                    Thread.sleep(Math.round(seconds * 1000f));
                } catch (InterruptedException e) {
                    logger.error(e);
                }
            }
        }
    }


    /**
     * Alias to {@link #sleep(float)}, but using an int parameter type instead of a float.
     */
    public void sleep(int seconds) {
        sleep((float) seconds);
    }

    @Override
    public void sleep_frames(long frames) {
        long frameCount = 0;
        while (frameCount < frames) {
            try {
                synchronized (GaiaSky.instance.frameMonitor) {
                    GaiaSky.instance.frameMonitor.wait();
                }
                frameCount++;
            } catch (InterruptedException e) {
                logger.error("Error while waiting on frameMonitor", e);
            }
        }
    }

    @Override
    public void post_runnable(Runnable runnable) {
        GaiaSky.postRunnable(runnable);
    }


    @Override
    public void park_scene_runnable(String id, Runnable runnable) {
        if (api.validator.checkString(id, "id")) {
            em.post(Event.PARK_RUNNABLE, this, id, runnable);
        }
    }

    /**
     * Alias to {@link #park_scene_runnable(String, Runnable)}.
     */
    public void park_runnable(String id, Runnable runnable) {
        park_scene_runnable(id, runnable);
    }

    @Override
    public void park_camera_runnable(String id, Runnable runnable) {
        if (api.validator.checkString(id, "id")) {
            em.post(Event.PARK_CAMERA_RUNNABLE, this, id, runnable);
        }
    }

    @Override
    public void remove_runnable(String id) {
        if (api.validator.checkString(id, "id")) em.post(Event.UNPARK_RUNNABLE, this, id);
    }

}
