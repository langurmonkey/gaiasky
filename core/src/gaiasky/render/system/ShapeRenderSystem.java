/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.system;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.utils.Array;
import gaiasky.render.api.IRenderable;
import gaiasky.render.api.IShapeRenderable;
import gaiasky.render.RenderGroup;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.scenegraph.camera.ICamera;

/**
 * System that renders shapes through a {@link ShapeRenderer}
 */
public class ShapeRenderSystem extends AbstractRenderSystem {

    /**
     * The shape renderer
     */
    private final ShapeRenderer shapeRenderer;

    public ShapeRenderSystem(SceneRenderer sceneRenderer, final RenderGroup rg, final float[] alphas, final ShaderProgram spriteShader) {
        super(sceneRenderer, rg, alphas, null);
        this.shapeRenderer = new ShapeRenderer(5000, spriteShader);

    }

    @Override
    public void renderStud(Array<IRenderable> renderables, ICamera camera, double t) {
        shapeRenderer.begin(ShapeType.Line);
        renderables.forEach(r -> {
            IShapeRenderable sr = (IShapeRenderable) r;
            sr.render(shapeRenderer, rc, getAlpha(r), camera);
        });
        shapeRenderer.end();
    }

    @Override
    public void resize(int w, int h) {
        super.resize(w, h);
        updateBatchSize(w, h);
    }

    @Override
    public void updateBatchSize(int w, int h) {
        shapeRenderer.setProjectionMatrix(shapeRenderer.getProjectionMatrix().setToOrtho2D(0, 0, w, h));
    }

}
