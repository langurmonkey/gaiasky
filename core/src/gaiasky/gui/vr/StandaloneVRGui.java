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
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import gaiasky.gui.IGui;
import gaiasky.render.ComponentTypes;
import gaiasky.util.Bits;
import gaiasky.util.Logger;
import gaiasky.util.ModelCache;
import gaiasky.util.RenderUtils;
import gaiasky.util.gdx.IntModelBatch;
import gaiasky.util.gdx.model.IntModel;
import gaiasky.util.gdx.model.IntModelInstance;
import gaiasky.util.gdx.shader.Environment;
import gaiasky.util.gdx.shader.Material;
import gaiasky.util.gdx.shader.attribute.BlendingAttribute;
import gaiasky.util.gdx.shader.attribute.ColorAttribute;
import gaiasky.util.gdx.shader.attribute.IntAttribute;
import gaiasky.util.gdx.shader.attribute.TextureAttribute;
import gaiasky.util.gdx.shader.provider.GroundShaderProvider;
import gaiasky.util.math.Vector3d;
import gaiasky.vr.openxr.OpenXRDriver;
import gaiasky.vr.openxr.OpenXRRenderer;
import gaiasky.vr.openxr.XrHelper;
import gaiasky.vr.openxr.input.OpenXRInputListener;
import gaiasky.vr.openxr.input.actions.VRControllerDevice;
import org.joml.Matrix4f;
import org.lwjgl.openxr.*;
import org.lwjgl.system.MemoryStack;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryStack.stackPush;

/**
 * Manages the full rendering cycle of a VR user interface.
 * Creates a surface in front of the user (in 3D world) with the given IGui, and
 * takes into account the HMD position/orientation. It also renders the detected
 * controller(s).
 */
public class StandaloneVRGui<T extends IGui> implements IGui, OpenXRRenderer {
    private static final Logger.Log logger = Logger.getLogger(StandaloneVRGui.class);

    OpenXRInputListener listener;
    int vrWidth, vrHeight;
    int guiWidth = 2960, guiHeight = 1440;
    Skin skin;
    Class<T> guiClass;
    T gui;
    PerspectiveCamera camera;
    IntModelInstance instance;
    IntModelBatch batch;
    Environment uiEnvironment, controllersEnvironment;
    OpenXRDriver driver;
    FrameBuffer fbGui;
    SpriteBatch sbScreen;
    Array<VRControllerDevice> controllers;
    Vector2 lastSize = new Vector2();

    private boolean positionSet = false;
    private boolean renderToScreen = false;

    public StandaloneVRGui(OpenXRDriver vrContext, Class<T> guiClass, Skin skin, OpenXRInputListener listener) {
        this.driver = vrContext;
        this.vrWidth = vrContext.getWidth();
        this.vrHeight = vrContext.getHeight();
        this.guiClass = guiClass;
        this.skin = skin;
        this.listener = listener;
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
        Bits attributes = Bits.indexes(VertexAttributes.Usage.Position, VertexAttributes.Usage.Normal, VertexAttributes.Usage.Tangent, VertexAttributes.Usage.BiNormal, VertexAttributes.Usage.TextureCoordinates);
        var pair = cache.getModel("surface", params, attributes, GL20.GL_TRIANGLES);
        IntModel model = pair.getFirst();
        Map<String, Material> materials = pair.getSecond();
        Material material;
        if (materials.size() == 0) {
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
        batch = new IntModelBatch(new GroundShaderProvider(Gdx.files.internal("shader/normal.vertex.glsl"), Gdx.files.internal("shader/normal.fragment.glsl")));

        // Controller environment.
        controllersEnvironment = new Environment();
        controllersEnvironment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.2f, 0.2f, 0.2f, 1f));
        DirectionalLight dlight = new DirectionalLight();
        dlight.color.set(1f, 1f, 1f, 1f);
        dlight.direction.set(0, -1, 0);
        controllersEnvironment.add(dlight);

        // Sprite batch for rendering to screen.
        sbScreen = new SpriteBatch();

        if (driver != null && listener != null) {
            driver.addListener(listener);
        }

    }

    private void setSurfacePosition(XrCompositionLayerProjectionView view) {
        updateCamera(view, camera, OpenXRDriver.VR_Eye_Left, vrWidth, vrHeight);
        Vector3d dir = new Vector3d();
        dir.set(camera.direction);
        float angle = (float) dir.angle(Vector3d.getUnitX());
        if (dir.z > 0) {
            angle = -angle;
        }
        Vector3d pos = new Vector3d();
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

    private FrameBuffer lastFrameBuffer;

    @Override
    public void renderOpenXRView(XrCompositionLayerProjectionView layerView, XrSwapchainImageOpenGLKHR swapchainImage, FrameBuffer frameBuffer, int viewIndex) {
        if (!positionSet) {
            setSurfacePosition(layerView);
            positionSet = true;
        }

        frameBuffer.begin();
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, swapchainImage.image(), 0);

        XrRect2Di imageRect = layerView.subImage().imageRect();
        updateCamera(layerView, camera, viewIndex, imageRect.extent().width(), imageRect.extent().height());

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        batch.begin(camera);
        batch.render(instance, uiEnvironment);
        renderControllers();
        batch.end();
        frameBuffer.end();
        lastFrameBuffer = frameBuffer;
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

            if (driver.isRunning()) {
                // Delegate to OpenXR driver.
                driver.renderFrameOpenXR();
            }

            // Render to screen if necessary.
            if (renderToScreen && lastFrameBuffer != null) {
                RenderUtils.renderKeepAspect(lastFrameBuffer, sbScreen, Gdx.graphics, lastSize);
            }
        }
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

    private final Matrix4f projectionMatrix = new Matrix4f();
    private final Matrix4f viewMatrix = new Matrix4f();
    private final Quaternion quaternion = new Quaternion();

    private void updateCamera(XrCompositionLayerProjectionView layerView, PerspectiveCamera camera, int eye, int w, int h) {
        XrPosef pose = layerView.pose();
        XrVector3f position = pose.position$();
        XrQuaternionf orientation = pose.orientation();
        try (MemoryStack stack = stackPush()) {
            projectionMatrix.set(XrHelper.createProjectionMatrixBuffer(stack, layerView.fov(), camera.near, camera.far, false));
        }
        viewMatrix.translationRotateScaleInvert(position.x(), position.y(), position.z(), orientation.x(), orientation.y(), orientation.z(), orientation.w(), 1, 1, 1);

        projectionMatrix.get(camera.projection.val);
        viewMatrix.get(camera.view.val);
        camera.combined.set(camera.projection);
        Matrix4.mul(camera.combined.val, camera.view.val);

        quaternion.set(orientation.x(), orientation.y(), orientation.z(), orientation.w());
        camera.position.set(position.x(), position.y(), position.z());
        camera.direction.set(0, 0, -1).mul(quaternion);
        camera.up.set(0, 1, 0).mul(quaternion);
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
