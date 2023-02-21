package gaiasky.gui.vr;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
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
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FillViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.gui.FocusInfoInterface;
import gaiasky.gui.IGui;
import gaiasky.render.BlendMode;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.RenderGroup;
import gaiasky.scene.Archetype;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.tag.TagNoClosest;
import gaiasky.scene.record.ModelComponent;
import gaiasky.util.Constants;
import gaiasky.util.coord.StaticCoordinates;
import gaiasky.util.gdx.shader.attribute.TextureAttribute;
import gaiasky.util.math.Vector3d;
import gaiasky.util.scene2d.*;

import java.util.HashMap;
import java.util.Map;

public class VRUI implements InputProcessor, IGui, IObserver, Disposable {

    private final int WIDTH = 1280;
    private final int HEIGHT = 720;

    Scene scene;
    Stage stage;
    FrameBuffer buffer;
    Texture uiTexture;
    Entity entity;
    /** The 4 vertices of the UI surface. **/
    Vector3[] vertices;
    /** Vertices transformed to be in screen coordinates. **/
    Vector3[] verticesScreenSpace;

    FocusInfoInterface focusInfoInterface;
    ShapeRenderer shapeRenderer;

    public VRUI(Scene scene) {
        this.scene = scene;
    }

    public void initialize(AssetManager manager, SpriteBatch batch) {
        // Create stage.
        Viewport vp = new FillViewport(WIDTH, HEIGHT);
        stage = new Stage(vp, batch);
        shapeRenderer = new ShapeRenderer(100, GaiaSky.instance.getGlobalResources().getShapeShader());
        shapeRenderer.setAutoShapeType(true);

        // Create buffer.
        var builder = new FrameBufferBuilder(WIDTH, HEIGHT);
        builder.addBasicColorTextureAttachment(Format.RGBA8888);
        buffer = builder.build();
        uiTexture = buffer.getColorBufferTexture();

        // Events.
        EventManager.instance.subscribe(this, Event.SHOW_VR_UI);
    }

    @Override
    public void doneLoading(AssetManager assetManager) {

    }

    public void build(Skin skin) {
        Table content = new Table(skin);
        Table t = new Table(skin);
        t.setBackground("table-bg");

        // Focus interface.
        focusInfoInterface = new FocusInfoInterface(skin);

        // Main content.
        float width = 500;
        OwnLabel title = new OwnLabel("HELLO FROM VR", skin, "header-large");

        Button button = new OwnTextButton("Button", skin, "toggle-big");
        button.setWidth(width);
        button.pad(15);
        Button buttonChecked = new OwnTextButton("Button (checked)", skin, "toggle-big");
        buttonChecked.setWidth(width);
        buttonChecked.setChecked(true);
        buttonChecked.pad(15);
        Slider slider = new OwnSliderPlus("Slider", 0, 100, 1, skin);
        slider.setWidth(width);
        slider.setValue(80);
        OwnCheckBox checkBox = new OwnCheckBox(" Checked", skin);
        checkBox.setChecked(true);
        OwnCheckBox checkBoxNo = new OwnCheckBox(" Unchecked", skin);
        checkBoxNo.setChecked(false);
        SelectBox selectBox = new OwnSelectBox(skin, "big");
        selectBox.setItems("First item", "Second item", "Third item");

        t.pad(20);
        t.add(title).padBottom(60).row();
        t.add(button).left().top().padBottom(10).row();
        t.add(buttonChecked).left().top().padBottom(10).row();
        t.add(slider).left().top().padBottom(10).row();
        t.add(checkBox).left().top().padBottom(10).row();
        t.add(checkBoxNo).left().top().padBottom(10).row();
        t.add(selectBox).left().top().padBottom(10).row();

        content.add(focusInfoInterface).left().top().padRight(20);
        content.add(t).left().top();

        content.setFillParent(true);
        content.left().top();

        stage.clear();
        stage.addActor(content);
    }

    Vector2 p = new Vector2();
    Vector2 a = new Vector2();
    Vector2 b = new Vector2();
    Vector2 c = new Vector2();
    Vector2 d = new Vector2();
    Vector2 q = new Vector2();
    Vector2 e = new Vector2();
    Vector2 f = new Vector2();
    Vector2 g = new Vector2();
    Vector2 h = new Vector2();
    Vector2 uv = new Vector2();

    public void update(double dt) {
        stage.act((float) dt);

        // Compute screen-space vertex coordinates for the current camera.
        if (entity != null) {
            ICamera iCamera = GaiaSky.instance.getICamera();
            var camera = iCamera.getCamera();
            var model = Mapper.model.get(entity);
            for (int i = 0; i < vertices.length; i++) {
                var outVert = verticesScreenSpace[i];
                var in = vertices[i];
                outVert.set(in);
                // To world space.
                outVert.mul(model.model.instance.transform);
                // To screen coordinates.
                camera.project(outVert);
            }
        }
    }

