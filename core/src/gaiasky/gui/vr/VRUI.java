package gaiasky.gui.vr;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer.FrameBufferBuilder;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.gui.GamepadGui;
import gaiasky.gui.IGui;
import gaiasky.render.BlendMode;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.RenderGroup;
import gaiasky.scene.Archetype;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.Base;
import gaiasky.scene.component.VRDevice;
import gaiasky.scene.component.tag.TagNoClosest;
import gaiasky.scene.record.ModelComponent;
import gaiasky.util.Constants;
import gaiasky.util.Settings;
import gaiasky.util.coord.StaticCoordinates;
import gaiasky.util.gdx.shader.attribute.TextureAttribute;
import gaiasky.util.math.*;
import gaiasky.vr.openvr.VRContext;
import gaiasky.vr.openvr.VRDeviceListener;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Creates and manages the VR UI and all its interactions with controllers.
 */
public class VRUI implements VRDeviceListener, IGui, IObserver, Disposable {

    public static final int WIDTH = 1920;
    public static final int HEIGHT = 1080;

    /** Button that acts as left mouse button for the UI interaction. **/
    public static final int BUTTON_LEFT_MOUSE = VRContext.VRControllerButtons.SteamVR_Trigger;

    Scene scene;
    Stage stage;
    FrameBuffer buffer;
    Texture uiTexture;
    Entity entity;
    // VR UI position with respect to the user.
    Vector3d relativePosition = new Vector3d();
    // 5 pointers
    Vector2 pointer = new Vector2();
    boolean triggerPressed = false;

    ShapeRenderer shapeRenderer;
    Set<VRDevice> vrControllers;
    Map<VRContext.VRDevice, VRDevice> deviceToDevice;

    GamepadGui gamepadGui;

    /** Saves the controller that last interacted with the UI, so that we can only get its input. **/
    VRContext.VRDevice interactingController;

    public VRUI() {
    }

    public void setScene(Scene scene) {
        this.scene = scene;
    }

    public void initialize(AssetManager manager, SpriteBatch batch) {
        // Create stage.
        Viewport vp = new StretchViewport(WIDTH, HEIGHT);
        stage = new Stage(vp, batch);
        shapeRenderer = new ShapeRenderer(100, GaiaSky.instance.getGlobalResources().getShapeShader());
        shapeRenderer.setAutoShapeType(true);

        // Create controllers set.
        vrControllers = new HashSet<>();
        deviceToDevice = new HashMap<>();

        // Create buffer.
        var builder = new FrameBufferBuilder(WIDTH, HEIGHT);
        builder.addBasicColorTextureAttachment(Format.RGBA8888);
        buffer = builder.build();
        uiTexture = buffer.getColorBufferTexture();

        // Events.
        EventManager.instance.subscribe(this, Event.SHOW_VR_UI, Event.VR_CONTROLLER_INFO);
    }

    @Override
    public void doneLoading(AssetManager assetManager) {

    }

    public void build(Skin skin) {
        gamepadGui = new GamepadGui(skin, Gdx.graphics, 1f / Settings.settings.program.ui.scale, true);
        gamepadGui.initialize(stage);
        gamepadGui.build();
        Table content = gamepadGui.getContent();
        content.setFillParent(true);
        content.left().top();

        stage.clear();
        stage.addActor(content);
    }

    Vector3d point = new Vector3d();
    Vector3d normal = new Vector3d();
    Vector3d intersection = new Vector3d();
    Planed plane = new Planed(Vector3d.getUnitX(), 0.1 * Constants.M_TO_U);
    Matrix4d transform = new Matrix4d();
    Matrix4d inverse = new Matrix4d();

    public void update(double dt) {
        // Compute screen-space vertex coordinates for the current camera.
        if (entity != null) {
            var base = Mapper.base.get(entity);
            if (base.isVisible()) {
                // Handle controller interaction.
                if (!vrControllers.isEmpty()) {
                    var model = Mapper.model.get(entity);
                    transform.set(model.model.instance.transform);
                    inverse.set(transform).inv();
                    // Original plane in object space.
                    point.set(0, 0, 0);
                    normal.set(0, 1, 0);
                    // Move to world space by applying transform.
                    point.mul(transform);
                    normal.mul(transform);
                    plane.set(point, normal);
                    // Check intersection with each controller.
                    int intersecting = 0;
                    for (var device : vrControllers) {
                        if (device.device.isInitialized() && device.device.isConnected()) {
                            if (!deviceToDevice.containsKey(device.device)) {
                                deviceToDevice.put(device.device, device);
                            }
                            var intersects = IntersectorDouble.intersectSegmentPlane(device.beamP0, device.beamPn, plane, intersection);

                            if (intersects) {
                                // Use inverse transform to position on ZX plane.
                                intersection.mul(inverse);
                                double x3d = MathUtilsDouble.clamp(intersection.x, -0.28125, 0.28125);
                                double z3d = MathUtilsDouble.clamp(intersection.z, -0.5, 0.5);
                                double u = z3d + 0.5;
                                double v = (x3d + 0.28125) * 1 / 0.5625;

                                // Update hit.
                                device.hitUI = u > 0 && u < 1 && v > 0 && v < 1;
                                // Default to current.
                                if (interactingController == null) {
                                    interactingController = device.device;
                                }
                                device.interacting = device.device == interactingController;

                                if (device.hitUI && device.device == interactingController) {
                                    int x = (int) (u * Gdx.graphics.getWidth());
                                    int y = (int) (v * Gdx.graphics.getHeight());

                                    pointer.x = x;
                                    pointer.y = Gdx.graphics.getHeight() - y;

                                    if (triggerPressed) {
                                        stage.touchDragged(x, y, 0);
                                    } else {
                                        stage.mouseMoved(x, y);
                                    }
                                    intersecting++;
                                }

                            }
                        } else {
                            deviceToDevice.remove(device.device);
                        }
                    }
                    if (intersecting == 0) {
                        pointer.x = Float.NaN;
                        pointer.y = Float.NaN;
                    }
                }
                stage.act((float) dt);
            }
        }

    }

