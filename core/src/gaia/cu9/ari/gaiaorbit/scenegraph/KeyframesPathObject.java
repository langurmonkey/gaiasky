package gaia.cu9.ari.gaiaorbit.scenegraph;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.data.util.PointCloudData;
import gaia.cu9.ari.gaiaorbit.desktop.util.camera.CameraKeyframeManager;
import gaia.cu9.ari.gaiaorbit.desktop.util.camera.Keyframe;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.render.I3DTextRenderable;
import gaia.cu9.ari.gaiaorbit.render.ILineRenderable;
import gaia.cu9.ari.gaiaorbit.render.IPointRenderable;
import gaia.cu9.ari.gaiaorbit.render.RenderingContext;
import gaia.cu9.ari.gaiaorbit.render.system.FontRenderSystem;
import gaia.cu9.ari.gaiaorbit.render.system.LineRenderSystem;
import gaia.cu9.ari.gaiaorbit.render.system.PointRenderSystem;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.FovCamera;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.NaturalCamera;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;
import gaia.cu9.ari.gaiaorbit.util.gravwaves.RelativisticEffectsManager;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;

public class KeyframesPathObject extends VertsObject implements I3DTextRenderable, ILineRenderable, IPointRenderable {

    /**
     * Keyframe objects
     */
    public Array<Keyframe> keyframes;
    /**
     * Selected keyframe
     **/
    public Keyframe selected = null;

    /**
     * The actual path
     **/
    public VertsObject path;
    /**
     * The segments joining the knots
     **/
    public VertsObject segments;
    /**
     * The knots, or keyframe positions
     **/
    public VertsObject knots;
    /**
     * Knots which are also seams
     **/
    public VertsObject knotsSeam;
    /**
     * Selected knot
     **/
    public VertsObject selectedKnot;

    /**
     * Contains pairs of {direction, up} representing the orientation at each knot
     **/
    public Array<VertsObject> orientations;

    /**
     * Objects
     **/
    private Array<VertsObject> objects;

    /**
     * Invisible focus for camera
     */
    private Invisible focus;

    /**
     * Multiplier to primitive size
     **/
    private float ss = 1f;

    public KeyframesPathObject() {
        super(null);
    }

    public void initialize() {
        orientations = new Array<>();

        path = new Polyline(RenderGroup.LINE);
        path.setName("Keyframes.path");
        path.ct = this.ct;
        path.setColor(GlobalResources.gGreen);
        path.setClosedLoop(false);
        path.setPrimitiveSize(1.5f * ss);
        path.initialize();

        segments = new Polyline(RenderGroup.LINE);
        segments.setName("Keyframes.segments");
        segments.ct = this.ct;
        segments.setColor(GlobalResources.gYellow);
        segments.setClosedLoop(false);
        segments.setPrimitiveSize(1f * ss);
        segments.initialize();

        knots = new Points(RenderGroup.POINT);
        knots.setName("Keyframes.knots");
        knots.ct = this.ct;
        knots.setColor(GlobalResources.gWhite);
        knots.setClosedLoop(false);
        knots.setPrimitiveSize(4f * ss);
        knots.initialize();

        knotsSeam = new Points(RenderGroup.POINT);
        knotsSeam.setName("Keyframes.knots.seam");
        knotsSeam.ct = this.ct;
        knotsSeam.setColor(GlobalResources.gRed);
        knotsSeam.setClosedLoop(false);
        knotsSeam.setPrimitiveSize(4f * ss);
        knotsSeam.initialize();

        selectedKnot = new Points(RenderGroup.POINT);
        selectedKnot.setName("Keyframes.selknot");
        selectedKnot.ct = this.ct;
        selectedKnot.setColor(GlobalResources.gPink);
        selectedKnot.setClosedLoop(false);
        selectedKnot.setPrimitiveSize(8f * ss);
        selectedKnot.setDepth(false);
        selectedKnot.initialize();

        objects = new Array<>();
        objects.add(path);
        objects.add(segments);
        objects.add(knots);
        objects.add(knotsSeam);
        objects.add(selectedKnot);

    }

    public void setKeyframes(Array<Keyframe> keyframes) {
        this.keyframes = keyframes;
    }

