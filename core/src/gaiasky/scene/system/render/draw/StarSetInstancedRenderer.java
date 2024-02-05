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
import gaiasky.util.Settings;
import gaiasky.util.color.Colormap;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.gdx.shader.ExtShaderProgram;

/**
 * Renders star sets as instanced triangles.
 */
public class StarSetInstancedRenderer extends InstancedRenderSystem implements IObserver {
    protected static final Log logger = Logger.getLogger(StarSetInstancedRenderer.class);

    private final Vector3 aux1;
    private final Colormap cmap;
    private final ParticleUtils utils;
    private StarSetQuadComponent triComponent;

    public StarSetInstancedRenderer(SceneRenderer sceneRenderer,
                                    RenderGroup rg,
                                    float[] alphas,
                                    ExtShaderProgram[] shaders) {
        super(sceneRenderer, rg, alphas, shaders);
        utils = new ParticleUtils();
        cmap = new Colormap();

        aux1 = new Vector3();
        triComponent.setStarTexture(Settings.settings.scene.star.getStarTexture());

        EventManager.instance.subscribe(this, Event.STAR_BRIGHTNESS_CMD, Event.STAR_BRIGHTNESS_POW_CMD,
                Event.STAR_POINT_SIZE_CMD, Event.STAR_BASE_LEVEL_CMD, Event.BACKBUFFER_SCALE_CMD, Event.FOV_CHANGED_CMD,
                Event.GPU_DISPOSE_STAR_GROUP, Event.BILLBOARD_TEXTURE_IDX_CMD);
    }

    @Override
    protected void addAttributesDivisor1(Array<VertexAttribute> attributes,
                                         int primitive) {
        // Color, object position, proper motion and size are per instance
        attributes.add(new VertexAttribute(Usage.ColorPacked, 4, ExtShaderProgram.COLOR_ATTRIBUTE));
        attributes.add(new VertexAttribute(OwnUsage.ObjectPosition, 3, "a_starPos"));
        attributes.add(new VertexAttribute(OwnUsage.ProperMotion, 3, "a_pm"));
        attributes.add(new VertexAttribute(OwnUsage.Size, 1, "a_size"));
    }

    @Override
    protected void offsets0(MeshData curr, InstancedModel model) {
        // Not needed
    }

    @Override
    protected void offsets1(MeshData curr, InstancedModel model) {
        curr.colorOffset = curr.mesh.getInstancedAttribute(Usage.ColorPacked) != null ? curr.mesh.getInstancedAttribute(Usage.ColorPacked).offset / 4 : 0;
        model.properMotionOffset = curr.mesh.getInstancedAttribute(OwnUsage.ProperMotion) != null ? curr.mesh.getInstancedAttribute(OwnUsage.ProperMotion).offset / 4 : 0;
        model.sizeOffset = curr.mesh.getInstancedAttribute(OwnUsage.Size) != null ? curr.mesh.getInstancedAttribute(OwnUsage.Size).offset / 4 : 0;
        model.particlePosOffset = curr.mesh.getInstancedAttribute(OwnUsage.ObjectPosition) != null ? curr.mesh.getInstancedAttribute(OwnUsage.ObjectPosition).offset / 4 : 0;
    }

    @Override
    protected void initShaderProgram() {
        this.triComponent = new StarSetQuadComponent();
        this.triComponent.initShaderProgram(getShaderProgram());
    }

    protected void preRenderObjects(ExtShaderProgram shaderProgram,
                                    ICamera camera) {
        shaderProgram.setUniformMatrix("u_projView", camera.getCamera().combined);
        shaderProgram.setUniformf("u_camPos", camera.getPos().put(aux1));
        addCameraUpCubemapMode(shaderProgram, camera);
        addEffectsUniforms(shaderProgram, camera);
    }

