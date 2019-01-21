package gaia.cu9.ari.gaiaorbit.scenegraph;

import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.data.util.PointCloudData;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;

public class KeyframesPathObject extends VertsObject {
    private static double[] ggreen = new double[] { 0f / 255f, 135f / 255f, 68f / 255f, 1f };
    private static double[] gblue = new double[] { 0f / 255f, 87f / 255f, 231f / 255f, 1f };
    private static double[] gred = new double[] { 214f / 255f, 45f / 255f, 32f / 255f, 1f };
    private static double[] gyellow = new double[] { 255f / 255f, 167f / 255f, 0f / 255f, 1f };
    private static double[] gwhite = new double[] { 255f / 255f, 255f / 255f, 255f / 255f, 1f };

    public VertsObject path;
    public VertsObject segments;
    public VertsObject knots;
    /** Contains pairs of {direction, up} representing the orientation at each knot **/
    public Array<VertsObject> orientations;

    private Array<VertsObject> objects;

    public KeyframesPathObject() {
        super(null);

    }

    public void initialize() {
        orientations = new Array<>();

        path = new Polyline();
        path.setName("Keyframes.path");
        path.ct = this.ct;
        path.setColor(ggreen);
        path.setClosedLoop(false);
        path.setPrimitiveSize(2f);
        path.initialize();

        segments = new Polyline();
        segments.setName("Keyframes.segments");
        segments.ct = this.ct;
        segments.setColor(gyellow);
        segments.setClosedLoop(false);
        segments.setPrimitiveSize(1f);
        segments.initialize();

        knots = new VertsObject(RenderGroup.POINT_GPU);
        knots.setName("Keyframes.knots");
        knots.ct = this.ct;
        knots.setColor(gwhite);
        knots.setClosedLoop(false);
        knots.setPrimitiveSize(4f);
        knots.initialize();

        objects = new Array<>();
        objects.add(path);
        objects.add(segments);
        objects.add(knots);
    }

    public void setPathKnots(double[] kts, double[] dirs, double[] ups) {
        knots.setPoints(kts);
        clearOrientations();
        int n = kts.length;
        for (int i = 0; i < n; i += 3) {
            addKnot(i / 3, kts[i], kts[i + 1], kts[i + 2], dirs[i], dirs[i + 1], dirs[i + 2], ups[i], ups[i + 1], ups[i + 2]);
        }
    }

    public void addKnot(Vector3d knot, Vector3d dir, Vector3d up) {
        knots.addPoint(knot);
        addKnot(orientations.size / 2, knot.x, knot.y, knot.z, dir.x, dir.y, dir.z, up.x, up.y, up.z);
    }

    private void addKnot(int idx, double px, double py, double pz, double dx, double dy, double dz, double ux, double uy, double uz) {
        VertsObject dir = new Polyline();
        dir.setName("Keyframes.dir" + idx);
        dir.ct = this.ct;
        dir.setColor(gred);
        dir.setClosedLoop(false);
        dir.setPrimitiveSize(1f);
        dir.initialize();

        VertsObject up = new Polyline();
        up.setName("Keyframes.up" + idx);
        up.ct = this.ct;
        up.setColor(gblue);
        up.setClosedLoop(false);
        up.setPrimitiveSize(1f);
        up.initialize();

        dir.setPoints(new double[] { px, py, pz, px + dx, py + dy, pz + dz });
        up.setPoints(new double[] { px, py, pz, px + ux, py + uy, pz + uz });

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
            double len = Math.max(0.00005, Math.atan(0.02) * c.dst(p0));

            Vector3d v = c.set(p1).sub(p0).nor().scl(len);
            p.x.set(1, p0.x + v.x);
            p.y.set(1, p0.y + v.y);
            p.z.set(1, p0.z + v.z);
            vo.markForUpdate();
        }
    }

    @Override
    protected void addToRenderLists(ICamera camera) {
    }

    @Override
    public void clear() {
        for (VertsObject vo : objects)
            vo.clear();
        clearOrientations();
    }

    public void clearOrientations() {
        for (VertsObject vo : orientations)
            objects.removeValue(vo, true);
        orientations.clear();
    }
}
