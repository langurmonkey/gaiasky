/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.vr;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import gaiasky.gui.api.IGui;
import gaiasky.render.ComponentTypes;
import gaiasky.util.*;
import gaiasky.util.gdx.IntModelBatch;
import gaiasky.util.gdx.g2d.ExtSpriteBatch;
import gaiasky.util.gdx.graphics.TextureView;
import gaiasky.util.gdx.model.IntModel;
import gaiasky.util.gdx.model.IntModelInstance;
import gaiasky.util.gdx.shader.Environment;
import gaiasky.util.gdx.shader.Material;
import gaiasky.util.gdx.shader.attribute.BlendingAttribute;
import gaiasky.util.gdx.shader.attribute.ColorAttribute;
import gaiasky.util.gdx.shader.attribute.IntAttribute;
import gaiasky.util.gdx.shader.attribute.TextureAttribute;
import gaiasky.util.gdx.shader.provider.GroundShaderProvider;
import gaiasky.util.math.Vector3D;
import gaiasky.vr.openxr.XrDriver;
import gaiasky.vr.openxr.XrRenderer;
import gaiasky.vr.openxr.XrViewManager;
import gaiasky.vr.openxr.input.XrControllerDevice;
import gaiasky.vr.openxr.input.XrInputListener;
import org.lwjgl.opengl.GL40;
import org.lwjgl.openxr.XrCompositionLayerProjectionView;
import org.lwjgl.openxr.XrSwapchainImageOpenGLKHR;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL30.*;

public class StandaloneVRGui<T extends IGui> implements IGui, XrRenderer {
    private static final Logger.Log logger = Logger.getLogger(StandaloneVRGui.class);

    final int vrWidth, vrHeight;
    int guiWidth = 2960, guiHeight = 1440;
    Skin skin;
    Class<T> guiClass;
    T gui;
    PerspectiveCamera camera;
    IntModelInstance instance;
    IntModelBatch batch;
    Environment uiEnvironment, controllersEnvironment;
    final XrDriver driver;
    final XrInputListener listener;
    final XrViewManager viewManager;
    private final TextureView textureView;
    FrameBuffer fbGui;
    ExtSpriteBatch sbScreen;
    Array<XrControllerDevice> controllers;
    Vector2 lastSize = new Vector2();

    private boolean positionSet = false;
    private boolean renderToScreen = false;

    public StandaloneVRGui(XrDriver xrDriver, Class<T> guiClass, Skin skin, XrInputListener listener) {
        this.driver = xrDriver;
        this.vrWidth = xrDriver.getWidth();
        this.vrHeight = xrDriver.getHeight();
        this.guiClass = guiClass;
        this.skin = skin;
        this.listener = listener;
        this.viewManager = new XrViewManager();
        this.textureView = new TextureView(0, vrWidth, vrHeight);
    }

