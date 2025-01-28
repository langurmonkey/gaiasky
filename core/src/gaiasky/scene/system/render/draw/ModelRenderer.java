/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render.draw;

import com.badlogic.gdx.Gdx;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.render.RenderGroup;
import gaiasky.render.api.IRenderable;
import gaiasky.render.system.AbstractRenderSystem;
import gaiasky.scene.Mapper;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.Render;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.scene.system.render.draw.model.ModelEntityRenderSystem;
import gaiasky.util.gdx.IntModelBatch;
import gaiasky.util.gdx.shader.loader.RelativisticShaderProviderLoader;
import gaiasky.util.gdx.shader.provider.RelativisticShaderProvider;

import java.util.List;

public class ModelRenderer extends AbstractRenderSystem implements IObserver {

    private final ModelEntityRenderSystem renderObject;
    protected IntModelBatch batch;

    /**
     * Creates a new model renderer based on a single model batch.
     *
     * @param rg     The render group.
     * @param alphas The alphas list.
     * @param batch  The model batch.
     */
    public ModelRenderer(SceneRenderer sceneRenderer, RenderGroup rg, float[] alphas, IntModelBatch batch) {
        super(sceneRenderer, rg, alphas, null);
        this.batch = batch;
        this.renderObject = new ModelEntityRenderSystem(sceneRenderer);
        EventManager.instance.subscribe(this, Event.SHADER_RELOAD_CMD);
    }

    @Override
    public void renderStud(List<IRenderable> renderables, ICamera camera, double t) {
        if (mustRender()) {
            batch.begin(camera.getCamera());
            try {
                renderables.forEach(r -> {
                    Render render = (Render) r;
                    renderObject.render(render.entity, batch, camera, getAlpha(render.entity), t, rc, getRenderGroup(), !Mapper.tagBillboard.has(render.entity));
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

    @Override
    public void notify(Event event, Object source, Object... data) {
        if (event == Event.SHADER_RELOAD_CMD) {
           if (batch != null && getRenderGroup() == RenderGroup.MODEL_VERT_STAR) {
               // Dispose batch.
               batch.dispose();
               // Reload star surface model batch.
               var shaderProvider = new RelativisticShaderProvider(Gdx.files.internal("shader/starsurface.vertex.glsl"), Gdx.files.internal("shader/starsurface.fragment.glsl"));
               batch = new IntModelBatch(shaderProvider);
           }
        }
    }
}
