package gaia.cu9.ari.gaiaorbit.scenegraph;

import gaia.cu9.ari.gaiaorbit.render.ILineRenderable;
import gaia.cu9.ari.gaiaorbit.render.system.LineRenderSystem;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;

/**
 * Represents a polyline which is sent to the GPU
 *
 * @author tsagrista
 */
public class Polyline extends VertsObject implements ILineRenderable {


    public Polyline() {
        super(RenderGroup.LINE_GPU);
    }

    @Override
    public void render(LineRenderSystem renderer, ICamera camera, float alpha) {
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
