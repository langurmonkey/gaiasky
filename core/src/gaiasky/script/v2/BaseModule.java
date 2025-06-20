/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2;

import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.util.Settings;
import gaiasky.util.camera.rec.Camcorder;
import net.jafama.FastMath;

import static gaiasky.util.Logger.Log;
import static gaiasky.util.Logger.getLogger;

/**
 * The base module contains generic methods and functions that do not fit in any of the
 * other modules and carry out basic functionality.
 */
public class BaseModule extends APIModule {
    protected final static Log logger = getLogger(BaseModule.class.getSimpleName());

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

    /**
     * Sleeps for the given number of seconds in the application time (FPS), so
     * if we are capturing frames and the frame rate is set to 30 FPS, the
     * command sleep(1) will put the script to sleep for 30 frames.
     *
     * @param seconds The number of seconds to wait.
     */
    public void sleep(float seconds) {
        if (api.validator.checkNum(seconds, 0f, Float.MAX_VALUE, "seconds")) {
            if (seconds == 0f) return;

            if (is_frame_output_active()) {
                sleep_frames(Math.max(1, FastMath.round(get_frame_output_fps() * seconds)));
            } else if (Camcorder.instance.isRecording()) {
                sleep_frames(Math.max(1, FastMath.round(get_camcorder_fps() * seconds)));
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
     * Check whether the frame output system is currently on (i.e. frames are being saved to disk).
     * TODO move to output module
     *
     * @return True if the render output is active.
     */
    public boolean is_frame_output_active() {
        return Settings.settings.frame.active;
    }

    /**
     * Gets the current frame rate setting of the frame output system.
     * TODO move to output module
     *
     * @return The frame rate setting of the frame output system.
     */
    public double get_frame_output_fps() {
        return Settings.settings.frame.targetFps;
    }

    /**
     * Get the current frame rate setting of the camcorder.
     * TODO move to camcorder module
     *
     * @return The frame rate setting of the camcorder.
     */
    public double get_camcorder_fps() {
        return Settings.settings.camrecorder.targetFps;
    }

    /**
     * Sleeps for the given number of seconds in the application time (FPS), so
     * if we are capturing frames and the frame rate is set to 30 FPS, the
     * command sleep(1) will put the script to sleep for 30 frames.
     *
     * @param seconds The number of seconds to wait.
     */
    public void sleep(int seconds) {
        sleep((float) seconds);
    }

    /**
     * Sleeps for a number of frames. The frame monitor is notified at the beginning
     * of each frame, before the update-render cycle. When frames is 1, this method
     * returns just before the processing of the next frame starts.
     *
     * @param frames The number of frames to wait.
     */
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

    /**
     * Posts a {@link Runnable} to the main loop thread that runs once after the update-scene stage, and
     * before the render stage.
     *
     * @param runnable The runnable to run.
     */
    public void post_runnable(Runnable runnable) {
        GaiaSky.postRunnable(runnable);
    }

    /**
     * See {@link #park_scene_runnable(String, Runnable)}.
     */
    public void park_runnable(String id, Runnable runnable) {
        park_scene_runnable(id, runnable);
    }

    /**
     * <p>
     * Parks an update {@link Runnable} to the main loop thread, and keeps it running every frame
     * until it finishes, or it is removed by {@link #remove_runnable(String)}.
     * This object runs after the update-scene stage and before the render stage,
     * so it is intended for updating scene objects.
     * </p>
     * <p>
     * Be careful with this function, as it probably needs a cleanup before the script is finished. Otherwise,
     * all parked runnables will keep running until Gaia Sky is restarted, so make sure to
     * remove them with {@link #remove_runnable(String)} if needed.
     * </p>
     *
     * @param id       The string id to identify the runnable.
     * @param runnable The scene update runnable to park.
     */
    public void park_scene_runnable(String id, Runnable runnable) {
        if (api.validator.checkString(id, "id")) {
            em.post(Event.PARK_RUNNABLE, this, id, runnable);
        }
    }

    /**
     * <p>
     * Parks a camera update {@link Runnable} to the main loop thread, and keeps it running every frame
     * until it finishes, or it is removed by {@link #remove_runnable(String)}.
     * This object runs after the update-camera stage and before the update-scene, so it is intended for updating the
     * camera only.
     * </p>
     * <p>
     * Be careful with this function, as it probably needs a cleanup before the script is finished. Otherwise,
     * all parked runnables will keep running until Gaia Sky is restarted, so make sure to
     * remove them with {@link #remove_runnable(String)} if needed.
     * </p>
     *
     * @param id       The string id to identify the runnable.
     * @param runnable The camera update runnable to park.
     */
    public void park_camera_runnable(String id, Runnable runnable) {
        if (api.validator.checkString(id, "id")) {
            em.post(Event.PARK_CAMERA_RUNNABLE, this, id, runnable);
        }
    }

    /**
     * Removes the runnable with the given id, if any. Use this method to remove previously parked scene and camera
     * runnables.
     *
     * @param id The id of the runnable to remove.
     */
    public void remove_runnable(String id) {
        if (api.validator.checkString(id, "id")) em.post(Event.UNPARK_RUNNABLE, this, id);
    }

}
