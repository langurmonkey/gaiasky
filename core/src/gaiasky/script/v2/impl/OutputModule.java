/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2.impl;

import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.script.v2.api.OutputAPI;
import gaiasky.util.Constants;
import gaiasky.util.Settings;
import gaiasky.util.screenshot.ImageRenderer;

/**
 * The output module contains methods and calls that relate to the frame output system, the
 * screenshot system, and other types of output systems.
 * <p>
 * Screenshots and frames are saved to a pre-defined location. You can get the default locations
 * with {@link BaseModule#get_default_frame_output_dir()} and {@link BaseModule#get_default_screenshots_dir()}.
 */
public class OutputModule extends APIModule implements OutputAPI {
    /**
     * Create a new module with the given attributes.
     *
     * @param api  Reference to the API class.
     * @param name Name of the module.
     */
    public OutputModule(EventManager em, APIv2 api, String name) {
        super(em, api, name);
    }

    @Override
    public void configure_screenshots(int w, int h, String path, String prefix) {
        if (api.validator.checkNum(w, 1, Integer.MAX_VALUE, "width")
                && api.validator.checkNum(h, 1, Integer.MAX_VALUE, "height")
                && api.validator.checkString(path, "directory")
                && api.validator.checkDirectoryExists(path, "directory")
                && api.validator.checkString(prefix, "namePrefix")) {
            em.post(Event.SCREENSHOT_CMD, this, w, h, path);
        }
    }

    @Override
    public String get_current_screenshots_dir() {
        return Settings.settings.screenshot.location;
    }

    @Override
    public void screenshot_mode(String mode) {
        // Hack to keep compatibility with old scripts
        if (mode != null && mode.equalsIgnoreCase("redraw")) {
            mode = "ADVANCED";
        }
        if (api.validator.checkStringEnum(mode, Settings.ScreenshotMode.class, "screenshotMode")) {
            em.post(Event.SCREENSHOT_MODE_CMD, this, mode);
            api.base.post_runnable(() -> em.post(Event.SCREENSHOT_SIZE_UPDATE,
                                                 this,
                                                 Settings.settings.screenshot.resolution[0],
                                                 Settings.settings.screenshot.resolution[1]));
        }
    }

    @Override
    public void screenshot() {
        Settings.ScreenshotSettings ss = Settings.settings.screenshot;
        em.post(Event.SCREENSHOT_CMD, this, ss.resolution[0], ss.resolution[1], ss.location);
    }


    @Override
    public void configure_frame_output(int w, int h, int fps, String path, String prefix) {
        configure_frame_output(w, h, (double) fps, path, prefix);
    }

    @Override
    public String get_current_frame_output_dir() {
        return Settings.settings.frame.location;
    }

    @Override
    public void configure_frame_output(int w, int h, double fps, String path, String prefix) {
        if (api.validator.checkNum(w, 1, Integer.MAX_VALUE, "width")
                && api.validator.checkNum(h, 1, Integer.MAX_VALUE, "height")
                && api.validator.checkNum(fps, Constants.MIN_FPS, Constants.MAX_FPS, "FPS")
                && api.validator.checkString(path, "directory")
                && api.validator.checkDirectoryExists(path, "directory")
                && api.validator.checkString(prefix, "namePrefix")) {
            em.post(Event.FRAME_OUTPUT_MODE_CMD, this, Settings.ScreenshotMode.ADVANCED);
            em.post(Event.CONFIG_FRAME_OUTPUT_CMD, this, w, h, fps, path, prefix);
        }
    }

    @Override
    public void frame_output_mode(String mode) {
        // Hack to keep compatibility with old scripts
        if (mode != null && mode.equalsIgnoreCase("redraw")) {
            mode = "ADVANCED";
        }
        if (api.validator.checkStringEnum(mode, Settings.ScreenshotMode.class, "screenshotMode"))
            em.post(Event.FRAME_OUTPUT_MODE_CMD, this, mode);
    }

    @Override
    public void frame_output(boolean active) {
        em.post(Event.FRAME_OUTPUT_CMD, this, active);
    }

    @Override
    public boolean is_frame_output_active() {
        return Settings.settings.frame.active;
    }

    @Override
    public double get_frame_output_fps() {
        return Settings.settings.frame.targetFps;
    }

    @Override
    public void reset_frame_output_sequence_number() {
        ImageRenderer.resetSequenceNumber();
    }

}