    /**
     * Refreshes the positions and orientations from the keyframes
     */
    public void refreshData() {
        Array<Array<Vector3d>> kfPositionsSep = new Array<>();
        double[] kfPositions = new double[keyframes.size * 3];
        double[] kfDirections = new double[keyframes.size * 3];
        double[] kfUps = new double[keyframes.size * 3];
        boolean[] kfSeams = new boolean[keyframes.size];
        Array<Vector3d> current = new Array<>();
        int i = 0;
        for (Keyframe kf : keyframes) {

            // Fill positions
            if (kf.seam) {
                if (i > 0 && i < keyframes.size - 1) {
                    current.add(kf.pos);
                    kfPositionsSep.add(current);
                    current = new Array<>();
                }
            }
            current.add(kf.pos);

            kfPositions[i * 3 + 0] = kf.pos.x;
            kfPositions[i * 3 + 1] = kf.pos.y;
            kfPositions[i * 3 + 2] = kf.pos.z;

            kfDirections[i * 3 + 0] = kf.dir.x;
            kfDirections[i * 3 + 1] = kf.dir.y;
            kfDirections[i * 3 + 2] = kf.dir.z;

            kfUps[i * 3 + 0] = kf.up.x;
            kfUps[i * 3 + 1] = kf.up.y;
            kfUps[i * 3 + 2] = kf.up.z;

            kfSeams[i] = kf.seam;

            i++;
        }
        kfPositionsSep.add(current);

        setPathKnots(kfPositions, kfDirections, kfUps, kfSeams);
        if (keyframes.size > 1) {
            segments.setPoints(kfPositions);
            double[] pathSamples = CameraKeyframeManager.instance.samplePaths(kfPositionsSep, kfPositions, 20, GlobalConf.frame.KF_PATH_TYPE_POSITION);
            path.setPoints(pathSamples);
        } else {
            segments.clear();
            path.clear();
        }
    }

    /**
     * Refreshes the orientations from the keyframes
     */
    public void refreshOrientations() {
        int i = 0;
        for (Keyframe kf : keyframes) {
            VertsObject dir = orientations.get(i);
            VertsObject up = orientations.get(i + 1);

            refreshSingleVector(dir, kf.pos, kf.dir);
            refreshSingleVector(up, kf.pos, kf.up);

            i += 2;
        }
    }

    public void refreshSingleVector(VertsObject vo, Vector3d pos, Vector3d vec) {
        PointCloudData p = vo.pointCloudData;
        p.x.set(0, pos.x);
        p.y.set(0, pos.y);
        p.z.set(0, pos.z);

        p.x.set(1, pos.x + vec.x);
        p.y.set(1, pos.y + vec.y);
        p.z.set(1, pos.z + vec.z);
        vo.markForUpdate();
    }

    public void resamplePath() {
        if (keyframes.size > 0) {
            Array<Array<Vector3d>> kfPositionsSep = new Array<>();
            double[] kfPositions = new double[keyframes.size * 3];
            Array<Vector3d> current = new Array<>();
            int i = 0;
            for (Keyframe kf : keyframes) {
                // Fill model table
                if (kf.seam) {
                    if (i > 0 && i < keyframes.size - 1) {
                        current.add(kf.pos);
                        kfPositionsSep.add(current);
                        current = new Array<>();
                    }
                }
                current.add(kf.pos);

                kfPositions[i * 3 + 0] = kf.pos.x;
                kfPositions[i * 3 + 1] = kf.pos.y;
                kfPositions[i * 3 + 2] = kf.pos.z;
                i++;
            }
            kfPositionsSep.add(current);

            double[] pathSamples = CameraKeyframeManager.instance.samplePaths(kfPositionsSep, kfPositions, 20, GlobalConf.frame.KF_PATH_TYPE_POSITION);
            path.setPoints(pathSamples);
        }
    }

