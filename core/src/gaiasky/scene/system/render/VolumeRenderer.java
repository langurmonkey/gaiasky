/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render;

import com.badlogic.gdx.utils.IntMap;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.render.RenderGroup;
import gaiasky.render.api.IRenderable;
import gaiasky.render.system.AbstractRenderSystem;
import gaiasky.scene.Mapper;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.Render;
import gaiasky.scene.component.Volume;
import gaiasky.scene.system.render.draw.model.ModelEntityRenderSystem;
import gaiasky.util.gdx.IntModelBatch;
import gaiasky.util.gdx.shader.loader.ShaderTemplatingLoader;
import gaiasky.util.gdx.shader.provider.RelativisticShaderProvider;

import java.util.List;

/**
 * Renders models with arbitrary shaders, kept in a map. The shaders themselves are defined in the {@link Volume} component.
 * Optionally, the shader may receive one or more 3D texture (density, color, etc.). In this case, the shader must be a
 * full-on volume rendering shader.
 */
public class VolumeRenderer extends AbstractRenderSystem implements IObserver {

    private final ModelEntityRenderSystem renderObject;
    protected final IntMap<IntModelBatch> batches;

    /**
     * Creates a new volume model renderer which may use a number of different batches.
     *
     * @param rg     The render group.
     * @param alphas The alphas list.
     */
    public VolumeRenderer(SceneRenderer sceneRenderer, RenderGroup rg, float[] alphas) {
        super(sceneRenderer, rg, alphas, null);
        this.batches = new IntMap<>();
        this.renderObject = new ModelEntityRenderSystem(sceneRenderer);
        EventManager.instance.subscribe(this, Event.SHADER_RELOAD_CMD);
    }

    @Override
    public void renderStud(List<IRenderable> renderables, ICamera camera, double t) {
        if (mustRender()) {
            renderables.forEach(r -> {
                var render = (Render) r;
                var volume = Mapper.volume.get(render.entity);
                var batch = getBatch(volume);
                try {
                    batch.begin(camera.getCamera());
                    renderObject.render(render.entity, batch, camera, getAlpha(render.entity), t, rc, getRenderGroup(), false);
                } catch (Exception e) {
                    batch.cancel();
                    throw e;
                }
                batch.end();
            });
        }
    }

    private IntModelBatch getBatch(Volume volume) {
        var batch = batches.get(volume.key);
        if (batch == null) {
            var vertexCode = ShaderTemplatingLoader.load(volume.vertexShader);
            var fragmentCode = ShaderTemplatingLoader.load(volume.fragmentShader);
            var provider = new RelativisticShaderProvider(volume.vertexShader, volume.fragmentShader, vertexCode, fragmentCode);
            batch = new IntModelBatch(provider);
            batches.put(volume.key, batch);
        }
        return batch;
    }

    protected boolean mustRender() {
        return true;
    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        if (event == Event.SHADER_RELOAD_CMD) {
            GaiaSky.postRunnable(() -> {
                // Dispose current batches.
                for (IntMap.Entry<IntModelBatch> batch : batches) {
                    batch.value.dispose();
                }
                //Clear map.
                batches.clear();
            });
        }
    }
}
