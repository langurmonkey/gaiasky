/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render.draw;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.FloatTextureData;
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
import gaiasky.scene.component.Highlight;
import gaiasky.scene.component.ParticleSet;
import gaiasky.scene.component.Render;
import gaiasky.scene.record.ParticleVariable;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.color.Colormap;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders variable star sets as instanced triangles.
 */
public class VariableSetInstancedRenderer extends InstancedRenderSystem implements IObserver {
    // Maximum number of data points in the light curves
    public static final int MAX_VARI = 20;
    protected static final Log logger = Logger.getLogger(VariableSetInstancedRenderer.class);
    private final Colormap cmap;
    private StarSetQuadComponent triComponent;

    private static final int FLOATS_PER_VARI_ENTRY = 3; // mag, time, col (packed float)
    private final Map<Render, Texture> variabilityTextures = new HashMap<>();

    public VariableSetInstancedRenderer(SceneRenderer sceneRenderer,
                                        RenderGroup rg,
                                        float[] alphas,
                                        ExtShaderProgram[] shaders) {
        super(sceneRenderer, rg, alphas, shaders);
        cmap = new Colormap();

        triComponent.setStarTexture(Settings.settings.scene.star.getStarTexture());

        EventManager.instance.subscribe(this, Event.STAR_BRIGHTNESS_CMD, Event.STAR_BRIGHTNESS_POW_CMD,
                                        Event.STAR_POINT_SIZE_CMD, Event.STAR_BASE_LEVEL_CMD, Event.BACKBUFFER_SCALE_CMD, Event.FOV_CMD,
                                        Event.GPU_DISPOSE_VARIABLE_GROUP, Event.BILLBOARD_TEXTURE_IDX_CMD);
    }


    @Override
    protected void addAttributesDivisor1(Array<VertexAttribute> attributes,
                                         int primitive) {
        // Color, object position, proper motion and time series are per instance
        attributes.add(new VertexAttribute(Usage.ColorPacked, 4, ExtShaderProgram.COLOR_ATTRIBUTE));
        attributes.add(new VertexAttribute(OwnUsage.ProperMotion, 3, "a_pm"));
        attributes.add(new VertexAttribute(OwnUsage.ObjectPosition, 3, "a_starPos"));
        attributes.add(new VertexAttribute(OwnUsage.NumVariablePoints, 1, "a_nVari"));
        attributes.add(new VertexAttribute(OwnUsage.VariableIndex, 1, "a_varIndex"));
    }

    @Override
    protected void offsets0(MeshData curr, InstancedModel model) {
        // Not needed
    }

    @Override
    protected void offsets1(MeshData curr, InstancedModel model) {
        curr.colorOffset = curr.mesh.getInstancedAttribute(Usage.ColorPacked) != null ? curr.mesh.getInstancedAttribute(Usage.ColorPacked).offset / 4 : 0;
        model.properMotionOffset = curr.mesh.getInstancedAttribute(OwnUsage.ProperMotion) != null ? curr.mesh.getInstancedAttribute(OwnUsage.ProperMotion).offset / 4 : 0;
        model.particlePosOffset = curr.mesh.getInstancedAttribute(OwnUsage.ObjectPosition) != null ? curr.mesh.getInstancedAttribute(OwnUsage.ObjectPosition).offset / 4 : 0;
        model.nVariOffset = curr.mesh.getInstancedAttribute(OwnUsage.NumVariablePoints) != null ? curr.mesh.getInstancedAttribute(OwnUsage.NumVariablePoints).offset / 4 : 0;
        model.varIndexOffset = curr.mesh.getInstancedAttribute(OwnUsage.VariableIndex) != null ? curr.mesh.getInstancedAttribute(OwnUsage.VariableIndex).offset / 4 : 0;
    }

    @Override
    protected void initShaderProgram() {
        this.triComponent = new StarSetQuadComponent();
        this.triComponent.initShaderProgram(getShaderProgram());
    }

