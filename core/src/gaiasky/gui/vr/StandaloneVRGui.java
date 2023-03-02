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
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.BufferUtils;
import gaiasky.gui.IGui;
import gaiasky.render.ComponentTypes;
import gaiasky.util.*;
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
import gaiasky.vr.openvr.VRContext;
import org.lwjgl.openvr.*;

import java.lang.reflect.InvocationTargetException;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages the full rendering cycle of a VR user interface.
 * Creates a surface in front of the user (in 3D world) with the given IGui.
 */
public class StandaloneVRGui<T extends IGui> implements IGui {
    private static final Logger.Log logger = Logger.getLogger(StandaloneVRGui.class);

    int vrWidth, vrHeight;
    int guiWidth = 1920, guiHeight = 1080;
    Skin skin;
    Class<T> guiClass;
    T gui;
    PerspectiveCamera camera;
    IntModelInstance instance;
    IntModelBatch batch;
    Environment env;
    VRContext vrContext;
    FrameBuffer fbLeft, fbRight, fbGui;
    Texture texLeft, texRight;
    SpriteBatch sbScreen;
    Matrix4 eyeSpace = new Matrix4();
    Matrix4 invEyeSpace = new Matrix4();
    HmdMatrix44 projectionMat = HmdMatrix44.create();
    HmdMatrix34 eyeMat = HmdMatrix34.create();
    Vector2 lastSize = new Vector2();
    Vector3 aux = new Vector3();

    private boolean renderToScreen = false;

    public StandaloneVRGui(VRContext vrContext, Class<T> guiClass, Skin skin) {
        this.vrContext = vrContext;
        this.vrWidth = vrContext.getWidth();
        this.vrHeight = vrContext.getHeight();
        this.guiClass = guiClass;
        this.skin = skin;
    }

    @Override
    public void initialize(AssetManager assetManager, SpriteBatch spriteBatch) {
        try {
            // Create GUI.
            gui = guiClass.getDeclaredConstructor(Skin.class, Graphics.class, Float.class, Boolean.class).newInstance(skin, Gdx.graphics, 1f, true);
            gui.setVr(true);
            gui.initialize(assetManager, spriteBatch);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            logger.error(e);
        }

        // Create GUI buffer.
        var builder = new GLFrameBuffer.FrameBufferBuilder(guiWidth, guiHeight);
        builder.addBasicColorTextureAttachment(Pixmap.Format.RGBA8888);
        fbGui = builder.build();

        // FOV determination.
        FloatBuffer fovt = BufferUtils.newFloatBuffer(1);
        FloatBuffer fovb = BufferUtils.newFloatBuffer(1);
        FloatBuffer fovr = BufferUtils.newFloatBuffer(1);
        FloatBuffer fovl = BufferUtils.newFloatBuffer(1);
        VRSystem.VRSystem_GetProjectionRaw(VR.EVREye_Eye_Left, fovl, fovr, fovt, fovb);

        double fovT = Math.toDegrees(Math.atan(fovt.get()));
        double fovB = Math.toDegrees(Math.atan(fovb.get()));
        float fov = (float) (fovB - fovT);

        // The camera.
        camera = new PerspectiveCamera(fov, vrWidth, vrHeight);
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

        env = new Environment();
        env.set(new ColorAttribute(ColorAttribute.AmbientLight, 1f, 1f, 1f, 1f));
        instance = new IntModelInstance(model);
        setSurfacePosition();

        // Model batch.
        batch = new IntModelBatch(new GroundShaderProvider(Gdx.files.internal("shader/normal.vertex.glsl"), Gdx.files.internal("shader/normal.fragment.glsl")));

        // Frame buffer builder.
        GLFrameBuffer.FrameBufferBuilder frameBufferBuilder = new GLFrameBuffer.FrameBufferBuilder(vrContext.getWidth(), vrContext.getHeight());
        int internalFormat = org.lwjgl.opengl.GL30.GL_RGBA8;
        if (Settings.settings.graphics.useSRGB) {
            internalFormat = org.lwjgl.opengl.GL30.GL_SRGB8_ALPHA8;
        }
        frameBufferBuilder.addColorTextureAttachment(internalFormat, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE);
        frameBufferBuilder.addBasicDepthRenderBuffer();

        // Frame buffers.
        fbLeft = frameBufferBuilder.build();
        texLeft = org.lwjgl.openvr.Texture.create();
        texLeft.set(fbLeft.getColorBufferTexture().getTextureObjectHandle(), VR.ETextureType_TextureType_OpenGL, VR.EColorSpace_ColorSpace_Auto);

        fbRight = frameBufferBuilder.build();
        texRight = org.lwjgl.openvr.Texture.create();
        texRight.set(fbRight.getColorBufferTexture().getTextureObjectHandle(), VR.ETextureType_TextureType_OpenGL, VR.EColorSpace_ColorSpace_Auto);

        // Sprite batch for rendering to screen.
        sbScreen = new SpriteBatch();

    }