    public void setPathKnots(double[] kts, double[] dirs, double[] ups, boolean[] seams) {
        // Points - distribute seams and no seams
        int nSeams = 0, nNoSeams = 0;
        for (int i = 0; i < seams.length; i++) {
            if (seams[i])
                nSeams++;
            else
                nNoSeams++;
        }
        double[] ktsS = new double[nSeams * 3];
        double[] ktsN = new double[nNoSeams * 3];
        int ktsi = 0, ktsni = 0;
        for (int i = 0; i < seams.length; i++) {
            if (seams[i]) {
                ktsS[ktsi] = kts[i * 3];
                ktsS[ktsi + 1] = kts[i * 3 + 1];
                ktsS[ktsi + 2] = kts[i * 3 + 2];
                ktsi += 3;
            } else {
                ktsN[ktsni] = kts[i * 3];
                ktsN[ktsni + 1] = kts[i * 3 + 1];
                ktsN[ktsni + 2] = kts[i * 3 + 2];
                ktsni += 3;
            }
        }
        knots.setPoints(ktsN);
        knotsSeam.setPoints(ktsS);

        int n = kts.length;
        if (orientations.size == (dirs.length + ups.length) / 3) {
            // We can just update what we have
            int j = 0;
            for (int i = 0; i < orientations.size; i++) {
                double[] targ = (i % 2 == 0) ? dirs : ups;
                VertsObject vo = orientations.get(i);
                PointCloudData p = vo.getPointCloud();
                p.x.set(0, kts[i / 2 * 3]);
                p.y.set(0, kts[i / 2 * 3 + 1]);
                p.z.set(0, kts[i / 2 * 3 + 2]);

                p.x.set(1, kts[i / 2 * 3] + targ[j]);
                p.y.set(1, kts[i / 2 * 3 + 1] + targ[j + 1]);
                p.z.set(1, kts[i / 2 * 3 + 2] + targ[j + 2]);

                if (i % 2 == 1)
                    j += 3;
            }
        } else {
            // We start from scratch
            clearOrientations();
            for (int i = 0; i < n; i += 3) {
                addKnotOrientation(i / 3, kts[i], kts[i + 1], kts[i + 2], dirs[i], dirs[i + 1], dirs[i + 2], ups[i], ups[i + 1], ups[i + 2]);
            }
        }
    }

    public void addKnot(Vector3d knot, Vector3d dir, Vector3d up, boolean seam) {
        if (seam)
            knotsSeam.addPoint(knot);
        else
            knots.addPoint(knot);
        addKnotOrientation(orientations.size / 2, knot.x, knot.y, knot.z, dir.x, dir.y, dir.z, up.x, up.y, up.z);
    }

    private void addKnotOrientation(int idx, double px, double py, double pz, double dx, double dy, double dz, double ux, double uy, double uz) {
        VertsObject dir = new Polyline(RenderGroup.LINE);
        dir.setName("Keyframes.dir" + idx);
        dir.ct = this.ct;
        dir.setColor(GlobalResources.gRed);
        dir.setClosedLoop(false);
        dir.setPrimitiveSize(1f * ss);
        dir.initialize();

        VertsObject up = new Polyline(RenderGroup.LINE);
        up.setName("Keyframes.up" + idx);
        up.ct = this.ct;
        up.setColor(GlobalResources.gBlue);
        up.setClosedLoop(false);
        up.setPrimitiveSize(1f * ss);
        up.initialize();

        dir.setPoints(new double[]{px, py, pz, px + dx, py + dy, pz + dz});
        up.setPoints(new double[]{px, py, pz, px + ux, py + uy, pz + uz});

        objects.add(dir);
        objects.add(up);

        orientations.add(dir);
        orientations.add(up);
    }

    public void update(ITimeFrameProvider time, final Vector3d parentTransform, ICamera camera, float opacity) {
        for (VertsObject vo : objects)
            vo.update(time, parentTransform, camera, opacity);

        // Update length of orientations
        for (VertsObject vo : orientations) {
            Vector3d p0 = aux3d1.get();
            Vector3d p1 = aux3d2.get();
            PointCloudData p = vo.pointCloudData;
            p0.set(p.x.get(0), p.y.get(0), p.z.get(0));
            p1.set(p.x.get(1), p.y.get(1), p.z.get(1));

            Vector3d c = aux3d3.get().set(camera.getPos());
            double len = Math.max(0.00008, Math.atan(0.03) * c.dst(p0));

            Vector3d v = c.set(p1).sub(p0).nor().scl(len);
            p.x.set(1, p0.x + v.x);
            p.y.set(1, p0.y + v.y);
            p.z.set(1, p0.z + v.z);
            vo.markForUpdate();
        }

        this.addToRenderLists(camera);
    }

