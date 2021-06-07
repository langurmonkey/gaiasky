/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.desktop.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.Graphics.Monitor;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.util.GlobalConf;
import gaiasky.util.I18n;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;

/**
 * Manages screen mode changes (fullscreen, windowed)
 */
public class ScreenModeCmd implements IObserver {
    private static final Log logger = Logger.getLogger(ScreenModeCmd.class);
    
    public static ScreenModeCmd instance;

    public static void initialize() {
        ScreenModeCmd.instance = new ScreenModeCmd();
    }

    private ScreenModeCmd() {
        EventManager.instance.subscribe(this, Events.SCREEN_MODE_CMD);
    }

    @Override
    public void notify(final Events event, final Object... data) {
        switch (event) {
        case SCREEN_MODE_CMD:
            boolean toFullscreen = GlobalConf.screen.FULLSCREEN;
            if (toFullscreen) {
                // TODO hack
                Monitor m = Gdx.graphics.getPrimaryMonitor();
                // Available modes for this monitor
                DisplayMode[] modes = Gdx.graphics.getDisplayModes(m);
                // Find best mode
                DisplayMode mymode = null;
                for (DisplayMode mode : modes) {
                    if (mode.height == GlobalConf.screen.FULLSCREEN_HEIGHT && mode.width == GlobalConf.screen.FULLSCREEN_WIDTH) {
                        mymode = mode;
                        break;
                    }
                }
                // If no mode found, get default
                if (mymode == null) {
                    mymode = Gdx.graphics.getDisplayMode(m);
                    GlobalConf.screen.FULLSCREEN_WIDTH = mymode.width;
                    GlobalConf.screen.FULLSCREEN_HEIGHT = mymode.height;
                }

                // set the window to fullscreen mode
                boolean good = Gdx.graphics.setFullscreenMode(mymode);
                if (!good) {
                    logger.error(I18n.txt("notif.error", I18n.txt("gui.fullscreen")));
                }

            } else {
                int width = GlobalConf.screen.SCREEN_WIDTH;
                int height = GlobalConf.screen.SCREEN_HEIGHT;

                boolean good = Gdx.graphics.setWindowedMode(width, height);
                if (!good) {
                    logger.error(I18n.txt("notif.error", I18n.txt("gui.windowed")));
                }

            }
            Gdx.graphics.setVSync(GlobalConf.screen.VSYNC);
            break;
        default:
            break;

        }
    }
}
