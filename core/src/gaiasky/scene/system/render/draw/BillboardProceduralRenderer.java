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
import gaiasky.render.RenderGroup;
import gaiasky.render.api.IRenderable;
import gaiasky.render.system.AbstractRenderSystem;
import gaiasky.scene.Mapper;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.AffineTransformations;
import gaiasky.scene.component.Body;
import gaiasky.scene.component.RefSysTransform;
import gaiasky.scene.component.Render;
import gaiasky.scene.record.BillboardDataset;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.util.Logger;
import gaiasky.util.Settings;
import gaiasky.util.SysUtils;
import gaiasky.util.gdx.mesh.IntMesh;
import gaiasky.util.gdx.shader.ComputeShaderProgram;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.math.Vector3Q;
import org.lwjgl.opengl.GL43;

import java.util.Arrays;
import java.util.List;

/**
 * Renders billboard datasets generated procedurally using a compute shader stage.
 */
public class BillboardProceduralRenderer extends AbstractRenderSystem {
    protected static final Logger.Log logger = Logger.getLogger(BillboardProceduralRenderer.class);

    /** The compute shader that generates particles. **/
    private final ComputeShaderProgram computeShader;
    /** Maps objects to SSBO IDs. Objects are of the {@link BillboardDataset} type. **/
    private final ObjectIntMap<BillboardDataset> ssbos;
    /** Quad mesh, same for everyone. **/
    private IntMesh quadMesh;
    /** Auxiliary matrix. **/
    protected final Matrix4 auxMat = new Matrix4();

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
        this.ssbos = new ObjectIntMap<>();
        createQuadMesh();
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


    private int datasetNum = -1;

    /**
     * Prepares a given object for rendering by allocating the SSBO.
     *
     * @param body    The body component.
     * @param dataset The billboard dataset object.
     */
    private void prepareGPUBuffer(Body body, RefSysTransform transform, BillboardDataset dataset) {
        if (!isPrepared(dataset)) {
            datasetNum++;
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
                                  123 + datasetNum,
                                  body.pos,
                                  body.size,
                                  transform.matrix.putIn(new Matrix4()));

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
                                       Vector3Q bodyPos,
                                       double bodySize,
                                       Matrix4 transform) {
        if (computeShader != null && computeShader.isCompiled()) {
            // Total element count.
            int elementCount = dataset.particleCount;
            float[] col0, col1;
            if (dataset.baseColors == null) {
                col0 = new float[]{1.0f, 1.0f, 1.0f};
                col1 = new float[]{-1.0f, -1.0f, -1.0f};
            } else if (dataset.baseColors.length >= 3 && dataset.baseColors.length < 6) {
                col0 = Arrays.copyOfRange(dataset.baseColors, 0, 3);
                col1 = new float[]{-1.0f, -1.0f, -1.0f};
            } else if (dataset.baseColors.length >= 6) {
                col0 = Arrays.copyOfRange(dataset.baseColors, 0, 3);
                col1 = Arrays.copyOfRange(dataset.baseColors, 3, 6);
            } else {
                col0 = new float[]{1.0f, 1.0f, 1.0f};
                col1 = new float[]{-1.0f, -1.0f, -1.0f};
                logger.warn("Incorrect size of baseColor array: " + Arrays.toString(dataset.baseColors));
            }

            computeShader.begin();

            // Verify the program is actually active
            int currentProgram = GL43.glGetInteger(GL43.GL_CURRENT_PROGRAM);
            if (currentProgram != computeShader.getProgram()) {
                logger.error("Shader program not active! Current: " + currentProgram + ", Expected: " + computeShader.getProgram());
            }

            var layers = prepareLayersUniform(dataset.layers);

            computeShader.setUniformUint("u_count", elementCount);
            computeShader.setUniformUint("u_distribution", dataset.distribution.ordinal());
            computeShader.setUniformUint("u_seed", seed);
            computeShader.setUniform("u_baseSize", dataset.size);
            computeShader.setUniform("u_baseRadius", dataset.baseRadius);
            computeShader.setUniform("u_minRadius", dataset.minRadius);
            computeShader.setUniform("u_baseColor0", col0[0], col0[1], col0[2]);
            computeShader.setUniform("u_baseColor1", col1[0], col1[1], col1[2]);
            computeShader.setUniform("u_eccentricity", dataset.eccentricity);
            computeShader.setUniform("u_aspect", dataset.aspect);
            computeShader.setUniform("u_heightScale", dataset.heightScale);
            computeShader.setUniform("u_spiralAngle", dataset.spiralAngle);
            computeShader.setUniformUint("u_spiralArms", dataset.spiralArms);
            if (dataset.displacement != null)
                computeShader.setUniform("u_displacement", dataset.displacement[0], dataset.displacement[1]);

            computeShader.setUniform("u_bodySize", (float) bodySize);
            computeShader.setUniform("u_bodyPos", bodyPos.put(new Vector3()));
            computeShader.setUniformMatrix("u_transform", transform);
            computeShader.setUniform("u_layers[0]", layers);

            // Check for uniform errors
            SysUtils.checkForOpenGLErrors("After uniform setting");

            computeShader.end(elementCount);
        }
    }

    protected void addAffineTransformUniforms(ExtShaderProgram program, AffineTransformations affine) {
        // Arbitrary affine transformations.
        if (affine != null && !affine.isEmpty()) {
            program.setUniformi("u_transformFlag", 1);
            affine.apply(auxMat.idt());
            program.setUniformMatrix("u_transform", auxMat);
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
            var fade = Mapper.fade.get(render.entity);
            var affine = Mapper.affine.get(render.entity);
            var billboard = Mapper.billboardSet.get(render.entity);


            // COMPUTE--Create SSBO and generate particle positions.
            for (var dataset : billboard.datasets) {
                var body = Mapper.body.get(render.entity);
                var transform = Mapper.transform.get(render.entity);
                prepareGPUBuffer(body, transform, dataset);
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
            shaderProgram.setUniformf("u_edges", (float) fade.fadeIn.y, (float) fade.fadeOut.y);

            // Arbitrary affine transformations.
            addAffineTransformUniforms(shaderProgram, affine);

            // Rel, grav, z-buffer
            addEffectsUniforms(shaderProgram, camera);

            int qualityIndex = Settings.settings.graphics.quality.ordinal();

            // Disable depth test because we are rendering to empty half-res buffer.
            Gdx.gl20.glDisable(GL20.GL_DEPTH_TEST);

            for (var dataset : billboard.datasets) {
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

                // Enable instancing (pass the SSBO data to the shader)
                GL43.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, ssbos.get(dataset, 0));

                // Draw instanced quads
                quadMesh.render(shaderProgram, GL20.GL_TRIANGLES, 0, 6, dataset.particleCount);

            }
            shaderProgram.end();
        }
    }

    @Override
    public void dispose() {
        super.dispose();

        if (ssbos != null && !ssbos.isEmpty()) {
            var ids = ssbos.values();
            while (ids.hasNext()) {
                var id = ids.next();
                GL43.glDeleteBuffers(id);
            }
        }

        if (quadMesh != null) {
            quadMesh.dispose();
        }
    }

}
