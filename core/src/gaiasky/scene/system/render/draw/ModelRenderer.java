/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render.draw;

import com.badlogic.gdx.utils.Array;
import gaiasky.render.*;
import gaiasky.render.api.IRenderable;
import gaiasky.render.system.AbstractRenderSystem;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.*;
import gaiasky.scene.system.render.draw.model.ModelEntityRender;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.gdx.IntModelBatch;

/**
 * Renders simple models using a model batch.
 */
public class ModelRenderer extends AbstractRenderSystem {

    protected IntModelBatch batch;
    private ModelEntityRender renderObject;

    /**
     * Creates a new model batch render component.
     *
     * @param rg     The render group.
     * @param alphas The alphas list.
     * @param batch  The model batch.
     */
    public ModelRenderer(RenderGroup rg, float[] alphas, IntModelBatch batch) {
        super(rg, alphas, null);
        this.batch = batch;
        this.renderObject = new ModelEntityRender();
    }

    @Override
    public void renderStud(Array<IRenderable> renderables, ICamera camera, double t) {
        if (mustRender()) {
            batch.begin(camera.getCamera());
            renderables.forEach(r -> {
                Render render = (Render) r;
                renderObject.render(render.entity, batch, camera, getAlpha(render.entity), t, rc, getRenderGroup(), !Mapper.tagQuatOrientation.has(render.entity));
            });
            batch.end();
        }
    }

    protected boolean mustRender() {
        return true;
    }

}