    /**
     * This function returns the projected position in VR UI coordinates for the
     * given x and y screen coordinates.
     * <p>First, the VR UI surface model is converted to world space and then
     * to screen space. This happens every frame in the update() method.
     * Then, we check whether the screen coordinates are inside
     * the VR UI area, and if so, we perform an inverse bilinear interpolation to
     * get the UV of our screen coordinates in VR UI space. Finally, we convert these
     * to pixels and return.</p>
     *
     * @param x   The X screen coordinate.
     * @param y   The y screen coordinate.
     * @param out The output vector.
     *
     * @return The coordinates in VR UI space of the given screen coordinates, if the screen coordinates collide
     * with the VR UI. Otherwise, it returns null.
     */
    public Vector2 getProjectedPosition(int x, int y, Vector2 out) {
        if (verticesScreenSpace != null) {
            float width = Gdx.graphics.getWidth();
            float height = Gdx.graphics.getHeight();
            y = (int) height - y;

            var in = contains(x, y, verticesScreenSpace);
            if (in) {
                p.set(x / width, y / height);
                a.set(verticesScreenSpace[0].x / width, verticesScreenSpace[0].y / height);
                b.set(verticesScreenSpace[1].x / width, verticesScreenSpace[1].y / height);
                c.set(verticesScreenSpace[2].x / width, verticesScreenSpace[2].y / height);
                d.set(verticesScreenSpace[3].x / width, verticesScreenSpace[3].y / height);

                e.set(b).sub(a);
                f.set(d).sub(a);
                g.set(a).sub(b).add(c).sub(d);
                h.set(p).sub(a);

                float k2 = g.crs(f);
                float k1 = e.crs(f) + h.crs(g);
                float k0 = h.crs(e);

                if (Math.abs(k2) < 0.001) {
                    // Linear form, edges are parallel.
                    uv.x = (h.x * k1 + f.x * k0) / (e.x * k1 - g.x * k0);
                    uv.y = -k0 / k1;
                } else {
                    // Quadratic form.
                    float w = k1 * k1 - 4.0f * k0 * k2;
                    if (w < 0.0) {
                        uv.set(-1, -1);
                    } else {
                        w = (float) Math.sqrt(w);

                        float ik2 = 0.5f / k2;
                        float v = (-k1 - w) * ik2;
                        float u = (h.x - f.x * v) / (e.x + g.x * v);

                        if (u < 0.0 || u > 1.0 || v < 0.0 || v > 1.0) {
                            v = (-k1 + w) * ik2;
                            u = (h.x - f.x * v) / (e.x + g.x * v);
                        }
                        uv.set(1 - u, 1 - v);
                    }
                }

                int xUI = (int) (uv.x * width);
                int yUI = (int) (height - uv.y * height);

                return out.set(xUI, yUI);
            }
        }
        return null;
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
    public boolean contains(int x, int y, Vector3[] points) {
        int i;
        int j;
        boolean result = false;
        for (i = 0, j = points.length - 1; i < points.length; j = i++) {
            if ((points[i].y > y) != (points[j].y > y) &&
                    (x < (points[j].x - points[i].x) * (y - points[i].y) / (points[j].y - points[i].y) + points[i].x)) {
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

                    // Get vertices.
                    this.vertices = new Vector3[4];
                    this.verticesScreenSpace = new Vector3[4];
                    int vertSize = 14;
                    var verts = model.model.instance.model.meshes.get(0).getVertices(new float[vertSize * 4]);
                    for (int i = 0; i < vertices.length; i++) {
                        vertices[i] = new Vector3(verts[i * vertSize], 0, verts[i * vertSize + 2]);
                        verticesScreenSpace[i] = new Vector3();
                    }
                    // Swap 0 and 1 to create correct order for polygon.
                    var aux = vertices[0];
                    vertices[0] = vertices[1];
                    vertices[1] = aux;

                    // Add to scene.
                    EventManager.publish(Event.SCENE_ADD_OBJECT_NO_POST_CMD, this, entity, true);
                }

                Runnable r = () -> {
                    // Set position and orientation
                    // 10 meters in front of the camera, on the equatorial plane.
                    ICamera camera = GaiaSky.instance.getICamera();
                    var dir = camera.getDirection().cpy();
                    dir.y = 0;
                    dir.nor();
                    var body = Mapper.body.get(entity);
                    body.pos.set(camera.getPos().cpy().add(dir.scl(150 * Constants.KM_TO_U)));
                    body.pos.add(0, 80.0 * Constants.KM_TO_U, 0);

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
                };

                r.run();
            });

        }
    }

    private final Vector2 aux = new Vector2();

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
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        var pos = getProjectedPosition(screenX, screenY, aux);
        if (pos != null) {
            stage.touchDown((int) pos.x, (int) pos.y, pointer, button);
            return button == Input.Buttons.LEFT;
        }
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        var pos = getProjectedPosition(screenX, screenY, aux);
        if (pos != null) {
            stage.touchUp((int) pos.x, (int) pos.y, pointer, button);
            return button == Input.Buttons.LEFT;
        }
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        var pos = getProjectedPosition(screenX, screenY, aux);
        if (pos != null) {
            stage.touchDragged((int) pos.x, (int) pos.y, pointer);
            return Gdx.input.isButtonPressed(Input.Buttons.LEFT) && stage.getKeyboardFocus() != null && !(stage.getKeyboardFocus() instanceof Slider);
        }
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        var pos = getProjectedPosition(screenX, screenY, aux);
        if (pos != null) {
            stage.mouseMoved((int) pos.x, (int) pos.y);
        }
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        return false;
    }
}