    private void setSurfacePosition() {
        updateCamera(camera, VR.EVREye_Eye_Left, false, vrWidth, vrHeight);
        Vector3d dir = new Vector3d();
        dir.set(camera.direction);
        float angle = (float) dir.angle(Vector3d.getUnitX());
        if(dir.z > 0) {
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
        gui.update(dt);
    }

    @Override
    public void render(int rw, int rh) {
        // Render GUI to frame buffer.
        fbGui.begin();
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        gui.render(rw, rh);
        fbGui.end();

        if (vrContext != null) {
            try {
                vrContext.pollEvents();
            } catch (Exception e) {
                logger.error(e);
            }

            // Left.
            updateCamera(camera, VR.EVREye_Eye_Left, false, vrWidth, vrHeight);
            fbLeft.begin();
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
            batch.begin(camera);
            batch.render(instance, env);
            batch.end();
            fbLeft.end();

            // Right.
            updateCamera(camera, VR.EVREye_Eye_Right, false, vrWidth, vrHeight);
            fbRight.begin();
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
            batch.begin(camera);
            batch.render(instance, env);
            batch.end();
            fbRight.end();

            /* SUBMIT TO VR COMPOSITOR */
            VRCompositor.VRCompositor_Submit(VR.EVREye_Eye_Left, texLeft, null, VR.EVRSubmitFlags_Submit_Default);
            VRCompositor.VRCompositor_Submit(VR.EVREye_Eye_Right, texRight, null, VR.EVRSubmitFlags_Submit_Default);

            /* Render to screen */
            if (renderToScreen) {
                RenderUtils.renderKeepAspect(fbRight, sbScreen, Gdx.graphics, lastSize);
            }
        }
    }

    private void updateCamera(PerspectiveCamera camera, int eye, boolean updateFrustum, int w, int h) {
        // get the projection matrix from the HDM
        VRSystem.VRSystem_GetProjectionMatrix(eye, camera.near, camera.far, projectionMat);
        VRContext.hmdMat4toMatrix4(projectionMat, camera.projection);

        // get the eye space matrix from the HDM
        VRSystem.VRSystem_GetEyeToHeadTransform(eye, eyeMat);
        VRContext.hmdMat34ToMatrix4(eyeMat, eyeSpace);
        invEyeSpace.set(eyeSpace).inv();

        // get the pose matrix from the HDM
        VRContext.VRDevice hmd = vrContext.getDeviceByType(VRContext.VRDeviceType.HeadMountedDisplay);
        Vector3 up = hmd.getUp(VRContext.Space.Tracker);
        Vector3 dir = hmd.getDirection(VRContext.Space.Tracker);
        Vector3 pos = hmd.getPosition(VRContext.Space.Tracker);

        // Update Eye camera
        camera.viewportWidth = w;
        camera.viewportHeight = h;
        camera.view.idt();
        camera.view.setToLookAt(pos, aux.set(pos).add(dir), up);

        camera.combined.set(camera.projection);
        Matrix4.mul(camera.combined.val, invEyeSpace.val);
        Matrix4.mul(camera.combined.val, camera.view.val);

        camera.up.set(up);
        camera.direction.set(dir);
        camera.position.set(pos);

        if (updateFrustum) {
            camera.invProjectionView.set(camera.combined);
            Matrix4.inv(camera.invProjectionView.val);
            camera.frustum.update(camera.invProjectionView);
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
    public void setVr(boolean vr) {
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
        fbLeft.dispose();
        fbRight.dispose();
        batch.dispose();
        sbScreen.dispose();
    }

    public void setRenderToScreen(boolean renderToScreen) {
        this.renderToScreen = renderToScreen;
    }

}
