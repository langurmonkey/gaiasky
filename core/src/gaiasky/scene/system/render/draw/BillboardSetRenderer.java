/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render.draw;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.utils.Array;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.render.RenderGroup;
import gaiasky.render.api.IRenderable;
import gaiasky.render.system.InstancedRenderSystem;
import gaiasky.scene.Mapper;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.Render;
import gaiasky.scene.record.BillboardDataset;
import gaiasky.scene.record.BillboardDataset.ParticleType;
import gaiasky.scene.record.ParticleVector;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.math.StdRandom;
import gaiasky.util.tree.LoadStatus;
import net.jafama.FastMath;
import org.lwjgl.opengl.GL41;

import java.util.List;

public class BillboardSetRenderer extends InstancedRenderSystem implements IObserver {
    protected static final Log logger = Logger.getLogger(BillboardSetRenderer.class);

    private final ColorGenerator starColorGenerator;
    private final ColorGenerator dustColorGenerator;


    /**
     * Creates a billboard set renderer.
     *
     * @param sceneRenderer The scene renderer.
     * @param rg            The render group.
     * @param alphas        Alphas array.
     * @param shaders       The shaders.
     */
    public BillboardSetRenderer(SceneRenderer sceneRenderer,
                                RenderGroup rg,
                                float[] alphas,
                                ExtShaderProgram[] shaders) {
        super(sceneRenderer, rg, alphas, shaders);

        starColorGenerator = new StarColorGenerator();
        dustColorGenerator = new DustColorGenerator();

        EventManager.instance.subscribe(this, Event.GPU_DISPOSE_BILLBOARD_DATASET);
    }

    @Override
    protected void addAttributesDivisor1(Array<VertexAttribute> attributes, int primitive) {
        attributes.add(new VertexAttribute(Usage.ColorUnpacked, 3, ExtShaderProgram.COLOR_ATTRIBUTE));
        attributes.add(new VertexAttribute(OwnUsage.ObjectPosition, 3, "a_particlePos"));
        attributes.add(new VertexAttribute(OwnUsage.Additional, 3, "a_additional"));
    }

    @Override
    protected void offsets0(MeshData curr, InstancedRenderSystem.InstancedModel model) {
        // Not needed
    }

    @Override
    protected void offsets1(MeshData curr, InstancedRenderSystem.InstancedModel model) {
        curr.colorOffset = curr.mesh.getInstancedAttribute(Usage.ColorUnpacked) != null ? curr.mesh.getInstancedAttribute(
                Usage.ColorUnpacked).offset / 4 : 0;
        model.particlePosOffset =
                curr.mesh.getInstancedAttribute(OwnUsage.ObjectPosition) != null ? curr.mesh.getInstancedAttribute(
                        OwnUsage.ObjectPosition).offset / 4 : 0;
        model.additionalOffset = curr.mesh.getInstancedAttribute(OwnUsage.Additional) != null ? curr.mesh.getInstancedAttribute(
                OwnUsage.Additional).offset / 4 : 0;
    }


    @Override
    protected void initShaderProgram() {
        for (ExtShaderProgram shaderProgram : programs) {
            if (shaderProgram.isCompiled()) {
                shaderProgram.begin();
                shaderProgram.setUniformf("u_pointAlphaMin", 0.1f);
                shaderProgram.setUniformf("u_pointAlphaMax", 1.0f);
                shaderProgram.end();
            }
        }
    }

