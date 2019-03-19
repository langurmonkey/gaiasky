package gaia.cu9.ari.gaiaorbit.render;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;

public interface IQuadRenderable extends IRenderable {

    /**
     * Renders the renderable as a quad using the star shader.
     *
     * @param shader
     * @param alpha
     * @param mesh
     * @param camera
     */
    void render(ShaderProgram shader, float alpha, Mesh mesh, ICamera camera);
}
