/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

import gaiasky.render.IPointRenderable;
import gaiasky.render.system.PointRenderSystem;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.GlobalConf;
import gaiasky.util.math.Vector3d;

/**
 * A vgroup of points
 */
public class Points extends VertsObject implements IPointRenderable {

    public Points(RenderGroup rg){
        super(rg);
    }

    @Override
    public void render(PointRenderSystem renderer, ICamera camera, float alpha) {
        // Render points CPU
        Vector3d v = aux3d1.get();
        for (int i = 0; i < pointCloudData.getNumPoints(); i++) {
            pointCloudData.loadPoint(v, i);
            v.add(translation);
            renderer.addPoint(this, (float) v.x, (float) v.y, (float) v.z, getPrimitiveSize() * GlobalConf.UI_SCALE_FACTOR, cc[0], cc[1], cc[2], cc[3]);
        }
    }
}
