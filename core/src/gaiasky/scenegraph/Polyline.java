/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

import com.badlogic.gdx.graphics.GL20;
import gaiasky.render.ILineRenderable;
import gaiasky.render.SceneGraphRenderer;
import gaiasky.render.SceneGraphRenderer.RenderGroup;
import gaiasky.render.system.LineRenderSystem;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.math.Vector3d;

/**
 * Represents a polyline. Can use GPU or CPU method.
 *
 * @author tsagrista
 */
public class Polyline extends VertsObject implements ILineRenderable {


    public Polyline() {
        super(RenderGroup.LINE_GPU, GL20.GL_LINE_STRIP);
    }

    public Polyline(int primitive) {
        super(RenderGroup.LINE_GPU, primitive);
    }

    public Polyline(RenderGroup rg) {
        super(rg, GL20.GL_LINE_STRIP);
    }

    @Override
    public void render(LineRenderSystem renderer, ICamera camera, float alpha) {
        // Render line CPU
        if (pointCloudData != null && pointCloudData.getNumPoints() > 1) {
            Vector3d prev = aux3d1.get();

            for (int i = 0; i < pointCloudData.getNumPoints(); i++) {
                pointCloudData.loadPoint(prev, i);
                prev.add(translation);
                renderer.addPoint(this, (float) prev.x, (float) prev.y, (float) prev.z, cc[0], cc[1], cc[2], (alpha) * cc[3]);
            }
            renderer.breakLine();
        }
    }

    @Override
    protected void addToRenderLists(ICamera camera) {
        // Lines only make sense with 2 or more points
        if (pointCloudData != null && pointCloudData.getNumPoints() > 1)
            addToRender(this, renderGroup);
    }

    @Override
    public float getLineWidth() {
        return getPrimitiveSize();
    }

}
