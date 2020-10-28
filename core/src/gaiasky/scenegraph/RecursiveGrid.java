/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Method;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import gaiasky.GaiaSky;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.I3DTextRenderable;
import gaiasky.render.ILineRenderable;
import gaiasky.render.IModelRenderable;
import gaiasky.render.RenderingContext;
import gaiasky.render.SceneGraphRenderer.RenderGroup;
import gaiasky.render.system.FontRenderSystem;
import gaiasky.render.system.LineRenderSystem;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.component.ModelComponent;
import gaiasky.util.*;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.format.INumberFormat;
import gaiasky.util.format.NumberFormatFactory;
import gaiasky.util.gdx.IntModelBatch;
import gaiasky.util.gdx.g2d.ExtSpriteBatch;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.gdx.shader.FloatExtAttribute;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector3d;
import gaiasky.util.time.ITimeFrameProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A recursive grid which adapts to equatorial, ecliptic and galactic systems.
 *
 * @author tsagrista
 */
public class RecursiveGrid extends FadeNode implements IModelRenderable, I3DTextRenderable, ILineRenderable, IObserver {
    protected String transformName;
    public ModelComponent mc;
    private Matrix4 coordinateSystem;
    private Matrix4d coordinateSystemd;
    private Matrix4d mat4daux;
    private boolean label;

    private float[] cc;
    private float[] ccEq = ColorUtils.gRed;
    private float[] ccEcl = ColorUtils.gGreen;
    private float[] ccGal = ColorUtils.gBlue;

    private float[] ccL = ColorUtils.gYellow;

    private Pair<Double, Double> scalingFading;
    private float fovFactor;
    private List<Pair<Double, String>> annotations;

    // Mid-points of lines in refys mode
    private Vector3d p01, p02, a, b, c, d;
    private double d01, d02;

    private INumberFormat nf;

    private RenderGroup renderGroupModel = RenderGroup.MODEL_VERT_RECGRID;

    public RecursiveGrid() {
        super();
        localTransform = new Matrix4();
    }

    @Override
    public void initialize() {
        transformName = GlobalConf.scene.VISIBILITY[ComponentType.Galactic.ordinal()] ? "galacticToEquatorial" : (GlobalConf.scene.VISIBILITY[ComponentType.Ecliptic.ordinal()] ? "eclipticToEquatorial" : null);
        coordinateSystem = new Matrix4();
        coordinateSystemd = new Matrix4d();
        mat4daux = new Matrix4d();
        updateCoordinateSystem();

        nf = NumberFormatFactory.getFormatter("0.###E0");

        cc = GlobalConf.scene.VISIBILITY[ComponentType.Galactic.ordinal()] ? ccGal : (GlobalConf.scene.VISIBILITY[ComponentType.Ecliptic.ordinal()] ? ccEcl : ccEq);
        labelcolor = cc;
        label = true;
        labelPosition = new Vector3d();
        localTransform = new Matrix4();

        p01 = new Vector3d();
        p02 = new Vector3d();
        d01 = -1;
        d02 = -1;
        a = new Vector3d();
        b = new Vector3d();
        c = new Vector3d();
        d = new Vector3d();

        // Init billboard model
        mc = new ModelComponent();
        mc.setType("twofacedbillboard");
        Map<String, Object> p = new HashMap<>();
        p.put("diameter", 1d);
        mc.setParams(p);
        mc.forceInit = true;
        mc.initialize();
        mc.env.set(new ColorAttribute(ColorAttribute.AmbientLight, cc[0], cc[1], cc[2], cc[3]));
        mc.setDepthTest(GL20.GL_NONE, false);

        // Initialize annotations vectorR
        initAnnotations();
    }

