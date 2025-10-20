/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.process;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.render.api.IPostProcessor.PostProcessBean;
import gaiasky.render.api.IRenderMode;
import gaiasky.render.api.ISceneRenderer;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.camera.NaturalCamera;
import gaiasky.scene.record.ModelComponent;
import gaiasky.util.RenderUtils;
import gaiasky.util.gdx.g2d.ExtSpriteBatch;
import gaiasky.util.gdx.graphics.TextureView;
import gaiasky.util.gdx.shader.Environment;
import gaiasky.util.gdx.shader.attribute.ColorAttribute;
import gaiasky.vr.openxr.XrDriver;
import gaiasky.vr.openxr.XrRenderer;
import gaiasky.vr.openxr.XrViewManager;
import gaiasky.vr.openxr.input.XrControllerDevice;
import org.lwjgl.opengl.GL40;
import org.lwjgl.openxr.XrCompositionLayerProjectionView;
import org.lwjgl.openxr.XrSwapchainImageOpenGLKHR;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL30.*;

public class RenderModeOpenXR extends RenderModeAbstract implements IRenderMode, XrRenderer {
    private final Scene scene;
    private final XrDriver driver;
    private final XrViewManager viewManager;

    public Array<Entity> controllerObjects;
    private final Map<XrControllerDevice, Entity> vrDeviceToModel;
    private Environment controllersEnvironment;
    private TextureView textureView;

    // GUI
    private ExtSpriteBatch sbScreen;

    private Vector2 lastSize;

    public RenderModeOpenXR(final Scene scene,
                            final XrDriver xrDriver,
                            final ExtSpriteBatch spriteBatch) {
        super();
        this.scene = scene;
        this.driver = xrDriver;
        this.viewManager = new XrViewManager();
        this.vrDeviceToModel = new HashMap<>();

        if (xrDriver != null) {
            this.textureView = new TextureView(0, xrDriver.getWidth(), xrDriver.getHeight());
            // Aux vectors
            lastSize = new Vector2();

            // Controllers
            Array<XrControllerDevice> controllers = xrDriver.getControllerDevices();

            // Env
            controllersEnvironment = new Environment();
            controllersEnvironment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.6f, 0.6f, 0.6f, 1f));
            DirectionalLight directionalLight = new DirectionalLight();
            directionalLight.color.set(1f, 1f, 1f, 1f);
            directionalLight.direction.set(0f, -0.3f, -4.0f);
            controllersEnvironment.add(directionalLight);

            // Controller objects
            controllerObjects = new Array<>(false, controllers.size);
            for (XrControllerDevice controller : controllers) {
                if (!controller.isInitialized())
                    controller.initialize(xrDriver);
                addVRController(controller);
            }

            // Screen.
            sbScreen = spriteBatch;

            // Set renderer.
            this.driver.setRenderer(this);

        }
    }

    private ISceneRenderer sgr;
    private ICamera camera;
    private PostProcessBean ppb;
    private int rw, rh;
    private double t;

    @Override
    public void render(ISceneRenderer sgr,
                       ICamera camera,
                       double t,
                       int rw,
                       int rh,
                       int tw,
                       int th,
                       FrameBuffer fb,
                       PostProcessBean ppb) {
        if (driver != null && !driver.getLastPollEventsResult()) {
            rc.ppb = null;

            // Add controller objects to render lists.
            for (Entity controller : controllerObjects) {
                var model = Mapper.model.get(controller);

                if (model.model.instance != null) {
                    scene.extractEntity(controller);
                }
            }

            // Instruct driver to render frames.
            if (driver.isRunning() && driver.hasRenderer()) {
                this.sgr = sgr;
                this.camera = camera;
                this.ppb = ppb;
                this.rw = rw;
                this.rh = rh;
                this.t = t;
                driver.renderFrameOpenXR();
            }
        }
    }

    @Override
    public void renderOpenXRView(XrCompositionLayerProjectionView layerView,
                                 XrSwapchainImageOpenGLKHR swapChainImage,
                                 FrameBuffer frameBuffer,
                                 int viewIndex) {
        rc.ppb = null;
        sgr.getLightGlowPass().render(camera, sgr.getGlowFrameBuffer());

        // Update camera.
        viewManager.updateCamera(layerView, camera.getCamera(), (NaturalCamera) camera.getCurrent(), rc);
        boolean postProcess = postProcessCapture(ppb, frameBuffer, rw, rh, ppb::captureVR);

        // Render scene.
        sgr.renderScene(camera, t, rc);

        // Render camera.
        camera.render(rw, rh);
        sendOrientationUpdate(camera.getCamera(), rw, rh);

        // To swap-chain texture.
        frameBuffer.begin();
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, swapChainImage.image(), 0);
        frameBuffer.end();
        postProcessRender(ppb, frameBuffer, postProcess, camera, rw, rh);

        /* Render to screen */
        if (viewIndex == 0) {
            textureView.setTexture(swapChainImage.image(), driver.getWidth(), driver.getHeight());
            Gdx.gl.glEnable(GL40.GL_FRAMEBUFFER_SRGB);
            RenderUtils.renderKeepAspect(textureView, sbScreen, Gdx.graphics, lastSize);
            Gdx.gl.glDisable(GL40.GL_FRAMEBUFFER_SRGB);
        }
    }

    public void resize(int rw,
                       int rh,
                       int tw,
                       int th) {
        if (lastSize != null) {
            lastSize.set(-1, -1);
        }
    }

    private Entity newVRDeviceModelEntity(XrControllerDevice device,
                                          Environment environment) {
        var archetype = scene.archetypes().get("gaiasky.scenegraph.VRDeviceModel");
        var entity = archetype.createEntity();
        var vr = Mapper.vr.get(entity);
        vr.device = device;
        var model = Mapper.model.get(entity);
        model.model = new ModelComponent();
        model.model.env = environment;
        scene.initializeEntity(entity);
        scene.setUpEntity(entity);
        scene.engine.addEntity(entity);
        EventManager.publish(Event.SCENE_ADD_OBJECT_CMD, this, entity, false);
        return entity;
    }

    private void addVRController(XrControllerDevice device) {
        if (!vrDeviceToModel.containsKey(device)) {
            Entity entity = newVRDeviceModelEntity(device, controllersEnvironment);
            controllerObjects.add(entity);
            vrDeviceToModel.put(device, entity);
        }
    }

    private void removeVRController(XrControllerDevice device) {
        if (vrDeviceToModel.containsKey(device)) {
            Entity entity = vrDeviceToModel.get(device);
            controllerObjects.removeValue(entity, true);
            vrDeviceToModel.remove(device);
        }
    }

    public Map<XrControllerDevice, Entity> getXRControllerToModel() {
        return vrDeviceToModel;
    }

    @Override
    public void dispose() {

    }
}
