/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.system;

import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Method;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import gaiasky.GaiaSky;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.IRenderable;
import gaiasky.render.RenderingContext;
import gaiasky.render.SceneGraphRenderer.RenderGroup;
import gaiasky.scenegraph.IFocus;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.component.ModelComponent;
import gaiasky.util.Constants;
import gaiasky.util.GlobalConf;
import gaiasky.util.Logger;
import gaiasky.util.Pair;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.gdx.IntModelBatch;
import gaiasky.util.gdx.shader.FloatExtAttribute;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector3d;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders with a given model batch.
 *
 * @author Toni Sagrista
 */
public class SqGridRenderSystem extends ModelBatchRenderSystem implements IObserver {

    private ModelComponent mc;
    private Matrix4 localTransform;
    private String transformName;
    private Vector3d aux3d;
    private Vector3 aux3;
    private ComponentTypes ct;
    private float opacity;
    private float[] cc;
    private float[] ccEq = new float[] { 0.9f, 0.3f, 0.2f, 1f };
    private float[] ccEcl = new float[] { 0.2f, 0.9f, 0.3f, 1f };
    private float[] ccGal = new float[] { 0.2f, 0.3f, 0.9f, 1f };

    /**
     * Creates a new model batch render component.
     *
     * @param rg     The render group.
     * @param alphas The alphas list.
     * @param batch  The model batch.
     * @param type   The model render type
     */
    public SqGridRenderSystem(RenderGroup rg, float[] alphas, IntModelBatch batch, ModelRenderType type) {
        super(rg, alphas, batch, type);

        ct = new ComponentTypes(ComponentType.RecursiveGrid);

        // Init transform and color
        transformName = GlobalConf.scene.VISIBILITY[ComponentType.Galactic.ordinal()] ? "galacticToEquatorial" : (GlobalConf.scene.VISIBILITY[ComponentType.Ecliptic.ordinal()] ? "eclipticToEquatorial" : null);
        cc = GlobalConf.scene.VISIBILITY[ComponentType.Galactic.ordinal()] ? ccGal : (GlobalConf.scene.VISIBILITY[ComponentType.Ecliptic.ordinal()] ? ccEcl : ccEq);
        localTransform = new Matrix4();

        // Init billboard model
        mc = new ModelComponent();
        mc.setType("twofacedbillboard");
        Map<String, Object> p = new HashMap<>();
        p.put("diameter", 1d);
        mc.setParams(p);
        mc.forceInit = true;
        mc.initialize();
        mc.env.set(new ColorAttribute(ColorAttribute.AmbientLight, cc[0], cc[1], cc[2], cc[3]));

        aux3d = new Vector3d();
        aux3 = new Vector3();

        // Model
        mc.doneLoading(GaiaSky.instance.manager, localTransform, cc);

        // Listen
        EventManager.instance.subscribe(this, Events.TOGGLE_VISIBILITY_CMD);
    }

    private void updateLocalTransform(ICamera camera) {
        IFocus focus = camera.getFocus();
        localTransform.idt();
        if (focus == null) {
            // Coordinate origin - Sun
            localTransform.translate(camera.getInversePos().put(aux3).setLength(1));
        } else {
            // Focus object
            localTransform.translate(focus.getAbsolutePosition(aux3d).sub(camera.getPos()).setLength(1).put(aux3));
        }
        localTransform.scl((float) (0.067d * Constants.AU_TO_U * Constants.DISTANCE_SCALE_FACTOR));
        if (transformName != null) {
            Class<Coordinates> c = Coordinates.class;
            try {
                Method m = ClassReflection.getMethod(c, transformName);
                Matrix4d trf = (Matrix4d) m.invoke(null);
                Matrix4 aux = trf.putIn(new Matrix4());
                localTransform.mul(aux);
            } catch (ReflectionException e) {
                Logger.getLogger(this.getClass()).error("Error getting/invoking method Coordinates." + transformName + "()");
            }
        } else {
            // Equatorial, nothing
        }

        // Must rotate due to orientation of billboard?
        localTransform.rotate(1, 0, 0, 90);

    }

    @Override
    public void render(List<IRenderable> renderables, ICamera camera, double t, RenderingContext rc) {
        opacity = getAlpha(ct);
        if (opacity > 0) {
            // Update
            updateLocalTransform(camera);

            this.rc = rc;
            run(preRunnables, null, camera);
            renderStud(null, camera, t);
            run(postRunnables, null, camera);
        }
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
        if (focus == null) {
            return camera.getPos().put(aux3).len();
        } else {
            return focus.getDistToCamera();
        }
    }

    @Override
    public void renderStud(List<IRenderable> renderables, ICamera camera, double t) {
        if (mustRender()) {
            batch.begin(camera.getCamera());
            mc.update(opacity);
            // Distance in u_tessQuality
            Pair<Double, Double> scalingFading = getGridScaling(getDistanceToOrigin(camera));
            mc.setFloatExtAttribute(FloatExtAttribute.TessQuality, (float) (scalingFading.getFirst() * Constants.DISTANCE_SCALE_FACTOR));
            // Fading in u_heightScale
            mc.setFloatExtAttribute(FloatExtAttribute.HeightScale, scalingFading.getSecond().floatValue());
            batch.render(mc.instance, mc.env);
            batch.end();
        }
    }

    protected boolean mustRender() {
        return true;
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
                } else if (ct.equals(ComponentType.Ecliptic)) {
                    // Activate ecliptic
                    transformName = "eclipticToEquatorial";
                    cc = ccEcl;
                } else if (ct.equals(ComponentType.Galactic)) {
                    // Activate galactic
                    transformName = "galacticToEquatorial";
                    cc = ccGal;
                }
                mc.setColorAttribute(ColorAttribute.Diffuse, cc);
            }
            break;
        default:
            break;
        }

    }
}