    private void initAnnotations() {
        annotations = new ArrayList<>();
        annotation(1d * Constants.M_TO_U, "1 m");
        annotation(50d * Constants.M_TO_U, "50 m");
        annotation(100d * Constants.M_TO_U, "100 m");
        annotation(200d * Constants.M_TO_U, "200 m");
        annotation(500d * Constants.M_TO_U, "500 m");

        annotation(1d * Constants.KM_TO_U, "1 Km");
        annotation(10d * Constants.KM_TO_U, "10 Km");
        annotation(100d * Constants.KM_TO_U, "100 Km");
        annotation(250d * Constants.KM_TO_U, "250 Km");
        annotation(500d * Constants.KM_TO_U, "500 Km");
        annotation(1000d * Constants.KM_TO_U, "1000 Km");
        annotation(2500d * Constants.KM_TO_U, "2500 Km");
        annotation(5000d * Constants.KM_TO_U, "5000 Km");
        annotation(10000d * Constants.KM_TO_U, "10000 Km");
        annotation(75000d * Constants.KM_TO_U, "75000 Km");
        annotation(370000d * Constants.KM_TO_U, "370000 Km");
        annotation(1500000d * Constants.KM_TO_U, "1.5M Km");
        annotation(5000000d * Constants.KM_TO_U, "5M Km");
        annotation(10000000d * Constants.KM_TO_U, "10M Km");

        annotation(0.1d * Constants.AU_TO_U, "0.1 AU");
        annotation(0.5d * Constants.AU_TO_U, "0.5 AU");
        annotation(1d * Constants.AU_TO_U, "1 AU");
        annotation(2d * Constants.AU_TO_U, "2 AU");
        annotation(5d * Constants.AU_TO_U, "5 AU");
        annotation(10d * Constants.AU_TO_U, "10 AU");
        annotation(50d * Constants.AU_TO_U, "50 AU");
        annotation(100d * Constants.AU_TO_U, "100 AU");
        annotation(500d * Constants.AU_TO_U, "500 AU");
        annotation(1000d * Constants.AU_TO_U, "1000 AU");
        annotation(5000d * Constants.AU_TO_U, "5000 AU");
        annotation(10000d * Constants.AU_TO_U, "10000 AU");
        annotation(50000d * Constants.AU_TO_U, "50000 AU");

        annotation(1d * Constants.LY_TO_U, "1 ly");
        annotation(2d * Constants.LY_TO_U, "2 ly");

        annotation(1d * Constants.PC_TO_U, "1 pc");
        annotation(2.5d * Constants.PC_TO_U, "2.5 pc");
        annotation(5d * Constants.PC_TO_U, "5 pc");
        annotation(10d * Constants.PC_TO_U, "10 pc");
        annotation(25d * Constants.PC_TO_U, "25 pc");
        annotation(50d * Constants.PC_TO_U, "50 pc");
        annotation(100d * Constants.PC_TO_U, "100 pc");
        annotation(250d * Constants.PC_TO_U, "250 pc");
        annotation(500d * Constants.PC_TO_U, "500 pc");

        annotation(1000d * Constants.PC_TO_U, "1 Kpc");
        annotation(2500d * Constants.PC_TO_U, "2.5 Kpc");
        annotation(5000d * Constants.PC_TO_U, "5 Kpc");
        annotation(10000d * Constants.PC_TO_U, "10 Kpc");
        annotation(25000d * Constants.PC_TO_U, "25 Kpc");
        annotation(50000d * Constants.PC_TO_U, "50 Kpc");
        annotation(100000d * Constants.PC_TO_U, "100 Kpc");
        annotation(250000d * Constants.PC_TO_U, "250 Kpc");
        annotation(500000d * Constants.PC_TO_U, "500 Kpc");

        annotation(1000000d * Constants.PC_TO_U, "1 Mpc");
        annotation(2500000d * Constants.PC_TO_U, "2.5 Mpc");
        annotation(5000000d * Constants.PC_TO_U, "5 Mpc");
        annotation(10000000d * Constants.PC_TO_U, "10 Mpc");
        annotation(25000000d * Constants.PC_TO_U, "25 Mpc");
        annotation(50000000d * Constants.PC_TO_U, "50 Mpc");
        annotation(100000000d * Constants.PC_TO_U, "100 Mpc");
        annotation(500000000d * Constants.PC_TO_U, "500 Mpc");

        annotation(1000000000d * Constants.PC_TO_U, "1 Gpc");
        annotation(2500000000d * Constants.PC_TO_U, "2.5 Gpc");
        annotation(5000000000d * Constants.PC_TO_U, "5 Gpc");
        annotation(10000000000d * Constants.PC_TO_U, "10 Gpc");
        annotation(50000000000d * Constants.PC_TO_U, "50 Gpc");
        annotation(100000000000d * Constants.PC_TO_U, "100 Gpc");
    }

    private void annotation(double dist, String text) {
        annotations.add(new Pair<Double, String>(dist, text));
    }