    /**
     * Updates the position of the VRUI object to follow the camera around.
     */
    private void updatePosition() {
        if (entity != null) {
            var base = Mapper.base.get(entity);
            if (base.isVisible()) {
                var camera = GaiaSky.instance.getICamera();
                var body = Mapper.body.get(entity);
                var coord = Mapper.coordinates.get(entity);
                // Update VR UI position.
                body.pos.set(camera.getPos()).add(relativePosition);
                ((StaticCoordinates) coord.coordinates).setPosition(body.pos);
            }
        }
    }

    private boolean isVRUIVisible() {
        return entity != null && Mapper.base.get(entity).isVisible();
    }

    private void showVRUI(Base base) {
        if (base != null) {
            base.visible = true;
            // Camera runnable to update the position of the VR UI if necessary.
            GaiaSky.instance.scripting().parkCameraRunnable("VRUI-pos-updater", this::updatePosition);
        }
    }

    private void hideVRUI(Base base) {
        if (base != null) {
            base.visible = false;
            for (var device : vrControllers) {
                device.hitUI = false;
            }
            // Camera runnable to update the position of the VR UI if necessary.
            GaiaSky.instance.scripting().removeRunnable("VRUI-pos-updater");
        }
    }

    /**
     * Checks whether the quadrilateral or polygon defined by points contains the point [x,y].
     *
     * @param x      The coordinate X of the point to test.
     * @param y      The coordinate Y of the point to test.
     * @param points The points defining the polygon.
     * @return Whether the point is in the polygon.
     */
    public boolean contains(int x, int y, Vector3[] points) {
        int i;
        int j;
        boolean result = false;
        for (i = 0, j = points.length - 1; i < points.length; j = i++) {
            if ((points[i].y > y) != (points[j].y > y) && (x < (points[j].x - points[i].x) * (y - points[i].y) / (points[j].y - points[i].y) + points[i].x)) {
                result = !result;
            }
        }
        return result;
    }

    @Override
    public void render(int rw, int rh) {
        buffer.begin();
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        stage.draw();

        if (isVRUIVisible() && pointer != null) {
            shapeRenderer.begin(ShapeType.Filled);
            var pi = pointer;
            if (Float.isFinite(pi.x) && Float.isFinite(pi.y)) {
                shapeRenderer.setColor(1f, 0f, 0f, 1f);
                shapeRenderer.circle(pi.x, pi.y, 10);
            }
            shapeRenderer.end();
        }
        buffer.end();
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
        return stage;
    }

    @Override
    public void setVisibilityToggles(ComponentType[] entities, ComponentTypes visible) {
    }

    @Override
    public Actor findActor(String name) {
        return null;
    }

    @Override
    public void sethOffset(int hOffset) {
    }

    @Override
    public void setVr(boolean vr) {
    }

    @Override
    public boolean mustDraw() {
        return true;
    }

    @Override
    public boolean updateUnitsPerPixel(float upp) {
        return false;
    }

