/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Method;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import gaiasky.GaiaSky;
import gaiasky.data.group.PointDataProvider;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.*;
import gaiasky.render.RenderGroup;
import gaiasky.render.api.I3DTextRenderable;
import gaiasky.render.api.IBillboardDatasetProvider;
import gaiasky.render.api.IFadeObject;
import gaiasky.render.api.IStatusObject;
import gaiasky.render.system.FontRenderSystem;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.component.GalaxydataComponent;
import gaiasky.scenegraph.particle.BillboardDataset;
import gaiasky.scenegraph.particle.IParticleRecord;
import gaiasky.util.Constants;
import gaiasky.util.GlobalResources;
import gaiasky.util.Logger;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.gdx.g2d.ExtSpriteBatch;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.gravwaves.RelativisticEffectsManager;
import gaiasky.util.math.*;
import gaiasky.util.time.ITimeFrameProvider;
import gaiasky.util.tree.LoadStatus;

import java.util.List;

/**
 * The milky way model. This is obsolete, use {@link BillboardDataset} instead.
 * @deprecated Use {@link BillboardDataset} instead.
 */
@Deprecated
public class MilkyWay extends SceneGraphNode implements I3DTextRenderable, IStatusObject, IFadeObject, IBillboardDatasetProvider, IObserver {
    String transformName;
    Matrix4 coordinateSystem;

    public BillboardDataset[] datasets;

    protected String provider;
    public GalaxydataComponent gc;

    /** Status of data in the GPU **/
    private LoadStatus status = LoadStatus.NOT_LOADED;

    private Vector3d labelPosition;

    /**
     * Fade in low and high limits
     */
    private Vector2d fadeIn;

    /**
     * Fade out low and high limits
     */
    private Vector2d fadeOut;

    /**
     * The current distance at each cycle, in internal units
     */
    private double currentDistance;

    public MilkyWay() {
        super();
        localTransform = new Matrix4();
    }

    public void initialize() {
        if (datasets == null && gc != null) {
            // In order to be backwards compatible, we initialize the list of
            // datasets from the GalaxydataComponent
            datasets = gc.transformToDatasets();
        } else {
            reloadData();
        }
    }

    private boolean reloadData() {
        try {
            PointDataProvider provider = new PointDataProvider();
            boolean reload = false;
            for (BillboardDataset dataset : datasets) {
                boolean reloadNeeded = dataset.initialize(provider, reload);
                reload = reload || reloadNeeded;
            }
            return reload;
        } catch (Exception e) {
            Logger.getLogger(this.getClass()).error(e);
        }
        return false;
    }

    @Override
    public void doneLoading(AssetManager manager) {
        super.doneLoading(manager);

        transformData();

        EventManager.instance.subscribe(this, Event.GRAPHICS_QUALITY_UPDATED);
    }

    private void transformData() {
        // Set static coordinates to position
        coordinates.getEquatorialCartesianCoordinates(null, pos);

        // Initialise transform
        if (transformName != null) {
            Class<Coordinates> c = Coordinates.class;
            try {
                Method m = ClassReflection.getMethod(c, transformName);
                Matrix4d trf = (Matrix4d) m.invoke(null);

                coordinateSystem = trf.putIn(new Matrix4());

            } catch (ReflectionException e) {
                Logger.getLogger(this.getClass()).error("Error getting/invoking method Coordinates." + transformName + "()");
            }
        } else {
            // Equatorial, nothing
        }

        // Model
        Vector3 aux = new Vector3();
        Vector3 pos3 = pos.toVector3();

        // Transform all
        for (BillboardDataset bd : datasets) {
            List<IParticleRecord> a = bd.data;
            if (a != null) {
                for (int i = 0; i < a.size(); i++) {
                    IParticleRecord pr = a.get(i);

                    aux.set((float) pr.x(), (float) pr.z(), (float) pr.y());
                    aux.scl(size).rotate(-90, 0, 1, 0).mul(coordinateSystem).add(pos3);
                    pr.setPos(aux.x, aux.y, aux.z);
                }

            }
        }
    }

    public void update(ITimeFrameProvider time, final Vector3b parentTransform, ICamera camera, float opacity) {
        this.opacity = opacity * this.opacity;
        translation.set(parentTransform);
        this.currentDistance = camera.getDistance() * camera.getFovFactor();

        // Update with translation/rotation/etc
        updateLocal(time, camera);

        if (children != null && currentDistance < fadeIn.y) {
            for (int i = 0; i < children.size; i++) {
                float childOpacity = 1 - this.opacity;
                SceneGraphNode child = children.get(i);
                child.update(time, translation, camera, childOpacity);
            }
        }
    }

    @Override
    public void update(ITimeFrameProvider time, Vector3b parentTransform, ICamera camera) {
        update(time, parentTransform, camera, 1f);
    }

    @Override
    public void updateLocal(ITimeFrameProvider time, ICamera camera) {
        super.updateLocal(time, camera);

        // Update alpha
        this.opacity = this.getVisibilityOpacityFactor();
        if (fadeIn != null)
            this.opacity *= MathUtilsd.lint((float) this.currentDistance, fadeIn.x, fadeIn.y, 0, 1);
        if (fadeOut != null)
            this.opacity *= MathUtilsd.lint((float) this.currentDistance, fadeOut.x, fadeOut.y, 1, 0);

        // Directional light comes from up
        updateLocalTransform();

    }

