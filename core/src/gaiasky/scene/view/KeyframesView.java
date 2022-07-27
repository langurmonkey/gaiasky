package gaiasky.scene.view;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.data.util.PointCloudData;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.render.RenderGroup;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.component.Keyframes;
import gaiasky.scene.entity.KeyframeUtils;
import gaiasky.scenegraph.Invisible;
import gaiasky.scenegraph.Polyline;
import gaiasky.scenegraph.camera.NaturalCamera;
import gaiasky.util.Constants;
import gaiasky.util.Settings;
import gaiasky.util.camera.rec.CameraKeyframeManager;
import gaiasky.util.camera.rec.Keyframe;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;

public class KeyframesView extends BaseView {

    /** The keyframed path component. **/
    private Keyframes kf;

    private Scene scene;
    private final VertsView verts;
    private final FocusView focus;
    private KeyframeUtils utils;

    private Vector3 F31 = new Vector3();
    private Vector3d D31 = new Vector3d();
    private Vector3d D32 = new Vector3d();

    public KeyframesView(Scene scene) {
        this.scene = scene;
        this.verts = new VertsView();
        this.focus = new FocusView();
        this.utils = new KeyframeUtils(scene);
    }

    @Override
    protected void entityCheck(Entity entity) {
        super.entityCheck(entity);
        check(entity, Mapper.keyframes, Keyframes.class);

    }

    @Override
    protected void entityChanged() {
        super.entityChanged();
        this.kf = Mapper.keyframes.get(entity);
    }

    @Override
    protected void entityCleared() {
        this.kf = null;
    }

    /**
     * Refreshes the positions and orientations from the keyframes
     */
    public void refreshData() {
        Array<Keyframe> keyframes = kf.keyframes;

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

            kfPositions[i * 3] = kf.pos.x;
            kfPositions[i * 3 + 1] = kf.pos.y;
            kfPositions[i * 3 + 2] = kf.pos.z;

            kfDirections[i * 3] = kf.dir.x;
            kfDirections[i * 3 + 1] = kf.dir.y;
            kfDirections[i * 3 + 2] = kf.dir.z;

            kfUps[i * 3] = kf.up.x;
            kfUps[i * 3 + 1] = kf.up.y;
            kfUps[i * 3 + 2] = kf.up.z;

            kfSeams[i] = kf.seam;

            i++;
        }
        kfPositionsSep.add(current);

