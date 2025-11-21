/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render.draw;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ObjectIntMap;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.render.RenderGroup;
import gaiasky.render.api.IRenderable;
import gaiasky.render.system.AbstractRenderSystem;
import gaiasky.scene.Mapper;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.*;
import gaiasky.scene.record.BillboardDataset;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Settings;
import gaiasky.util.SysUtils;
import gaiasky.util.gdx.mesh.IntMesh;
import gaiasky.util.gdx.shader.ComputeShaderProgram;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.math.Matrix4Utils;
import gaiasky.util.math.Vector3Q;
import org.lwjgl.opengl.GL43;

import java.util.List;

/**
 * Renders billboard datasets generated procedurally using a compute shader stage.
 */
public class BillboardProceduralRenderer extends AbstractRenderSystem implements IObserver {
    protected static final Logger.Log logger = Logger.getLogger(BillboardProceduralRenderer.class);

    /** The compute shader that generates particles. **/
    private final ComputeShaderProgram computeShader;
    /** Maps objects to SSBO IDs. Objects are of the {@link BillboardDataset} type. **/
    private final ObjectIntMap<BillboardDataset> ssbos;
    /** Quad mesh, same for everyone. **/
    private IntMesh quadMesh;
    /** Auxiliary matrices. **/
    protected final Matrix4 auxMat1 = new Matrix4();
    protected final Matrix4 auxMat2 = new Matrix4();

    /**
     * Creates a billboard set renderer.
     *
     * @param sceneRenderer The scene renderer.
     * @param rg            The render group.
     * @param alphas        Alphas array.
     * @param computeShader The shader.
     */
    public BillboardProceduralRenderer(SceneRenderer sceneRenderer,
                                       RenderGroup rg,
                                       float[] alphas,
                                       ExtShaderProgram[] shaders,
                                       ComputeShaderProgram computeShader) {
        super(sceneRenderer, rg, alphas, shaders);
        this.computeShader = computeShader;
        this.ssbos = new ObjectIntMap<>(15);
        createQuadMesh();

        EventManager.instance.subscribe(this, Event.SHADER_RELOAD_CMD, Event.GPU_DISPOSE_BILLBOARD_DATASET);
    }

    private void createQuadMesh() {
        if (quadMesh == null) {
            // Create a simple quad with texture coordinates
            float[] vertices = {
                    1f, 1f, 1.0f, 1.0f,
                    1f, -1f, 1.0f, 0.0f,
                    -1f, -1f, 0.0f, 0.0f,
                    -1f, -1f, 0.0f, 0.0f,
                    -1f, 1f, 0.0f, 1.0f,
                    1f, 1f, 1.0f, 1.0f
            };

            quadMesh = new IntMesh(true,
                                   vertices.length / 4,
                                   0,
                                   new VertexAttribute[]{
                                           new VertexAttribute(VertexAttributes.Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE),
                                           VertexAttribute.TexCoords(0)});

            quadMesh.setVertices(vertices);
        }
    }

    private boolean isPrepared(BillboardDataset object) {
        return ssbos.containsKey(object);
    }

    /**
     * Prepares a given object for rendering by allocating the SSBO.
     *
     * @param body      The body component.
     * @param billboard The billboard set component.
     * @param dataset   The billboard dataset object.
     */
    private void prepareGPUBuffer(Body body, BillboardSet billboard, BillboardDataset dataset) {
        if (!isPrepared(dataset)) {
            SysUtils.checkForOpenGLErrors("prepareGPUBuffer() start", true);

            int bufferId = GL43.glGenBuffers();

            // Check if buffer was created
            if (bufferId == 0) {
                logger.error("Failed to generate buffer ID");
                return;
            }

            GL43.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, bufferId);
            GL43.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, (long) dataset.particleCount * 12 * 4, GL43.GL_DYNAMIC_DRAW);

            // Add to map.
            ssbos.put(dataset, bufferId);