    @Override
    protected void addToRenderLists(ICamera camera) {
        if (this.shouldRender() && (fadeIn == null || currentDistance > fadeIn.x) && (fadeOut == null || currentDistance < fadeOut.y)) {

            if (renderText()) {
                addToRender(this, RenderGroup.FONT_LABEL);
            }
            addToRender(this, RenderGroup.BILLBOARD_GROUP);
        }

    }

    /**
     * Update the local transform with the transform and the rotations/scales
     * necessary. Override if your model contains more than just the position
     * and size.
     */
    protected void updateLocalTransform() {
        // Scale + Rotate + Tilt + Translate
        translation.getMatrix(localTransform).scl(size);
        localTransform.mul(coordinateSystem);
    }

    /**
     * Label rendering.
     */
    @Override
    public void render(ExtSpriteBatch batch, ExtShaderProgram shader, FontRenderSystem sys, RenderingContext rc, ICamera camera) {
        Vector3d pos = D31.get();
        textPosition(camera, pos);
        shader.setUniformf("u_viewAngle", 90f);
        shader.setUniformf("u_viewAnglePow", 1f);
        shader.setUniformf("u_thOverFactor", 1f);
        shader.setUniformf("u_thOverFactorScl", 1f);
        render3DLabel(batch, shader, sys.fontDistanceField, camera, rc, text(), pos, distToCamera, textScale(), textSize() * camera.getFovFactor(), this.forceLabel);
    }

    public void setTransformName(String transformName) {
        this.transformName = transformName;
    }

    @Override
    public boolean renderText() {
        return GaiaSky.instance.isOn(ComponentType.Labels);
    }

    /**
     * Sets the absolute size of this entity
     *
     * @param size
     */
    public void setSize(Double size) {
        this.size = (float) (size * Constants.KM_TO_U);
    }

    public void setLabelposition(double[] labelposition) {
        this.labelPosition = new Vector3d(labelposition[0] * Constants.PC_TO_U, labelposition[1] * Constants.PC_TO_U, labelposition[2] * Constants.PC_TO_U);
    }

    @Override
    public float[] textColour() {
        return labelcolor;
    }

    @Override
    public float textSize() {
        return (float) distToCamera * 2e-3f;
    }

    @Override
    public float textScale() {
        return 3f;
    }

    @Override
    public void textPosition(ICamera cam, Vector3d out) {
        out.set(labelPosition).add(cam.getInversePos());
        GlobalResources.applyRelativisticAberration(out, cam);
        RelativisticEffectsManager.getInstance().gravitationalWavePos(out);
    }

    @Override
    public String text() {
        return getLocalizedName();
    }

    @Override
    public void textDepthBuffer() {
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(false);
    }

    @Override
    public boolean isLabel() {
        return true;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public void setGalaxydata(GalaxydataComponent gc) {
        this.gc = gc;
    }

    public void setData(Object[] data) {
        int nData = data.length;
        this.datasets = new BillboardDataset[nData];
        for (int i = 0; i < nData; i++) {
            this.datasets[i] = (BillboardDataset) data[i];
        }
    }

    /**
     * Sets the size of this entity in kilometres
     *
     * @param size The diameter of the entity
     */
    public void setSize(Float size) {
        this.size = (float) (size * Constants.KM_TO_U);
    }

    @Override
    public void updateLocalValues(ITimeFrameProvider time, ICamera camera) {
    }

    public Vector2d getFadeIn() {
        return fadeIn;
    }

    public void setFadein(double[] fadein) {
        fadeIn = new Vector2d(fadein[0] * Constants.PC_TO_U, fadein[1] * Constants.PC_TO_U);
    }

    @Override
    public void setFadeIn(double nearPc, double farPc) {
        fadeIn = new Vector2d(nearPc * Constants.PC_TO_U, farPc * Constants.PC_TO_U);
    }

    public Vector2d getFadeOut() {
        return fadeOut;
    }

    @Override
    public void setFadeOut(double nearPc, double farPc) {
        fadeOut = new Vector2d(nearPc * Constants.PC_TO_U, farPc * Constants.PC_TO_U);
    }

    public void setFadeout(double[] fadeout) {
        fadeOut = new Vector2d(fadeout[0] * Constants.PC_TO_U, fadeout[1] * Constants.PC_TO_U);
    }

    @Override
    public float getTextOpacity() {
        return getOpacity();
    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        switch (event) {
        case GRAPHICS_QUALITY_UPDATED:
            // Reload data files with new graphics setting
            boolean reloaded = reloadData();
            if (reloaded) {
                GaiaSky.postRunnable(() -> {
                    transformData();
                    EventManager.publish(Event.GPU_DISPOSE_BILLBOARD_DATASET, this);
                    this.status = LoadStatus.NOT_LOADED;
                });
            }

            break;
        default:
            break;
        }
    }

    @Override
    public LoadStatus getStatus() {
        return status;
    }

    @Override
    public void setStatus(LoadStatus status) {
        this.status = status;
    }

    @Override
    public BillboardDataset[] getDatasets() {
        return datasets;
    }
}
