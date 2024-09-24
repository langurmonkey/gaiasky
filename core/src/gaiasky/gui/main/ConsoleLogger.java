/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.main;

import com.badlogic.gdx.Gdx;
import gaiasky.data.util.PointCloudData;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.gui.iface.NotificationsInterface;
import gaiasky.scene.camera.CameraManager.CameraMode;
import gaiasky.scene.view.FocusView;
import gaiasky.util.Logger;
import gaiasky.util.Logger.LoggerLevel;
import gaiasky.util.Settings;
import gaiasky.util.i18n.I18n;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class ConsoleLogger implements IObserver {
    private static final long DEFAULT_TIMEOUT = 5000;
    private static final String TAG_SEPARATOR = " - ";
    DateTimeFormatter df;
    long msTimeout;
    boolean useHistorical;

    public ConsoleLogger() {
        this(true);
    }

    /**
     * Initializes the notifications interface.
     *
     * @param useHistorical Keep logs.
     */
    private ConsoleLogger(boolean useHistorical) {
        this.msTimeout = DEFAULT_TIMEOUT;
        this.useHistorical = useHistorical;

        try {
            this.df = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss").withLocale(I18n.locale).withZone(ZoneOffset.UTC);
        } catch (Exception e) {
            this.df = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.MEDIUM).withLocale(I18n.locale).withZone(ZoneOffset.UTC);
        }
        subscribe();
    }

    public void setUseHistorical(boolean useHistorical) {
        this.useHistorical = useHistorical;
    }

    public void subscribe() {
        EventManager.instance.subscribe(this, Event.POST_NOTIFICATION, Event.FOCUS_CHANGED, Event.TIME_STATE_CMD, Event.TOGGLE_VISIBILITY_CMD, Event.CAMERA_MODE_CMD, Event.TIME_WARP_CHANGED_INFO, Event.FOCUS_LOCK_CMD, Event.JAVA_EXCEPTION, Event.ORBIT_DATA_LOADED, Event.SCREENSHOT_INFO, Event.STEREOSCOPIC_CMD, Event.DISPLAY_GUI_CMD, Event.FRAME_OUTPUT_CMD, Event.STEREO_PROFILE_CMD, Event.OCTREE_PARTICLE_FADE_CMD);
    }

    public void unsubscribe() {
        EventManager.instance.removeAllSubscriptions(this);
    }

    private void addMessage(String msg) {
        Instant date = Instant.now();
        log(df.format(date), msg, LoggerLevel.INFO);
        if (useHistorical) {
            NotificationsInterface.historical.add(new MessageBean(date, msg));
        }
    }

    /**
     * Prepares the log tag using the date and the level
     *
     * @param date  The date
     * @param level The logging level
     * @return The tag
     */
    private String tag(Instant date, LoggerLevel level) {
        String lvl = level.ordinal() != LoggerLevel.INFO.ordinal() ? " " + level : "";
        return df.format(date) + lvl;
    }

    private void addMessage(String msg, LoggerLevel level) {
        Instant date = Instant.now();
        log(tag(date, level), msg, level);
        if (useHistorical) {
            NotificationsInterface.historical.add(new MessageBean(date, msg));
        }
    }

    private void log(String tag, String msg, LoggerLevel level) {
        boolean debug = level.equals(LoggerLevel.DEBUG);
        boolean err = level.equals(LoggerLevel.ERROR);
        if (Gdx.app != null) {
            if (debug) {
                Gdx.app.debug(tag, msg);
            } else if (err) {
                Gdx.app.error(tag, msg);
            } else {
                Gdx.app.log(tag, msg);
            }
        } else {
            if (Logger.level.ordinal() >= level.ordinal()) {
                if (level.equals(LoggerLevel.ERROR))
                    System.err.println("[" + tag + "] " + msg);
                else
                    System.out.println("[" + tag + "] " + msg);
            }
        }
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        switch (event) {
            case POST_NOTIFICATION -> {
                LoggerLevel level = (LoggerLevel) data[0];
                Object[] dat = (Object[]) data[1];
                StringBuilder message = new StringBuilder();
                for (int i = 0; i < dat.length; i++) {
                    if (i != dat.length - 1 || !(dat[i] instanceof Boolean)) {
                        message.append(dat[i].toString());
                        if (i < dat.length - 1 && !(i == dat.length - 2 && dat[data.length - 1] instanceof Boolean)) {
                            message.append(TAG_SEPARATOR);
                        }
                    }
                }
                addMessage(message.toString(), level);
            }
            case FOCUS_CHANGED -> {
                if (data[0] != null) {
                    if (data[0] instanceof String) {
                        addMessage(I18n.msg("notif.camerafocus", data[0]));
                    } else {
                        var focus = (FocusView) data[0];
                        addMessage(I18n.msg("notif.camerafocus", focus.getName()));
                    }
                }
            }
            case TIME_STATE_CMD -> {
                Boolean bool = (Boolean) data[0];
                if (bool == null) {
                    addMessage(I18n.msg("notif.toggle", I18n.msg("gui.time")));
                } else {
                    addMessage(I18n.msg("notif.simulation." + (bool ? "resume" : "pause")));
                }
            }
            case TOGGLE_VISIBILITY_CMD -> {
                if (data.length == 2)
                    addMessage(I18n.msg("notif.visibility." + (((Boolean) data[1]) ? "on" : "off"), I18n.msg((String) data[0])));
                else
                    addMessage(I18n.msg("notif.visibility.toggle", I18n.msg((String) data[0])));
            }
            case OCTREE_PARTICLE_FADE_CMD -> {
                var key = (Boolean) data[0] ? "notif.activated" : "notif.deactivated";
                addMessage(I18n.msg(key, I18n.msg("element.octreeparticlefade")));
            }
            case FOCUS_LOCK_CMD -> {
                var key = (Boolean) data[0] ? "notif.activated" : "notif.deactivated";
                addMessage(I18n.msg(key, I18n.msg("gui.camera.lock")));
            }
            case ORIENTATION_LOCK_CMD -> {
                var key = (Boolean) data[0] ? "notif.activated" : "notif.deactivated";
                addMessage(I18n.msg(key, I18n.msg("gui.camera.lock.orientation")));
            }
            case CAMERA_MODE_CMD -> {
                CameraMode cm = (CameraMode) data[0];
                if (cm != CameraMode.FOCUS_MODE)
                    addMessage(I18n.msg("notif.cameramode.change", data[0]));
            }
            case TIME_WARP_CHANGED_INFO -> addMessage(I18n.msg("notif.timepace.change", data[0]));
            case JAVA_EXCEPTION -> {
                Throwable t = (Throwable) data[0];
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                t.printStackTrace(pw);
                String stackTrace = sw.toString();
                if (data.length == 1) {
                    if (I18n.messages != null)
                        addMessage(I18n.msg("notif.error", stackTrace));
                    else
                        addMessage("Error: " + stackTrace);
                } else {
                    if (I18n.messages != null)
                        addMessage(I18n.msg("notif.error", data[1] + TAG_SEPARATOR + stackTrace));
                    else
                        addMessage("Error: " + data[1] + TAG_SEPARATOR + stackTrace);
                }
            }
            case ORBIT_DATA_LOADED ->
                    addMessage(I18n.msg("notif.orbitdata.loaded", data[1], ((PointCloudData) data[0]).getNumPoints()), LoggerLevel.DEBUG);
            case SCREENSHOT_INFO -> addMessage(I18n.msg("notif.screenshot", data[0]));
            case STEREOSCOPIC_CMD -> {
                if (!Settings.settings.runtime.openXr)
                    addMessage(I18n.msg("notif.toggle", I18n.msg("notif.stereoscopic")));
            }
            case DISPLAY_GUI_CMD -> {
                boolean displayGui = (Boolean) data[0];
                addMessage(I18n.msg("notif." + (!displayGui ? "activated" : "deactivated"), data[1]));
            }
            case STEREO_PROFILE_CMD ->
                    addMessage(I18n.msg("notif.stereoscopic.profile", Settings.StereoProfile.values()[(Integer) data[0]].toString()));
            case FRAME_OUTPUT_CMD -> {
                boolean activated = (Boolean) data[0];
                if (activated) {
                    addMessage(I18n.msg("notif.activated", I18n.msg("element.frameoutput")));
                } else {
                    addMessage(I18n.msg("notif.deactivated", I18n.msg("element.frameoutput")));
                }
            }
            default -> {
            }
        }
    }

    public void dispose() {
        unsubscribe();
    }

}
