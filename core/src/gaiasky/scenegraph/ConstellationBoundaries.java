/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

import com.badlogic.gdx.graphics.GL20;
import gaiasky.render.RenderGroup;
import gaiasky.render.api.ILineRenderable;
import gaiasky.render.system.LineRenderSystem;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.math.Vector3d;
import gaiasky.util.time.ITimeFrameProvider;

import java.util.ArrayList;
import java.util.List;

public class ConstellationBoundaries extends SceneGraphNode implements ILineRenderable {
    private final float alpha = .2f;
    public List<List<Vector3d>> boundaries;

    public ConstellationBoundaries() {
        super();
        cc = new float[] { .8f, .8f, 1f, alpha };
        this.setName("Constellation boundaries");
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
                    renderer.addLine(this, (float) previous.x, (float) previous.y, (float) previous.z, (float) point.x, (float) point.y, (float) point.z, cc[0], cc[1], cc[2], alpha * this.opacity);
                }
                previous = point;
            }

        }
    }

    public void setBoundaries(List<List<Vector3d>> boundaries) {
        this.boundaries = boundaries;
    }

    public void setBoundaries(double[][][] ids) {
        this.boundaries = new ArrayList<>(ids.length);
        for(double[][] dd : ids) {
            List<Vector3d> ii = new ArrayList<>(dd.length);
            for(int j =0; j < dd.length; j++) {
                double[] v = dd[j];
                Vector3d vec = new Vector3d(v);
                ii.add(vec);
            }
            this.boundaries.add(ii);
        }
    }

    @Override
    protected void addToRenderLists(ICamera camera) {
        // Add to toRender list
        if (this.shouldRender())
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

    @Override
    public int getGlPrimitive() {
        return GL20.GL_LINES;
    }
}
