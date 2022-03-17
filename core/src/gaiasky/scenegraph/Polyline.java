/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

import com.badlogic.gdx.graphics.GL20;
import gaiasky.render.ILineRenderable;
import gaiasky.render.SceneGraphRenderer.RenderGroup;
import gaiasky.render.system.LineRenderSystem;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.math.Vector3d;

/**
 * Represents a polyline. Can use GPU or CPU method.
 */
public class Polyline extends VertsObject implements ILineRenderable {

    /**
     * Paint arrow caps
     */
    private boolean arrowCap = true;

    public Polyline(boolean arrowCap, RenderGroup rg, int primitive) {
        super(rg, primitive);
        this.arrowCap = arrowCap;
    }
    public Polyline(boolean arrowCap, int primitive) {
        this(arrowCap, arrowCap ? RenderGroup.LINE : RenderGroup.LINE_GPU, primitive);
    }

    public Polyline(boolean arrowCap) {
        this(arrowCap, GL20.GL_LINE_STRIP);
    }

    public Polyline() {
        this(false, GL20.GL_LINE_STRIP);
    }

    public Polyline(int primitive) {
        this(false, primitive);
    }

    public Polyline(RenderGroup rg) {
        super(rg, GL20.GL_LINE_STRIP);
    }
    public Polyline(boolean arrowCap, RenderGroup rg) {
        this(arrowCap, rg, GL20.GL_LINE_STRIP);
    }

    @Override
    public void render(LineRenderSystem renderer, ICamera camera, float alpha) {
        // Render line CPU
        alpha *= cc[3];
        if (pointCloudData != null && pointCloudData.getNumPoints() > 1) {
            Vector3d prev = D31.get();

            for (int i = 0; i < pointCloudData.getNumPoints(); i++) {
                pointCloudData.loadPoint(prev, i);
                prev.add(translation);
                renderer.addPoint(this, (float) prev.x, (float) prev.y, (float) prev.z, cc[0], cc[1], cc[2], alpha);

            }
            renderer.breakLine();

            // Render cap if needed
            if (arrowCap) {
                // Get two last points of line
                Vector3d p1 = D32.get().set(pointCloudData.getX(0), pointCloudData.getY(0), pointCloudData.getZ(0));
                Vector3d p2 = D33.get().set(pointCloudData.getX(1), pointCloudData.getY(1), pointCloudData.getZ(1));
                Vector3d ppm = D34.get().set(p1).sub(p2);
                double p1p2len = ppm.len();
                p1.sub(camera.getPos());
                p2.sub(camera.getPos());

                // Add Arrow cap
                Vector3d p3 = ppm.nor().scl(p1p2len * 0.7).add(p2);
                p3.rotate(p1, 30);
                renderer.addPoint(this, p1.x, p1.y, p1.z, cc[0], cc[1], cc[2], alpha);
                renderer.addPoint(this, p3.x, p3.y, p3.z, cc[0], cc[1], cc[2], alpha);
                renderer.breakLine();
                p3.rotate(p1, -60);
                renderer.addPoint(this, p1.x, p1.y, p1.z, cc[0], cc[1], cc[2], alpha);
                renderer.addPoint(this, p3.x, p3.y, p3.z, cc[0], cc[1], cc[2], alpha);
                renderer.breakLine();

            }
        }

    }

    @Override
    protected void addToRenderLists(ICamera camera) {
        // Lines only make sense with 2 or more points
        if (this.shouldRender() && pointCloudData != null && pointCloudData.getNumPoints() > 1)
            addToRender(this, renderGroup);
    }

    @Override
    public float getLineWidth() {
        return getPrimitiveSize();
    }

    public void setArrowCap(boolean arrowCap) {
        this.arrowCap = arrowCap;
    }

}