        setPathKnots(kfPositions, kfDirections, kfUps, kfSeams);
        if (keyframes.size > 1) {
            // Set points to segments
            synchronized (verts) {
                verts.setEntity(kf.segments);
                verts.setPoints(kfPositions);
            }

            // Set points to path
            double[] pathSamples = CameraKeyframeManager.instance.samplePaths(kfPositionsSep, kfPositions, 20, Settings.settings.camrecorder.keyframe.position);
            synchronized (verts) {
                verts.setEntity(kf.path);
                verts.setPoints(pathSamples);
            }
        } else {
            synchronized (verts) {
                verts.setEntity(kf.segments);
                verts.clear();
            }

            synchronized (verts) {
                verts.setEntity(kf.path);
                verts.clear();
            }
        }
    }

    /**
     * Refreshes the orientations from the keyframes
     */
    public void refreshOrientations() {
        int i = 0;
        for (Keyframe keyframe : kf.keyframes) {
            Entity dir = kf.orientations.get(i);
            Entity up = kf.orientations.get(i + 1);

            refreshSingleVector(dir, keyframe.pos, keyframe.dir);
            refreshSingleVector(up, keyframe.pos, keyframe.up);

            i += 2;
        }
    }

    public void refreshSingleVector(Entity vo, Vector3d pos, Vector3d vec) {
        synchronized (verts) {
            verts.setEntity(vo);
            PointCloudData p = verts.getPointCloud();
            p.x.set(0, pos.x);
            p.y.set(0, pos.y);
            p.z.set(0, pos.z);

            p.x.set(1, pos.x + vec.x);
            p.y.set(1, pos.y + vec.y);
            p.z.set(1, pos.z + vec.z);
            verts.markForUpdate();
        }
    }

    public void resamplePath() {
        Array<Keyframe> keyframes = kf.keyframes;

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

                kfPositions[i * 3] = kf.pos.x;
                kfPositions[i * 3 + 1] = kf.pos.y;
                kfPositions[i * 3 + 2] = kf.pos.z;
                i++;
            }
            kfPositionsSep.add(current);

            double[] pathSamples = CameraKeyframeManager.instance.samplePaths(kfPositionsSep, kfPositions, 20, Settings.settings.camrecorder.keyframe.position);
            synchronized (verts) {
                verts.setEntity(kf.path);
                verts.setPoints(pathSamples);
            }
        }
    }

    public void setPathKnots(double[] kts, double[] dirs, double[] ups, boolean[] seams) {
        // Points - distribute seams and no seams
        int nSeams = 0, nNoSeams = 0;
        for (boolean seam : seams) {
            if (seam)
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
        synchronized (verts) {
            verts.setEntity(kf.knots);
            verts.setPoints(ktsN);
        }

        synchronized (verts) {
            verts.setEntity(kf.knotsSeam);
            verts.setPoints(ktsS);
        }

        int n = kts.length;
        if (kf.orientations.size == (dirs.length + ups.length) / 3) {
            // We can just update what we have
            int j = 0;
            for (int i = 0; i < kf.orientations.size; i++) {
                double[] targ = (i % 2 == 0) ? dirs : ups;
                Entity vo = kf.orientations.get(i);
                PointCloudData p = Mapper.verts.get(vo).pointCloudData;
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
            kf.clearOrientations();
            for (int i = 0; i < n; i += 3) {
                addKnotOrientation(i / 3, kts[i], kts[i + 1], kts[i + 2], dirs[i], dirs[i + 1], dirs[i + 2], ups[i], ups[i + 1], ups[i + 2]);
            }
        }
    }

    public void addKnot(Vector3d knot, Vector3d dir, Vector3d up, boolean seam) {
        if (seam) {
            synchronized (verts) {
                verts.setEntity(kf.knotsSeam);
                verts.addPoint(knot);
            }
        } else {
            synchronized (verts) {
                verts.setEntity(kf.knots);
                verts.addPoint(knot);
            }
        }
        addKnotOrientation(kf.orientations.size / 2, knot.x, knot.y, knot.z, dir.x, dir.y, dir.z, up.x, up.y, up.z);
    }

    private void addKnotOrientation(int idx, double px, double py, double pz, double dx, double dy, double dz, double ux, double uy, double uz) {
        Entity dir = utils.newVerts(scene, "Keyframes.dir", base.ct, Polyline.class, ColorUtils.gRed, RenderGroup.LINE, false, 0.6f * kf.ss);
        Entity up = utils.newVerts(scene, "Keyframes.up", base.ct, Polyline.class, ColorUtils.gBlue, RenderGroup.LINE, false, 0.6f * kf.ss);

        synchronized (verts) {
            verts.setEntity(dir);
            verts.setPoints(new double[] { px, py, pz, px + dx, py + dy, pz + dz });
        }

        synchronized (verts) {
            verts.setEntity(up);
            verts.setPoints(new double[] { px, py, pz, px + ux, py + uy, pz + uz });
        }

        kf.objects.add(dir);
        kf.objects.add(up);

        kf.orientations.add(dir);
        kf.orientations.add(up);
    }

    public FocusView select(int screenX, int screenY, int minPixDist, NaturalCamera camera) {

        Vector3 pos = F31;
        Vector3d aux = D31;
        for (Keyframe keyframe : kf.keyframes) {
            Vector3d posd = aux.set(keyframe.pos).add(camera.getInversePos());
            pos.set(posd.valuesf());

            if (camera.direction.dot(posd) > 0) {
                // The object is in front of us
                double angle = 0.0001;

                PerspectiveCamera perspectiveCamera;
                if (Settings.settings.program.modeStereo.active) {
                    if (screenX < Gdx.graphics.getWidth() / 2f) {
                        perspectiveCamera = camera.getCameraStereoLeft();
                    } else {
                        perspectiveCamera = camera.getCameraStereoRight();
                    }
                    perspectiveCamera.update();
                } else {
                    perspectiveCamera = camera.camera;
                }

                angle = (float) Math.toDegrees(angle * camera.getFovFactor()) * (40f / perspectiveCamera.fieldOfView);
                double pixelSize = Math.max(minPixDist, ((angle * perspectiveCamera.viewportHeight) / perspectiveCamera.fieldOfView) / 2);
                perspectiveCamera.project(pos);
                pos.y = perspectiveCamera.viewportHeight - pos.y;
                if (Settings.settings.program.modeStereo.active) {
                    pos.x /= 2;
                }
                // Check click distance
                if (checkClickDistance(screenX, screenY, pos, camera, perspectiveCamera, pixelSize)) {
                    //Hit
                    select(keyframe);
                    initFocus();
                    synchronized (focus) {
                        focus.setEntity(kf.focus);
                        focus.getPos().set(kf.selected.pos);
                        Mapper.base.get(kf.focus).setName(kf.selected.name);
                    }

                    return focus;
                }
            }
        }
        return null;
    }

    protected boolean checkClickDistance(int screenX, int screenY, Vector3 pos, NaturalCamera camera, PerspectiveCamera pcamera, double pixelSize) {
        return pos.dst(screenX % pcamera.viewportWidth, screenY, pos.z) <= pixelSize;
    }

    public void highlight(Keyframe keyframe) {
        unhighlight();
        kf.highlighted = keyframe;

        synchronized (verts) {
            verts.setEntity(kf.highlightedKnot);
            verts.setPoints(keyframe.pos.values());
        }
    }

    public void unhighlight(Keyframe keyframe) {
        if (kf.highlighted == keyframe) {
            unhighlight();
        }
    }

    public void unhighlight() {
        if (kf.highlighted != null) {
            kf.highlighted = null;

            synchronized (verts) {
                verts.setEntity(kf.highlightedKnot);
                verts.clear();
            }
        }
    }

    public void select(Keyframe keyframe) {
        unselect();
        kf.selected = keyframe;

        synchronized (verts) {
            verts.setEntity(kf.selectedKnot);
            verts.setPoints(keyframe.pos.values());
            if (kf.selected.seam) {
                verts.setColor(ColorUtils.gRed);
            } else {
                verts.setColor(ColorUtils.gPink);
            }
        }
        int i = kf.keyframes.indexOf(keyframe, true) * 2;
        if (i >= 0) {
            Entity dir = kf.orientations.get(i);
            Entity up = kf.orientations.get(i + 1);

            synchronized (verts) {
                verts.setEntity(dir);
                verts.setPrimitiveSize(0.8f * kf.ss);
            }

            synchronized (verts) {
                verts.setEntity(up);
                verts.setPrimitiveSize(0.8f * kf.ss);
            }
        }
        EventManager.publish(Event.KEYFRAME_SELECT, this, keyframe);
    }

    private void initFocus() {
        if (kf.focus == null || Mapper.graph.get(kf.focus).parent == null) {
            Entity focus = scene.archetypes().get(Invisible.class.getName()).createEntity();
            var base = Mapper.base.get(focus);
            base.setName("");
            base.setCt("Invisible");

            var body = Mapper.body.get(focus);
            body.size = (float) (0.01 * Constants.KM_TO_U);

            var graph = Mapper.graph.get(focus);
            graph.translation = new Vector3b();

            scene.initializeEntity(focus);

            kf.focus = focus;

            EventManager.publish(Event.SCENE_ADD_OBJECT_CMD, this, focus, false);
        }
    }

    public void unselect() {
        if (kf.selected != null) {
            int i = kf.keyframes.indexOf(kf.selected, true) * 2;
            if (i >= 0) {
                Entity dir = kf.orientations.get(i);
                Entity up = kf.orientations.get(i + 1);

                synchronized (verts) {
                    verts.setEntity(dir);
                    verts.setPrimitiveSize(0.6f * kf.ss);
                }

                synchronized (verts) {
                    verts.setEntity(up);
                    verts.setPrimitiveSize(0.6f * kf.ss);
                }
            }
            initFocus();
            synchronized (focus) {
                focus.setEntity(kf.focus);
                focus.setName("");
            }
            Keyframe aux = kf.selected;
            kf.selected = null;

            synchronized (verts) {
                verts.setEntity(kf.selectedKnot);
                verts.clear();
            }

            EventManager.publish(Event.KEYFRAME_UNSELECT, this, aux);
        }
    }

    public boolean isSelected() {
        return kf.selected != null;
    }

    public boolean moveSelection(int screenX, int screenY, NaturalCamera camera) {
        if (kf.selected != null) {
            double originalDist = D31.set(kf.selected.pos).add(camera.getInversePos()).len();
            Vector3 aux = F31.set(screenX, screenY, 0.5f);
            camera.getCamera().unproject(aux);
            Vector3d newLocation = D32.set(aux).setLength(originalDist);
            kf.selected.pos.set(newLocation).add(camera.getPos());

            synchronized (verts) {
                verts.setEntity(kf.selectedKnot);
                verts.setPoints(kf.selected.pos.values());
            }
            refreshData();
            return true;
        }
        return false;
    }

    public boolean rotateAroundDir(double dx, double dy, NaturalCamera camera) {
        if (kf.selected != null) {
            kf.selected.up.rotate(kf.selected.dir, (float) ((dx + dy) * 500d));
            refreshOrientations();
            return true;
        }
        return false;
    }

    public boolean rotateAroundUp(double dx, double dy, NaturalCamera camera) {
        if (kf.selected != null) {
            kf.selected.dir.rotate(kf.selected.up, (float) ((dx + dy) * 500d));
            refreshOrientations();
            return true;
        }
        return false;
    }

    public boolean rotateAroundCrs(double dx, double dy, NaturalCamera camera) {
        if (kf.selected != null) {
            Vector3d crs = D31.set(kf.selected.dir).crs(kf.selected.up);
            kf.selected.dir.rotate(crs, (float) ((dx + dy) * 500d));
            kf.selected.up.rotate(crs, (float) ((dx + dy) * 500d));
            refreshOrientations();
            return true;
        }
        return false;
    }

    public void clearOrientations() {
        for (Entity vo : kf.orientations)
            kf.objects.removeValue(vo, true);
        kf.orientations.clear();
    }

    public void clear() {
        // Clear GPU objects
        VertsView v = new VertsView();
        for (Entity vo : kf.objects) {
            v.setEntity(vo);
            v.clear();
        }

        // Clear orientations
        clearOrientations();

        // Unselect
        unselect();
    }
}
