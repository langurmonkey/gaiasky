/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render.draw;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.math.MathUtils;
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
import gaiasky.scene.api.IParticleRecord;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.Render;
import gaiasky.scene.entity.ParticleUtils;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.ModelCache;
import gaiasky.util.Settings.SceneSettings.StarSettings;
import gaiasky.util.color.Colormap;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.math.StdRandom;

import java.util.Random;

/**
 * Renders particle sets as instanced triangles. It contains the base particle renderer (only positions), and the
 * extended particle renderer (with positions, sizes, colors, proper motions, etc.).
 */
public class ParticleSetInstancedRenderer extends InstancedRenderSystem implements IObserver {
    protected static final Log logger = Logger.getLogger(ParticleSetInstancedRenderer.class);

    /** Whether to use the extended particle set mode or not. **/
    private final boolean extended;
    private final Vector3 aux1;
    private final Random rand;
    private final Colormap cmap;
    private final ParticleUtils utils;
    private int sizeOffset, particlePosOffset, properMotionOffset, textureIndexOffset;

    /**
     * Constructs a particle set instanced renderer using the given model
     * (see {@link ModelCache} for more information on available models)
     * and the given parameters.
     *
     * @param sceneRenderer The scene renderer.
     * @param rg            The render group.
     * @param alphas        The alphas.
     * @param quadShaders   The shader programs to render quads.
     * @param modelType     The type of model to use.
     */
    public ParticleSetInstancedRenderer(SceneRenderer sceneRenderer,
                                        RenderGroup rg,
                                        float[] alphas,
                                        ExtShaderProgram[] quadShaders,
                                        ModelType modelType) {
        super(sceneRenderer, rg, alphas, quadShaders, modelType);
        utils = new ParticleUtils();
        extended = rg == RenderGroup.PARTICLE_GROUP_EXT || rg == RenderGroup.PARTICLE_GROUP_EXT_SPHERE;

        rand = new Random(123);
        aux1 = new Vector3();
        cmap = new Colormap();
        EventManager.instance.subscribe(this, Event.GPU_DISPOSE_PARTICLE_GROUP);
    }

    /**
     * Constructs a particle set instanced renderer using the default quad model
     * and the given parameters.
     *
     * @param sceneRenderer The scene renderer.
     * @param rg            The render group.
     * @param alphas        The alphas.
     * @param shaders       The shader programs to render quads.
     */
    public ParticleSetInstancedRenderer(SceneRenderer sceneRenderer,
                                        RenderGroup rg,
                                        float[] alphas,
                                        ExtShaderProgram[] shaders) {

        this(sceneRenderer, rg, alphas, shaders, ModelType.QUAD);
    }

    @Override
    protected void initShaderProgram() {
        // Empty
    }

    @Override
    protected void addAttributesDivisor0(Array<VertexAttribute> attributes) {
        // Vertex position and texture coordinates are global
        attributes.add(new VertexAttribute(Usage.Position, modelType.isQuad() ? 2 : 3, ExtShaderProgram.POSITION_ATTRIBUTE));
        if (modelType.isQuad()) {
            attributes.add(new VertexAttribute(Usage.TextureCoordinates, 2, ExtShaderProgram.TEXCOORD_ATTRIBUTE));
        }
    }

    @Override
    protected void addAttributesDivisor1(Array<VertexAttribute> attributes) {
        // Color, object position, proper motion and size are per instance
        attributes.add(new VertexAttribute(Usage.ColorPacked, 4, ExtShaderProgram.COLOR_ATTRIBUTE));
        attributes.add(new VertexAttribute(OwnUsage.ObjectPosition, 3, "a_particlePos"));
        if (extended) {
            attributes.add(new VertexAttribute(OwnUsage.ProperMotion, 3, "a_pm"));
        }
        attributes.add(new VertexAttribute(OwnUsage.Size, 1, "a_size"));
        attributes.add(new VertexAttribute(OwnUsage.TextureIndex, 1, "a_textureIndex"));
    }

    @Override
    protected void offsets0(MeshData curr) {
        // Not needed
    }

    @Override
    protected void offsets1(MeshData curr) {
        curr.colorOffset = curr.mesh.getInstancedAttribute(Usage.ColorPacked) != null ? curr.mesh.getInstancedAttribute(Usage.ColorPacked).offset / 4 : 0;
        sizeOffset = curr.mesh.getInstancedAttribute(OwnUsage.Size) != null ? curr.mesh.getInstancedAttribute(OwnUsage.Size).offset / 4 : 0;
        textureIndexOffset = curr.mesh.getInstancedAttribute(OwnUsage.TextureIndex) != null ? curr.mesh.getInstancedAttribute(OwnUsage.TextureIndex).offset / 4 : 0;
        particlePosOffset = curr.mesh.getInstancedAttribute(OwnUsage.ObjectPosition) != null ? curr.mesh.getInstancedAttribute(OwnUsage.ObjectPosition).offset / 4 : 0;
        if (extended) {
            properMotionOffset = curr.mesh.getInstancedAttribute(OwnUsage.ProperMotion) != null ? curr.mesh.getInstancedAttribute(OwnUsage.ProperMotion).offset / 4 : 0;
        }
    }