    @Override
    public void doneLoading(AssetManager manager) {
        super.doneLoading(manager);

        // Model
        mc.doneLoading(GaiaSky.instance.manager, localTransform, cc);

        // Listen
        EventManager.instance.subscribe(this, Events.TOGGLE_VISIBILITY_CMD);

    }

    @Override
    protected void addToRenderLists(ICamera camera) {
        // Render group never changes
        // Add to toRender list
        if (opacity > 0) {
            addToRender(this, renderGroupModel);
            if (label) {
                addToRender(this, RenderGroup.FONT_LABEL);
            }
            if (GlobalConf.program.RECURSIVE_GRID_ORIGIN.isRefsys() && GlobalConf.program.RECURSIVE_GRID_ORIGIN_LINES && camera.getFocus() != null) {
                addToRender(this, RenderGroup.LINE);
            }
        }
    }

    @Override
    public void update(ITimeFrameProvider time, final Vector3d parentTransform, ICamera camera, float opacity) {
        this.distToCamera = getDistanceToOrigin(camera);
        this.opacity = opacity;
        if (GlobalConf.program.RECURSIVE_GRID_ORIGIN.isFocus() && camera.getFocus() != null) {
            IFocus focus = camera.getFocus();
            this.opacity *= MathUtilsd.lint(this.distToCamera, focus.getRadius() * 5d, focus.getRadius() * 50d, 0d, 1d);
        }
        this.fovFactor = camera.getFovFactor() * .75e-3f;

        updateLocalTransform(camera);
        // Distance in u_tessQuality
        scalingFading = getGridScaling(distToCamera);

        // Compute projection lines to refsys
        if (GlobalConf.program.RECURSIVE_GRID_ORIGIN.isRefsys() && GlobalConf.program.RECURSIVE_GRID_ORIGIN_LINES && camera.getFocus() != null) {
            IFocus focus = camera.getFocus();
            Vector3d cpos = aux3d1.get();
            Vector3d fpos = aux3d2.get();
            getCFPos(cpos, fpos, camera, focus);

            // Line in XZ
            getZXLine(a, b, cpos, fpos);
            d01 = p01.set(b).sub(a).len();
            p01.setLength(d01 / 2d).add(a);

            // Line in Y
            getYLine(c, d, cpos, fpos);
            d02 = p02.set(c).sub(d).len();
            p02.setLength(d02 / 2d).add(d);
        } else {
            d01 = -1;
            d02 = -1;
        }

            if (!this.copy && this.opacity > 0) {
            addToRenderLists(camera);
        }
    }

    private void updateCoordinateSystem() {
        if (transformName != null) {
            Class<Coordinates> c = Coordinates.class;
            try {
                Method m = ClassReflection.getMethod(c, transformName);
                coordinateSystemd.set((Matrix4d) m.invoke(null));
                coordinateSystemd.putIn(coordinateSystem);
            } catch (ReflectionException e) {
                Logger.getLogger(this.getClass()).error("Error getting/invoking method Coordinates." + transformName + "()");
            }
        } else {
            // Equatorial, nothing
            coordinateSystem.idt();
            coordinateSystemd.idt();
        }
    }

    private void updateLocalTransform(ICamera camera) {
        IFocus focus = camera.getFocus();
        localTransform.idt();
        if (GlobalConf.program.RECURSIVE_GRID_ORIGIN.isRefsys() || focus == null) {
            // Coordinate origin - Sun
            localTransform.translate(camera.getInversePos().put(aux3f1.get()).setLength(1));
        } else {
            // Focus object
            localTransform.translate(focus.getAbsolutePosition(aux3d1.get()).sub(camera.getPos()).setLength(1).put(aux3f1.get()));
        }
        localTransform.scl((float) (0.067d * Constants.AU_TO_U * Constants.DISTANCE_SCALE_FACTOR));
        if (coordinateSystem != null)
            localTransform.mul(coordinateSystem);

        // Must rotate due to orientation of billboard
        localTransform.rotate(1, 0, 0, 90);

    }

