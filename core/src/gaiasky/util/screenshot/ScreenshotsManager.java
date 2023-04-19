/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.screenshot;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.gui.IGui;
import gaiasky.gui.RenderGui;
import gaiasky.render.api.IPostProcessor.PostProcessBean;
import gaiasky.render.api.IPostProcessor.RenderType;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.util.GlobalResources;
import gaiasky.util.OneTimeRunnable;
import gaiasky.util.Settings;
import gaiasky.util.Settings.ImageFormat;
import gaiasky.util.i18n.I18n;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ScreenshotsManager implements IObserver {

    private static final String SCREENSHOT_FILENAME = "screenshot";

    private final GaiaSky gaiaSky;
    private final SceneRenderer sceneRenderer;
    private final IGui renderGui;
    public IFileImageRenderer frameRenderer, screenshotRenderer;

    public ScreenshotsManager(final GaiaSky gaiaSky, final SceneRenderer sceneRenderer, final GlobalResources globalResources) {
        super();
        this.gaiaSky = gaiaSky;
        this.sceneRenderer = sceneRenderer;
        this.frameRenderer = new BasicFileImageRenderer();
        this.screenshotRenderer = new BasicFileImageRenderer();

        // Frame output GUI
        this.renderGui = new RenderGui(globalResources.getSkin(), Gdx.graphics, Settings.settings.program.ui.scale);
        this.renderGui.initialize(null, globalResources.getSpriteBatch());
        this.renderGui.doneLoading(null);

        EventManager.instance.subscribe(this, Event.RENDER_FRAME, Event.RENDER_FRAME_BUFFER, Event.FLUSH_FRAMES, Event.SCREENSHOT_CMD, Event.UPDATE_GUI, Event.DISPOSE);
    }

    private static String getCurrentTimeStamp() {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyyMMdd");//dd/MM/yyyy
        Date now = new Date();
        return sdfDate.format(now);
    }

    public void renderFrame() {
        gaiaSky.getCameraManager().backupCamera();
        final Settings settings = Settings.settings;
        if (settings.frame.active) {
            switch (settings.frame.mode) {
            case SIMPLE -> frameRenderer.saveScreenshot(settings.frame.location, settings.frame.prefix, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true, settings.frame.format, settings.frame.quality);
            case ADVANCED -> {
                // Do not resize post processor
                GaiaSky.instance.resizeImmediate(settings.frame.resolution[0], settings.frame.resolution[1], false, true, false, true);
                renderToImage(sceneRenderer, gaiaSky.getCameraManager(), gaiaSky.getT(), gaiaSky.getPostProcessor().getPostProcessBean(RenderType.frame), settings.frame.resolution[0], settings.frame.resolution[1], settings.frame.location, settings.frame.prefix, frameRenderer, settings.frame.format, settings.frame.quality);
                GaiaSky.instance.resizeImmediate(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false, true, false, true);
            }
            }
        }
        gaiaSky.getCameraManager().restoreCamera();
    }

    private void renderScreenshot(final int width, final int height, final String directory) {
        gaiaSky.getCameraManager().backupCamera();
        final Settings settings = Settings.settings;
        String file = null;
        String filename = getCurrentTimeStamp() + "_" + SCREENSHOT_FILENAME;
        switch (settings.screenshot.mode) {
        case SIMPLE -> file = ImageRenderer.renderToImageGl20(directory, filename, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), settings.screenshot.format, settings.screenshot.quality);
        case ADVANCED -> {
            // Do not resize post processor
            GaiaSky.instance.resizeImmediate(width, height, false, true, false, true);
            file = renderToImage(sceneRenderer, gaiaSky.getCameraManager(), gaiaSky.getT(), gaiaSky.getPostProcessor().getPostProcessBean(RenderType.screenshot), width, height, directory, filename, screenshotRenderer, settings.screenshot.format, settings.screenshot.quality);
            GaiaSky.instance.resizeImmediate(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false, true, false, true);
        }
        }
        if (file != null) {
            EventManager.publish(Event.SCREENSHOT_INFO, this, file);
            EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("notif.screenshot", file));
        }
        gaiaSky.getCameraManager().restoreCamera();
    }

    public void renderCurrentFrameBuffer(String folder, String file, int w, int h) {
        String f = ImageRenderer.renderToImageGl20(folder, file, w, h, Settings.settings.screenshot.format, Settings.settings.screenshot.quality);
        if (f != null) {
            EventManager.publish(Event.SCREENSHOT_INFO, this, f);
            EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("notif.screenshot", file));
        }
    }

    /**
     * Posts a runnable that renders a screenshot after the current update-render cycle.
     *
     * @param width     The width of the screenshot.
     * @param height    The height of the screenshot.
     * @param directory The directory to save the screenshot.
     */
    public void takeScreenshot(int width, int height, String directory) {
        var process = new OneTimeRunnable("screenshot-cmd") {
            @Override
            protected void process() {
                renderScreenshot(width, height, directory);
            }
        };
        process.post();
    }

    /**
     * Renders the current scene to an image and returns the file name where it
     * has been written to
     *
     * @param sceneRenderer The main renderer to use.
     * @param camera        The camera.
     * @param width         The width of the image.
     * @param height        The height of the image.
     * @param folder        The folder to save the image to.
     * @param filename      The file name prefix.
     * @param imageRenderer the {@link IFileImageRenderer} to use.
     *
     * @return String with the path to the screenshot image file.
     */
    public String renderToImage(SceneRenderer sceneRenderer, ICamera camera, double dt, PostProcessBean ppb, int width, int height, String folder, String filename, IFileImageRenderer imageRenderer, ImageFormat format, float quality) {
        FrameBuffer frameBuffer = sceneRenderer.getFrameBuffer(width, height);
        // TODO That's a dirty trick, we should find a better way (i.e. making buildEnabledEffectsList() method public)
        boolean postprocessing = ppb.pp.buildEnabledEffectsList() > 0;
        if (!postprocessing) {
            // If post-processing is not active, we must start the buffer now.
            // Otherwise, it is used in the render method to write the results
            // of the pp.
            frameBuffer.begin();
        }

        // this is the main render function
        sceneRenderer.clearScreen();
        sceneRenderer.render(camera, dt, width, height, width, height, frameBuffer, ppb);

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

        String res = imageRenderer.saveScreenshot(folder, filename, width, height, false, format, quality);

        frameBuffer.end();
        return res;
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        switch (event) {
        case RENDER_FRAME:
            renderFrame();
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
            takeScreenshot((int) data[0], (int) data[1], (String) data[2]);
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
}
