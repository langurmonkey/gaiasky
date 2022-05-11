/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.system;

import com.badlogic.gdx.utils.Array;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.api.IModelRenderable;
import gaiasky.render.api.IRenderable;
import gaiasky.render.RenderGroup;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.gdx.IntModelBatch;

/**
 * Renders simple models using a batch.
 */
public class ModelBatchRenderSystem extends AbstractRenderSystem {

    public enum ModelRenderType {
        NORMAL,
        ATMOSPHERE,
        CLOUD
    }

    protected ComponentTypes ctAtm, ctClouds;

    protected IntModelBatch batch;
    protected ModelRenderType type;

    /**
     * Creates a new model batch render component.
     *
     * @param rg     The render group.
     * @param alphas The alphas list.
     * @param batch  The model batch.
     */
    public ModelBatchRenderSystem(RenderGroup rg, float[] alphas, IntModelBatch batch) {
        super(rg, alphas, null);
        this.batch = batch;

        this.ctAtm = new ComponentTypes(ComponentType.Atmospheres);
        this.ctClouds = new ComponentTypes(ComponentType.Clouds);
    }

    @Override
    public void renderStud(Array<IRenderable> renderables, ICamera camera, double t) {
        if (mustRender()) {
            batch.begin(camera.getCamera());
            renderables.forEach(r -> {
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
