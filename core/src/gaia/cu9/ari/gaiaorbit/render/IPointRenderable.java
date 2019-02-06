package gaia.cu9.ari.gaiaorbit.render;

import gaia.cu9.ari.gaiaorbit.render.system.PointRenderSystem;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;

/**
 * Interface to be implemented by those entities that can be rendered
 * as a single point, floated by the camera position in the CPU.
 * @author Toni Sagrista
 *
 */
public interface IPointRenderable extends IRenderable {

    void render(PointRenderSystem renderer, ICamera camera, float alpha);

    void blend();
    void depth();

}
