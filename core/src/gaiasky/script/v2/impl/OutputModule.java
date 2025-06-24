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

/**
 * The output module contains methods and calls that relate to the frame output system, the
 * screenshot system, and other types of output systems.
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
    public void configure_screenshots(int width, int height, String directory, String namePrefix) {
        if (api.validator.checkNum(width, 1, Integer.MAX_VALUE, "width")
                && api.validator.checkNum(height, 1, Integer.MAX_VALUE, "height")
                && api.validator.checkString(directory, "directory")
                && api.validator.checkDirectoryExists( directory, "directory")
                && api.validator.checkString(namePrefix, "namePrefix")) {
            em.post(Event.SCREENSHOT_CMD, this, width, height, directory);
        }
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
    public void configure_frame_output(int width, int height, int fps, String directory, String namePrefix) {
        configure_frame_output(width, height, (double) fps, directory, namePrefix);
    }

    @Override
    public void configure_frame_output(int width, int height, double fps, String directory, String namePrefix) {
        if (api.validator.checkNum(width, 1, Integer.MAX_VALUE, "width")
                && api.validator.checkNum(height, 1, Integer.MAX_VALUE, "height")
                && api.validator.checkNum(fps, Constants.MIN_FPS, Constants.MAX_FPS, "FPS")
                && api.validator.checkString( directory, "directory")
                && api.validator.checkDirectoryExists(directory, "directory")
                && api.validator.checkString(namePrefix, "namePrefix")) {
            em.post(Event.FRAME_OUTPUT_MODE_CMD, this, Settings.ScreenshotMode.ADVANCED);
            em.post(Event.CONFIG_FRAME_OUTPUT_CMD, this, width, height, fps, directory, namePrefix);
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

}