    protected void preRenderObjects(ExtShaderProgram shaderProgram,
                                    ICamera camera) {
        shaderProgram.setUniformMatrix("u_projView", camera.getCamera().combined);
        shaderProgram.setUniformf("u_camPos", camera.getPos().put(aux1));
        addCameraUpCubemapMode(shaderProgram, camera);
        addEffectsUniforms(shaderProgram, camera);
    }

    @Override
    protected void renderObject(ExtShaderProgram shaderProgram,
                                IRenderable renderable) {
        final var render = (Render) renderable;
        var base = Mapper.base.get(render.entity);
        var body = Mapper.body.get(render.entity);
        var set = Mapper.particleSet.get(render.entity);
        var hl = Mapper.highlight.get(render.entity);
        var desc = Mapper.datasetDescription.get(render.entity);

        float sizeFactor = utils.getDatasetSizeFactor(render.entity, hl, desc);

        if (!set.disposed) {
            boolean hlCmap = hl.isHighlighted() && !hl.isHlplain();
            int n = set.pointData.size();
            if (!inGpu(render)) {
                int offset = addMeshData(numModelVertices, n);
                setOffset(render, offset);
                curr = meshes.get(offset);
                ensureInstanceAttribsSize(n * curr.instanceSize);

                float[] c = utils.getColor(body, hl);
                float[] colorMin = set.getColorMin();
                float[] colorMax = set.getColorMax();
                double minDistance = set.getMinDistance();
                double maxDistance = set.getMaxDistance();

                int numParticlesAdded = 0;
                for (int i = 0; i < n; i++) {
                    if (utils.filter(i, set, desc) && set.isVisible(i)) {
                        IParticleRecord particle = set.get(i);
                        double[] p = particle.rawDoubleData();

                        // COLOR
                        if (hl.isHighlighted()) {
                            if (hlCmap) {
                                // Color map.
                                double[] color = cmap.colormap(hl.getHlcmi(), hl.getHlcma().get(particle), hl.getHlcmmin(), hl.getHlcmmax());
                                tempInstanceAttribs[curr.instanceIdx + curr.colorOffset] = Color.toFloatBits((float) color[0], (float) color[1], (float) color[2], 1.0f);
                            } else {
                                // Plain highlight color.
                                tempInstanceAttribs[curr.instanceIdx + curr.colorOffset] = Color.toFloatBits(c[0], c[1], c[2], c[3]);
                            }
                        } else {
                            if (extended && particle.hasColor() && Float.isFinite(particle.col())) {
                                // Use particle color.
                                tempInstanceAttribs[curr.instanceIdx + curr.colorOffset] = particle.col();
                            } else {
                                // Generate color.
                                if (colorMin != null && colorMax != null) {
                                    double dist = Math.sqrt(p[0] * p[0] + p[1] * p[1] + p[2] * p[2]);
                                    // fac = 0 -> colorMin,  fac = 1 -> colorMax
                                    double fac = (dist - minDistance) / (maxDistance - minDistance);
                                    interpolateColor(colorMin, colorMax, c, fac);
                                }
                                float r = 0, g = 0, b = 0;
                                if (set.colorNoise != 0) {
                                    r = (float) ((StdRandom.uniform() - 0.5) * 2.0 * set.colorNoise);
                                    g = (float) ((StdRandom.uniform() - 0.5) * 2.0 * set.colorNoise);
                                    b = (float) ((StdRandom.uniform() - 0.5) * 2.0 * set.colorNoise);
                                }
                                tempInstanceAttribs[curr.instanceIdx + curr.colorOffset] = Color.toFloatBits(MathUtils.clamp(c[0] + r, 0, 1),
                                                                                                             MathUtils.clamp(c[1] + g, 0, 1),
                                                                                                             MathUtils.clamp(c[2] + b, 0, 1),
                                                                                                             MathUtils.clamp(c[3], 0, 1));
                            }
                        }

                        // SIZE
                        if (extended && particle.hasSize()) {
                            tempInstanceAttribs[curr.instanceIdx + sizeOffset] = (float) (particle.size() * Constants.PC_TO_U * sizeFactor);
                        } else {
                            tempInstanceAttribs[curr.instanceIdx + sizeOffset] = (body.size + (float) (rand.nextGaussian() * body.size / 5.0)) * sizeFactor;
                        }

                        // TEXTURE INDEX
                        float textureIndex = -1.0f;
                        if (set.textureArray != null) {
                            int nTextures = set.textureArray.getDepth();
                            textureIndex = (float) rand.nextInt(nTextures);
                        }
                        tempInstanceAttribs[curr.instanceIdx + textureIndexOffset] = textureIndex;

                        // PARTICLE POSITION
                        tempInstanceAttribs[curr.instanceIdx + particlePosOffset] = (float) p[0];
                        tempInstanceAttribs[curr.instanceIdx + particlePosOffset + 1] = (float) p[1];
                        tempInstanceAttribs[curr.instanceIdx + particlePosOffset + 2] = (float) p[2];

                        if (extended) {
                            // PROPER MOTION
                            if (particle.hasProperMotion()) {
                                tempInstanceAttribs[curr.instanceIdx + properMotionOffset] = (float) particle.pmx();
                                tempInstanceAttribs[curr.instanceIdx + properMotionOffset + 1] = (float) particle.pmy();
                                tempInstanceAttribs[curr.instanceIdx + properMotionOffset + 2] = (float) particle.pmz();
                            } else {
                                tempInstanceAttribs[curr.instanceIdx + properMotionOffset] = 0f;
                                tempInstanceAttribs[curr.instanceIdx + properMotionOffset + 1] = 0f;
                                tempInstanceAttribs[curr.instanceIdx + properMotionOffset + 2] = 0f;
                            }
                        }

                        curr.instanceIdx += curr.instanceSize;
                        curr.numVertices++;
                        numParticlesAdded++;
                    }
                }
                // Global (divisor=0) vertices (position, uv)
                curr.mesh.setVertices(tempVerts, 0, numModelVertices * modelVertexSize);
                // Per instance (divisor=1) vertices
                int count = numParticlesAdded * curr.instanceSize;
                setCount(render, count);
                curr.mesh.setInstanceAttribs(tempInstanceAttribs, 0, count);

                setInGpu(render, true);
            }

            /*
             * RENDER
             */
            curr = meshes.get(getOffset(render));
            if (curr != null) {
                // Only quads are textured.
                if (set.textureArray != null && modelType.isQuad()) {
                    set.textureArray.bind(2201);
                    shaderProgram.setUniformi("u_textures", 2201);
                }

                if (extended) {
                    // Days since epoch
                    // Emulate double with floats, for compatibility
                    double curRt = AstroUtils.getDaysSince(GaiaSky.instance.time.getTime(), set.epochJd);
                    float curRt2 = (float) (curRt - (double) ((float) curRt));
                    shaderProgram.setUniformf("u_t", (float) curRt, curRt2);
                }

                float meanDist = (float) (set.getMeanDistance());

                shaderProgram.setUniformf("u_alpha", alphas[base.ct.getFirstOrdinal()] * base.opacity);
                shaderProgram.setUniformf("u_falloff", set.profileDecay);
                shaderProgram.setUniformf("u_sizeLimits", (float) (set.particleSizeLimits[0] * sizeFactor), (float) (set.particleSizeLimits[1] * sizeFactor));
                if (extended) {
                    shaderProgram.setUniformf("u_sizeFactor", (float) (sizeFactor / Constants.DISTANCE_SCALE_FACTOR));
                } else {
                    double s = .3e-4f;
                    shaderProgram.setUniformf("u_sizeFactor",
                                              (float) (((StarSettings.getStarPointSize() * s)) * sizeFactor * meanDist / Constants.DISTANCE_SCALE_FACTOR));
                }

                try {
                    int primitive = modelType.isQuad() ? GL20.GL_TRIANGLES : GL20.GL_LINES;
                    curr.mesh.render(shaderProgram, primitive, 0, numModelVertices, n);
                } catch (IllegalArgumentException e) {
                    logger.error(e, "Render exception");
                }
            }
        }
    }

    private void interpolateColor(float[] c0,
                                  float[] c1,
                                  float[] result,
                                  double factor) {
        float f = (float) factor;
        result[0] = (1 - f) * c0[0] + f * c1[0];
        result[1] = (1 - f) * c0[1] + f * c1[1];
        result[2] = (1 - f) * c0[2] + f * c1[2];
        result[3] = (1 - f) * c0[3] + f * c1[3];
    }

    protected void setInGpu(IRenderable renderable,
                            boolean state) {
        if (inGpu != null) {
            if (inGpu.contains(renderable) && !state) {
                EventManager.publish(Event.GPU_DISPOSE_PARTICLE_GROUP, renderable);
            }
            super.setInGpu(renderable, state);
        }
    }

    @Override
    public void notify(final Event event,
                       Object source,
                       final Object... data) {
        if (event == Event.GPU_DISPOSE_PARTICLE_GROUP) {
            IRenderable renderable = (IRenderable) source;
            int offset = getOffset(renderable);
            clearMeshData(offset);
            if (inGpu != null)
                inGpu.remove(renderable);
        }
    }

}
