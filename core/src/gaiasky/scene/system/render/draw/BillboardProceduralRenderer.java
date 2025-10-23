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
import net.jafama.FastMath;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL43;

import java.nio.FloatBuffer;
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
                    1f, 1f, 0.0f, 1.0f, 1.0f,
                    1f, -1f, 0.0f, 1.0f, 0.0f,
                    -1f, -1f, 0.0f, 0.0f, 0.0f,
                    -1f, -1f, 0.0f, 0.0f, 0.0f,
                    -1f, 1f, 0.0f, 0.0f, 1.0f,
                    1f, 1f, 0.0f, 1.0f, 1.0f
            };

            quadMesh = new IntMesh(true, vertices.length / 5, 0, new VertexAttribute[]{VertexAttribute.Position(), VertexAttribute.TexCoords(0)});

            quadMesh.setVertices(vertices);
        }
    }

    private boolean isPrepared(BillboardDataset object) {
        return ssbos.containsKey(object);
    }


    /**
     * Prepares a given object for rendering by allocating the SSBO.
     *
     * @param body    The body component.
     * @param dataset The billboard dataset object.
     */
    private void prepareGPUBuffer(Body body, RefSysTransform transform, BillboardDataset dataset) {
        if (!isPrepared(dataset)) {
            SysUtils.checkForOpenGLErrors("prepareGPUBuffer() start", true);

            int bufferId = GL43.glGenBuffers();

            // Check if buffer was created
            if (bufferId == 0) {
                logger.error("Failed to generate buffer ID");
                return;
            }
            logger.info("Generated buffer ID: " + bufferId);

            GL43.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, bufferId);
            GL43.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, (long) dataset.particleCount * 12 * 4, GL43.GL_DYNAMIC_DRAW);


            // Add to map.
            ssbos.put(dataset, bufferId);
            logger.info("Buffer created successfully!");

            // Bind SSBO and dispatch compute shader
            GL43.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, bufferId);

            // Double-check the binding worked
            SysUtils.checkForOpenGLErrors("After bindBufferBase()");

            dispatchComputeShader(dataset.particleCount,
                                  123,
                                  dataset.size,
                                  dataset.type,
                                  body.pos,
                                  body.size,
                                  transform.matrix.putIn(new Matrix4()));

            // When reading back, account for 12 floats per particle (48 bytes / 4)
            FloatBuffer buff = BufferUtils.createFloatBuffer(dataset.particleCount * 12);
            GL43.glGetBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0, buff);
            GL43.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0); // Unbind
            for (int i = 0; i < FastMath.min(10, dataset.particleCount); i++) {
                int baseIndex = i * 12; // 12 floats per particle with padding

                var x = buff.get(baseIndex);
                var y = buff.get(baseIndex + 1);
                var z = buff.get(baseIndex + 2);
                buff.get(baseIndex + 3); // Padding - ignore

                var r = buff.get(baseIndex + 4);
                var g = buff.get(baseIndex + 5);
                var b = buff.get(baseIndex + 6);
                buff.get(baseIndex + 7); // Padding - ignore

                var size = buff.get(baseIndex + 8);
                var type = (int) buff.get(baseIndex + 9);
                var layer = (int) buff.get(baseIndex + 10);
                buff.get(baseIndex + 11); // Padding - ignore

                logger.info(String.format("Particle %d: pos[%.3f %.3f %.3f] col[%.2f %.2f %.2f] size[%.2f] type[%d] l[%d]",
                                          i, x, y, z, r, g, b, size, type, layer));
            }
        }
    }

    private void dispatchComputeShader(int elementCount,
                                       int seed,
                                       float radius,
                                       BillboardDataset.ParticleType type,
                                       Vector3Q bodyPos,
                                       double bodySize,
                                       Matrix4 transform) {
        if (computeShader != null && computeShader.isCompiled()) {
            computeShader.begin();

            // Verify the program is actually active
            int currentProgram = GL43.glGetInteger(GL43.GL_CURRENT_PROGRAM);
            if (currentProgram != computeShader.getProgram()) {
                logger.error("Shader program not active! Current: " + currentProgram + ", Expected: " + computeShader.getProgram());
            }

            computeShader.setUniformUint("u_count", elementCount);
            computeShader.setUniformUint("u_type", type.ordinal());
            computeShader.setUniformUint("u_seed", seed);
            computeShader.setUniform("u_radius", radius);

            computeShader.setUniform("u_bodySize", (float) bodySize);
            computeShader.setUniform("u_bodyPos", bodyPos.put(new Vector3()));
            computeShader.setUniformMatrix("u_transform", transform);

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