    private Pair<Double, Double> getGridScaling(double camdist) {
        double au = camdist * Constants.U_TO_AU;
        Pair<Double, Double> res = new Pair<>(au, 0d);

        for (int i = -25; i < 25; i++) {
            if (au < Math.pow(10, i)) {
                double fading = MathUtilsd.lint(au, Math.pow(10d, i - 1), Math.pow(10d, i), 1d, 0d);
                res.setFirst(au * Math.pow(10, -i));
                res.setSecond(fading);
                return res;
            }
        }
        return res;
    }

    private double getDistanceToOrigin(ICamera camera) {
        IFocus focus = camera.getFocus();
        if (GlobalConf.program.RECURSIVE_GRID_ORIGIN.isRefsys() || focus == null) {
            return camera.getPos().len();
        } else {
            return focus.getDistToCamera();
        }
    }

    /**
     * Model rendering.
     */
    @Override
    public void render(IntModelBatch modelBatch, float alpha, double t, RenderingContext rc) {
        mc.update(alpha * cc[3] * opacity);
        mc.setFloatExtAttribute(FloatExtAttribute.TessQuality, (float) (scalingFading.getFirst() * Constants.DISTANCE_SCALE_FACTOR));
        // Fading in u_heightScale
        mc.setFloatExtAttribute(FloatExtAttribute.HeightScale, scalingFading.getSecond().floatValue());
        // FovFactor
        mc.setFloatExtAttribute(FloatExtAttribute.Ts, this.fovFactor);
        modelBatch.render(mc.instance, mc.env);
    }

    /**
     * Label rendering.
     */
    @Override
    public void render(ExtSpriteBatch batch, ExtShaderProgram shader, FontRenderSystem sys, RenderingContext rc, ICamera camera) {
        int index = annotations.size() - 1;
        for (int i = 1; i < annotations.size(); i++) {
            if (distToCamera > annotations.get(i - 1).getFirst() && distToCamera <= annotations.get(i).getFirst()) {
                index = i;
                break;
            }
        }

        // n up, n down (if possible)
        int n = 2;
        for (int i = index - n; i < index + n; i++) {
            if (i >= 0 && i < annotations.size()) {
                // Render
                renderDistanceLabel(batch, shader, sys, rc, camera, annotations.get(i).getFirst(), annotations.get(i).getSecond());
            }
        }

        if(GlobalConf.program.RECURSIVE_GRID_ORIGIN.isRefsys() && camera.getFocus() != null && d01 > 0 && d02 > 0){
            shader.setUniform4fv("u_color", ccL, 0, 4);
            Pair<Double, String> d = GlobalResources.doubleToDistanceString(d01);
            render3DLabel(batch, shader, sys.fontDistanceField, camera, rc, nf.format(d.getFirst()) + " " + d.getSecond(), p01, textScale(), (float) (d01 * 2e-3d * camera.getFovFactor()));
            d = GlobalResources.doubleToDistanceString(d02);
            render3DLabel(batch, shader, sys.fontDistanceField, camera, rc, nf.format(d.getFirst()) + " " + d.getSecond(), p02, textScale(), (float) (d02 * 2e-3d * camera.getFovFactor()));
        }

    }

    private void renderDistanceLabel(ExtSpriteBatch batch, ExtShaderProgram shader, FontRenderSystem sys, RenderingContext rc, ICamera camera, double dist, String text) {
        shader.setUniformf("u_viewAngle", 90f);
        shader.setUniformf("u_viewAnglePow", 1);
        shader.setUniformf("u_thOverFactor", 1);
        shader.setUniformf("u_thOverFactorScl", 1);

        IFocus focus = camera.getFocus();
        Vector3d v = aux3d1.get().setZero();
        if (GlobalConf.program.RECURSIVE_GRID_ORIGIN.isFocus() && focus != null) {
            focus.getAbsolutePosition(v);
        } else {

        }

        // +Z
        labelPosition.set(0d, 0d, dist);
        labelPosition.mul(coordinateSystemd);
        labelPosition.add(v).sub(camera.getPos());
        render3DLabel(batch, shader, sys.fontDistanceField, camera, rc, text, labelPosition, textScale(), (float) (dist * 2e-3d * camera.getFovFactor()));

        // -Z
        labelPosition.set(0d, 0d, -dist);
        labelPosition.mul(coordinateSystemd);
        labelPosition.add(v).sub(camera.getPos());
        render3DLabel(batch, shader, sys.fontDistanceField, camera, rc, text, labelPosition, textScale(), (float) (dist * 2e-3d * camera.getFovFactor()));
    }