    @Override
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
                                ICamera camera,
                                IRenderable renderable) {
        final Render render = (Render) renderable;
        var base = Mapper.base.get(render.entity);
        var set = Mapper.starSet.get(render.entity);
        var hl = Mapper.highlight.get(render.entity);
        var desc = Mapper.datasetDescription.get(render.entity);


        if (!set.disposed) {
            boolean hlCmap = hl.isHighlighted() && !hl.isHlplain();
            var model = getModel(set, getOffset(render));
            int n = set.data().size();
            if (!inGpu(render)) {
                float sizeFactor = utils.getDatasetSizeFactor(render.entity, hl, desc);
                int offset = addMeshData(model, model.numVertices, n, 0, set.modelFile, set.modelType, set.modelPrimitive);
                setModel(offset, model);
                setOffset(render, offset);
                curr = meshes.get(offset);
                model.ensureInstanceAttribsSize(n * curr.instanceSize);
                int numStarsAdded = 0;


                for (int i = 0; i < n; i++) {
                    if (utils.filter(i, set, desc) && set.isVisible(i)) {
                        var particle = (ParticleVariable) set.get(i);
                        if (!Double.isFinite(particle.size())) {
                            logger.debug("Star " + particle.id() + " has a non-finite size");
                            continue;
                        }

                        // COLOR
                        if (hlCmap) {
                            // Color map
                            double[] color = cmap.colormap(hl.getHlcmi(), hl.getHlcma().getNumber(particle), hl.getHlcmmin(), hl.getHlcmmax());
                            model.instanceAttributes[curr.instanceIdx + curr.colorOffset] = Color.toFloatBits((float) color[0],
                                                                                                              (float) color[1],
                                                                                                              (float) color[2],
                                                                                                              hl.getHlcmAlpha());
                        } else {
                            // Plain
                            model.instanceAttributes[curr.instanceIdx + curr.colorOffset] = utils.saturateColor(i, set, hl);
                        }

                        // VARIABLE STARS (TBO index and number)
                        model.instanceAttributes[curr.instanceIdx + model.nVariOffset] = particle.nVari();
                        int fixedOffset = i * MAX_VARI;
                        model.instanceAttributes[curr.instanceIdx + model.varIndexOffset] = (float) fixedOffset;

                        // PROPER MOTION [u/yr]
                        model.instanceAttributes[curr.instanceIdx + model.properMotionOffset] = particle.vx();
                        model.instanceAttributes[curr.instanceIdx + model.properMotionOffset + 1] = particle.vy();
                        model.instanceAttributes[curr.instanceIdx + model.properMotionOffset + 2] = particle.vz();

                        // STAR POSITION [u]
                        model.instanceAttributes[curr.instanceIdx + model.particlePosOffset] = (float) particle.x();
                        model.instanceAttributes[curr.instanceIdx + model.particlePosOffset + 1] = (float) particle.y();
                        model.instanceAttributes[curr.instanceIdx + model.particlePosOffset + 2] = (float) particle.z();

                        curr.instanceIdx += curr.instanceSize;
                        curr.numVertices++;
                        numStarsAdded++;
                    }
                }
                uploadVariabilityTexture(render, hl, set, sizeFactor, set.data());
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
                if (hl.dirty) {
                    triComponent.updatePointScale(utils.getDatasetSizeFactor(render.entity, hl, desc));
                    hl.dirty = false;
                }

                // Bind data texture (variability)
                Texture varTex = variabilityTextures.get(render);
                if (varTex != null) {
                    varTex.bind(1); // Bind to texture unit 1
                    shaderProgram.setUniformi("u_variabilityTex", 1);

                    // Pass width for linear→2D coordinate conversion
                    shaderProgram.setUniformi("u_varTexWidth", varTex.getWidth());
                }

                if (triComponent.starTex != null) {
                    triComponent.starTex.bind(0);
                    shaderProgram.setUniformi("u_starTex", 0);
                }

                triComponent.alphaSizeBr[0] = base.opacity * alphas[base.ct.getFirstOrdinal()];
                shaderProgram.setUniform3fv("u_alphaSizeBr", triComponent.alphaSizeBr, 0, 3);

                // Fixed size.
                shaderProgram.setUniformf("u_fixedAngularSize", (float) (set.fixedAngularSize));

                // Days since epoch.
                // Emulate double with floats, for compatibility.
                double curRt = AstroUtils.getDaysSince(GaiaSky.instance.time.getTime(), set.epochJd);
                float curRt2 = (float) (curRt - (double) ((float) curRt));
                shaderProgram.setUniformf("u_t", (float) curRt, curRt2);

                curRt = AstroUtils.getDaysSince(GaiaSky.instance.time.getTime(), set.variabilityEpochJd);
                shaderProgram.setUniformf("u_s", (float) curRt);

                // Opacity limits.
                triComponent.setOpacityLimitsUniform(shaderProgram, hl);

                // Affine transformations.
                addAffineTransformUniforms(shaderProgram, Mapper.affine.get(render.entity));

                // Override streak if needed.
                if (!set.allowStreaks) {
                    shaderProgram.setUniformf("u_camVel", 0, 0, 0);
                }

                try {
                    curr.mesh.render(shaderProgram, GL30.GL_TRIANGLES, 0, model.numVertices, getCount(render));
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
                EventManager.publish(Event.GPU_DISPOSE_VARIABLE_GROUP, renderable);
            }
            if (state) {
                inGpu.add(renderable);
            } else {
                inGpu.remove(renderable);
            }
        }
    }


    private Texture initVariabilityTexture(Render render, int nStars) {
        // Calculate total entries needed: mag/time/col per sample × MAX_VARI × nStars
        int totalTexels = nStars * MAX_VARI;

        // Query hardware limit
        int maxSize = GL30.glGetInteger(GL30.GL_MAX_TEXTURE_SIZE);

        // Compute optimal dimensions: pack linearly, width up to maxSize
        int texWidth = Math.min(maxSize, Math.max(1, totalTexels));
        int texHeight = Math.max(1, (totalTexels + texWidth - 1) / texWidth);

        // Check if we already have a correctly-sized texture
        Texture tex = variabilityTextures.get(render);
        if (tex != null && tex.getWidth() == texWidth && tex.getHeight() == texHeight) {
            return tex;
        }

        // Dispose old texture if dimensions changed
        if (tex != null) {
            tex.dispose();
            variabilityTextures.remove(render);
        }

        // Create new texture
        var td = new FloatTextureData(texWidth, texHeight, GL30.GL_RGBA32F, GL20.GL_RGBA, GL30.GL_FLOAT, false);
        tex = new Texture(td);
        tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        tex.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);

        variabilityTextures.put(render, tex);

        return tex;
    }


    private static final float SENTINEL = -9999f;
    private void uploadVariabilityTexture(Render render, Highlight hl, ParticleSet set, float sizeFactor, List<IParticleRecord> particles) {
        var tex = initVariabilityTexture(render, particles.size());

        // Create buffer
        var texData = ((FloatTextureData)tex.getTextureData());

        // Use FloatBuffer for RGBA32F
        FloatBuffer buffer = texData.getBuffer();
        // Fill with sentinel (-9999.0)
        for (int i = 0; i < buffer.capacity(); i++) {
            buffer.put(SENTINEL);
        }
        buffer.rewind();

        for (int starIdx = 0; starIdx < particles.size(); starIdx++) {
            ParticleVariable p = (ParticleVariable) particles.get(starIdx);
            int nVari = p.nVari();
            boolean hasColors = p.hasVariColors();
            float fallbackColor = hasColors ? utils.saturateColor(starIdx, set, hl) : SENTINEL;

            for (int sample = 0; sample < MAX_VARI; sample++) {
                if (sample < nVari) {
                    float mag = (float) (p.variMag(sample) * Constants.STAR_SIZE_FACTOR) * sizeFactor;
                    float time = (float) p.variTime(sample);
                    float col = hasColors ? p.variColor(sample) : fallbackColor;

                    // Store in .r, .g, .b channels; .a unused or for flags
                    buffer.put(mag);    // R
                    buffer.put(time);   // G
                    buffer.put(col);    // B
                    buffer.put(0f); // A (padding)
                } else {
                    // Pad unused samples with sentinel values
                    buffer.put(SENTINEL); // mag
                    buffer.put(SENTINEL); // time
                    buffer.put(SENTINEL); // col
                    buffer.put(SENTINEL);
                }
            }
        }

        buffer.flip();

        // Upload to texture
        tex.load(texData);
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
            case BACKBUFFER_SCALE_CMD, FOV_CMD -> {
                triComponent.updateMinQuadSolidAngle(Settings.settings.graphics.backBufferResolution);
                triComponent.touchStarParameters(getShaderProgram());
            }
            case GPU_DISPOSE_VARIABLE_GROUP -> {
                IRenderable renderable = (IRenderable) source;
                int offset = getOffset(renderable);
                if (offset >= 0) {
                    clearMeshData(offset);
                    models.set(offset, null);
                    inGpu.remove(renderable);
                }
                if (variabilityTextures.containsKey((Render) renderable)) {
                    variabilityTextures.get((Render) renderable).dispose();
                    variabilityTextures.remove((Render) renderable);
                }
            }
            case BILLBOARD_TEXTURE_IDX_CMD -> GaiaSky.postRunnable(() -> triComponent.setStarTexture(Settings.settings.scene.star.getStarTexture()));
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