    @Override
    public void initialize(AssetManager assetManager, SpriteBatch spriteBatch) {
        try {
            // Create GUI.
            gui = guiClass.getDeclaredConstructor(Skin.class, Graphics.class, Float.class, Boolean.class).newInstance(skin, Gdx.graphics, 1f, true);
            gui.setVR(true);
            gui.initialize(assetManager, spriteBatch);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            logger.error(e);
        }

        // Create GUI buffer.
        var builder = new GLFrameBuffer.FrameBufferBuilder(guiWidth, guiHeight);
        builder.addBasicColorTextureAttachment(Pixmap.Format.RGBA8888);
        fbGui = builder.build();

        // The camera.
        camera = new PerspectiveCamera(50, vrWidth, vrHeight);
        camera.near = 0.1f;
        camera.far = 10f;
        camera.position.set(0, 0, 0);
        camera.direction.set(1, 0, 0);
        camera.up.set(0, 0, 1);
        camera.update();

        // Create a model.
        Map<String, Object> params = new HashMap<>();
        params.put("divisionsu", 1L);
        params.put("divisionsv", 1L);
        params.put("width", 1.0);
        params.put("height", (double) guiWidth / (double) guiHeight);
        params.put("flip", true);

        ModelCache cache = new ModelCache();
        Bits attributes = Bits.indices(VertexAttributes.Usage.Position, VertexAttributes.Usage.Normal, VertexAttributes.Usage.Tangent, VertexAttributes.Usage.BiNormal, VertexAttributes.Usage.TextureCoordinates);
        var pair = cache.getModel("surface", params, attributes, GL20.GL_TRIANGLES);
        IntModel model = pair.getFirst();
        Map<String, Material> materials = pair.getSecond();
        Material material;
        if (materials.isEmpty()) {
            material = new Material();
            materials.put("base", material);
        } else {
            var key = materials.keySet().toArray()[0];
            material = materials.get(key);
        }
        material.set(new TextureAttribute(TextureAttribute.Diffuse, fbGui.getColorBufferTexture()));
        material.set(new BlendingAttribute(true, GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, 1f));
        material.set(new IntAttribute(IntAttribute.CullFace, GL20.GL_BACK));

        uiEnvironment = new Environment();
        uiEnvironment.set(new ColorAttribute(ColorAttribute.AmbientLight, 1f, 1f, 1f, 1f));
        instance = new IntModelInstance(model);

        // Model batch.
        batch = new IntModelBatch(new GroundShaderProvider(Gdx.files.internal("shader/pbr.vertex.glsl"), Gdx.files.internal("shader/pbr.fragment.glsl")));

        // Controller environment.
        controllersEnvironment = new Environment();
        controllersEnvironment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.6f, 0.6f, 0.6f, 1f));
        DirectionalLight directionalLight = new DirectionalLight();
        directionalLight.color.set(1f, 1f, 1f, 1f);
        directionalLight.direction.set(0f, -0.3f, -4.0f);
        controllersEnvironment.add(directionalLight);

        // Sprite batch for rendering to screen.
        sbScreen = new ExtSpriteBatch(5);

        if (driver != null && listener != null) {
            driver.addListener(listener);
        }

    }

    private void setSurfacePosition(XrCompositionLayerProjectionView view) {
        viewManager.updateCamera(view, camera);
        Vector3D dir = new Vector3D();
        dir.set(camera.direction);
        float angle = (float) dir.angle(Vector3D.getUnitX());
        if (dir.z > 0) {
            angle = -angle;
        }
        Vector3D pos = new Vector3D();
        pos.set(camera.position);
        dir.y = 0;
        pos.add(dir.nor().scl(1.5f));
        instance.transform.idt().translate((float) pos.x, (float) pos.y, (float) pos.z).rotate(0, 1, 0, 90 + angle).rotate(0, 0, 1, 180).rotate(1, 0, 0, 90);
    }

    @Override
    public void doneLoading(AssetManager assetManager) {
    }

    @Override
    public void update(double dt) {
        // Initialize controllers if needed.
        if (controllers == null) {
            controllers = driver.getControllerDevices();
        }
        if (controllers != null) {
            for (var controller : controllers) {
                if (!controller.isInitialized()) {
                    controller.initialize(driver);
                }
            }
        }

        gui.update(dt);
    }

    @Override
    public void render(int rw, int rh) {
        // OpenXR render.
        if (!driver.pollEvents()) {
            // First render GUI to frame buffer.
            fbGui.begin();
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
            gui.render(rw, rh);
            fbGui.end();

            if (driver.isRunning() && driver.hasRenderer()) {
                // Delegate to OpenXR driver.
                driver.renderFrameOpenXR();
            }

        }
    }

    @Override
    public void renderOpenXRView(XrCompositionLayerProjectionView layerView, XrSwapchainImageOpenGLKHR swapchainImage, FrameBuffer frameBuffer, int viewIndex) {
        if (!positionSet) {
            setSurfacePosition(layerView);
            positionSet = true;
        }

        // Update camera with view position and orientation.
        viewManager.updateCamera(layerView, camera);

        frameBuffer.begin();
        // Attach swapchain image to frame buffer.
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, swapchainImage.image(), 0);

        // Actual rendering.
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        batch.begin(camera);
        batch.render(instance, uiEnvironment);
        renderControllers();
        batch.end();
        frameBuffer.end();

        // Render to screen if necessary.
        if (viewIndex == 0 && renderToScreen) {
            // Set to texture view for rendering to screen.
            textureView.setTexture(swapchainImage.image(), driver.getWidth(), driver.getHeight());
            Gdx.gl.glEnable(GL40.GL_FRAMEBUFFER_SRGB);
            RenderUtils.renderKeepAspect(textureView, sbScreen, Gdx.graphics, lastSize);
            Gdx.gl.glDisable(GL40.GL_FRAMEBUFFER_SRGB);
        }
    }

    @Override
    public void renderMirrorToDesktop(int textureHandle) {
    }


    private void renderControllers() {
        if (controllers != null) {
            for (var controller : controllers) {
                if (controller.isInitialized() && controller.isActive()) {
                    var controllerInstance = controller.modelInstance;
                    if (controllerInstance != null) {
                        batch.render(controllerInstance, controllersEnvironment);
                    }
                }
            }
        }
    }


    @Override
    public void resize(int width, int height) {

    }

    @Override
    public void resizeImmediate(int width, int height) {

    }

    @Override
    public boolean cancelTouchFocus() {
        return false;
    }

    @Override
    public Stage getGuiStage() {
        return null;
    }

    @Override
    public void setVisibilityToggles(ComponentTypes.ComponentType[] entities, ComponentTypes visible) {
    }

    @Override
    public Actor findActor(String name) {
        return null;
    }

    @Override
    public void setVR(boolean vr) {
    }

    @Override
    public boolean isVR() {
        return true;
    }

    @Override
    public boolean mustDraw() {
        return false;
    }

    @Override
    public boolean updateUnitsPerPixel(float upp) {
        return false;
    }

    @Override
    public float getUnitsPerPixel() {
        return 1;
    }

    @Override
    public void setBackBufferSize(int width, int height) {
    }

    @Override
    public void dispose() {
        if (driver != null && listener != null) {
            driver.removeListener(listener);
        }
        batch.dispose();
        sbScreen.dispose();
    }

    public void setRenderToScreen(boolean renderToScreen) {
        this.renderToScreen = renderToScreen;
    }
}