    /**
     * Prepares the data of a billboard dataset for the GPU.
     *
     * @param bd The billboard dataset.
     * @param cg The color generator.
     *
     */
    private void prepareGPUData(IRenderable render, BillboardDataset bd, ColorGenerator cg) {
        // True particle count for this dataset.
        int totalCount = bd.data.size();
        // Particle count after applying completion factor.
        int count = 0;
        // Compute what particles are in based on completion.
        float completion = bd.completion == null ? 1f : bd.completion[Settings.settings.graphics.quality.ordinal()];
        StdRandom.setSeed(11447799L);
        byte[] in = new byte[totalCount];
        for (int i = 0; i < totalCount; i++) {
            in[i] = (completion == 1f || StdRandom.uniform() <= completion) ? (byte) 1 : (byte) 0;
            count += in[i];
        }

        var model = getModel(null, "quad", null, GL41.GL_TRIANGLES, getOffset(render));
        int offset = addMeshData(model, model.numVertices, count, model.numIndices, null, "quad",
                                 GL41.GL_TRIANGLES);
        setOffset(render, offset);
        curr = meshes.get(offset);
        model.ensureInstanceAttribsSize(count * curr.instanceSize);

        int nLayers = bd.layers.length;

        for (int i = 0; i < in.length; i++) {
            if (in[i] != 0) {
                var particle = bd.data.get(i);
                int layer = StdRandom.uniform(nLayers);
                var pv = (ParticleVector) particle;

                // COLOR
                double[] doubleData = pv.data();
                float[] col = doubleData.length >= 7 ? new float[]{(float) doubleData[4], (float) doubleData[5], (float) doubleData[6]} : cg.generateColor();
                model.instanceAttributes[curr.instanceIdx + curr.colorOffset] = col[0];
                model.instanceAttributes[curr.instanceIdx + curr.colorOffset + 1] = col[1];
                model.instanceAttributes[curr.instanceIdx + curr.colorOffset + 2] = col[2];

                // SIZE, TYPE, TEX LAYER
                double starSize = particle.size();
                model.instanceAttributes[curr.instanceIdx + model.additionalOffset] = (float) starSize;
                model.instanceAttributes[curr.instanceIdx + model.additionalOffset + 1] = (float) bd.type.ordinal();
                model.instanceAttributes[curr.instanceIdx + model.additionalOffset + 2] = (float) bd.layers[layer];

                // OBJECT POSITION
                model.instanceAttributes[curr.instanceIdx + model.particlePosOffset] = (float) particle.x();
                model.instanceAttributes[curr.instanceIdx + model.particlePosOffset + 1] = (float) particle.y();
                model.instanceAttributes[curr.instanceIdx + model.particlePosOffset + 2] = (float) particle.z();

                curr.instanceIdx += curr.instanceSize;
                curr.numVertices++;
            }
        }
        // Global (divisor=0) vertices (position, uv?) plus optional indices
        curr.mesh.setVertices(model.vertices, 0, model.numVertices * model.modelVertexSize);
        if (model.numIndices > 0) {
            curr.mesh.setIndices(model.indices, 0, model.numIndices);
        }
        // Per instance (divisor=1) vertices
        int size = count * curr.instanceSize;
        setCount(render, count);
        curr.mesh.setInstanceAttribs(model.instanceAttributes, 0, size);
        model.instanceAttributes = null;

        setInGpu(render, true);
    }

    private ColorGenerator getColorGenerator(final ParticleType type) {
        return type == ParticleType.DUST ? dustColorGenerator : starColorGenerator;
    }


    @Override
    public void renderStud(List<IRenderable> renderables, ICamera camera, double t) {
        for (IRenderable renderable : renderables) {
            Render render = (Render) renderable;
            var set = Mapper.billboardSet.get(render.entity);

            switch (set.status.get()) {
                case NOT_LOADED -> {
                    // Load data.
                    set.setStatus(LoadStatus.LOADING);
                    for (var bds : set.datasets) {
                        prepareGPUData(render, bds, getColorGenerator(bds.type));
                    }
                    set.setStatus(LoadStatus.LOADED);
                }
                case LOADED -> render(render, camera);
            }
        }
    }

