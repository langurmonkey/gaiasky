/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.screenshot;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.gui.IGui;
import gaiasky.gui.RenderGui;
import gaiasky.render.api.IMainRenderer;
import gaiasky.render.api.IPostProcessor.PostProcessBean;
import gaiasky.render.api.IPostProcessor.RenderType;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.GlobalResources;
import gaiasky.util.Settings;
import gaiasky.util.Settings.ImageFormat;
import gaiasky.util.i18n.I18n;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ScreenshotsManager implements IObserver {

    /** Command to take screenshot **/
    private static class ScreenshotCmd {
        public static final String FILENAME = "screenshot";
        public String folder;
        public int width, height;
        public boolean active = false;

        public ScreenshotCmd() {
            super();
        }

        public void takeScreenshot(int width, int height, String folder) {
            this.folder = folder;
            this.width = width;
            this.height = height;
            this.active = true;
        }

    }

    public IFileImageRenderer frameRenderer, screenshotRenderer;
    private final ScreenshotCmd screenshot;
    private IGui renderGui;

    public ScreenshotsManager(final GlobalResources globalResources) {
        super();
        frameRenderer = new BasicFileImageRenderer();
        screenshotRenderer = new BasicFileImageRenderer();
        screenshot = new ScreenshotCmd();

        // Frame output GUI
        renderGui = new RenderGui(globalResources.getSkin(), (Lwjgl3Graphics) Gdx.graphics, Settings.settings.program.ui.scale);
        renderGui.initialize(null, globalResources.getSpriteBatch());
        renderGui.doneLoading(null);

        EventManager.instance.subscribe(this, Event.RENDER_FRAME, Event.RENDER_SCREENSHOT, Event.RENDER_FRAME_BUFFER, Event.FLUSH_FRAMES, Event.SCREENSHOT_CMD, Event.UPDATE_GUI, Event.DISPOSE);
    }

    public void renderFrame(IMainRenderer mr) {
        final Settings settings = Settings.settings;
        if (settings.frame.active) {
            switch (settings.frame.mode) {
            case SIMPLE -> frameRenderer.saveScreenshot(settings.frame.location, settings.frame.prefix, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true, settings.frame.format, settings.frame.quality);
            case ADVANCED -> {
                // Do not resize post processor
                GaiaSky.instance.resizeImmediate(settings.frame.resolution[0], settings.frame.resolution[1], false, true, false, true);
                renderToImage(mr, mr.getCameraManager(), mr.getT(), mr.getPostProcessor().getPostProcessBean(RenderType.frame), settings.frame.resolution[0], settings.frame.resolution[1], settings.frame.location, settings.frame.prefix, frameRenderer, settings.frame.format, settings.frame.quality);
                GaiaSky.instance.resizeImmediate(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false, true, false, true);
            }
            }
        }
    }

    public void renderScreenshot(IMainRenderer mr) {
        if (screenshot.active) {
            final Settings settings = Settings.settings;
            String file = null;
            String filename = getCurrentTimeStamp() + "_" + ScreenshotCmd.FILENAME;
            switch (settings.screenshot.mode) {
            case SIMPLE -> file = ImageRenderer.renderToImageGl20(screenshot.folder, filename, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), settings.screenshot.format, settings.screenshot.quality);
            case ADVANCED -> {
                // Do not resize post processor
                GaiaSky.instance.resizeImmediate(screenshot.width, screenshot.height, false, true, false, true);
                file = renderToImage(mr, mr.getCameraManager(), mr.getT(), mr.getPostProcessor().getPostProcessBean(RenderType.screenshot), screenshot.width, screenshot.height, screenshot.folder, filename, screenshotRenderer, settings.screenshot.format, settings.screenshot.quality);
                GaiaSky.instance.resizeImmediate(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false, true, false, true);
            }
            }
            if (file != null) {
                screenshot.active = false;
                EventManager.publish(Event.SCREENSHOT_INFO, this, file);
                EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("notif.screenshot", file));
            }

        }
    }

    public void renderCurrentFrameBuffer(String folder, String file, int w, int h) {
        String f = ImageRenderer.renderToImageGl20(folder, file, w, h, Settings.settings.screenshot.format, Settings.settings.screenshot.quality);
        if (f != null) {
            EventManager.publish(Event.SCREENSHOT_INFO, this, f);
            EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("notif.screenshot", file));
        }
    }

    /**
     * Renders the current scene to an image and returns the file name where it
     * has been written to
     *
     * @param mr       The main renderer to use.
     * @param camera   The camera.
     * @param width    The width of the image.
     * @param height   The height of the image.
     * @param folder   The folder to save the image to.
     * @param filename The file name prefix.
     * @param renderer the {@link IFileImageRenderer} to use.
     * @return String with the path to the screenshot image file.
     */
    public String renderToImage(IMainRenderer mr, ICamera camera, double dt, PostProcessBean ppb, int width, int height, String folder, String filename, IFileImageRenderer renderer, ImageFormat format, float quality) {
        FrameBuffer frameBuffer = mr.getFrameBuffer(width, height);
        // TODO That's a dirty trick, we should find a better way (i.e. making buildEnabledEffectsList() method public)
        boolean postprocessing = ppb.pp.buildEnabledEffectsList() > 0;
        if (!postprocessing) {
            // If post-processing is not active, we must start the buffer now.
            // Otherwise, it is used in the render method to write the results
            // of the pp.
            frameBuffer.begin();
        }

        // this is the main render function
        mr.preRenderScene();
        mr.renderSgr(camera, dt, width, height, width, height, frameBuffer, ppb);

        if (postprocessing) {
            // If post-processing is active, we have to begin the buffer now again because
            // the renderSgr() has closed it.
            frameBuffer.begin();
        }
        if (Settings.settings.frame.time) {
            // Timestamp
            renderGui().resize(width, height);
            renderGui().render(width, height);
        }

        String res = renderer.saveScreenshot(folder, filename, width, height, false, format, quality);

        frameBuffer.end();
        return res;
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        switch (event) {
        case RENDER_FRAME:
            IMainRenderer mr = (IMainRenderer) data[0];
            renderFrame(mr);
            break;
        case RENDER_SCREENSHOT:
            mr = (IMainRenderer) data[0];
            renderScreenshot(mr);
            break;
        case RENDER_FRAME_BUFFER:
            String folder = (String) data[0];
            String file = (String) data[1];
            Integer w = (Integer) data[2];
            Integer h = (Integer) data[3];
            renderCurrentFrameBuffer(folder, file, w, h);
            break;
        case FLUSH_FRAMES:
            frameRenderer.flush();
            break;
        case SCREENSHOT_CMD:
            screenshot.takeScreenshot((int) data[0], (int) data[1], (String) data[2]);
            break;
        case UPDATE_GUI:
            renderGui().update((Double) data[0]);
            break;
        case DISPOSE:
            if (renderGui != null)
                renderGui.dispose();
            break;
        }

    }

    private IGui renderGui() {
        return renderGui;
    }

    private static String getCurrentTimeStamp() {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyyMMdd");//dd/MM/yyyy
        Date now = new Date();
        return sdfDate.format(now);
    }
}