    @Override
    public void dispose() {
        if (buffer != null) {
            buffer.dispose();
        }
        if (stage != null) {
            stage.dispose();
        }
    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        if (event == Event.SHOW_VR_UI) {

            GaiaSky.postRunnable(() -> {
                if (entity == null) {
                    Archetype at = scene.archetypes().get("gaiasky.scenegraph.ModelBody");
                    Entity entity = at.createEntity();

                    // Add tags.
                    entity.add(new TagNoClosest());

                    var base = Mapper.base.get(entity);
                    base.setName("VRUI");
                    base.archetype = at;
                    base.ct = new ComponentTypes(ComponentType.Others.ordinal());
                    base.visible = false;

                    var body = Mapper.body.get(entity);
                    body.setColor(new float[] { 1, 1, 1, 1 });
                    body.setLabelColor(new float[] { 0, 0, 0, 0 });
                    body.setSizeKm(100.0);

                    var affine = Mapper.affine.get(entity);
                    affine.initialize();

                    var coord = Mapper.coordinates.get(entity);
                    var staticCoordinates = new StaticCoordinates();
                    staticCoordinates.setPosition(body.pos);
                    coord.coordinates = staticCoordinates;

                    var graph = Mapper.graph.get(entity);
                    graph.setParent(Scene.ROOT_NAME);

                    Map<String, Object> params = new HashMap<>();
                    params.put("divisionsu", 1L);
                    params.put("divisionsv", 1L);
                    params.put("width", 1.0);
                    params.put("height", (double) HEIGHT / (double) WIDTH);
                    params.put("flip", false);

                    var model = Mapper.model.get(entity);
                    model.model = new ModelComponent();
                    model.model.type = "surface";
                    model.modelSize = 1;
                    model.model.setPrimitiveType(GL20.GL_TRIANGLES);
                    model.model.setParams(params);
                    model.model.setStaticLight(1.0);
                    model.model.setBlendMode(BlendMode.ALPHA);

                    var rt = Mapper.renderType.get(entity);
                    rt.renderGroup = RenderGroup.MODEL_PIX_TRANSPARENT;

                    // Initialize shape.
                    scene.initializeEntity(entity);
                    scene.setUpEntity(entity);

                    // Set texture.
                    var mat = model.model.instance.materials.get(0);
                    if (mat != null) {
                        mat.set(TextureAttribute.createDiffuse(buffer.getColorBufferTexture()));
                    }
                    this.entity = entity;

                    // Add to scene.
                    EventManager.publish(Event.SCENE_ADD_OBJECT_NO_POST_CMD, this, entity, true);
                }

                // Position UI.
                Runnable r = () -> {
                    var base = Mapper.base.get(entity);
                    // Toggle visibility.
                    if (base.isVisible()) {
                        hideVRUI(base);
                    } else {
                        showVRUI(base);
                        // Set position and orientation
                        // 10 meters in front of the camera, on the equatorial plane.
                        ICamera camera = GaiaSky.instance.getICamera();
                        var dir = camera.getDirection().cpy();
                        dir.y = 0;
                        dir.nor();
                        var body = Mapper.body.get(entity);
                        body.pos.set(camera.getPos().cpy().add(dir.scl(190 * Constants.KM_TO_U)));
                        body.pos.add(0, 80.0 * Constants.KM_TO_U, 0);

                        // Save relative position.
                        relativePosition.set(body.pos).sub(camera.getPos());

                        // Set new static coordinates.
                        var coord = Mapper.coordinates.get(entity);
                        ((StaticCoordinates) coord.coordinates).setPosition(body.pos);

                        // Set rotation.
                        var affine = Mapper.affine.get(entity);
                        affine.transformations.clear();
                        dir.nor();
                        var angle = dir.anglePrecise(new Vector3d(0, 0, 1));
                        if (dir.x < 0) {
                            angle = -angle;
                        }
                        affine.setQuaternion(new double[] { 0, 1, 0 }, angle);
                        affine.setQuaternion(new double[] { 1, 0, 0 }, 90);
                        affine.setQuaternion(new double[] { 0, 1, 0 }, -90);
                    }
                };

                r.run();
            });

        } else if (event == Event.VR_CONTROLLER_INFO) {
            vrControllers.add((VRDevice) data[0]);
        }
    }

    @Override
    public void connected(VRContext.VRDevice device) {

    }

    @Override
    public void disconnected(VRContext.VRDevice device) {

    }

    @Override
    public boolean buttonPressed(VRContext.VRDevice device, int button) {
        if (button == BUTTON_LEFT_MOUSE) {
            var dev = deviceToDevice.get(device);
            if (dev.hitUI && !dev.interacting) {
                interactingController = dev.device;
                dev.interacting = true;
            }
            if (Float.isFinite(pointer.x) && Float.isFinite(pointer.y) && device == interactingController) {
                stage.touchDown((int) pointer.x, (int) (Gdx.graphics.getHeight() - pointer.y), 0, Input.Buttons.LEFT);
                triggerPressed = true;
                interactingController = device;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean buttonReleased(VRContext.VRDevice device, int button) {
        if (button == BUTTON_LEFT_MOUSE) {
            var dev = deviceToDevice.get(device);
            if (dev.hitUI && !dev.interacting) {
                interactingController = dev.device;
                dev.interacting = true;
            }
            if (Float.isFinite(pointer.x) && Float.isFinite(pointer.y) && device == interactingController) {
                stage.touchUp((int) pointer.x, (int) (Gdx.graphics.getHeight() - pointer.y), 0, Input.Buttons.LEFT);
                triggerPressed = false;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean buttonTouched(VRContext.VRDevice device, int button) {
        return false;
    }

    @Override
    public boolean buttonUntouched(VRContext.VRDevice device, int button) {
        return false;
    }

    @Override
    public boolean axisMoved(VRContext.VRDevice device, int axis, float valueX, float valueY) {
        return false;
    }

    @Override
    public void event(int code) {

    }
}
