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
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.render.RenderGroup;
import gaiasky.render.api.IRenderable;
import gaiasky.render.system.InstancedRenderSystem;
import gaiasky.scene.Mapper;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.RefSysTransform;
import gaiasky.scene.component.Render;
import gaiasky.scene.record.BillboardDataset;
import gaiasky.scene.record.CPUGalGenFallback;
import gaiasky.scene.record.ParticleVector;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.gdx.mesh.IntMesh;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.math.Matrix4Utils;
import gaiasky.util.math.Vector3Q;
import gaiasky.util.tree.GenStatus;
import org.lwjgl.opengl.GL41;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fallback renderer to {@link BillboardProceduralRenderer}, where the particle generation is done in the CPU in Java. For systems
 * that do not support OpenGL 4.3+.
 */
public class BillboardProceduralCPURenderer extends InstancedRenderSystem implements IObserver {
    protected static final Log logger = Logger.getLogger(BillboardProceduralCPURenderer.class);


    private final Map<BillboardDataset, Integer> offsets;

    /** Quad mesh, same for everyone. **/
    private IntMesh quadMesh;

    /**
     * Creates a billboard set renderer.
     *
     * @param sceneRenderer The scene renderer.
     * @param rg            The render group.
     * @param alphas        Alphas array.
     * @param shaders       The shaders.
     */
    public BillboardProceduralCPURenderer(SceneRenderer sceneRenderer,
                                          RenderGroup rg,
                                          float[] alphas,
                                          ExtShaderProgram[] shaders) {
        super(sceneRenderer, rg, alphas, shaders);
        this.offsets = new HashMap<>();

        EventManager.instance.subscribe(this, Event.GPU_DISPOSE_BILLBOARD_DATASET);
    }

    @Override
    protected void addAttributesDivisor1(Array<VertexAttribute> attributes, int primitive) {
        attributes.add(new VertexAttribute(Usage.ColorUnpacked, 3, ExtShaderProgram.COLOR_ATTRIBUTE));
        attributes.add(new VertexAttribute(OwnUsage.ObjectPosition, 3, "a_particlePos"));
        attributes.add(new VertexAttribute(OwnUsage.Additional, 3, "a_additional"));
    }

    @Override
    protected void offsets0(MeshData curr, InstancedModel model) {
        // Not needed
    }

