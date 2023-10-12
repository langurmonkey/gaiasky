/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render.draw;

import gaiasky.render.RenderGroup;
import gaiasky.render.api.IRenderable;
import gaiasky.render.system.AbstractRenderSystem;
import gaiasky.scene.Mapper;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.Render;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.scene.system.render.draw.model.ModelEntityRenderSystem;
import gaiasky.util.gdx.IntModelBatch;

import java.util.List;

public class ModelRenderer extends AbstractRenderSystem {

    private final ModelEntityRenderSystem renderObject;
    protected IntModelBatch batch;

    /**
     * Creates a new model batch render component.
     *
     * @param rg     The render group.
     * @param alphas The alphas list.
     * @param batch  The model batch.
     */
    public ModelRenderer(SceneRenderer sceneRenderer, RenderGroup rg, float[] alphas, IntModelBatch batch) {
        super(sceneRenderer, rg, alphas, null);
        this.batch = batch;
        this.renderObject = new ModelEntityRenderSystem(sceneRenderer);
    }

    @Override
    public void renderStud(List<IRenderable> renderables, ICamera camera, double t) {
        if (mustRender()) {
            batch.begin(camera.getCamera());
            try {
                renderables.forEach(r -> {
                    Render render = (Render) r;
                    renderObject.render(render.entity, batch, camera, getAlpha(render.entity), t, rc, getRenderGroup(), !Mapper.tagQuatOrientation.has(render.entity));
                });
            } catch(Exception e) {
                batch.cancel();
                throw e;
            }
            batch.end();
        }
    }

    protected boolean mustRender() {
        return true;
    }

}
