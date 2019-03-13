package gaia.cu9.ari.gaiaorbit.render;

import gaia.cu9.ari.gaiaorbit.render.system.LineRenderSystem;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;

/**
 * Interface to implement by all entities that are to be rendered as lines whose
 * points are floated by the camera position in the CPU.
 *
 * @author Toni Sagrista
 */
public interface ILineRenderable extends IRenderable {

    float getLineWidth();

    void render(LineRenderSystem renderer, ICamera camera, float alpha);

}