    @Override
    protected void offsets1(MeshData curr, InstancedModel model) {
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

    private void setOffset(BillboardDataset bd, int offset) {
        offsets.put(bd, offset);
    }

    private int getOffset(BillboardDataset bd) {
        return offsets.getOrDefault(bd, -1);
    }

    private boolean isPrepared(BillboardDataset object) {
        return offsets.containsKey(object);
    }

    /**
     * Prepares the data of a billboard dataset for the GPU.
     *
     * @param bd The billboard dataset.
     *
     */
    private void prepareGPUData(IRenderable render, BillboardDataset bd) {
        // Particle count after applying completion factor.
        int count = bd.data.size();

        var model = getModel(null, "quad", null, GL41.GL_TRIANGLES, -1);
        int offset = addMeshData(model, model.numVertices, count, model.numIndices, null, "quad",
                                 GL41.GL_TRIANGLES);
        setOffset(bd, offset);
        curr = meshes.get(offset);
        model.ensureInstanceAttribsSize(count * curr.instanceSize);

        for (int i = 0; i < count; i++) {
            var particle = bd.data.get(i);
            var pv = (ParticleVector) particle;

            // COLOR
            double[] dd = pv.data();
            model.instanceAttributes[curr.instanceIdx + curr.colorOffset] = (float) dd[3];
            model.instanceAttributes[curr.instanceIdx + curr.colorOffset + 1] = (float) dd[4];
            model.instanceAttributes[curr.instanceIdx + curr.colorOffset + 2] = (float) dd[5];

            // SIZE, TYPE, TEX LAYER
            double starSize = particle.size();
            model.instanceAttributes[curr.instanceIdx + model.additionalOffset] = (float) dd[6];
            model.instanceAttributes[curr.instanceIdx + model.additionalOffset + 1] = (float) dd[7];
            model.instanceAttributes[curr.instanceIdx + model.additionalOffset + 2] = (float) dd[8];

            // OBJECT POSITION
            model.instanceAttributes[curr.instanceIdx + model.particlePosOffset] = (float) dd[0];
            model.instanceAttributes[curr.instanceIdx + model.particlePosOffset + 1] = (float) dd[1];
            model.instanceAttributes[curr.instanceIdx + model.particlePosOffset + 2] = (float) dd[2];

            curr.instanceIdx += curr.instanceSize;
            curr.numVertices++;
        }
        // Global (divisor=0) vertices (position, uv?) plus optional indices
        curr.mesh.setVertices(model.vertices, 0, model.numVertices * model.modelVertexSize);
        if (model.numIndices > 0) {
            curr.mesh.setIndices(model.indices, 0, model.numIndices);
        }
        // Per instance (divisor=1) vertices
        int size = count * curr.instanceSize;
        curr.mesh.setInstanceAttribs(model.instanceAttributes, 0, size);
        model.instanceAttributes = null;
    }

    protected void addObjectTransformUniform(ExtShaderProgram program, RefSysTransform transform, Vector3Q bodyPos, double bodySize) {
        Matrix4 objectTransform = transform.matrix == null ? auxMat.idt() : transform.matrix.putIn(auxMat);
        objectTransform.setTranslation(bodyPos.put(new Vector3()));
        Matrix4Utils.setScaling(objectTransform, (float) bodySize);

        program.setUniformMatrix("u_baseTransform", objectTransform);

    }

    @Override
    public void renderStud(List<IRenderable> renderables, ICamera camera, double t) {
        for (IRenderable renderable : renderables) {
            Render render = (Render) renderable;
            var body = Mapper.body.get(render.entity);
            var set = Mapper.billboardSet.get(render.entity);

            int i = -1;
            for (var bd : set.datasets) {
                i++;
                // Compute shaders NOT supported -- Fallback to CPU.
                if (bd.getGenStatus() == GenStatus.NOT_STARTED) {
                    // Start generation in the CPU.
                    bd.setGenStatus(GenStatus.RUNNING);
                    final var index = i;
                    final Runnable genTask = () -> {
                        try {
                            var generator = new CPUGalGenFallback(body.size);
                            var data = generator.generate(bd, (int) set.seed + index);
                            bd.data = data;
                        } catch (Exception e) {
                            bd.setGenStatus(GenStatus.FAILED);
                            logger.error(e);
                        }
                        // Done.
                        bd.setGenStatus(GenStatus.DONE);
                    };
                    GaiaSky.instance.getExecutorService().execute(genTask);
                } else if (bd.getGenStatus() == GenStatus.DONE &&
                        !isPrepared(bd)) {
                    prepareGPUData(render, bd);
                }
            }
            // Render
            render(render, camera);
        }
    }

    private void render(Render render, ICamera camera) {
        // RENDER
        float alpha = getAlpha(render);
        if (alpha > 0) {
            var affine = Mapper.affine.get(render.entity);
            var billboard = Mapper.billboardSet.get(render.entity);
            var body = Mapper.body.get(render.entity);
            var trf = Mapper.transform.get(render.entity);

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

            // Arbitrary affine transformations.
            addAffineTransformUniforms(shaderProgram, affine);
            addObjectTransformUniform(shaderProgram, trf, body.pos, body.size);

            // Rel, grav, z-buffer
            addEffectsUniforms(shaderProgram, camera);

            int qualityIndex = Settings.settings.graphics.quality.ordinal();

            Gdx.gl20.glDisable(GL20.GL_DEPTH_TEST);
            for (var dataset : billboard.datasets) {
                if (isPrepared(dataset)) {
                    var offset = getOffset(dataset);
                    curr = meshes.get(offset);

                    if (curr != null) {
                        // Blend mode
                        switch (dataset.blending) {
                            case ALPHA -> {
                                Gdx.gl20.glEnable(GL20.GL_BLEND);
                                Gdx.gl20.glBlendEquation(GL20.GL_FUNC_ADD);
                                Gdx.gl20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
                            }
                            case ADDITIVE -> {
                                Gdx.gl20.glEnable(GL20.GL_BLEND);
                                Gdx.gl20.glBlendEquation(GL20.GL_FUNC_ADD);
                                Gdx.gl20.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE);
                            }
                            case COLOR -> {
                                Gdx.gl20.glEnable(GL20.GL_BLEND);
                                Gdx.gl20.glBlendEquation(GL20.GL_FUNC_ADD);
                                Gdx.gl20.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_COLOR);
                            }
                            case SUBTRACTIVE -> {
                                Gdx.gl20.glBlendEquation(GL20.GL_FUNC_REVERSE_SUBTRACT);
                                Gdx.gl20.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE);
                            }
                            case NONE -> {
                                Gdx.gl20.glBlendEquation(GL20.GL_FUNC_ADD);
                                Gdx.gl20.glDisable(GL20.GL_BLEND);
                            }
                        }

                        // Specific uniforms
                        double pointScaleFactor = 1.8e7;
                        shaderProgram.setUniformf("u_maxPointSize", (float) dataset.maxSizes[qualityIndex]);
                        shaderProgram.setUniformf("u_sizeFactor", (float) (dataset.size * pointScaleFactor));
                        shaderProgram.setUniformf("u_intensity", dataset.intensity);

                        Gdx.gl20.glDepthMask(dataset.depthMask);
                        // Render mesh
                        int count = curr.mesh.getNumIndices() > 0 ? curr.mesh.getNumIndices() : curr.mesh.getNumVertices();
                        curr.mesh.render(shaderProgram, GL20.GL_TRIANGLES, 0, count, dataset.particleCount);
                    }
                }
            }
            shaderProgram.end();

            // Restore blending
            Gdx.gl20.glEnable(GL20.GL_BLEND);
            Gdx.gl20.glBlendEquation(GL20.GL_FUNC_ADD);
            Gdx.gl20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        if (event == Event.GPU_DISPOSE_BILLBOARD_DATASET) {
            var renderable = (Render) data[0];
            var bb = Mapper.billboardSet.get(renderable.getEntity());
            for (var bd : bb.datasets) {
                int offset = getOffset(bd);
                if (offset >= 0) {
                    clearMeshData(offset);
                    offsets.remove(bd);
                    bd.setGenStatus(GenStatus.NOT_STARTED);
                }
            }
        }
    }

}
