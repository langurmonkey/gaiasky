/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2.impl;

import com.badlogic.gdx.Gdx;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.script.v2.api.BaseAPI;
import gaiasky.util.Constants;
import gaiasky.util.Settings;
import gaiasky.util.SettingsManager;
import gaiasky.util.SysUtils;
import gaiasky.util.camera.rec.Camcorder;
import net.jafama.FastMath;

import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * The base module contains methods and calls that are of global nature.
 */
public class BaseModule extends APIModule implements BaseAPI {

    /** Settings stack. **/
    private final Deque<Settings> settingsStack;

    /**
     * Create a new module with the given attributes.
     *
     * @param em   Reference to the event manager.
     * @param api  Reference to the API class.
     * @param name Name of the module.
     */
    public BaseModule(EventManager em, APIv2 api, String name) {
        super(em, api, name);
        this.settingsStack = new ConcurrentLinkedDeque<>();
    }

    @Override
    public String get_version() {
        return Settings.settings.version.version + '\n' + Settings.settings.version.build + '\n' + Settings.settings.version.system + '\n' + Settings.settings.version.builder + '\n' + Settings.settings.version.buildTime;
    }

    @Override
    public String get_version_number() {
        return Settings.settings.version.version;
    }

    @Override
    public String get_build_string() {
        return Settings.settings.version.build;
    }

    @Override
    public String get_assets_dir() {
        return Settings.ASSETS_LOC;
    }

    @Override
    public String get_default_frame_output_dir() {
        return SysUtils.getDefaultFramesDir().toAbsolutePath().toString();
    }

    @Override
    public String get_default_screenshots_dir() {
        return SysUtils.getDefaultScreenshotsDir().toAbsolutePath().toString();
    }

    @Override
    public String get_camcorder_dir() {
        return SysUtils.getDefaultCameraDir().toAbsolutePath().toString();
    }

    @Override
    public String get_mappings_dir() {
        return SysUtils.getDefaultMappingsDir().toAbsolutePath().toString();
    }

    @Override
    public String get_data_dir() {
        return SysUtils.getDataDir().toAbsolutePath().toString();
    }

    @Override
    public String get_config_dir() {
        return SysUtils.getConfigDir().toAbsolutePath().toString();
    }

    @Override
    public String get_default_datasets_dir() {
        return SysUtils.getDefaultDatasetsDir().toAbsolutePath().toString();
    }

    @Override
    public void sleep(float seconds) {
        if (api.validator.checkNum(seconds, 0f, Float.MAX_VALUE, "seconds")) {
            if (seconds == 0f) return;

            if (api.output.is_frame_output_active()) {
                sleep_frames(Math.max(1, FastMath.round(api.output.get_frame_output_fps() * seconds)));
            } else if (Camcorder.instance.isRecording()) {
                sleep_frames(Math.max(1, FastMath.round(api.camcorder.get_fps() * seconds)));
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

    @Override
    public void settings_backup() {
        settingsStack.push(Settings.settings.clone());
    }

    @Override
    public boolean settings_restore() {
        if (settingsStack.isEmpty()) {
            return false;
        }

        if (SettingsManager.setSettingsInstance(settingsStack.pop())) {
            // Apply settings.
            Settings.settings.apply();
            // Reload UI.
            // postRunnable(()->{
            // em.post(Event.UI_RELOAD_CMD, this, GaiaSky.instance.getGlobalResources());
            // });
            return true;
        }
        return false;
    }

    @Override
    public void settings_clear_stack() {
        settingsStack.clear();
    }

    @Override
    public double m_to_internal() {
        return Constants.M_TO_U;
    }

    @Override
    public double internal_to_m() {
        return Constants.U_TO_M;
    }

    @Override
    public double internal_to_m(double internalUnits) {
        return internalUnits * Constants.U_TO_M;
    }

    @Override
    public double internal_to_km(double internalUnits) {
        return internalUnits * Constants.U_TO_KM;
    }

    @Override
    public double[] internal_to_km(double[] internalUnits) {
        double[] result = new double[internalUnits.length];
        for (int i = 0; i < internalUnits.length; i++) {
            result[i] = internal_to_km(internalUnits[i]);
        }
        return result;
    }

    @Override
    public double internal_to_pc(double internalUnits) {
        return internalUnits * Constants.U_TO_PC;
    }

    @Override
    public double[] internal_to_pc(double[] internalUnits) {
        double[] result = new double[internalUnits.length];
        for (int i = 0; i < internalUnits.length; i++) {
            result[i] = internal_to_pc(internalUnits[i]);
        }
        return result;
    }

    public double[] internal_to_km(List<?> internalUnits) {
        double[] result = new double[internalUnits.size()];
        for (int i = 0; i < internalUnits.size(); i++) {
            result[i] = internal_to_km((double) internalUnits.get(i));
        }
        return result;
    }

    @Override
    public double m_to_internal(double metres) {
        return metres * Constants.M_TO_U;
    }

    @Override
    public double km_to_internal(double kilometres) {
        return kilometres * Constants.KM_TO_U;
    }

    @Override
    public double pc_to_internal(double parsecs) {
        return parsecs * Constants.PC_TO_U;
    }

    public double kilometersToInternalUnits(double kilometres) {
        return kilometres * Constants.KM_TO_U;
    }

    @Override
    public void print(String message) {
        logger.info(message);
    }

    @Override
    public void log(String message) {
        logger.info(message);
    }

    @Override
    public void error(String message) {
        logger.error(message);
    }

    @Override
    public void quit() {
        Gdx.app.exit();
    }

}
