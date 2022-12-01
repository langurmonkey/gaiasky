package gaiasky.scene.system.update;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.math.Vector3;
import gaiasky.GaiaSky;
import gaiasky.scene.Mapper;
import gaiasky.scene.api.IFocus;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.camera.NaturalCamera;
import gaiasky.scene.component.Body;
import gaiasky.scene.component.GraphNode;
import gaiasky.scene.component.GridRecursive;
import gaiasky.scene.component.RefSysTransform;
import gaiasky.util.Constants;
import gaiasky.util.Pair;
import gaiasky.util.Settings;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;

public class GridRecUpdater extends AbstractUpdateSystem {

    private final Vector3d D33, D34;
    private final Vector3 F31, F34;
    private final Vector3b B31;

    private final Matrix4d mat4daux;

    public GridRecUpdater(Family family, int priority) {
        super(family, priority);

        D33 = new Vector3d();
        D34 = new Vector3d();
        F31 = new Vector3();
        F34 = new Vector3();
        B31 = new Vector3b();
        mat4daux = new Matrix4d();
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        updateEntity(entity, deltaTime);
    }

    @Override
    public void updateEntity(Entity entity, float deltaTime) {
        ICamera camera = GaiaSky.instance.getICamera();

        var body = Mapper.body.get(entity);
        var base = Mapper.base.get(entity);
        var graph = Mapper.graph.get(entity);
        var fade = Mapper.fade.get(entity);
        var gr = Mapper.gridRec.get(entity);
        var transform = Mapper.transform.get(entity);

        body.distToCamera = getDistanceToOrigin(camera);
        fade.currentDistance = body.distToCamera;
        gr.regime = body.distToCamera * Constants.DISTANCE_SCALE_FACTOR > 5e7 * Constants.PC_TO_U ? (byte) 2 : (byte) 1;
        if (Settings.settings.program.recursiveGrid.origin.isFocus() && camera.hasFocus()) {
            // Baked fade-in as we get close to focus
            IFocus focus = camera.getFocus();
            base.opacity *= MathUtilsd.lint(body.distToCamera, focus.getRadius() * 4d, focus.getRadius() * 10d, 0d, 1d);
        }

        gr.fovFactor = camera.getFovFactor() * .75e-3f;

        updateLocalTransform(camera, body, gr, graph, transform);
        // Distance in u_tessQuality
        getGridScaling(body.distToCamera, gr.scalingFading);

        // Compute projection lines to refsys
        if (Settings.settings.program.recursiveGrid.origin.isRefSys() && Settings.settings.program.recursiveGrid.projectionLines && camera.hasFocus()) {
            IFocus focus = camera.getFocus();
            Vector3d cpos = D33;
            Vector3d fpos = D34;
            getCFPos(cpos, fpos, camera, focus, transform);

            // Line in XZ
            getZXLine(gr.a, gr.b, cpos, fpos, transform);
            gr.d01 = gr.p01.set(gr.b).sub(gr.a).len();
            gr.p01.setLength(gr.d01 / 2d).add(gr.a);

            // Line in Y
            getYLine(gr.c, gr.d, cpos, fpos, transform);
            gr.d02 = gr.p02.set(gr.c).sub(gr.d).len();
            gr.p02.setLength(gr.d02 / 2d).add(gr.d);
        } else {
            gr.d01 = -1;
            gr.d02 = -1;
        }

    }

    private void updateLocalTransform(ICamera camera, Body body, GridRecursive gr, GraphNode graph, RefSysTransform transform) {
        IFocus focus = camera.getFocus();
        graph.localTransform.idt();

        Vector3 vroffset = F34;
        float vrScl = 1f;
        if (Settings.settings.runtime.openVr) {
            vrScl = 100f;
            if (camera.getCurrent() instanceof NaturalCamera) {
                ((NaturalCamera) camera.getCurrent()).vrOffset.put(vroffset);
                vroffset.scl((float) (1f / Constants.M_TO_U));
            }
        } else {
            vroffset.set(0, 0, 0);
        }

        if (Settings.settings.program.recursiveGrid.origin.isRefSys() || focus == null) {
            // Coordinate origin - Sun
            if (gr.regime == 1)
                graph.localTransform.translate(camera.getInversePos().put(F31));
            else
                graph.localTransform.translate(camera.getInversePos().put(F31).setLength(vrScl).add(vroffset));
        } else {
            // Focus object
            if (gr.regime == 1)
                graph.localTransform.translate(focus.getAbsolutePosition(B31).sub(camera.getPos()).put(F31));
            else
                graph.localTransform.translate(focus.getAbsolutePosition(B31).sub(camera.getPos()).setLength(vrScl).add(vroffset).put(F31));
        }
        if (gr.regime == 1)
            graph.localTransform.scl((float) (body.distToCamera * 0.067d * Constants.AU_TO_U / Constants.DISTANCE_SCALE_FACTOR));
        else
            graph.localTransform.scl((float) (0.067f * vrScl * Constants.AU_TO_U / Constants.DISTANCE_SCALE_FACTOR));

        if (transform.matrixf != null)
            graph.localTransform.mul(transform.matrixf);

        // Must rotate due to orientation of billboard
        graph.localTransform.rotate(1, 0, 0, 90);

    }

    private Pair<Double, Double> getGridScaling(double camdist, Pair<Double, Double> res) {
        double au = camdist * Constants.U_TO_AU;
        res.set(au, 0d);

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
        if (Settings.settings.program.recursiveGrid.origin.isRefSys() || !camera.hasFocus()) {
            return camera.getPos().lend();
        } else {
            IFocus focus = camera.getFocus();
            return focus.getDistToCamera();
        }
    }

    private void getCFPos(Vector3d cpos, Vector3d fpos, ICamera camera, IFocus focus, RefSysTransform tr) {
        Matrix4d inv = tr.matrix;
        Matrix4d trf = inv != null ? mat4daux.set(inv).inv() : mat4daux.idt();
        camera.getPos().put(cpos).mul(trf);
        Vector3b v3b = new Vector3b(fpos);
        focus.getPredictedPosition(v3b, GaiaSky.instance.time, camera, false).mul(trf);
        v3b.put(fpos).sub(cpos);
    }

    private void getZXLine(Vector3d a, Vector3d b, Vector3d cpos, Vector3d fpos, RefSysTransform tr) {
        Matrix4d inv = tr.matrix;
        a.set(-cpos.x, -cpos.y, -cpos.z);
        b.set(fpos.x, -cpos.y, fpos.z);
        if (inv != null) {
            // Back to equatorial
            a.mul(inv);
            b.mul(inv);
        }
    }

    private void getYLine(Vector3d a, Vector3d b, Vector3d cpos, Vector3d fpos, RefSysTransform tr) {
        Matrix4d inv = tr.matrix;
        a.set(fpos.x, -cpos.y, fpos.z);
        b.set(fpos.x, fpos.y, fpos.z);
        if (inv != null) {
            // Back to equatorial
            a.mul(inv);
            b.mul(inv);
        }
    }
}