    protected void renderObject(ExtShaderProgram shaderProgram,
                                IRenderable renderable) {
        final Render render = (Render) renderable;
        var base = Mapper.base.get(render.entity);
        var set = Mapper.starSet.get(render.entity);
        var hl = Mapper.highlight.get(render.entity);
        var desc = Mapper.datasetDescription.get(render.entity);

        float sizeFactor = utils.getDatasetSizeFactor(render.entity, hl, desc);

        if (!set.disposed) {
            boolean hlCmap = hl.isHighlighted() && !hl.isHlplain();
            var model = getModel(set, getOffset(render));
            int n = set.data().size();
            if (!inGpu(render)) {
                int offset = addMeshData(model, model.numVertices, n, 0, set.modelFile, set.modelType, set.modelPrimitive);
                setModel(offset, model);
                setOffset(render, offset);
                curr = meshes.get(offset);
                model.ensureInstanceAttribsSize(n * curr.instanceSize);
                int numStarsAdded = 0;

                for (int i = 0; i < n; i++) {
                    if (utils.filter(i, set, desc) && set.isVisible(i)) {
                        IParticleRecord particle = set.get(i);
                        if (!Double.isFinite(particle.size())) {
                            logger.debug("Star " + particle.id() + " has a non-finite size");
                            continue;
                        }

                        // COLOR
                        if (hlCmap) {
                            // Color map.
                            double[] color = cmap.colormap(hl.getHlcmi(), hl.getHlcma().get(particle), hl.getHlcmmin(), hl.getHlcmmax());
                            model.instanceAttributes[curr.instanceIdx + curr.colorOffset] = Color.toFloatBits((float) color[0], (float) color[1], (float) color[2], hl.getHlcmAlpha());
                        } else {
                            // Plain color.
                            model.instanceAttributes[curr.instanceIdx + curr.colorOffset] = utils.getColor(i, set, hl);
                        }

                        // SIZE
                        model.instanceAttributes[curr.instanceIdx + model.sizeOffset] = (float) (particle.size() * Constants.STAR_SIZE_FACTOR) * sizeFactor;

                        // PROPER MOTION [u/yr]
                        model.instanceAttributes[curr.instanceIdx + model.properMotionOffset] = (float) particle.pmx();
                        model.instanceAttributes[curr.instanceIdx + model.properMotionOffset + 1] = (float) particle.pmy();
                        model.instanceAttributes[curr.instanceIdx + model.properMotionOffset + 2] = (float) particle.pmz();

                        // STAR POSITION [u]
                        model.instanceAttributes[curr.instanceIdx + model.particlePosOffset] = (float) particle.x();
                        model.instanceAttributes[curr.instanceIdx + model.particlePosOffset + 1] = (float) particle.y();
                        model.instanceAttributes[curr.instanceIdx + model.particlePosOffset + 2] = (float) particle.z();

                        curr.instanceIdx += curr.instanceSize;
                        curr.numVertices++;
                        numStarsAdded++;
                    }
                }
                // Global (divisor=0) vertices (position, uv)
                curr.mesh.setVertices(model.vertices, 0, model.numVertices * model.modelVertexSize);
                // Per instance (divisor=1) vertices
                int count = numStarsAdded * curr.instanceSize;
                setCount(render, numStarsAdded);
                curr.mesh.setInstanceAttribs(model.instanceAttributes, 0, count);

                setInGpu(render, true);
            }

            /*
             * RENDER
             */
            curr = meshes.get(getOffset(render));
            if (curr != null) {
                if (triComponent.starTex != null) {
                    triComponent.starTex.bind(0);
                    shaderProgram.setUniformi("u_starTex", 0);
                }

                triComponent.alphaSizeBr[0] = base.opacity * alphas[base.ct.getFirstOrdinal()];
                triComponent.alphaSizeBr[1] = triComponent.starPointSize * 1e6f * sizeFactor;
                shaderProgram.setUniform3fv("u_alphaSizeBr", triComponent.alphaSizeBr, 0, 3);

                // Fixed size.
                shaderProgram.setUniformf("u_fixedAngularSize", (float) (set.fixedAngularSize));

                // Days since epoch.
                // Emulate double with floats, for compatibility.
                double curRt = AstroUtils.getDaysSince(GaiaSky.instance.time.getTime(), set.epochJd);
                float curRt2 = (float) (curRt - (double) ((float) curRt));
                shaderProgram.setUniformf("u_t", (float) curRt, curRt2);

                // Opacity limits.
                triComponent.setOpacityLimitsUniform(shaderProgram, hl);

                // Affine transformation.
                addAffineTransformUniforms(shaderProgram, Mapper.affine.get(render.entity));

                try {
                    curr.mesh.render(shaderProgram, GL20.GL_TRIANGLES, 0, model.numVertices, getCount(render));
                } catch (IllegalArgumentException e) {
                    logger.error(e, "Render exception");
                }
            }
        } else {
            throw new RuntimeException("No suitable model found for type '" + set.modelType + "' and primitive '" + set.modelPrimitive + "'");
        }
    }

    protected void setInGpu(IRenderable renderable,
                            boolean state) {
        if (inGpu != null) {
            if (inGpu.contains(renderable) && !state) {
                EventManager.publish(Event.GPU_DISPOSE_STAR_GROUP, renderable);
            }
            if (state) {
                inGpu.add(renderable);
            } else {
                inGpu.remove(renderable);
            }
        }
    }

    @Override
    public void notify(final Event event,
                       Object source,
                       final Object... data) {
        switch (event) {
            case STAR_BASE_LEVEL_CMD -> {
                triComponent.updateStarOpacityLimits((float) data[0], Settings.settings.scene.star.opacity[1]);
                triComponent.touchStarParameters(getShaderProgram());
            }
            case STAR_BRIGHTNESS_CMD -> {
                triComponent.updateStarBrightness((float) data[0]);
                triComponent.touchStarParameters(getShaderProgram());
            }
            case STAR_BRIGHTNESS_POW_CMD -> {
                triComponent.updateBrightnessPower((float) data[0]);
                triComponent.touchStarParameters(getShaderProgram());
            }
            case STAR_POINT_SIZE_CMD -> {
                triComponent.updateStarPointSize((float) data[0]);
                triComponent.touchStarParameters(getShaderProgram());
            }
            case BACKBUFFER_SCALE_CMD, FOV_CHANGED_CMD -> {
                triComponent.updateMinQuadSolidAngle(Settings.settings.graphics.backBufferResolution);
                triComponent.touchStarParameters(getShaderProgram());
            }
            case GPU_DISPOSE_STAR_GROUP -> {
                IRenderable renderable = (IRenderable) source;
                int offset = getOffset(renderable);
                if (offset >= 0) {
                    clearMeshData(offset);
                    models.set(offset, null);
                    inGpu.remove(renderable);
                }
            }
            case BILLBOARD_TEXTURE_IDX_CMD ->
                    GaiaSky.postRunnable(() -> triComponent.setStarTexture(Settings.settings.scene.star.getStarTexture()));
            default -> {
            }
        }
    }

    @Override
    public void resize(int w, int h) {
        triComponent.updateMinQuadSolidAngle(Settings.settings.graphics.backBufferResolution);
        triComponent.touchStarParameters(getShaderProgram());
    }
}