    public IFocus select(int screenX, int screenY, int minPixDist, NaturalCamera camera) {

        Vector3 pos = aux3f1.get();
        Vector3d aux = aux3d1.get();
        for (Keyframe kf : keyframes) {
            Vector3d posd = aux.set(kf.pos).add(camera.getInversePos());
            pos.set(posd.valuesf());

            if (camera.direction.dot(posd) > 0) {
                // The object is in front of us
                double angle = 0.001;

                PerspectiveCamera pcamera;
                if (GlobalConf.program.STEREOSCOPIC_MODE) {
                    if (screenX < Gdx.graphics.getWidth() / 2f) {
                        pcamera = camera.getCameraStereoLeft();
                        pcamera.update();
                    } else {
                        pcamera = camera.getCameraStereoRight();
                        pcamera.update();
                    }
                } else {
                    pcamera = camera.camera;
                }

                angle = (float) Math.toDegrees(angle * camera.getFovFactor()) * (40f / pcamera.fieldOfView);
                double pixelSize = Math.max(minPixDist, ((angle * pcamera.viewportHeight) / pcamera.fieldOfView) / 2);
                pcamera.project(pos);
                pos.y = pcamera.viewportHeight - pos.y;
                if (GlobalConf.program.STEREOSCOPIC_MODE) {
                    pos.x /= 2;
                }
                // Check click distance
                if (checkClickDistance(screenX, screenY, pos, camera, pcamera, pixelSize)) {
                    //Hit
                    select(kf);
                    initFocus();
                    focus.pos.set(selected.pos);
                    focus.name = selected.name;

                    return focus;
                }
            }
        }
        return null;
    }

    protected boolean checkClickDistance(int screenX, int screenY, Vector3 pos, NaturalCamera camera, PerspectiveCamera pcamera, double pixelSize) {
        return pos.dst(screenX % pcamera.viewportWidth, screenY, pos.z) <= pixelSize;
    }

    public void select(Keyframe kf) {
        unselect();
        selected = kf;
        selectedKnot.setPoints(kf.pos.values());
        if (selected.seam) {
            selectedKnot.setColor(GlobalResources.gRed);
        } else {
            selectedKnot.setColor(GlobalResources.gPink);
        }
        int i = keyframes.indexOf(kf, true) * 2;
        if (i >= 0) {
            VertsObject dir = orientations.get(i);
            VertsObject up = orientations.get(i + 1);

            dir.setPrimitiveSize(2f * ss);
            up.setPrimitiveSize(2f * ss);
        }
        EventManager.instance.post(Events.KEYFRAME_SELECT, kf);
    }

    private void initFocus(){
       if(focus == null || focus.parent == null){
           focus = new Invisible("", 5 * Constants.KM_TO_U);
           EventManager.instance.post(Events.SCENE_GRAPH_ADD_OBJECT_CMD, focus, false);
       }
    }

    public void unselect() {
        if (selected != null) {
            int i = keyframes.indexOf(selected, true) * 2;
            if (i >= 0) {
                VertsObject dir = orientations.get(i);
                VertsObject up = orientations.get(i + 1);

                dir.setPrimitiveSize(1f * ss);
                up.setPrimitiveSize(1f * ss);
            }
            initFocus();
            focus.name = "";
            Keyframe aux = selected;
            selected = null;
            selectedKnot.clear();
            EventManager.instance.post(Events.KEYFRAME_UNSELECT, aux);
        }
    }

    public boolean isSelected() {
        return selected != null;
    }

    public boolean moveSelection(int screenX, int screenY, NaturalCamera camera) {
        if (selected != null) {
            double originalDist = aux3d1.get().set(selected.pos).add(camera.getInversePos()).len();
            Vector3 aux = aux3f1.get().set(screenX, screenY, 0.5f);
            camera.getCamera().unproject(aux);
            Vector3d newLocation = aux3d2.get().set(aux).setLength(originalDist);
            selected.pos.set(newLocation).add(camera.getPos());
            selectedKnot.setPoints(selected.pos.values());
            refreshData();
            return true;
        }
        return false;
    }

