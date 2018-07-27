package gaia.cu9.ari.gaiaorbit.scenegraph;

import java.util.List;

import gaia.cu9.ari.gaiaorbit.render.system.LineRenderSystem;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;

public class ConstellationBoundaries extends LineObject {
    float alpha = .8f;
    List<List<Vector3d>> boundaries;

    public ConstellationBoundaries() {
        super();
        cc = new float[] { .8f, .8f, 1f, alpha };
        this.name = "Constellation boundaries";
        this.parentName = SceneGraphNode.ROOT_NAME;
    }

    @Override
    public void render(LineRenderSystem renderer, ICamera camera, float alpha) {
        alpha *= this.alpha;
        // This is so that the shape renderer does not mess up the z-buffer
        for (List<Vector3d> points : boundaries) {

            Vector3d previous = null;
            for (Vector3d point : points) {
                if (previous != null) {
                    renderer.addLine(this, (float) previous.x, (float) previous.y, (float) previous.z, (float) point.x, (float) point.y, (float) point.z, cc[0], cc[1], cc[2], alpha);
                }
                previous = point;
            }

        }
    }

    public void setBoundaries(List<List<Vector3d>> boundaries) {
        this.boundaries = boundaries;
    }

    @Override
    protected void addToRenderLists(ICamera camera) {
        // Add to toRender list
        addToRender(this, RenderGroup.LINE);
    }

    @Override
    public void updateLocalValues(ITimeFrameProvider time, ICamera camera) {
    }

    @Override
    public void initialize() {
    }

    @Override
    public float getLineWidth() {
        return 1;
    }

}
