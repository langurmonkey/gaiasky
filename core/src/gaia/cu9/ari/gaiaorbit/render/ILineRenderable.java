package gaia.cu9.ari.gaiaorbit.render;

import gaia.cu9.ari.gaiaorbit.render.system.LineRenderSystem;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;

/**
 * Interface to implement by all entities that are to be rendered as lines.
 * 
 * @author Toni Sagrista
 *
 */
public interface ILineRenderable extends IRenderable {

    public float getLineWidth();

    public void render(LineRenderSystem renderer, ICamera camera, float alpha);

}
