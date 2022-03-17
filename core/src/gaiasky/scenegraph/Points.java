/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

import com.badlogic.gdx.graphics.GL20;
import gaiasky.render.IPointRenderable;
import gaiasky.render.SceneGraphRenderer.RenderGroup;
import gaiasky.render.system.PointRenderSystem;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.math.Vector3d;

/**
 * A group of points
 */
public class Points extends VertsObject implements IPointRenderable {

    public Points(RenderGroup rg){
        super(rg, GL20.GL_POINTS);
    }

    @Override
    public void render(PointRenderSystem renderer, ICamera camera, float alpha) {
        // Render points CPU
        Vector3d v = D31.get();
        for (int i = 0; i < pointCloudData.getNumPoints(); i++) {
            pointCloudData.loadPoint(v, i);
            v.add(translation);
            renderer.addPoint(this, (float) v.x, (float) v.y, (float) v.z, getPrimitiveSize(), cc[0], cc[1], cc[2], alpha * cc[3]);
        }
    }
}