    /**
     * Line rendering.
     *
     * @param renderer The line renderer.
     * @param camera   The camera.
     * @param alpha    The alpha opacity.
     */
    @Override
    public void render(LineRenderSystem renderer, ICamera camera, float alpha) {
        // Here, we must have a focus and be in refsys mode
        IFocus focus = camera.getFocus();
        if (focus != null) {
            // Line in ZX
            renderer.addLine(this, a.x, a.y, a.z, b.x, b.y, b.z, ccL[0], ccL[1], ccL[2], ccL[3]);
            // Line in Y
            renderer.addLine(this, c.x, c.y, c.z, d.x, d.y, d.z, ccL[0], ccL[1], ccL[2], ccL[3]);
        }
    }

    private void getCFPos(Vector3d cpos, Vector3d fpos, ICamera camera, IFocus focus){
        Matrix4d inv = coordinateSystemd;
        Matrix4d trf = mat4daux.set(inv).inv();
        camera.getPos().put(cpos).mul(trf);
        focus.getAbsolutePosition(fpos).mul(trf);
        fpos.sub(cpos);
    }

    private void getZXLine(Vector3d a, Vector3d b, Vector3d cpos, Vector3d fpos){
        Matrix4d inv = coordinateSystemd;
        a.set(-cpos.x, -cpos.y, -cpos.z);
        b.set(fpos.x, -cpos.y, fpos.z);
        // Back to equatorial
        a.mul(inv);
        b.mul(inv);
    }
    private void getYLine(Vector3d a, Vector3d b, Vector3d cpos, Vector3d fpos){
        Matrix4d inv = coordinateSystemd;
        a.set(fpos.x, -cpos.y, fpos.z);
        b.set(fpos.x, fpos.y, fpos.z);
        // Back to equatorial
        a.mul(inv);
        b.mul(inv);
    }

    public void setTransformName(String transformName) {
        this.transformName = transformName;
    }

    @Override
    public boolean hasAtmosphere() {
        return false;
    }

    public void setModel(ModelComponent mc) {
        this.mc = mc;
    }

    public void setLabel(Boolean label) {
        this.label = label;
    }

    @Override
    public boolean renderText() {
        return label;
    }

    @Override
    public float[] textColour() {
        return this.cc;
    }

    @Override
    public float textSize() {
        return (float) distToCamera * 2e-3f;
    }

    @Override
    public float textScale() {
        return 1f;
    }

    @Override
    public void textPosition(ICamera cam, Vector3d out) {
    }

    @Override
    public String text() {
        return null;
    }

    @Override
    public void textDepthBuffer() {
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(true);
    }

    @Override
    public boolean isLabel() {
        return label;
    }

    @Override
    public float getTextOpacity() {
        return getOpacity();
    }

    public void setRendergroup(String rg) {
        this.renderGroupModel = RenderGroup.valueOf(rg);
    }

    @Override
    public void setSize(Double size) {
        this.size = (float) (size * Constants.DISTANCE_SCALE_FACTOR);
    }

    @Override
    public void setSize(Long size) {
        this.size = (float) (size * Constants.DISTANCE_SCALE_FACTOR);
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case TOGGLE_VISIBILITY_CMD:
            ComponentType ct = ComponentType.getFromKey((String) data[0]);
            if (ct != null && GlobalConf.scene.VISIBILITY[ct.ordinal()]) {
                if (ct.equals(ComponentType.Equatorial)) {
                    // Activate equatorial
                    transformName = null;
                    cc = ccEq;
                    labelcolor = ccEq;
                } else if (ct.equals(ComponentType.Ecliptic)) {
                    // Activate ecliptic
                    transformName = "eclipticToEquatorial";
                    cc = ccEcl;
                    labelcolor = ccEq;
                } else if (ct.equals(ComponentType.Galactic)) {
                    // Activate galactic
                    transformName = "galacticToEquatorial";
                    cc = ccGal;
                    labelcolor = ccEq;
                }
                updateCoordinateSystem();
                mc.setColorAttribute(ColorAttribute.Diffuse, cc);
            }
            break;
        default:
            break;
        }
    }

    @Override
    public float getLineWidth() {
        return 0.5f;
    }

    @Override
    public int getGlPrimitive() {
        return GL20.GL_LINES;
    }
}