    public boolean rotateAroundDir(double dx, double dy, NaturalCamera camera) {
        if (selected != null) {
            selected.up.rotate(selected.dir, (float) ((dx + dy) * 500d));
            refreshOrientations();
            return true;
        }
        return false;
    }

    public boolean rotateAroundUp(double dx, double dy, NaturalCamera camera) {
        if (selected != null) {
            selected.dir.rotate(selected.up, (float) ((dx + dy) * 500d));
            refreshOrientations();
            return true;
        }
        return false;
    }

    public boolean rotateAroundCrs(double dx, double dy, NaturalCamera camera) {
        if (selected != null) {
            Vector3d crs = aux3d1.get().set(selected.dir).crs(selected.up);
            selected.dir.rotate(crs, (float) ((dx + dy) * 500d));
            selected.up.rotate(crs, (float) ((dx + dy) * 500d));
            refreshOrientations();
            return true;
        }
        return false;
    }

    @Override
    protected void addToRenderLists(ICamera camera) {
        if (selected != null) {
            addToRender(this, RenderGroup.FONT_LABEL);
        }
    }

    @Override
    public void clear() {
        // Clear GPU objects
        for (VertsObject vo : objects)
            vo.clear();

        // Clear orientations
        clearOrientations();

        // Unselect
        unselect();
    }

    public boolean isEmpty() {
        return keyframes.isEmpty() && knots.isEmpty() && path.isEmpty() && segments.isEmpty();
    }

    public void clearOrientations() {
        for (VertsObject vo : orientations)
            objects.removeValue(vo, true);
        orientations.clear();
    }

    @Override
    public boolean renderText() {
        return selected != null;
    }

    /**
     * Label render
     *
     * @param batch  The sprite batch
     * @param shader The shader
     * @param sys    The font render system
     * @param rc     The render context
     * @param camera The camera
     */
    @Override
    public void render(SpriteBatch batch, ShaderProgram shader, FontRenderSystem sys, RenderingContext rc, ICamera camera) {
        if (camera.getCurrent() instanceof FovCamera) {
        } else {
            // 3D distance font
            Vector3d pos = aux3d1.get();
            textPosition(camera, pos);
            float distToCam = (float) aux3d2.get().set(selected.pos).add(camera.getInversePos()).len();
            shader.setUniformf("u_viewAngle", 90f);
            shader.setUniformf("u_viewAnglePow", 1);
            shader.setUniformf("u_thOverFactor", 1);
            shader.setUniformf("u_thOverFactorScl", 1);

            render3DLabel(batch, shader, sys.fontDistanceField, camera, rc, text(), pos, textScale() * camera.getFovFactor(), textSize() * camera.getFovFactor() * distToCam);
        }

    }

    @Override
    public float[] textColour() {
        return GlobalResources.gPink;
    }

    @Override
    public float textSize() {
        return .5e-3f;
    }

    @Override
    public float textScale() {
        return .3f;
    }

    @Override
    public void textPosition(ICamera cam, Vector3d out) {
        selected.pos.put(out).add(cam.getInversePos());

        Vector3d aux = aux3d2.get();
        aux.set(cam.getUp());

        aux.crs(out).nor();

        aux.add(cam.getUp()).nor().scl(-Math.tan(0.00872) * out.len());

        out.add(aux);

        GlobalResources.applyRelativisticAberration(out, cam);
        RelativisticEffectsManager.getInstance().gravitationalWavePos(out);
    }

    @Override
    public String text() {
        return selected.name;
    }

    @Override
    public void textDepthBuffer() {
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(false);
    }

    @Override
    public boolean isLabel() {
        return false;
    }

    @Override
    public float getTextOpacity() {
        return getOpacity();
    }

    @Override
    public float getLineWidth() {
        return 1;
    }

    @Override
    public void render(LineRenderSystem renderer, ICamera camera, float alpha) {
        // Void
    }

    @Override
    public void render(PointRenderSystem renderer, ICamera camera, float alpha) {
        // Void
    }


}
