/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.data.util.PointCloudData;
import gaiasky.util.camera.rec.CameraKeyframeManager;
import gaiasky.util.camera.rec.Keyframe;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.render.api.I3DTextRenderable;
import gaiasky.render.api.ILineRenderable;
import gaiasky.render.api.IPointRenderable;
import gaiasky.render.RenderingContext;
import gaiasky.render.RenderGroup;
import gaiasky.render.system.FontRenderSystem;
import gaiasky.render.system.LineRenderSystem;
import gaiasky.render.system.PointRenderSystem;
import gaiasky.scenegraph.camera.FovCamera;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.camera.NaturalCamera;
import gaiasky.util.Constants;
import gaiasky.util.GlobalResources;
import gaiasky.util.Settings;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.gdx.g2d.ExtSpriteBatch;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.gravwaves.RelativisticEffectsManager;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;
import gaiasky.util.time.ITimeFrameProvider;

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
     * Highlighted keyframe
     */
    public Keyframe highlighted = null;

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
     * Highlighted knot
     */
    public VertsObject highlightedKnot;

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
    private final float ss = 1f;

    public KeyframesPathObject() {
        super(null, -1);
    }

    public void initialize() {
        orientations = new Array<>();

        path = new Polyline(false, RenderGroup.LINE);
        path.setName("Keyframes.path");
        path.ct = this.ct;
        path.setColor(ColorUtils.gGreen);
        path.setClosedLoop(false);
        path.setPrimitiveSize(0.5f * ss);
        path.initialize();

        segments = new Polyline(false, RenderGroup.LINE);
        segments.setName("Keyframes.segments");
        segments.ct = this.ct;
        segments.setColor(ColorUtils.gYellow);
        segments.setClosedLoop(false);
        segments.setPrimitiveSize(0.6f * ss);
        segments.initialize();

        knots = new Points(RenderGroup.POINT);
        knots.setName("Keyframes.knots");
        knots.ct = this.ct;
        knots.setColor(ColorUtils.gGreen);
        knots.setClosedLoop(false);
        knots.setPrimitiveSize(8f * ss);
        knots.initialize();

        knotsSeam = new Points(RenderGroup.POINT);
        knotsSeam.setName("Keyframes.knots.seam");
        knotsSeam.ct = this.ct;
        knotsSeam.setColor(ColorUtils.gRed);
        knotsSeam.setClosedLoop(false);
        knotsSeam.setPrimitiveSize(8f * ss);
        knotsSeam.setBlend(false);
        knotsSeam.initialize();

        selectedKnot = new Points(RenderGroup.POINT);
        selectedKnot.setName("Keyframes.selknot");
        selectedKnot.ct = this.ct;
        selectedKnot.setColor(ColorUtils.gPink);
        selectedKnot.setClosedLoop(false);
        selectedKnot.setPrimitiveSize(12f * ss);
        selectedKnot.setDepth(false);
        selectedKnot.setBlend(false);
        selectedKnot.initialize();

        highlightedKnot = new Points(RenderGroup.POINT);
        highlightedKnot.setName("Keyframes.highknot");
        highlightedKnot.ct = this.ct;
        highlightedKnot.setColor(ColorUtils.gYellow);
        highlightedKnot.setClosedLoop(false);
        highlightedKnot.setPrimitiveSize(12f * ss);
        highlightedKnot.setDepth(false);
        highlightedKnot.setBlend(false);
        highlightedKnot.initialize();

        objects = new Array<>();
        objects.add(path);
        objects.add(segments);
        objects.add(knots);
        objects.add(knotsSeam);
        objects.add(selectedKnot);
        objects.add(highlightedKnot);

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
            segments.setPoints(kfPositions);
            double[] pathSamples = CameraKeyframeManager.instance.samplePaths(kfPositionsSep, kfPositions, 20, Settings.settings.camrecorder.keyframe.position);
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

                kfPositions[i * 3] = kf.pos.x;
                kfPositions[i * 3 + 1] = kf.pos.y;
                kfPositions[i * 3 + 2] = kf.pos.z;
                i++;
            }
            kfPositionsSep.add(current);

            double[] pathSamples = CameraKeyframeManager.instance.samplePaths(kfPositionsSep, kfPositions, 20, Settings.settings.camrecorder.keyframe.position);
            path.setPoints(pathSamples);
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
        VertsObject dir = new Polyline(false, RenderGroup.LINE);
        dir.setName("Keyframes.dir" + idx);
        dir.ct = this.ct;
        dir.setColor(ColorUtils.gRed);
        dir.setClosedLoop(false);
        dir.setPrimitiveSize(0.6f * ss);
        dir.initialize();

        VertsObject up = new Polyline(false, RenderGroup.LINE);
        up.setName("Keyframes.up" + idx);
        up.ct = this.ct;
        up.setColor(ColorUtils.gBlue);
        up.setClosedLoop(false);
        up.setPrimitiveSize(0.6f * ss);
        up.initialize();

        dir.setPoints(new double[] { px, py, pz, px + dx, py + dy, pz + dz });
        up.setPoints(new double[] { px, py, pz, px + ux, py + uy, pz + uz });

        objects.add(dir);
        objects.add(up);

        orientations.add(dir);
        orientations.add(up);
    }

    public void update(ITimeFrameProvider time, final Vector3b parentTransform, ICamera camera, float opacity) {
        for (VertsObject vo : objects)
            vo.update(time, parentTransform, camera, opacity);

        // Update length of orientations
        for (VertsObject vo : orientations) {
            Vector3d p0 = D31.get();
            Vector3d p1 = D32.get();
            PointCloudData p = vo.pointCloudData;
            p0.set(p.x.get(0), p.y.get(0), p.z.get(0));
            p1.set(p.x.get(1), p.y.get(1), p.z.get(1));

            Vector3d c = D33.get().set(camera.getPos());
            double len = Math.max(1e-9, Math.atan(0.03) * c.dst(p0));

            Vector3d v = c.set(p1).sub(p0).nor().scl(len);
            p.x.set(1, p0.x + v.x);
            p.y.set(1, p0.y + v.y);
            p.z.set(1, p0.z + v.z);
            vo.markForUpdate();
        }

        this.addToRenderLists(camera);
    }

    public IFocus select(int screenX, int screenY, int minPixDist, NaturalCamera camera) {

        Vector3 pos = F31.get();
        Vector3d aux = D31.get();
        for (Keyframe kf : keyframes) {
            Vector3d posd = aux.set(kf.pos).add(camera.getInversePos());
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
                    select(kf);
                    initFocus();
                    focus.pos.set(selected.pos);
                    focus.setName(selected.name);

                    return focus;
                }
            }
        }
        return null;
    }

    protected boolean checkClickDistance(int screenX, int screenY, Vector3 pos, NaturalCamera camera, PerspectiveCamera pcamera, double pixelSize) {
        return pos.dst(screenX % pcamera.viewportWidth, screenY, pos.z) <= pixelSize;
    }

    public void highlight(Keyframe kf) {
        unhighlight();
        highlighted = kf;
        highlightedKnot.setPoints(kf.pos.values());
    }

    public void unhighlight(Keyframe kf) {
        if (highlighted == kf) {
            unhighlight();
        }
    }

    public void unhighlight() {
        if (highlighted != null) {
            highlighted = null;
            highlightedKnot.clear();
        }
    }

    public void select(Keyframe kf) {
        unselect();
        selected = kf;
        selectedKnot.setPoints(kf.pos.values());
        if (selected.seam) {
            selectedKnot.setColor(ColorUtils.gRed);
        } else {
            selectedKnot.setColor(ColorUtils.gPink);
        }
        int i = keyframes.indexOf(kf, true) * 2;
        if (i >= 0) {
            VertsObject dir = orientations.get(i);
            VertsObject up = orientations.get(i + 1);

            dir.setPrimitiveSize(0.8f * ss);
            up.setPrimitiveSize(0.8f * ss);
        }
        EventManager.publish(Event.KEYFRAME_SELECT, this, kf);
    }

    private void initFocus() {
        if (focus == null || focus.parent == null) {
            focus = new Invisible("", 0.01 * Constants.KM_TO_U);
            EventManager.publish(Event.SCENE_GRAPH_ADD_OBJECT_CMD, this, focus, false);
        }
    }

    public void unselect() {
        if (selected != null) {
            int i = keyframes.indexOf(selected, true) * 2;
            if (i >= 0) {
                VertsObject dir = orientations.get(i);
                VertsObject up = orientations.get(i + 1);

                dir.setPrimitiveSize(0.6f * ss);
                up.setPrimitiveSize(0.6f * ss);
            }
            initFocus();
            focus.setName("");
            Keyframe aux = selected;
            selected = null;
            selectedKnot.clear();
            EventManager.publish(Event.KEYFRAME_UNSELECT, this, aux);
        }
    }

    public boolean isSelected() {
        return selected != null;
    }

    public boolean moveSelection(int screenX, int screenY, NaturalCamera camera) {
        if (selected != null) {
            double originalDist = D31.get().set(selected.pos).add(camera.getInversePos()).len();
            Vector3 aux = F31.get().set(screenX, screenY, 0.5f);
            camera.getCamera().unproject(aux);
            Vector3d newLocation = D32.get().set(aux).setLength(originalDist);
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
            Vector3d crs = D31.get().set(selected.dir).crs(selected.up);
            selected.dir.rotate(crs, (float) ((dx + dy) * 500d));
            selected.up.rotate(crs, (float) ((dx + dy) * 500d));
            refreshOrientations();
            return true;
        }
        return false;
    }

    @Override
    protected void addToRenderLists(ICamera camera) {
        if (this.shouldRender() && (selected != null || highlighted != null)) {
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
    public void render(ExtSpriteBatch batch, ExtShaderProgram shader, FontRenderSystem sys, RenderingContext rc, ICamera camera) {
        if (camera.getCurrent() instanceof FovCamera) {
        } else {
            if (selected != null)
                renderKeyframeLabel(selected, batch, shader, sys, rc, camera);
            if (highlighted != null)
                renderKeyframeLabel(highlighted, batch, shader, sys, rc, camera);
        }

    }

    private void renderKeyframeLabel(Keyframe kf, ExtSpriteBatch batch, ExtShaderProgram shader, FontRenderSystem sys, RenderingContext rc, ICamera camera) {
        Vector3d pos = D31.get();
        getTextPosition(camera, pos, kf);
        float distToCam = (float) D32.get().set(kf.pos).add(camera.getInversePos()).len();
        shader.setUniformf("u_viewAngle", 90f);
        shader.setUniformf("u_viewAnglePow", 1);
        shader.setUniformf("u_thOverFactor", 1);
        shader.setUniformf("u_thOverFactorScl", 1);
        shader.setUniform4fv("u_color", textColour(kf), 0, 4);

        render3DLabel(batch, shader, sys.fontDistanceField, camera, rc, getText(kf), pos, distToCamera, textScale() * camera.getFovFactor(), textSize() * camera.getFovFactor() * distToCam, this.forceLabel);

    }

    @Override
    public float[] textColour() {
        return ColorUtils.gPink;
    }

    public float[] textColour(Keyframe kf) {
        if (kf == highlighted)
            return ColorUtils.gYellow;
        else
            return textColour();
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
        getTextPosition(cam, out, selected);
    }

    public void getTextPosition(ICamera cam, Vector3d out, Keyframe kf) {
        kf.pos.put(out).add(cam.getInversePos());

        Vector3d aux = D32.get();
        aux.set(cam.getUp());

        aux.crs(out).nor();

        aux.add(cam.getUp()).nor().scl(-Math.tan(0.00872) * out.len());

        out.add(aux);

        GlobalResources.applyRelativisticAberration(out, cam);
        RelativisticEffectsManager.getInstance().gravitationalWavePos(out);
    }

    @Override
    public String text() {
        return getText(selected);
    }

    public String getText(Keyframe kf) {
        return kf.name;
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

    @Override
    public int getGlPrimitive() {
        return GL20.GL_LINE_STRIP;
    }

}
