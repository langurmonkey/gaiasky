/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.Graphics.Monitor;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.i18n.I18n;

public class ScreenModeCmd implements IObserver {
    private static final Log logger = Logger.getLogger(ScreenModeCmd.class);

    public static ScreenModeCmd instance;

    private ScreenModeCmd() {
        EventManager.instance.subscribe(this, Event.SCREEN_MODE_CMD);
    }

    public static void initialize() {
        ScreenModeCmd.instance = new ScreenModeCmd();
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        if (event == Event.SCREEN_MODE_CMD) {
            boolean toFullScreen = Settings.settings.graphics.fullScreen.active;
            if (toFullScreen) {
                Monitor m = Gdx.graphics.getMonitor();
                // Available modes for this monitor
                DisplayMode[] modes = Gdx.graphics.getDisplayModes(m);
                // Find best mode
                DisplayMode myMode = null;
                for (DisplayMode mode : modes) {
                    if (mode.height == Settings.settings.graphics.fullScreen.resolution[1]
                            && mode.width == Settings.settings.graphics.fullScreen.resolution[0]
                            && mode.refreshRate == Settings.settings.graphics.fullScreen.refreshRate
                            && mode.bitsPerPixel == Settings.settings.graphics.fullScreen.bitDepth) {
                        myMode = mode;
                        break;
                    }
                }
                // If no mode found, get default
                if (myMode == null) {
                    myMode = Gdx.graphics.getDisplayMode(m);
                    Settings.settings.graphics.fullScreen.resolution[0] = myMode.width;
                    Settings.settings.graphics.fullScreen.resolution[1] = myMode.height;
                    Settings.settings.graphics.fullScreen.refreshRate = myMode.refreshRate;
                    Settings.settings.graphics.fullScreen.bitDepth = myMode.bitsPerPixel;
                }

                // set the window to full screen mode
                boolean good = Gdx.graphics.setFullscreenMode(myMode);
                if (!good) {
                    logger.error(I18n.msg("notif.error", I18n.msg("gui.fullscreen")));
                }

            } else {
                int width = Settings.settings.graphics.resolution[0];
                int height = Settings.settings.graphics.resolution[1];

                boolean good = Gdx.graphics.setWindowedMode(width, height);
                if (!good) {
                    logger.error(I18n.msg("notif.error", I18n.msg("gui.windowed")));
                }

            }
            if (!GaiaSky.instance.isHeadless())
                Gdx.graphics.setVSync(Settings.settings.graphics.vsync);
        }
    }
}
