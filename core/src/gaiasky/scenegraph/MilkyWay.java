/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Method;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import gaiasky.GaiaSky;
import gaiasky.data.group.PointDataProvider;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.I3DTextRenderable;
import gaiasky.render.RenderingContext;
import gaiasky.render.system.FontRenderSystem;
import gaiasky.scenegraph.ParticleGroup.ParticleBean;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.component.GalaxydataComponent;
import gaiasky.scenegraph.component.ModelComponent;
import gaiasky.util.Constants;
import gaiasky.util.GlobalResources;
import gaiasky.util.Logger;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.gdx.g2d.ExtSpriteBatch;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.gravwaves.RelativisticEffectsManager;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector3d;
import gaiasky.util.time.ITimeFrameProvider;
import gaiasky.util.tree.LoadStatus;

public class MilkyWay extends AbstractPositionEntity implements I3DTextRenderable {
    float[] labelColour = new float[] { 1f, 1f, 1f, 1f };
    String transformName;
    Matrix4 coordinateSystem;

    public Array<? extends ParticleBean> starData, bulgeData, dustData, hiiData, gasData;
    protected String provider;
    public GalaxydataComponent gc;

    /** Status of data in the GPU **/
    public LoadStatus status = LoadStatus.NOT_LOADED;

    private Vector3d labelPosition;

    /**
     * Fade in low and high limits
     */
    private Vector2 fadeIn;

    /**
     * Fade out low and high limits
     */
    private Vector2 fadeOut;

    /**
     * The current distance at each cycle, in internal units
     */
    private double currentDistance;

    public MilkyWay() {
        super();
        localTransform = new Matrix4();
    }

    public void initialize() {
        /** Load data **/
        PointDataProvider provider = new PointDataProvider();
        try {
            if (gc.starsource != null)
                starData = provider.loadData(gc.starsource);
            if (gc.bulgesource != null)
                bulgeData = provider.loadData(gc.bulgesource);
            if (gc.dustsource != null)
                dustData = provider.loadData(gc.dustsource);
            if (gc.hiisource != null)
                hiiData = provider.loadData(gc.hiisource);
            if (gc.gassource != null)
                gasData = provider.loadData(gc.gassource);
        } catch (Exception e) {
            Logger.getLogger(this.getClass()).error(e);
        }

    }

    @Override
    public void doneLoading(AssetManager manager) {
        super.doneLoading(manager);

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

        Array<? extends ParticleBean>[] all = new Array[]{starData, hiiData, dustData, bulgeData, gasData};

        // Transform all
        for(Array<? extends ParticleBean> a : all){
            if(a != null){
                for (int i = 0; i < a.size; i++) {
                    double[] pointf = a.get(i).data;

                    aux.set((float) pointf[0], (float) pointf[2], (float) pointf[1]);
                    aux.scl(size).rotate(-90, 0, 1, 0).mul(coordinateSystem).add(pos3);
                    pointf[0] = aux.x;
                    pointf[1] = aux.y;
                    pointf[2] = aux.z;
                }

            }
        }
    }

    public void update(ITimeFrameProvider time, final Vector3d parentTransform, ICamera camera, float opacity) {
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
    public void update(ITimeFrameProvider time, Vector3d parentTransform, ICamera camera) {
        update(time, parentTransform, camera, 1f);
    }

    @Override
    public void updateLocal(ITimeFrameProvider time, ICamera camera) {
        super.updateLocal(time, camera);

        // Update alpha
        this.opacity = 1;
        if (fadeIn != null)
            this.opacity *= MathUtilsd.lint((float) this.currentDistance, fadeIn.x, fadeIn.y, 0, 1);
        if (fadeOut != null)
            this.opacity *= MathUtilsd.lint((float) this.currentDistance, fadeOut.x, fadeOut.y, 1, 0);

        // Directional light comes from up
        updateLocalTransform();

    }

    @Override
    protected void addToRenderLists(ICamera camera) {
        if ((fadeIn == null || currentDistance > fadeIn.x) && (fadeOut == null || currentDistance < fadeOut.y)) {

            if (renderText()) {
                addToRender(this, RenderGroup.FONT_LABEL);
            }
            addToRender(this, RenderGroup.GALAXY);
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
        Vector3d pos = aux3d1.get();
        textPosition(camera, pos);
        shader.setUniformf("u_viewAngle", 90f);
        shader.setUniformf("u_viewAnglePow", 1f);
        shader.setUniformf("u_thOverFactor", 1f);
        shader.setUniformf("u_thOverFactorScl", 1f);
        render3DLabel(batch, shader, sys.fontDistanceField, camera, rc, text(), pos, textScale(), textSize() * camera.getFovFactor());
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

    public void setFadein(double[] fadein) {
        fadeIn = new Vector2((float) (fadein[0] * Constants.PC_TO_U), (float) (fadein[1] * Constants.PC_TO_U));
    }

    public void setFadeout(double[] fadeout) {
        fadeOut = new Vector2((float) (fadeout[0] * Constants.PC_TO_U), (float) (fadeout[1] * Constants.PC_TO_U));
    }

    public void setLabelposition(double[] labelposition) {
        this.labelPosition = new Vector3d(labelposition[0] * Constants.PC_TO_U, labelposition[1] * Constants.PC_TO_U, labelposition[2] * Constants.PC_TO_U);
    }

    @Override
    public float[] textColour() {
        return labelColour;
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
        return names[0];
    }

    @Override
    public void textDepthBuffer() {
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(false);
    }

    public void setLabelcolor(double[] labelcolor) {
        this.labelColour = GlobalResources.toFloatArray(labelcolor);

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

    /**
     * Sets the size of this entity in kilometres
     * 
     * @param size
     *            The diameter of the entity
     */
    public void setSize(Float size) {
        this.size = (float) (size * Constants.KM_TO_U);
    }

    @Override
    public void updateLocalValues(ITimeFrameProvider time, ICamera camera) {
    }

    public void setModel(ModelComponent mc) {
    }

    public Vector2 getFadeIn() {
        return fadeIn;
    }

    public Vector2 getFadeOut() {
        return fadeOut;
    }

    @Override
    public float getTextOpacity(){
        return getOpacity();
    }
}
