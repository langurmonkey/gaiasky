/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.api;

import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.gdx.mesh.IntMesh;
import gaiasky.util.gdx.shader.ExtShaderProgram;

/**
 * Implemented by all objects that can be rendered by a camera-oriented
 * quad, otherwise called a billboard.
 */
public interface IBillboardRenderable extends IRenderable {

    /**
     * Renders the billboard object using {@link gaiasky.render.system.BillboardRenderSystem}.
     *
     * @param shader The shader program.
     * @param alpha  The alpha value.
     * @param mesh   The mesh.
     * @param camera The camera.
     */
    void render(ExtShaderProgram shader, float alpha, IntMesh mesh, ICamera camera);
}
