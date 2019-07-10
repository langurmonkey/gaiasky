/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.render.system;

import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.render.IModelRenderable;
import gaia.cu9.ari.gaiaorbit.render.IRenderable;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode.RenderGroup;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.comp.ModelComparator;
import gaia.cu9.ari.gaiaorbit.util.gdx.IntModelBatch;
import org.lwjgl.opengl.GL41;

public class ModelBatchTessellationRenderSystem extends AbstractRenderSystem {
    private IntModelBatch batch;

    /**
     * Creates a new model batch render component.
     *
     * @param rg     The render vgroup.
     * @param alphas The alphas list.
     * @param batch  The model batch.
     */
    public ModelBatchTessellationRenderSystem(RenderGroup rg, float[] alphas, IntModelBatch batch) {
        super(rg, alphas, null);
        this.batch = batch;
        comp = new ModelComparator<>();
    }

    @Override
    public void renderStud(Array<IRenderable> renderables, ICamera camera, double t) {
        if (mustRender()) {
            // Triangles for tessellation
            GL41.glPatchParameteri(GL41.GL_PATCH_VERTICES, 3);
            batch.begin(camera.getCamera());
            int size = renderables.size;
            for (int i = 0; i < size; i++) {
                IModelRenderable s = (IModelRenderable) renderables.get(i);
                s.render(batch, getAlpha(s), t);
            }
            batch.end();

        }
    }

    protected boolean mustRender() {
        return true;
    }

}
