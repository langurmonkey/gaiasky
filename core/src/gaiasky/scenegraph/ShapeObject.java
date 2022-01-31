/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.I3DTextRenderable;
import gaiasky.render.IModelRenderable;
import gaiasky.render.RenderingContext;
import gaiasky.render.SceneGraphRenderer.RenderGroup;
import gaiasky.render.system.FontRenderSystem;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.camera.NaturalCamera;
import gaiasky.scenegraph.component.ModelComponent;
import gaiasky.scenegraph.component.RotationComponent;
import gaiasky.util.GlobalResources;
import gaiasky.util.ModelCache;
import gaiasky.util.Pair;
import gaiasky.util.Settings;
import gaiasky.util.gdx.IntModelBatch;
import gaiasky.util.gdx.g2d.ExtSpriteBatch;
import gaiasky.util.gdx.model.IntModel;
import gaiasky.util.gdx.model.IntModelInstance;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.gravwaves.RelativisticEffectsManager;
import gaiasky.util.math.Quaterniond;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;
import gaiasky.util.time.ITimeFrameProvider;
import net.jafama.FastMath;

import java.util.Locale;
import java.util.Map;

public class ShapeObject extends SceneGraphNode implements IFocus, IModelRenderable, I3DTextRenderable {

    private ModelComponent mc;

    private IntModel model;
    private String modelShape;
    private Map<String, Object> modelParams;
    private int primitiveType;
    private boolean showLabel;

    private IFocus track;
    private String trackName;

    public ShapeObject(String[] names, String parentName, IFocus track, String trackName, boolean showLabel) {
        super();
        this.parentName = parentName;
        this.setNames(names);
        this.track = track;
        this.trackName = trackName;
        this.showLabel = showLabel;
    }

    public ShapeObject(String[] names, String parentName, Vector3b pos, String trackName, boolean showLabel) {
        super();
        this.parentName = parentName;
        this.setNames(names);
        this.pos = pos;
        this.trackName = trackName;
        this.showLabel = showLabel;
    }

    public ShapeObject(String[] names, String parentName, Vector3b pos, String trackName, boolean showLabel, float[] color) {
        this(names, parentName, pos, trackName, showLabel);
        this.setColor(color);
    }

    public ShapeObject(String[] names, String parentName, IFocus track, String trackName, boolean showLabel, float[] color) {
        this(names, parentName, track, trackName, showLabel);
        this.setColor(color);
    }

    public void setModel(String shape, int primitiveType, Map<String, Object> params) {
        this.modelShape = shape;
        this.modelParams = params;
        this.primitiveType = primitiveType;
    }

    public void initModel() {
        this.localTransform = new Matrix4();
        if (model == null) {
            Pair<IntModel, Map<String, Material>> m = ModelCache.cache.getModel(modelShape, modelParams, Usage.Position, primitiveType);
            model = m.getFirst();
            for (Map.Entry<String, Material> material : m.getSecond().entrySet()) {
                material.getValue().set(new BlendingAttribute(GL20.GL_ONE, GL20.GL_ONE));
                material.getValue().set(new ColorAttribute(ColorAttribute.Diffuse, cc[0], cc[1], cc[2], cc[3]));
            }

            mc = new ModelComponent(false);
            mc.initialize(null, 0L);
            DirectionalLight dLight = new DirectionalLight();
            dLight.set(1, 1, 1, 1, 1, 1);
            mc.env = new Environment();
            mc.env.add(dLight);
            mc.env.set(new ColorAttribute(ColorAttribute.AmbientLight, 1.0f, 1.0f, 1.0f, 1f));
            mc.env.set(new FloatAttribute(FloatAttribute.Shininess, 0.2f));
            mc.instance = new IntModelInstance(model, new Matrix4());

            // Relativistic effects
            if (Settings.settings.runtime.relativisticAberration)
                mc.rec.setUpRelativisticEffectsMaterial(mc.instance.materials);
            // Gravitational waves
            if (Settings.settings.runtime.gravitationalWaves)
                mc.rec.setUpGravitationalWavesMaterial(mc.instance.materials);
        }
    }

    public void initialize() {
    }

    @Override
    public void doneLoading(AssetManager manager) {
        initModel();
    }

    @Override
    public void setColor(double[] color) {
        super.setColor(color);
    }

    @Override
    public void setColor(float[] color) {
        super.setColor(color);
    }

    /**
     * Updates the local transform matrix.
     *
     * @param time   The time frame provider.
     * @param camera The camera.
     */
    @Override
    public void updateLocal(ITimeFrameProvider time, ICamera camera) {
        if (track != null) {
            track.getAbsolutePosition(trackName.toLowerCase(Locale.ROOT), pos);
        }
        // Update pos, local transform
        this.translation.add(pos);

        this.localTransform.idt().translate(this.translation.put(aux3f1.get())).scl(this.size);

        Vector3d aux = aux3d1.get();
        this.distToCamera = (float) aux.set(translation).len();
        this.viewAngle = (float) FastMath.atan(size / distToCamera);
        this.viewAngleApparent = this.viewAngle / camera.getFovFactor();
        if (!copy) {
            addToRenderLists(camera);
        }

        this.opacity *= 0.5f * this.getVisibilityOpacityFactor();
    }

