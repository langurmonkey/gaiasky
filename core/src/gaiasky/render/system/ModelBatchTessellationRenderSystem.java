/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.system;

import com.badlogic.gdx.utils.Array;
import gaiasky.render.RenderGroup;
import gaiasky.render.api.IModelRenderable;
import gaiasky.render.api.IRenderable;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.gdx.IntModelBatch;
import org.lwjgl.opengl.GL41;

public class ModelBatchTessellationRenderSystem extends AbstractRenderSystem {
    private final IntModelBatch batch;

    /**
     * Creates a new model batch render component.
     *
     * @param rg     The render group.
     * @param alphas The alphas list.
     * @param batch  The model batch.
     */
    public ModelBatchTessellationRenderSystem(SceneRenderer sceneRenderer, RenderGroup rg, float[] alphas, IntModelBatch batch) {
        super(sceneRenderer, rg, alphas, null);
        this.batch = batch;
    }

    @Override
    public void renderStud(Array<IRenderable> renderables, ICamera camera, double t) {
        if (mustRender()) {
            // Triangles for tessellation
            GL41.glPatchParameteri(GL41.GL_PATCH_VERTICES, 3);
            batch.begin(camera.getCamera());
            renderables.forEach(r ->{
                IModelRenderable s = (IModelRenderable) r;
                s.render(batch, getAlpha(s), t, rc, getRenderGroup());
            });
            batch.end();
        }
    }

    protected boolean mustRender() {
        return true;
    }

}
