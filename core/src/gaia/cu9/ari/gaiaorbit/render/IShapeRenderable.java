package gaia.cu9.ari.gaiaorbit.render;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;

public interface IShapeRenderable extends IRenderable {

    /**
     * Renders the shape(s).
     */
    void render(ShapeRenderer shapeRenderer, RenderingContext rc, float alpha, ICamera camera);
}