    @Override
    public void updateLocalValues(ITimeFrameProvider time, ICamera camera) {
    }

    @Override
    protected void addToRenderLists(ICamera camera) {
        if (this.shouldRender()) {
            addToRender(this, RenderGroup.MODEL_VERT_ADDITIVE);
            if (showLabel)
                addToRender(this, RenderGroup.FONT_LABEL);
        }
    }

    @Override
    public Vector3b getAbsolutePosition(Vector3b aux) {
        aux.set(pos);
        SceneGraphNode entity = this;
        while (entity.parent != null) {
            entity = entity.parent;
            aux.add(entity.pos);
        }
        return aux;
    }

    /**
     * Model rendering
     */
    @Override
    public void render(IntModelBatch modelBatch, float alpha, double t, RenderingContext rc, RenderGroup group) {
        mc.update(null, alpha * opacity * cc[3], GL20.GL_ONE, GL20.GL_ONE);
        // Depth reads, no depth writes
        mc.setDepthTest(GL20.GL_LEQUAL, false);
        Gdx.gl20.glLineWidth(1.5f);
        mc.instance.transform.set(this.localTransform);
        modelBatch.render(mc.instance, mc.env);

    }

    /**
     * Label rendering
     */
    @Override
    public void render(ExtSpriteBatch batch, ExtShaderProgram shader, FontRenderSystem sys, RenderingContext rc, ICamera camera) {
        Vector3d pos = aux3d1.get();
        textPosition(camera, pos);
        shader.setUniformf("u_viewAngle", (float) this.viewAngle * 500f);
        shader.setUniformf("u_viewAnglePow", 1f);
        shader.setUniformf("u_thOverFactor", 1f);
        shader.setUniformf("u_thOverFactorScl", 1f);

        render3DLabel(batch, shader, sys.fontDistanceField, camera, rc, text(), pos, distToCamera, textScale() * camera.getFovFactor(), textSize() * camera.getFovFactor(), this.forceLabel);
    }

    @Override
    public boolean hasAtmosphere() {
        return false;
    }

    @Override
    public boolean renderText() {
        return names != null && GaiaSky.instance.isOn(ComponentType.Labels) && this.opacity > 0;
    }

    @Override
    public float[] textColour() {
        return labelcolor;
    }

    @Override
    public float textSize() {
        return (float) distToCamera * .5e-3f;
    }

    @Override
    public float textScale() {
        return 0.2f;
    }

    @Override
    public void textPosition(ICamera cam, Vector3d out) {
        out.set(translation);
        double len = out.len();
        out.clamp(0, len - getRadius()).scl(0.9f);
        out.x += getRadius() * 0.5;

        Vector3d aux = aux3d2.get();
        aux.set(cam.getUp());

        aux.crs(out).nor();

        float dist = -0.015f * (float) out.len();

        aux.add(cam.getUp()).nor().scl(dist);

        out.add(aux);
        GlobalResources.applyRelativisticAberration(out, cam);
        RelativisticEffectsManager.getInstance().gravitationalWavePos(out);
    }

    @Override
    public String text() {
        return names[0];
    }

    @Override
    public void textDepthBuffer() {
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(false);
    }

    @Override
    public boolean isLabel() {
        return true;
    }

    public IFocus getTrack() {
        return track;
    }

    public String getTrackName() {
        return trackName;
    }

    @Override
    public long getCandidateId() {
        return id;
    }

    @Override
    public String getCandidateName() {
        return names[0];
    }

    @Override
    public boolean isActive() {
        return GaiaSky.instance.isOn(ct) && this.opacity > 0;
    }

    /**
     * Adds all the children that are focusable objects to the list.
     *
     * @param list The list of focusable objects.
     */
    public void addFocusableObjects(Array<IFocus> list) {
        list.add(this);
        super.addFocusableObjects(list);
    }

    @Override
    public double getCandidateViewAngleApparent() {
        return this.viewAngleApparent;
    }

    @Override
    public float getAppmag() {
        return 0f;
    }

    @Override
    public float getAbsmag() {
        return 0f;
    }

    @Override
    public RotationComponent getRotationComponent() {
        return null;
    }

    @Override
    public Quaterniond getOrientationQuaternion() {
        return null;
    }

    @Override
    public void addHit(int screenX, int screenY, int w, int h, int pxdist, NaturalCamera camera, Array<IFocus> hits) {
    }

    @Override
    public void addHit(Vector3d p0, Vector3d p1, NaturalCamera camera, Array<IFocus> hits) {
    }

    @Override
    public void makeFocus() {
    }

    @Override
    public IFocus getFocus(String name) {
        return this;
    }

    @Override
    public String getClosestName() {
        return getName();
    }

    @Override
    public double getClosestDistToCamera() {
        return getDistToCamera();
    }

    @Override
    public Vector3b getClosestAbsolutePos(Vector3b out) {
        return getAbsolutePosition(out);
    }

    @Override
    public boolean isCoordinatesTimeOverflow() {
        return false;
    }

    @Override
    public float getTextOpacity() {
        return getOpacity();
    }

    @Override
    public <T extends SceneGraphNode> T getSimpleCopy() {
        ShapeObject copy = super.getSimpleCopy();
        copy.localTransform.set(this.localTransform);
        return (T) copy;
    }

}