    private void render(Render render, ICamera camera) {
        // RENDER
        float alpha = getAlpha(render);
        if (alpha > 0) {
            var fade = Mapper.fade.get(render.entity);
            var affine = Mapper.affine.get(render.entity);
            var billboard = Mapper.billboardSet.get(render.entity);

            ExtShaderProgram shaderProgram = getShaderProgram();

            shaderProgram.begin();

            // Global uniforms.
            if (billboard.textureArray != null) {
                billboard.textureArray.bind(GL20.GL_TEXTURE10);
            }
            shaderProgram.setUniformMatrix("u_projView", camera.getCamera().combined);
            shaderProgram.setUniformf("u_camPos", camera.getPos());
            addCameraUpCubemapMode(shaderProgram, camera);
            shaderProgram.setUniformf("u_alpha", render.getOpacity() * alpha * 1.5f);
            shaderProgram.setUniformf("u_edges", (float) fade.fadeIn.y, (float) fade.fadeOut.y);

            // Arbitrary affine transformations.
            addAffineTransformUniforms(shaderProgram, affine);

            // Rel, grav, z-buffer
            addEffectsUniforms(shaderProgram, camera);

            int qualityIndex = Settings.settings.graphics.quality.ordinal();

            // Disable depth test because we are rendering to empty half-res buffer.
            Gdx.gl20.glDisable(GL20.GL_DEPTH_TEST);

            var offsets = getOffsets(render);
            for (int i = 0; i < offsets.size; i++) {
                var offset = offsets.get(i);
                curr = meshes.get(offset);

                if (curr != null) {
                    BillboardDataset dataset = billboard.datasets[i];
                    // Blend mode
                    Gdx.gl20.glBlendEquation(GL20.GL_FUNC_ADD);
                    Gdx.gl20.glEnable(GL20.GL_BLEND);
                    switch (dataset.blending) {
                        case ALPHA -> Gdx.gl20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
                        case ADDITIVE -> Gdx.gl20.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE);
                        case COLOR -> Gdx.gl20.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_COLOR);
                        case SUBTRACTIVE -> {
                            Gdx.gl20.glBlendEquation(GL20.GL_FUNC_REVERSE_SUBTRACT);
                            Gdx.gl20.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE);
                        }
                        case NONE -> Gdx.gl20.glDisable(GL20.GL_BLEND);
                    }

                    // Specific uniforms
                    double pointScaleFactor = 1.8e7;
                    shaderProgram.setUniformf("u_maxPointSize", (float) dataset.maxSizes[qualityIndex]);
                    shaderProgram.setUniformf("u_sizeFactor", (float) (dataset.size * pointScaleFactor));
                    shaderProgram.setUniformf("u_intensity", dataset.intensity);

                    Gdx.gl20.glDepthMask(dataset.depthMask);
                    // Render mesh
                    int count = curr.mesh.getNumIndices() > 0 ? curr.mesh.getNumIndices() : curr.mesh.getNumVertices();
                    curr.mesh.render(shaderProgram, GL20.GL_TRIANGLES, 0, count, getCounts(render).get(i));
                }
            }
            shaderProgram.end();
        }
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        if (event == Event.GPU_DISPOSE_BILLBOARD_DATASET) {
            IRenderable renderable = (IRenderable) source;
            int offset = getOffset(renderable);
            if (offset >= 0) {
                clearMeshData(offset);
                models.set(offset, null);
            }
        }
    }

    private interface ColorGenerator {
        float[] generateColor();
    }

    private static class StarColorGenerator implements ColorGenerator {
        public float[] generateColor() {
            float r = (float) StdRandom.gaussian() * 0.15f;
            if (StdRandom.uniform(2) == 0) {
                // Blue/white star
                return new float[]{0.95f - r, 0.8f - r, 0.6f};
            } else {
                // Red/white star
                return new float[]{0.95f, 0.8f - r, 0.6f - r};
            }
        }

    }

    private static class DustColorGenerator implements ColorGenerator {
        @Override
        public float[] generateColor() {
            float r = (float) FastMath.abs(StdRandom.uniform() * 0.2 + 0.07);
            return new float[]{r, r, r};
        }
    }
}
