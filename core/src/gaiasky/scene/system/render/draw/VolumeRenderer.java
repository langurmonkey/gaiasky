/*
 * Copyright (c) 2023-2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render.draw;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.IntMap;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.render.RenderGroup;
import gaiasky.render.api.IRenderable;
import gaiasky.render.gdx.graphics.VolumeDepthMask;
import gaiasky.render.system.AbstractRenderSystem;
import gaiasky.scene.Mapper;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.Render;
import gaiasky.scene.component.Volume;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.scene.system.render.draw.model.ModelEntityRenderSystem;
import gaiasky.render.gdx.IntModelBatch;
import gaiasky.render.gdx.shader.loader.ShaderTemplatingLoader;
import gaiasky.render.gdx.shader.provider.RelativisticShaderProvider;
import gaiasky.render.gdx.shader.provider.ShaderProgramProvider;

import java.util.List;

public class VolumeRenderer extends AbstractRenderSystem implements IObserver {

    /**
     * Prepended to the fragment shader of the depth-only pre-pass. {@link
     * gaiasky.render.gdx.shader.provider.ShaderProgramProvider#getShaderCode(String, String)}
     * inserts it right after the <code>#version</code> line, which is where a <code>#define</code>
     * has to be.
     */
    private static final String DEPTH_PASS_DEFINE = "#define DEPTH_PASS\n";

    private final ModelEntityRenderSystem renderObject;
    protected final IntMap<IntModelBatch> batches;
    /** Per-volume depth targets for the depth-only pre-pass. See {@link VolumeDepthMask}. */
    private final VolumeDepthMask depthMask = new VolumeDepthMask();
    /**
     * Whether this renderer is the depth-only pre-pass (see {@link RenderGroup#VOLUME_DEPTH}). If
     * so, the volume shader is compiled with {@code DEPTH_PASS} defined: it marches to the first
     * sample dense enough to occlude (see {@link gaiasky.render.gdx.shader.attribute.Texture3DAttribute#depthThreshold})
     * and returns, so the pass costs one ray-voxel-step per pixel instead of the whole
     * integration, and only the depth write survives the colour mask.
     */
    private final boolean depthPass;

    /**
     * Creates a new volume model renderer which may use a number of different batches.
     *
     * @param rg     The render group.
     * @param alphas The alphas list.
     */
    public VolumeRenderer(SceneRenderer sceneRenderer, RenderGroup rg, float[] alphas) {
        this(sceneRenderer, rg, alphas, false);
    }

    /**
     * Creates a new volume model renderer which may use a number of different batches.
     *
     * @param rg        The render group.
     * @param alphas    The alphas list.
     * @param depthPass Whether this is the depth-only pre-pass. Compiles the volume shader with
     *                  {@code DEPTH_PASS} defined, which makes the raymarch stop at the first
     *                  sample that found material.
     */
    public VolumeRenderer(SceneRenderer sceneRenderer, RenderGroup rg, float[] alphas, boolean depthPass) {
        super(sceneRenderer, rg, alphas, null);
        this.depthPass = depthPass;
        this.batches = new IntMap<>();
        this.renderObject = new ModelEntityRenderSystem(sceneRenderer);
        EventManager.instance.subscribe(this, Event.SHADER_RELOAD_CMD);
    }

    @Override
    public void renderStud(List<IRenderable> renderables, ICamera camera, double t) {
        if (!mustRender()) {
            return;
        }
        if (depthPass) {
            renderDepthPass(renderables, camera, t);
            return;
        }
        renderables.forEach(r -> {
            var render = (Render) r;
            var volume = Mapper.volume.get(render.entity);
            // Before the batch: the mask #define comes from the material's attributes.
            bindDepthMask(render, volume);
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

    /**
     * Depth-only pre-pass: one volume into a target of its own, then composed into the scene depth.
     * The stored fronts let the colour pass tell its own dust from an occluder (see VolumeDepthMask).
     */
    private void renderDepthPass(List<IRenderable> renderables,
                                 ICamera camera,
                                 double t) {
        try {
            for (var r : renderables) {
                var render = (Render) r;
                var volume = Mapper.volume.get(render.entity);
                if (depthMask.begin(volume) == null) {
                    // No depth textures here: volumes still draw, they just occlude nothing.
                    return;
                }
                bindDepthMask(render, volume);
                var batch = getBatch(volume);
                try {
                    batch.begin(camera.getCamera());
                    renderObject.render(render.entity, batch, camera, getAlpha(render.entity), t, rc, getRenderGroup(), false);
                } catch (Exception e) {
                    batch.cancel();
                    throw e;
                }
                batch.end();
            }
        } finally {
            // Back to the scene's framebuffer, whatever happened.
            depthMask.restore();
        }
        // One fullscreen quad per volume: a minimum against what the scene depth already holds.
        for (var r : renderables) {
            depthMask.compose(depthMask.texture(Mapper.volume.get(((Render) r).entity)));
        }
    }

    /** Binds this volume's own front and the shared scene depth to its material. */
    private void bindDepthMask(Render render,
                               Volume volume) {
        var model = Mapper.model.get(render.entity);
        if (model == null || model.model == null || model.model.mtc == null) {
            return;
        }
        var own = depthMask.texture(volume);
        var scene = sceneDepth();
        var pass = passBuffer(render);
        if (own == null || scene == null || pass == null) {
            return;
        }
        // Mask texels per fragment of this pass: 2 in the half-resolution buffer.
        model.model.mtc.setVolumeDepthMask(own, scene,
                                           own.getWidth() / (float) pass.getWidth(),
                                           own.getHeight() / (float) pass.getHeight());
    }

    /** @return The framebuffer this volume's colour pass draws into. */
    private FrameBuffer passBuffer(Render render) {
        var pingPong = rc != null && rc.ppb != null ? rc.ppb.pp : null;
        if (pingPong == null || pingPong.getCombinedBuffer() == null) {
            return null;
        }
        var combined = pingPong.getCombinedBuffer();
        return render.halfResolutionBuffer ? combined.getHalfBuffer() : combined.getFullBuffer();
    }

    /**
     * The depth buffer to compare against: always the full-resolution one, which matches the mask
     * texel for texel and holds everything drawn since the pre-pass.
     */
    private Texture sceneDepth() {
        var pingPong = rc != null && rc.ppb != null ? rc.ppb.pp : null;
        if (pingPong == null) {
            return null;
        }
        var combined = pingPong.getCombinedBuffer();
        if (combined == null) {
            return null;
        }
        var buffer = combined.getFullBuffer();
        return buffer == null ? null : buffer.getDepthBufferTexture();
    }

    private IntModelBatch getBatch(Volume volume) {
        var batch = batches.get(volume.key);
        if (batch == null) {
            var vertexCode = ShaderTemplatingLoader.load(volume.vertexShader);
            var fragmentCode = ShaderTemplatingLoader.load(volume.fragmentShader);
            if (depthPass) {
                // Same shader, compiled for the depth-only pre-pass: it stops at the first sample
                // with material instead of integrating the whole ray.
                fragmentCode = ShaderProgramProvider.getShaderCode(DEPTH_PASS_DEFINE, fragmentCode);
            }
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
    public void dispose() {
        depthMask.disposeAll();
        super.dispose();
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
