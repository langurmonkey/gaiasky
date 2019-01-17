package gaia.cu9.ari.gaiaorbit.scenegraph;

import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;

public class PathObject extends VertsObject {
    public VertsObject path;
    public VertsObject segments;
    public VertsObject knots;

    private Array<VertsObject> objects;

    public PathObject() {
        super(null);

    }

    public void initialize() {
        path = new Polyline();
        path.setName("Keyframes.path");
        path.ct = this.ct;
        path.setColor(new double[] { 1f, 0f, 0f, 1f });
        path.setClosedLoop(false);
        path.setPrimitiveSize(2f);
        path.initialize();

        segments = new Polyline();
        segments.setName("Keyframes.segments");
        segments.ct = this.ct;
        segments.setColor(new double[] { 0f, 0f, 1f, 1f });
        segments.setClosedLoop(false);
        segments.setPrimitiveSize(1f);
        segments.initialize();

        knots = new VertsObject(RenderGroup.POINT_GPU);
        knots.setName("Keyframes.knots");
        knots.ct = this.ct;
        knots.setColor(new double[] { 0f, 1f, 1f, 1f });
        knots.setClosedLoop(false);
        knots.setPrimitiveSize(4f);
        knots.initialize();

        objects = new Array<>();
        objects.add(path);
        objects.add(segments);
        objects.add(knots);
    }

    public void update(ITimeFrameProvider time, final Vector3d parentTransform, ICamera camera, float opacity) {
        for (VertsObject vo : objects)
            vo.update(time, parentTransform, camera, opacity);
    }

    @Override
    protected void addToRenderLists(ICamera camera) {
    }

    @Override
    public void clear() {
        for (VertsObject vo : objects)
            vo.clear();
    }
}
