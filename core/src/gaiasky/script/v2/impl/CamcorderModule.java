/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2.impl;

import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.script.v2.api.CamcorderAPI;
import gaiasky.util.Constants;
import gaiasky.util.Settings;

import java.nio.file.Path;
import java.util.Objects;

/**
 * The camcorder module contains methods and calls related to the camera path subsystem and
 * the camcorder, which enables capturing and playing back camera path files.
 */
public class CamcorderModule extends APIModule implements CamcorderAPI {
    /**
     * Create a new module with the given attributes.
     *
     * @param api  Reference to the API class.
     * @param name Name of the module.
     */
    public CamcorderModule(EventManager em, APIv2 api, String name) {
        super(em, api, name);
    }

    @Override
    public void set_fps(double fps) {
        if (api.validator.checkNum(fps, Constants.MIN_FPS, Constants.MAX_FPS, "targetFps")) {
            em.post(Event.CAMRECORDER_FPS_CMD, this, fps);
        }
    }

    @Override
    public double get_fps() {
        return Settings.settings.camrecorder.targetFps;
    }

    @Override
    public void start() {
        em.post(Event.RECORD_CAMERA_CMD, this, true, null);
    }

    @Override
    public void start(String path) {
        em.post(Event.RECORD_CAMERA_CMD, this, true, Path.of(path).getFileName().toString());
    }

    @Override
    public void stop() {
        em.post(Event.RECORD_CAMERA_CMD, this, false, null, false);
    }

    @Override
    public void play(String path, boolean sync) {
        em.post(Event.PLAY_CAMERA_CMD, this, true, path);

        // Wait if needed
        if (sync) {
            Object monitor = new Object();
            IObserver watcher = (event, source, data) -> {
                if (Objects.requireNonNull(event) == Event.CAMERA_PLAY_INFO) {
                    Boolean status = (Boolean) data[0];
                    if (!status) {
                        synchronized (monitor) {
                            monitor.notify();
                        }
                    }
                }
            };
            em.subscribe(watcher, Event.CAMERA_PLAY_INFO);
            // Wait for camera to finish
            synchronized (monitor) {
                try {
                    monitor.wait();
                } catch (InterruptedException e) {
                    logger.error(e, "Error waiting for camera file to finish");
                }
            }
        }
    }

    @Override
    public void play(String path) {
        play(path, false);
    }

}
