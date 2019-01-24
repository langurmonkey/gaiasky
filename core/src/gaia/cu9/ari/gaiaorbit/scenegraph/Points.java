package gaia.cu9.ari.gaiaorbit.scenegraph;

import gaia.cu9.ari.gaiaorbit.render.IPointRenderable;
import gaia.cu9.ari.gaiaorbit.render.system.PointRenderSystem;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;

/**
 * A group of points
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
            renderer.addPoint(this, (float) v.x, (float) v.y, (float) v.z, getPrimitiveSize() * GlobalConf.SCALE_FACTOR, cc[0], cc[1], cc[2], cc[3]);
        }
    }
}
