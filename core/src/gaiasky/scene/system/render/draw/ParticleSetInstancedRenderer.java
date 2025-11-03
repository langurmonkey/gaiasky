/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render.draw;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.math.MathUtils;
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
import gaiasky.util.parse.Parser;
import net.jafama.FastMath;

import java.util.Random;

/**
 * Renders particle sets as instanced triangles. It contains the base particle renderer (only positions), and the
 * extended particle renderer (with positions, sizes, colors, proper motions, etc.).
 */
public class ParticleSetInstancedRenderer extends InstancedRenderSystem implements IObserver {
    protected static final Log logger = Logger.getLogger(ParticleSetInstancedRenderer.class);

    /** Whether to use the extended particle set mode or not. **/
    private final boolean extended;
    private final Random rand;
    private final Colormap cmap;

    /**
     * Constructs a particle set instanced renderer using the given model
     * (see {@link ModelCache} for more information on available models)
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
        super(sceneRenderer, rg, alphas, shaders);
        extended = rg.toString()
                .contains("PARTICLE_GROUP_EXT");

        rand = new Random(123);
        cmap = new Colormap();
        EventManager.instance.subscribe(this, Event.GPU_DISPOSE_PARTICLE_GROUP);
    }

    @Override
    protected void addAttributesDivisor1(Array<VertexAttribute> attributes,
                                         int primitive) {
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
    protected void offsets0(MeshData curr,
                            InstancedModel model) {
        // Not needed
    }

    @Override
    protected void offsets1(MeshData curr,
                            InstancedModel model) {
        curr.colorOffset = curr.mesh.getInstancedAttribute(Usage.ColorPacked) != null ? curr.mesh.getInstancedAttribute(
                Usage.ColorPacked).offset / 4 : 0;
        model.sizeOffset = curr.mesh.getInstancedAttribute(OwnUsage.Size) != null ? curr.mesh.getInstancedAttribute(
                OwnUsage.Size).offset / 4 : 0;
        model.textureIndexOffset = curr.mesh.getInstancedAttribute(
                OwnUsage.TextureIndex) != null ? curr.mesh.getInstancedAttribute(OwnUsage.TextureIndex).offset / 4 : 0;
        model.particlePosOffset =
                curr.mesh.getInstancedAttribute(OwnUsage.ObjectPosition) != null ? curr.mesh.getInstancedAttribute(
                        OwnUsage.ObjectPosition).offset / 4 : 0;
        if (extended) {
            model.properMotionOffset =
                    curr.mesh.getInstancedAttribute(OwnUsage.ProperMotion) != null ? curr.mesh.getInstancedAttribute(
                            OwnUsage.ProperMotion).offset / 4 : 0;
        }
    }

    protected void preRenderObjects(ExtShaderProgram shaderProgram,
                                    ICamera camera) {
        shaderProgram.setUniformMatrix("u_projView", camera.getCamera().combined);
        shaderProgram.setUniformf("u_camPos", camera.getPos());
        addMotionTrailsUniforms(shaderProgram, camera);
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
            var model = getModel(set, getOffset(render));
            if (!inGpu(render)) {
                int n = set.pointData.size();
                int offset = addMeshData(model,
                                         model.numVertices,
                                         n,
                                         model.numIndices,
                                         set.modelFile,
                                         set.modelType,
                                         set.modelPrimitive);
                setModel(offset, model);
                setOffset(render, offset);
                curr = meshes.get(offset);
                model.ensureInstanceAttribsSize(n * curr.instanceSize);

                float[] c = utils.getColor(body, hl);
                float[] colorMin = set.getColorMin();
                float[] colorMax = set.getColorMax();
                double minDistance = set.getMinDistance();
                double maxDistance = set.getMaxDistance();

                int numParticlesAdded = 0;
                for (int i = 0; i < n; i++) {
                    if (utils.filter(i, set, desc) && set.isVisible(i)) {
                        IParticleRecord particle = set.get(i);
                        double x = particle.x();
                        double y = particle.y();
                        double z = particle.z();

                        // SIZE
                        if (extended && particle.hasSize()) {
                            model.instanceAttributes[curr.instanceIdx + model.sizeOffset] = particle.size();
                        } else {
                            model.instanceAttributes[curr.instanceIdx + model.sizeOffset] = (body.size + (float) (rand.nextGaussian() * body.size / 5.0));
                        }

                        // TEXTURE INDEX
                        float textureIndex = -1.0f;
                        if (set.textureArray != null && !set.isWireframe()) {
                            int nTextures = set.textureArray.getDepth();
                            if (set.textureAttribute != null && particle.hasExtra(set.textureAttribute)) {
                                var value = particle.getExtra(set.textureAttribute);
                                if (value instanceof Number num) {
                                    textureIndex = MathUtils.clamp(num.intValue() - 1, 0, nTextures - 1);
                                } else if (value instanceof String str) {
                                    // Try to parse it as integer, otherwise, use hash code.
                                    try {
                                        textureIndex = MathUtils.clamp((int) Parser.parseDoubleException(str) - 1, 0,
                                                                       nTextures - 1);
                                    } catch (NumberFormatException ignored) {
                                        textureIndex = value.hashCode() % nTextures;
                                    }
                                } else {
                                    // Any other type, use hash code.
                                    textureIndex = value.hashCode() % nTextures;
                                }
                            } else {
                                // Random index.
                                textureIndex = (float) rand.nextInt(nTextures);
                            }
                        }
                        model.instanceAttributes[curr.instanceIdx + model.textureIndexOffset] = textureIndex;

                        // COLOR
                        if (hl.isHighlighted()) {
                            if (hlCmap) {
                                // Color map.
                                double[] color = cmap.colormap(hl.getHlcmi(), hl.getHlcma()
                                        .getNumber(particle), hl.getHlcmmin(), hl.getHlcmmax());
                                model.instanceAttributes[curr.instanceIdx + curr.colorOffset] = Color.toFloatBits(
                                        (float) color[0], (float) color[1], (float) color[2],
                                        hl.getHlcmAlpha());
                            } else {
                                // Plain highlight color.
                                model.instanceAttributes[curr.instanceIdx + curr.colorOffset] = Color.toFloatBits(c[0], c[1],
                                                                                                                  c[2], c[3]);
                            }
                        } else {
                            if (extended && particle.hasColor() && Float.isFinite(particle.color())) {
                                // Use particle color.
                                model.instanceAttributes[curr.instanceIdx + curr.colorOffset] = particle.color();
                            } else {
                                // Generate color.
                                if (set.colorFromTexture && set.textureArray != null && textureIndex >= 0f) {
                                    // Generate color using texture index, so particles with the same index get the
                                    // same color.
                                    float r = 0, g = 0, b = 0;
                                    if (set.colorNoise != 0) {
                                        StdRandom.setSeed((long) textureIndex);
                                        r = (float) ((StdRandom.uniform() - 0.5) * 2.0 * set.colorNoise);
                                        g = (float) ((StdRandom.uniform() - 0.5) * 2.0 * set.colorNoise);
                                        b = (float) ((StdRandom.uniform() - 0.5) * 2.0 * set.colorNoise);
                                    }
                                    model.instanceAttributes[curr.instanceIdx + curr.colorOffset] = Color.toFloatBits(
                                            MathUtils.clamp(c[0] + r, 0, 1),
                                            MathUtils.clamp(c[1] + g, 0, 1),
                                            MathUtils.clamp(c[2] + b, 0, 1),
                                            MathUtils.clamp(c[3], 0, 1));

                                } else {
                                    if (colorMin != null && colorMax != null) {
                                        double dist = FastMath.sqrt(x * x + y * y + z * z);
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
                                    model.instanceAttributes[curr.instanceIdx + curr.colorOffset] = Color.toFloatBits(
                                            MathUtils.clamp(c[0] + r, 0, 1),
                                            MathUtils.clamp(c[1] + g, 0, 1),
                                            MathUtils.clamp(c[2] + b, 0, 1),
                                            MathUtils.clamp(c[3], 0, 1));
                                }
                            }
                        }

                        // PARTICLE POSITION
                        model.instanceAttributes[curr.instanceIdx + model.particlePosOffset] = (float) x;
                        model.instanceAttributes[curr.instanceIdx + model.particlePosOffset + 1] = (float) y;
                        model.instanceAttributes[curr.instanceIdx + model.particlePosOffset + 2] = (float) z;

                        if (extended) {
                            // PROPER MOTION
                            if (particle.hasProperMotion()) {
                                model.instanceAttributes[curr.instanceIdx + model.properMotionOffset] = particle.vx();
                                model.instanceAttributes[curr.instanceIdx + model.properMotionOffset + 1] = particle.vy();
                                model.instanceAttributes[curr.instanceIdx + model.properMotionOffset + 2] = particle.vz();
                            } else {
                                model.instanceAttributes[curr.instanceIdx + model.properMotionOffset] = 0f;
                                model.instanceAttributes[curr.instanceIdx + model.properMotionOffset + 1] = 0f;
                                model.instanceAttributes[curr.instanceIdx + model.properMotionOffset + 2] = 0f;
                            }
                        }

                        curr.instanceIdx += curr.instanceSize;
                        curr.numVertices++;
                        numParticlesAdded++;
                    }
                }
                // Global (divisor=0) vertices (position, uv?) plus optional indices
                curr.mesh.setVertices(model.vertices, 0, model.numVertices * model.modelVertexSize);
                if (model.numIndices > 0) {
                    curr.mesh.setIndices(model.indices, 0, model.numIndices);
                }
                // Per instance (divisor=1) vertices
                int count = numParticlesAdded * curr.instanceSize;
                setCount(render, numParticlesAdded);
                curr.mesh.setInstanceAttribs(model.instanceAttributes, 0, count);
                model.instanceAttributes = null;

                setInGpu(render, true);
            }

            /*
             * RENDER
             */
            curr = meshes.get(getOffset(render));
            if (curr != null) {
                // Only quads are textured.
                if (set.textureArray != null && !set.isWireframe()) {
                    set.textureArray.bind(0);
                }

                if (extended) {
                    // Days since epoch
                    // Emulate double with floats, for compatibility
                    double curRt = AstroUtils.getDaysSince(GaiaSky.instance.time.getTime(), set.epochJd);
                    float curRt2 = (float) (curRt - (double) ((float) curRt));
                    shaderProgram.setUniformf("u_t", (float) curRt, curRt2);
                }
                // Shading style.
                shaderProgram.setUniformf("u_appTime", (float) GaiaSky.instance.getRunTimeSeconds());
                shaderProgram.setUniformi("u_shadingStyle", set.shadingStyle.ordinal());

                float meanDist = (float) (set.getMeanDistance());

                shaderProgram.setUniformf("u_alpha", alphas[base.ct.getFirstOrdinal()] * base.opacity);
                shaderProgram.setUniformf("u_falloff", set.profileDecay);
                shaderProgram.setUniformf("u_sizeLimits", (float) (set.particleSizeLimits[0] * sizeFactor),
                                          (float) (set.particleSizeLimits[1] * sizeFactor));
                if (extended) {
                    shaderProgram.setUniformf("u_sizeFactor", (float) (sizeFactor / Constants.DISTANCE_SCALE_FACTOR));
                } else {
                    double s = .3e-4f;
                    shaderProgram.setUniformf("u_sizeFactor",
                                              (float) (((StarSettings.getStarPointSize() * s)) * sizeFactor * meanDist / Constants.DISTANCE_SCALE_FACTOR));
                }
                shaderProgram.setUniformf("u_proximityThreshold", (float) set.proximityThreshold);

                // Affine transformations.
                addAffineTransformUniforms(shaderProgram, Mapper.affine.get(render.entity));

                // Override streak if needed.
                if(!set.allowStreaks) {
                    shaderProgram.setUniformf("u_camVel", 0, 0, 0);
                }

                try {
                    Gdx.gl30.glEnable(GL30.GL_CULL_FACE);
                    Gdx.gl30.glCullFace(GL30.GL_BACK);
                    int count = curr.mesh.getNumIndices() > 0 ? curr.mesh.getNumIndices() : curr.mesh.getNumVertices();
                    curr.mesh.render(shaderProgram, set.modelPrimitive, 0, count, getCount(render));
                    Gdx.gl30.glDisable(GL30.GL_CULL_FACE);
                } catch (IllegalArgumentException e) {
                    logger.error(e, "Render exception");
                }
            }
        } else {
            throw new RuntimeException(
                    "No suitable model found for type '" + set.modelType + "' and primitive '" + set.modelPrimitive + "'");
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
            if (offset >= 0) {
                clearMeshData(offset);
                models.set(offset, null);
            }
            if (inGpu != null)
                inGpu.remove(renderable);
        }
    }

}
