package gaia.cu9.ari.gaiaorbit.scenegraph;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Method;
import com.badlogic.gdx.utils.reflect.ReflectionException;

import gaia.cu9.ari.gaiaorbit.GaiaSky;
import gaia.cu9.ari.gaiaorbit.data.group.PointDataProvider;
import gaia.cu9.ari.gaiaorbit.render.ComponentType;
import gaia.cu9.ari.gaiaorbit.render.I3DTextRenderable;
import gaia.cu9.ari.gaiaorbit.render.RenderingContext;
import gaia.cu9.ari.gaiaorbit.render.system.FontRenderSystem;
import gaia.cu9.ari.gaiaorbit.scenegraph.ParticleGroup.ParticleBean;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.scenegraph.component.GalaxydataComponent;
import gaia.cu9.ari.gaiaorbit.scenegraph.component.ModelComponent;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.coord.Coordinates;
import gaia.cu9.ari.gaiaorbit.util.gravwaves.RelativisticEffectsManager;
import gaia.cu9.ari.gaiaorbit.util.math.MathUtilsd;
import gaia.cu9.ari.gaiaorbit.util.math.Matrix4d;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;

public class MilkyWayReal extends AbstractPositionEntity implements I3DTextRenderable {
    float[] labelColour = new float[] { 1f, 1f, 1f, 1f };
    String model, transformName;
    Matrix4 coordinateSystem;

    public ModelComponent mc;

    public Array<? extends ParticleBean> starData, nebulaData;
    protected String provider;
    public GalaxydataComponent gc;

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

    public MilkyWayReal() {
        super();
        localTransform = new Matrix4();
    }

    public void initialize() {
        /** Load data **/
        PointDataProvider provider = new PointDataProvider();
        try {
            starData = provider.loadData(gc.starsource);
            nebulaData = provider.loadData(gc.nebulasource);
        } catch (Exception e) {
            Logger.error(e, getClass().getSimpleName());
        }
        mc.initialize();
        mc.env.set(new ColorAttribute(ColorAttribute.AmbientLight, cc[0], cc[1], cc[2], 1));

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
                Logger.error(this.getClass().getName(), "Error getting/invoking method Coordinates." + transformName + "()");
            }
        } else {
            // Equatorial, nothing
        }

        // Model
        mc.doneLoading(manager, localTransform, null);
        Vector3 aux = new Vector3();
        Vector3 pos3 = pos.toVector3();

        // Transform all
        for (int i = 0; i < starData.size; i++) {
            double[] pointf = starData.get(i).data;

            aux.set((float) pointf[0], (float) pointf[2], (float) pointf[1]);
            aux.scl(size).rotate(-90, 0, 1, 0).mul(coordinateSystem).add(pos3);
            pointf[0] = aux.x;
            pointf[1] = aux.y;
            pointf[2] = aux.z;
        }

        for (int i = 0; i < nebulaData.size; i++) {
            double[] pointf = nebulaData.get(i).data;
            aux.set((float) pointf[0], (float) pointf[2], (float) pointf[1]);
            aux.scl(size).rotate(-90, 0, 1, 0).mul(coordinateSystem).add(pos3);
            pointf[0] = aux.x;
            pointf[1] = aux.y;
            pointf[2] = aux.z;
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

        if (mc != null) {
            Vector3 dd = aux3f1.get();
            dd.set(0, 1, 0);
            dd.mul(coordinateSystem);

            mc.dlight.direction.set(dd);
        }

    }

    @Override
    protected void addToRenderLists(ICamera camera) {
        if ((fadeIn == null || fadeIn != null && currentDistance > fadeIn.x) && (fadeOut == null || fadeOut != null && currentDistance < fadeOut.y)) {

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
    public void render(SpriteBatch batch, ShaderProgram shader, FontRenderSystem sys, RenderingContext rc, ICamera camera) {
        Vector3d pos = aux3d1.get();
        textPosition(camera, pos);
        shader.setUniformf("u_viewAngle", 90f);
        shader.setUniformf("u_viewAnglePow", 1f);
        shader.setUniformf("u_thOverFactor", 1f);
        shader.setUniformf("u_thOverFactorScl", 1f);
        render3DLabel(batch, shader, sys.font3d, camera, rc, text(), pos, textScale() * camera.getFovFactor(), textSize() * camera.getFovFactor());
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
        return (float) distToCamera * 1e-3f;
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
        return name;
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

    public void setModel(String model) {
        this.model = model;
    }

    public void setModel(ModelComponent mc) {
        this.mc = mc;
    }
}
