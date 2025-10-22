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
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectIntMap;
import gaiasky.event.Event;
import gaiasky.render.RenderGroup;
import gaiasky.render.api.IRenderable;
import gaiasky.render.system.InstancedRenderSystem;
import gaiasky.scene.Mapper;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.Render;
import gaiasky.scene.record.BillboardDataset;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.util.Settings;
import gaiasky.util.gdx.mesh.IntMesh;
import gaiasky.util.gdx.shader.ComputeShaderProgram;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL43;

import java.nio.FloatBuffer;
import java.util.List;

/**
 * Renders billboard datasets generated procedurally using a compute shader stage.
 */
public class BillboardProceduralRenderer extends InstancedRenderSystem {

    /** The compute shader that generates particles. **/
    private final ComputeShaderProgram computeShader;
    /** Maps objects to SSBO IDs. Objects are of the {@link BillboardDataset} type. **/
    private final ObjectIntMap<BillboardDataset> ssbos;
    /** Quad mesh, same for everyone. **/
    private IntMesh quadMesh;

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
                    1f, 1f,  1.0f, 1.0f,  // Bottom-left
                    1f, -1f, 1.0f, 0.0f,  // Bottom-right
                    -1f, -1f, 0.0f, 0.0f,  // Top-left
                    -1f, -1f, 0.0f, 0.0f,  // Top-left
                    -1f, 1f, 0.0f, 1.0f,  // Top-right
                    1f, 1f, 1.0f, 1.0f  // Bottom-left
            };

            quadMesh = new IntMesh(true, vertices.length / 4, 0, new VertexAttribute[]{VertexAttribute.Position(), VertexAttribute.TexCoords(0)});

            quadMesh.setVertices(vertices);
        }
    }

    private boolean isPrepared(BillboardDataset object) {
        return ssbos.containsKey(object);
    }

    /**
     * Prepares a given object for rendering by allocating the SSBO.
     *
     * @param dataset The billboard dataset object.
     */
    private void prepareGPUBuffer(BillboardDataset dataset) {
        if (!isPrepared(dataset)) {
            // Create an SSBO for particle data (positions, sizes, etc.)
            FloatBuffer buffer = BufferUtils.createFloatBuffer(dataset.particleCount * 9); // 9 floats per particle, 3*vec3
            int bufferId = GL43.glGenBuffers();
            GL43.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, bufferId);
            GL43.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, buffer, GL43.GL_DYNAMIC_DRAW);
            GL43.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);

            // Put in map.
            ssbos.put(dataset, bufferId);

            // Run compute shader to generate particles.
            GL43.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, bufferId);
            dispatchComputeShader(123f, dataset.particleCount, 10f);

            // Debugging: Read back SSBO data after compute shader dispatch
            FloatBuffer buff = BufferUtils.createFloatBuffer(dataset.particleCount * 9);  // 9 floats per particle
            GL43.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, bufferId);
            GL43.glGetBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0, buff);
            for (int i = 0; i < dataset.particleCount; i++) {
                System.out.println("Particle " + i + ": " +
                                           buff.get(i * 9) + ", " +  // x position
                                           buff.get(i * 9 + 1) + ", " +  // y position
                                           buff.get(i * 9 + 2));  // z position
            }
        }
    }

    private void dispatchComputeShader(float seed, int particleCount, float radius) {
        if (computeShader != null && computeShader.isCompiled()) {
            computeShader.begin();
            computeShader.setUniform("seed", seed);
            computeShader.setUniform("count", particleCount);
            computeShader.setUniform("radius", radius);
            computeShader.end(particleCount);
        }
    }

    /**
     * Binds the SSBO.
     */
    private void bindBuffer(int id) {
        GL43.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, id);
    }


    @Override
    protected void addAttributesDivisor1(Array<VertexAttribute> attributes, int primitive) {
        attributes.add(new VertexAttribute(OwnUsage.ObjectPosition, 3, "a_particlePos"));
        attributes.add(new VertexAttribute(VertexAttributes.Usage.ColorUnpacked, 3, ExtShaderProgram.COLOR_ATTRIBUTE));
        attributes.add(new VertexAttribute(OwnUsage.Additional, 3, "a_additional"));
    }

    @Override
    protected void offsets0(MeshData curr, InstancedModel model) {
        // Not used.
    }

    @Override
    protected void offsets1(MeshData curr, InstancedModel model) {
        curr.colorOffset = curr.mesh.getInstancedAttribute(VertexAttributes.Usage.ColorUnpacked) != null ? curr.mesh.getInstancedAttribute(
                VertexAttributes.Usage.ColorUnpacked).offset / 4 : 0;
        model.particlePosOffset =
                curr.mesh.getInstancedAttribute(OwnUsage.ObjectPosition) != null ? curr.mesh.getInstancedAttribute(
                        OwnUsage.ObjectPosition).offset / 4 : 0;
        model.additionalOffset = curr.mesh.getInstancedAttribute(OwnUsage.Additional) != null ? curr.mesh.getInstancedAttribute(
                OwnUsage.Additional).offset / 4 : 0;
    }

    @Override
    public void renderStud(List<IRenderable> renderables, ICamera camera, double t) {
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
                prepareGPUBuffer(dataset);
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

                // Draw instanced quads for each particle
                quadMesh.render(shaderProgram, GL20.GL_TRIANGLES, 0, 6, 1000);  // 1000 is the particle count for instancing

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

    @Override
    public void notify(Event event, Object source, Object... data) {

    }
}