            // Bind SSBO and dispatch compute shader
            GL43.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, bufferId);

            // Double-check the binding worked
            SysUtils.checkForOpenGLErrors("After bindBufferBase()");

            dispatchComputeShader(dataset,
                                  (int) billboard.seed,
                                  body);

        }
    }

    private int[] prepareLayersUniform(int[] layers) {
        final int size = 15;
        int[] out = new int[size];
        for (int i = 0; i < out.length; i++) {
            if (i < layers.length) {
                out[i] = layers[i];
            } else {
                out[i] = -1;
            }
        }
        return out;
    }

    private void dispatchComputeShader(BillboardDataset dataset,
                                       int seed,
                                       Body body) {
        if (computeShader != null && computeShader.isCompiled()) {
            // Total element count.
            int elementCount = dataset.particleCount;

            computeShader.begin();

            // Verify the program is actually active
            int currentProgram = GL43.glGetInteger(GL43.GL_CURRENT_PROGRAM);
            if (currentProgram != computeShader.getProgram()) {
                logger.error("Shader program not active! Current: " + currentProgram + ", Expected: " + computeShader.getProgram());
            }

            var layers = prepareLayersUniform(dataset.layers);
            var bodySize = body.size;

            computeShader.setUniformUint("u_count", elementCount);
            computeShader.setUniformUint("u_distribution", dataset.distribution.ordinal());
            computeShader.setUniformUint("u_seed", seed);
            computeShader.setUniformUint("u_type", dataset.type.ordinal());
            computeShader.setUniform("u_sizeFactor", (float) (100 * bodySize / (26000.0 * Constants.PC_TO_U)));
            computeShader.setUniform("u_sizeNoise", dataset.sizeMask ? -Math.abs(dataset.sizeNoise) : Math.abs(dataset.sizeNoise));
            computeShader.setUniform("u_baseRadius", dataset.baseRadius);
            computeShader.setUniform("u_minRadius", dataset.minRadius);
            computeShader.setUniform3fv("u_baseColors[0]", dataset.baseColors);
            computeShader.setUniform("u_colorNoise", dataset.colorNoise);
            computeShader.setUniform2fv("u_eccentricity", dataset.eccentricity);
            computeShader.setUniform("u_aspect", dataset.aspect);
            computeShader.setUniform("u_heightScale", dataset.heightScale);
            computeShader.setUniform("u_baseAngle", dataset.baseAngle);
            computeShader.setUniformUint("u_numArms", dataset.numArms);
            computeShader.setUniform("u_armSigma", dataset.armSigma);
            if (dataset.spiralDeltaPos != null)
                computeShader.setUniform("u_sprialDeltaPos", dataset.spiralDeltaPos[0], dataset.spiralDeltaPos[1]);

            // Dataset and ref-sys transformations.
            addDatasetTransformUniform(dataset);
            // Layers.
            computeShader.setUniform("u_layers[0]", layers);

            // Check for uniform errors
            SysUtils.checkForOpenGLErrors("After uniform setting");

            computeShader.end(elementCount);
        }
    }

    protected void addDatasetTransformUniform(BillboardDataset dataset) {
        var datasetTransform = auxMat1;
        datasetTransform.setFromEulerAngles(dataset.rotation.y, dataset.rotation.x, dataset.rotation.z);
        datasetTransform.setTranslation(dataset.translation);
        Matrix4Utils.setScaling(datasetTransform, dataset.scale);

        computeShader.setUniformMatrix("u_baseTransform", datasetTransform);

    }

    protected void addBaseTransformUniform(ExtShaderProgram program, RefSysTransform transform, Vector3Q bodyPos, double bodySize) {
        Matrix4 objectTransform = transform.matrix == null ? auxMat2.idt() : transform.matrix.putIn(auxMat2);
        objectTransform.setTranslation(bodyPos.put(new Vector3()));
        Matrix4Utils.setScaling(objectTransform, (float) bodySize);

        program.setUniformMatrix("u_baseTransform", objectTransform);

    }

    protected void addAffineTransformUniforms(ExtShaderProgram program, AffineTransformations affine) {
        // Arbitrary affine transformations.
        if (affine != null && !affine.isEmpty()) {
            program.setUniformi("u_transformFlag", 1);
            affine.apply(auxMat1.idt());
            program.setUniformMatrix("u_transform", auxMat1);
        } else {
            program.setUniformi("u_transformFlag", 0);
        }
    }

    @Override
    public void renderStud(List<IRenderable> renderables, ICamera camera, double t) {
        if (!Settings.settings.runtime.compute) {
            return;
        }
        for (var renderable : renderables) {
            render((Render) renderable, camera);
        }
    }

    private void render(Render render, ICamera camera) {
        // RENDER
        float alpha = getAlpha(render);
        if (alpha > 0) {
            var body = Mapper.body.get(render.entity);
            var affine = Mapper.affine.get(render.entity);
            var trf = Mapper.transform.get(render.entity);
            var billboard = Mapper.billboardSet.get(render.entity);


            // COMPUTE--Create SSBO and generate particle positions.
            for (var dataset : billboard.datasets) {
                prepareGPUBuffer(body, billboard, dataset);
            }


            // RENDER--Actually render particles.
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
            addBaseTransformUniform(shaderProgram, trf, body.pos, body.size);

            // Rel, grav, z-buffer
            addEffectsUniforms(shaderProgram, camera);

            int qualityIndex = Settings.settings.graphics.quality.ordinal();

            // Disable depth test because we are rendering to empty half-res buffer.
            Gdx.gl20.glDisable(GL20.GL_DEPTH_TEST);

            for (var dataset : billboard.datasets) {
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

                // Enable instancing (pass the SSBO data to the shader)
                GL43.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, ssbos.get(dataset, 0));

                // Draw instanced quads
                quadMesh.render(shaderProgram, GL20.GL_TRIANGLES, 0, 6, dataset.particleCount);

            }
            shaderProgram.end();

            // Restore blending
            Gdx.gl20.glEnable(GL20.GL_BLEND);
            Gdx.gl20.glBlendEquation(GL20.GL_FUNC_ADD);
            Gdx.gl20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }
    }

    private void cleanSSBOs() {
        if (ssbos != null && !ssbos.isEmpty()) {
            var ids = ssbos.values();
            while (ids.hasNext()) {
                var id = ids.next();
                GL43.glDeleteBuffers(id);
            }
            ssbos.clear();
        }
    }

    private void cleanSSBO(BillboardDataset ds) {
        if (ssbos != null && !ssbos.isEmpty()) {
            if (ssbos.containsKey(ds)) {
                var id = ssbos.remove(ds, -1);
                if (id >= 0) {
                    GL43.glDeleteBuffers(id);
                }
            }
        }

    }

    @Override
    public void dispose() {
        super.dispose();
        // Clean SSBOs.
        cleanSSBOs();
        // Dispose quad mesh.
        if (quadMesh != null) {
            quadMesh.dispose();
        }
    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        if (event == Event.SHADER_RELOAD_CMD) {
            GaiaSky.postRunnable(() -> {
                cleanSSBOs();
                computeShader.reload();
            });
        } else if (event == Event.GPU_DISPOSE_BILLBOARD_DATASET) {
            IRenderable renderable = (IRenderable) data[0];
            if (renderable instanceof Render r) {
                GaiaSky.postRunnable(() -> {
                    var bb = Mapper.billboardSet.get(r.getEntity());
                    if (bb != null) {
                        for (var ds : bb.datasets) {
                            cleanSSBO(ds);
                        }
                    }
                });
            }
        }
    }
}
