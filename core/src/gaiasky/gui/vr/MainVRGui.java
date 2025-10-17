/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.vr;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer.FrameBufferBuilder;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.Viewport;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.gui.api.IGui;
import gaiasky.gui.main.GamepadGui;
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
import gaiasky.util.camera.CameraUtils;
import gaiasky.util.coord.StaticCoordinates;
import gaiasky.util.gdx.shader.attribute.TextureAttribute;
import gaiasky.util.math.IntersectorDouble;
import gaiasky.util.math.Matrix4D;
import gaiasky.util.math.Vector3Q;
import gaiasky.util.math.Vector3D;
import gaiasky.util.scene2d.FixedStage;
import gaiasky.vr.openxr.input.XrControllerDevice;
import gaiasky.vr.openxr.input.XrInputListener;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MainVRGui implements XrInputListener, InputProcessor, IGui, IObserver, Disposable {

    /** Width of the UI frame buffer. **/
    public static final int WIDTH = (int) (1920 * 0.85);
    /** Height of the UI frame buffer. **/
    public static final int HEIGHT = (int) (1080 * 0.85);

    /** Ratio between height and width. **/
    public static final double HEIGHT_OVER_WIDTH = (double) HEIGHT / (double) WIDTH;
    /** Half of the ratio between height and width. **/
    public static final double HEIGHT_OVER_2WIDTH = (double) HEIGHT / (2.0 * WIDTH);

    Scene scene;
    Stage stage;
    Skin skin;
    FrameBuffer buffer;
    Entity entity;
    // VR UI position with respect to the user.
    Vector3Q relativePosition = new Vector3Q();
    // 5 pointers
    Vector2 pointer = new Vector2(Float.NaN, Float.NaN);
    boolean triggerPressed = false;
    boolean mouseHit = false;
    boolean vr;
    boolean visible = false;

    ShapeRenderer shapeRenderer;
    SpriteBatch batch;
    Set<VRDevice> vrControllers;
    Map<XrControllerDevice, VRDevice> deviceToDevice;

    private GamepadGui gamepadGui;

    /** Saves the controller that last interacted with the UI, so that we can only get its input. **/
    private XrControllerDevice interactingController;

    private final Vector3D auxDouble = new Vector3D();
    private final Vector3 aux = new Vector3();
    private final Vector3D mouseP0 = new Vector3D();
    private final Vector3D mouseP1 = new Vector3D();
    private final Vector3D point = new Vector3D();
    private final Vector3D normal = new Vector3D();
    private final Vector3D p0 = new Vector3D();
    private final Vector3D p1 = new Vector3D();
    private final Vector3D intersection3D = new Vector3D();
    private final Vector3D intersection2D = new Vector3D();
    private final Matrix4D transform = new Matrix4D();
    private final Matrix4D inverse = new Matrix4D();
    private final Vector3D camDir = new Vector3D();
    private final Vector3D camUp = new Vector3D();
    private final Vector3D up = new Vector3D();
    private final Vector3D side = new Vector3D();

    public MainVRGui(Skin skin) {
        setSkin(skin);
    }

    public void initialize(AssetManager manager,
                           SpriteBatch batch) {
        setVR(Settings.settings.runtime.openXr);
        this.batch = new SpriteBatch(20, GaiaSky.instance.getGlobalResources().getSpriteShader());
        this.shapeRenderer = new ShapeRenderer(100, GaiaSky.instance.getGlobalResources().getShapeShader());
        this.shapeRenderer.setAutoShapeType(true);

        // Create controllers set.
        vrControllers = new HashSet<>();
        deviceToDevice = new HashMap<>();

        // Create buffer.
        var builder = new FrameBufferBuilder(WIDTH, HEIGHT);
        builder.addBasicColorTextureAttachment(Format.RGBA8888);
        buffer = builder.build();

        // Events.
        EventManager.instance.subscribe(this, Event.SHOW_VR_UI, Event.VR_CONTROLLER_INFO, Event.SCENE_LOADED);
    }

    @Override
    public void doneLoading(AssetManager assetManager) {

    }

    public void build(Skin skin) {
        if (stage != null) {
            stage.clear();
        } else {
            Viewport vp = new FixedScreenViewport(WIDTH, HEIGHT);
            stage = new FixedStage(vp, batch, WIDTH, HEIGHT);
        }

        if (gamepadGui == null) {
            gamepadGui = new GamepadGui(skin, Gdx.graphics, 1f, true);
            gamepadGui.initialize(stage);
        }
        gamepadGui.build();

        Table content = gamepadGui.getContent();
        content.setFillParent(true);
        content.left().top();

        stage.addActor(content);
    }


    public void update(double dt) {
        // Compute screen-space vertex coordinates for the current camera.
        if (entity != null) {
            var base = Mapper.base.get(entity);
            if (base.isVisible()) {
                // Handle controller interaction.
                if (!vr || !vrControllers.isEmpty()) {
                    var model = Mapper.model.get(entity);
                    var affine = Mapper.affine.get(entity);
                    transform.set(model.model.instance.transform);
                    inverse.set(transform).inv();
                    // Original plane in object space.
                    ICamera camera = GaiaSky.instance.getICamera();

                    point.set(0.0, 0.0, 0.0);
                    normal.set(0.0, -1.0, 0.0);
                    // Move to world space by applying transform.
                    point.mul(transform);
                    normal.mul(affine.apply(transform.idt()));
                    // Check intersection with each controller.
                    int intersecting = 0;
                    for (var device : vrControllers) {
                        if (device.device.isInitialized() && device.device.isActive()) {
                            if (!deviceToDevice.containsKey(device.device)) {
                                deviceToDevice.put(device.device, device);
                            }
                            intersecting += processDevice(device, p0.set(device.beamP0), p1.set(device.beamPn));
                        } else {
                            deviceToDevice.remove(device.device);
                        }
                    }
                    // Process mouse input.
                    if (!vr) {
                        var cam = GaiaSky.instance.getICamera();
                        aux.set(Gdx.input.getX(), Gdx.input.getY(), 0.1f);
                        CameraUtils.unproject(cam.getCamera(), aux, WIDTH, HEIGHT);

                        mouseP0.set(aux).scl(3200.0);
                        mouseP1.set(cam.getDirection()).nor().scl(10.0 * Constants.KM_TO_U).add(mouseP0);

                        var nHits = processDevice(null, p0.set(mouseP0), p1.set(mouseP1));
                        mouseHit = nHits > 0;
                        intersecting += nHits;
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
     * Processes the given VR controller, with the given start and end of its beam.
     *
     * @param device The VRDevice for this controller. If null, this is not processed as a VR device.
     * @param beamP0 The starting point of the beam.
     * @param beamP1 The end point of the beam.
     *
     * @return 1 if the intersection is successful, 0 otherwise.
     */
    private int processDevice(VRDevice device,
                              Vector3D beamP0,
                              Vector3D beamP1) {

        ICamera cam = GaiaSky.instance.getICamera();

        var intersects = IntersectorDouble.intersectLineSegmentPlane(beamP0, beamP1, normal, point, intersection3D);

        if (intersects) {
            // Use inverse transform to position on ZX plane.
            intersection2D.set(intersection3D).mul(inverse);
            double x3d = intersection2D.x;
            double v = (x3d + HEIGHT_OVER_2WIDTH) / HEIGHT_OVER_WIDTH;
            double z3d = intersection2D.z;
            double u = z3d + 0.5;

            // Update hit.
            if (device != null) {
                device.intersection.set(intersection3D);
                device.hitUI = u > 0 && u < 1 && v > 0 && v < 1;
                // Default to current.
                if (interactingController == null) {
                    interactingController = device.device;
                }
                device.interacting = device.device == interactingController;
            }

            if (device == null || (device.hitUI && device.device == interactingController)) {
                int x = (int) (u * WIDTH);
                int y = (int) (v * HEIGHT);

                pointer.x = x;
                pointer.y = HEIGHT - y;

                if (triggerPressed) {
                    stage.touchDragged(x, y, 0);
                } else {
                    stage.mouseMoved(x, y);
                }
                return 1;
            }
        }
        return 0;
    }

    /**
     * Updates the position of the VR user interface object to follow the camera around.
     */
    private void updatePosition() {
        if (entity != null) {
            var base = Mapper.base.get(entity);
            if (base.isVisible()) {
                var camera = GaiaSky.instance.getICamera();
                var body = Mapper.body.get(entity);
                var coord = Mapper.coordinates.get(entity);
                var affine = Mapper.affine.get(entity);
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
            gamepadGui.programmaticUpdate();
            gamepadGui.getContent().getColor().a = 0;
            base.visible = true;
            visible = true;
            gamepadGui.getContent().addAction(
                    Actions.sequence(
                            Actions.visible(true),
                            Actions.fadeIn(Settings.settings.program.ui.getAnimationSeconds())
                    )
            );
            if (!vr) {
                // Add processor to main input multiplexer.
                GaiaSky.instance.inputMultiplexer.addProcessor(this);
            }
            // Camera runnable to update the position of the VR UI if necessary.
            GaiaSky.instance.scripting().parkCameraRunnable("VRUI-pos-updater", this::updatePosition);
        }
    }

    private void hideVRUI(Base base) {
        if (base != null) {
            gamepadGui.getContent().getColor().a = 1;
            gamepadGui.getContent().addAction(
                    Actions.sequence(
                            Actions.fadeOut(Settings.settings.program.ui.getAnimationSeconds()),
                            Actions.run(() -> {
                                base.visible = false;
                                visible = false;
                                for (var device : vrControllers) {
                                    device.hitUI = false;
                                }
                                // Camera runnable to update the position of the VR UI if necessary.
                                GaiaSky.instance.scripting().removeRunnable("VRUI-pos-updater");
                            }),
                            Actions.visible(false)
                    ));
            if (!vr) {
                // Remove processor from main input multiplexer.
                GaiaSky.instance.inputMultiplexer.removeProcessor(this);
            }
        }
    }

    /**
     * Checks whether the quadrilateral or polygon defined by points contains the point [x,y].
     *
     * @param x      The coordinate X of the point to test.
     * @param y      The coordinate Y of the point to test.
     * @param points The points defining the polygon.
     *
     * @return Whether the point is in the polygon.
     */
    public boolean contains(int x,
                            int y,
                            Vector3[] points) {
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
    public void render(int rw,
                       int rh) {
        if (stage != null) {
            buffer.begin();
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
            stage.draw();
            buffer.end();
        }
    }

    @Override
    public void resize(int width,
                       int height) {
        if (shapeRenderer != null) {
            shapeRenderer.getProjectionMatrix().setToOrtho2D(0, 0, WIDTH, HEIGHT);
            shapeRenderer.updateMatrices();
        }
        if (batch != null) {
            batch.setProjectionMatrix(batch.getProjectionMatrix().setToOrtho2D(0, 0, WIDTH, HEIGHT));
        }
    }

    @Override
    public void resizeImmediate(int width,
                                int height) {
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
    public void setVisibilityToggles(ComponentType[] entities,
                                     ComponentTypes visible) {
    }

    @Override
    public Actor findActor(String name) {
        return null;
    }

    @Override
    public void setVR(boolean vr) {
        this.vr = vr;
    }

    @Override
    public boolean isVR() {
        return vr;
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
    public float getUnitsPerPixel() {
        return 1;
    }

    @Override
    public void setBackBufferSize(int width,
                                  int height) {

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
    public void notify(Event event,
                       Object source,
                       Object... data) {
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
                    if (vr) {
                        body.setSize(1.0);
                        body.sizeInUnitsFlag = true;
                    } else {
                        body.setSizeKm(1.0);
                    }

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

                    var rt = Mapper.render.get(entity);
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
                        // Build UI.
                        build(skin);
                        showVRUI(base);
                        // Set position and orientation
                        // ~2 meters in front of the camera, on the equatorial plane.
                        // This should NOT depend on internal units, and neither should the size!
                        ICamera camera = GaiaSky.instance.getICamera();
                        var dir = camDir.set(camera.getDirection());
                        dir.y = 0;
                        dir.nor();
                        var body = Mapper.body.get(entity);
                        body.setSize(0.95);
                        if (vr) {
                            body.pos.set(camera.getPos().cpy().add(camera.getCamera().position).add(dir.scl(0.9)));
                        } else {
                            body.pos.set(camera.getPos().cpy().add(dir.scl(2.0 * Constants.KM_TO_U)));
                        }

                        // Save relative position.
                        relativePosition.set(body.pos).sub(camera.getPos());

                        // Set new static coordinates.
                        var coord = Mapper.coordinates.get(entity);
                        ((StaticCoordinates) coord.coordinates).setPosition(body.pos);

                        // Set rotation.
                        var affine = Mapper.affine.get(entity);
                        affine.transformations.clear();
                        dir.set(camera.getCamera().direction);
                        var angle = dir.angle(new Vector3D(0, 0, 1));
                        if (dir.x < 0) {
                            angle = -angle;
                        }
                        affine.setQuaternion(new double[] { 0, 1, 0 }, angle);
                        affine.setQuaternion(new double[] { 1, 0, 0 }, 90.0);
                        affine.setQuaternion(new double[] { 0, 1, 0 }, -90.0);
                    }
                };

                r.run();
            });

        } else if (event == Event.VR_CONTROLLER_INFO) {
            vrControllers.add((VRDevice) data[0]);
        } else if (event == Event.SCENE_LOADED) {
            setScene((Scene) data[0]);
        }
    }

    public void setScene(Scene scene) {
        this.scene = scene;
    }

    public void setSkin(Skin skin) {
        this.skin = skin;
    }

    @Override
    public boolean keyDown(int keycode) {
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX,
                             int screenY,
                             int pointer,
                             int button) {
        if (!vr) {
            if (button == Input.Buttons.LEFT && mouseHit) {
                stage.touchDown((int) this.pointer.x, (int) (HEIGHT - this.pointer.y), 0, button);
                triggerPressed = true;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean touchUp(int screenX,
                           int screenY,
                           int pointer,
                           int button) {
        if (!vr) {
            if (button == Input.Buttons.LEFT && mouseHit) {
                stage.touchUp((int) this.pointer.x, (int) (HEIGHT - this.pointer.y), 0, button);
                triggerPressed = false;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX,
                                int screenY,
                                int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX,
                              int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(float amountX,
                            float amountY) {
        return false;
    }

    @Override
    public boolean showUI(boolean value,
                          XrControllerDevice device) {
        return false;
    }

    @Override
    public boolean accept(boolean value,
                          XrControllerDevice device) {
        return false;
    }

    @Override
    public boolean cameraMode(boolean value,
                              XrControllerDevice device) {
        return false;
    }

    @Override
    public boolean rotate(boolean value,
                          XrControllerDevice device) {
        return false;
    }

    @Override
    public boolean move(Vector2 value,
                        XrControllerDevice device) {
        return false;
    }

    @Override
    public boolean select(float value,
                          XrControllerDevice device) {
        if (visible) {
            if (value > 0) {
                // Pressed.
                var dev = deviceToDevice.get(device);
                if (dev.hitUI && !dev.interacting) {
                    interactingController = dev.device;
                    dev.interacting = true;
                }
                if (Float.isFinite(pointer.x) && Float.isFinite(pointer.y) /*&& device == interactingController*/) {
                    stage.touchDown((int) pointer.x, (int) (HEIGHT - pointer.y), 0, Input.Buttons.LEFT);
                    triggerPressed = true;
                    interactingController = device;
                    return true;
                }
            } else {
                // Released.
                var dev = deviceToDevice.get(device);
                if (dev.hitUI && !dev.interacting) {
                    interactingController = dev.device;
                    dev.interacting = true;
                }
                if (Float.isFinite(pointer.x) && Float.isFinite(pointer.y) && device == interactingController) {
                    stage.touchUp((int) pointer.x, (int) (HEIGHT - pointer.y), 0, Input.Buttons.LEFT);
                    triggerPressed = false;
                    return true;
                }

            }
        }
        return false;
    }
}
