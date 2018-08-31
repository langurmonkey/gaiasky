package gaia.cu9.ari.gaiaorbit.desktop.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.Graphics.Monitor;

import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;

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
    public void notify(Events event, Object... data) {
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
                    logger.error(I18n.bundle.format("notif.error", I18n.bundle.get("gui.fullscreen")));
                }

            } else {
                int width = GlobalConf.screen.SCREEN_WIDTH;
                int height = GlobalConf.screen.SCREEN_HEIGHT;

                boolean good = Gdx.graphics.setWindowedMode(width, height);
                if (!good) {
                    logger.error(I18n.bundle.format("notif.error", I18n.bundle.get("gui.windowed")));
                }

            }
            Gdx.graphics.setVSync(GlobalConf.screen.VSYNC);
            break;
        default:
            break;

        }
    }
}
