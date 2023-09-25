/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render.draw;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.render.RenderGroup;
import gaiasky.render.api.IRenderable;
import gaiasky.render.system.ImmediateModeRenderSystem;
import gaiasky.scene.Mapper;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.Render;
import gaiasky.scene.entity.ParticleUtils;
import gaiasky.scene.record.VariableRecord;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.Settings.SceneSettings.StarSettings;
import gaiasky.util.color.Colormap;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.gdx.mesh.IntMesh;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import org.lwjgl.opengl.GL30;

import java.util.List;

public class VariableSetPointRenderer extends ImmediateModeRenderSystem implements IObserver {
    protected static final Log logger = Logger.getLogger(VariableSetPointRenderer.class);

    private final double BRIGHTNESS_FACTOR;

    private final Vector3 aux1;
    private final float[] alphaSizeBrRc;
    private final float[] opacityLimitsHl;
    private final Colormap cmap;
    private final ParticleUtils utils;
    private int nVariOffset, variMagsOffset, variTimesOffset, pmOffset;
    private float[] opacityLimits;
    private Texture starTex;

    public VariableSetPointRenderer(SceneRenderer sceneRenderer, RenderGroup rg, float[] alphas, ExtShaderProgram[] shaders) {
        super(sceneRenderer, rg, alphas, shaders);
        BRIGHTNESS_FACTOR = 10;
        this.alphaSizeBrRc = new float[4];
        this.opacityLimitsHl = new float[] { 2, 4 };
        this.aux1 = new Vector3();
        cmap = new Colormap();
        utils = new ParticleUtils();
        setStarTexture(Settings.settings.scene.star.getStarTexture());

        EventManager.instance.subscribe(this, Event.STAR_BASE_LEVEL_CMD, Event.GPU_DISPOSE_VARIABLE_GROUP, Event.BILLBOARD_TEXTURE_IDX_CMD);
    }

    public void setStarTexture(String starTexture) {
        starTex = new Texture(Settings.settings.data.dataFileHandle(starTexture), true);
        starTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }

    @Override
    protected void initShaderProgram() {
        Gdx.gl.glEnable(GL30.GL_VERTEX_PROGRAM_POINT_SIZE);

        opacityLimits = new float[] { Settings.settings.scene.star.opacity[0], Settings.settings.scene.star.opacity[1] };

        ExtShaderProgram shaderProgram = getShaderProgram();
        shaderProgram.begin();
        // Uniforms that rarely change
        shaderProgram.setUniformf("u_thAnglePoint", 1e-10f, 1.5e-8f);
        shaderProgram.end();
    }

    @Override
    protected void initVertices() {
        // STARS
        meshes = new Array<>();
    }

    /**
     * Adds a new mesh data to the meshes list and increases the mesh data index
     *
     * @param nVertices The max number of vertices this mesh data can hold
     *
     * @return The index of the new mesh data
     */
    private int addMeshData(int nVertices) {
        int mdi = createMeshData();
        curr = meshes.get(mdi);

        VertexAttribute[] attributes = buildVertexAttributes();
        curr.mesh = new IntMesh(false, nVertices, 0, attributes);

        curr.vertexSize = curr.mesh.getVertexAttributes().vertexSize / 4;
        curr.colorOffset = curr.mesh.getVertexAttribute(Usage.ColorPacked) != null ? curr.mesh.getVertexAttribute(Usage.ColorPacked).offset / 4 : 0;
        pmOffset = curr.mesh.getVertexAttribute(Usage.Tangent) != null ? curr.mesh.getVertexAttribute(Usage.Tangent).offset / 4 : 0;
        nVariOffset = curr.mesh.getVertexAttribute(Usage.BiNormal) != null ? curr.mesh.getVertexAttribute(Usage.BiNormal).offset / 4 : 0;
        variMagsOffset = curr.mesh.getVertexAttribute(OwnUsage.VariableMagnitudes) != null ? curr.mesh.getVertexAttribute(OwnUsage.VariableMagnitudes).offset / 4 : 0;
        variTimesOffset = curr.mesh.getVertexAttribute(OwnUsage.VariableTimes) != null ? curr.mesh.getVertexAttribute(OwnUsage.VariableTimes).offset / 4 : 0;
        return mdi;
    }

    @Override
    public void renderStud(List<IRenderable> renderables, ICamera camera, double t) {
        if (!renderables.isEmpty()) {
            ExtShaderProgram shaderProgram = getShaderProgram();
            float starPointSize = StarSettings.getStarPointSize();

            shaderProgram.begin();
            // Global uniforms
            shaderProgram.setUniformMatrix("u_projView", camera.getCamera().combined);
            shaderProgram.setUniformf("u_camPos", camera.getPos().put(aux1));
            shaderProgram.setUniformf("u_camDir", camera.getCamera().direction);
            shaderProgram.setUniformi("u_cubemap", Settings.settings.program.modeCubemap.active ? 1 : 0);
            shaderProgram.setUniformf("u_brightnessPower", Settings.settings.scene.star.power);
            shaderProgram.setUniformf("u_ar", Settings.settings.program.modeStereo.isStereoHalfWidth() ? 2f : 1f);
            addEffectsUniforms(shaderProgram, camera);
            alphaSizeBrRc[2] = (float) (Settings.settings.scene.star.brightness * BRIGHTNESS_FACTOR);
            alphaSizeBrRc[3] = rc.scaleFactor;

            renderables.forEach(renderable -> {
                final Render render = (Render) renderable;
                var base = Mapper.base.get(render.entity);
                var set = Mapper.starSet.get(render.entity);
                var hl = Mapper.highlight.get(render.entity);
                var desc = Mapper.datasetDescription.get(render.entity);

                float sizeFactor = utils.getDatasetSizeFactor(render.entity, hl, desc);

                synchronized (render) {
                    if (!set.disposed) {
                        boolean hlCmap = hl.isHighlighted() && !hl.isHlplain();
                        if (!inGpu(render)) {
                            int n = set.data().size();
                            int offset = addMeshData(n);
                            setOffset(render, offset);
                            curr = meshes.get(offset);
                            ensureTempVertsSize(n * curr.vertexSize);
                            int numAdded = 0;
                            for (int i = 0; i < n; i++) {
                                if (utils.filter(i, set, desc) && set.isVisible(i)) {
                                    VariableRecord particle = (VariableRecord) set.get(i);
                                    if (!Double.isFinite(particle.size())) {
                                        logger.debug("Star " + particle.id() + " has a non-finite size");
                                        continue;
                                    }
                                    // COLOR
                                    if (hlCmap) {
                                        // Color map
                                        double[] color = cmap.colormap(hl.getHlcmi(), hl.getHlcma().get(particle), hl.getHlcmmin(), hl.getHlcmmax());
                                        tempVerts[curr.vertexIdx + curr.colorOffset] = Color.toFloatBits((float) color[0], (float) color[1], (float) color[2], hl.getHlcmAlpha());
                                    } else {
                                        // Plain
                                        tempVerts[curr.vertexIdx + curr.colorOffset] = utils.getColor(i, set, hl);
                                    }

                                    // VARIABLE STARS (magnitudes and times)
                                    tempVerts[curr.vertexIdx + nVariOffset] = particle.nVari;
                                    for (int k = 0; k < particle.nVari; k++) {
                                        if (hl.isHlAllVisible() && hl.isHighlighted()) {
                                            tempVerts[curr.vertexIdx + variMagsOffset + k] = Math.max(10f, (float) (particle.variMag(k) * Constants.STAR_SIZE_FACTOR) * sizeFactor);
                                        } else {
                                            tempVerts[curr.vertexIdx + variMagsOffset + k] = (float) (particle.variMag(k) * Constants.STAR_SIZE_FACTOR) * sizeFactor;
                                        }
                                        tempVerts[curr.vertexIdx + variTimesOffset + k] = (float) particle.variTime(k);
                                    }

                                    // POSITION [u]
                                    tempVerts[curr.vertexIdx] = (float) particle.x();
                                    tempVerts[curr.vertexIdx + 1] = (float) particle.y();
                                    tempVerts[curr.vertexIdx + 2] = (float) particle.z();

                                    // PROPER MOTION [u/yr]
                                    tempVerts[curr.vertexIdx + pmOffset] = (float) particle.pmx();
                                    tempVerts[curr.vertexIdx + pmOffset + 1] = (float) particle.pmy();
                                    tempVerts[curr.vertexIdx + pmOffset + 2] = (float) particle.pmz();

                                    curr.vertexIdx += curr.vertexSize;
                                    numAdded++;
                                }
                            }
                            int count = numAdded * curr.vertexSize;
                            setCount(render, count);
                            curr.mesh.setVertices(tempVerts, 0, count);

                            setInGpu(render, true);

                        }

                        /*
                         * RENDER
                         */
                        curr = meshes.get(getOffset(render));
                        if (curr != null) {

                            if (starTex != null) {
                                starTex.bind(0);
                                shaderProgram.setUniformi("u_starTex", 0);
                            }

                            shaderProgram.setUniform2fv("u_opacityLimits", hl.isHighlighted() && hl.isHlAllVisible() ? opacityLimitsHl : opacityLimits, 0, 2);

                            alphaSizeBrRc[0] = base.opacity * alphas[base.ct.getFirstOrdinal()];
                            alphaSizeBrRc[1] = ((Settings.settings.program.modeStereo.isStereoFullWidth() ? 1f : 2f) * starPointSize * rc.scaleFactor * sizeFactor) / camera.getFovFactor();
                            shaderProgram.setUniform4fv("u_alphaSizeBrRc", alphaSizeBrRc, 0, 4);

                            // Fixed size
                            shaderProgram.setUniformf("u_fixedAngularSize", (float) (set.fixedAngularSize));

                            // Days since epoch
                            // Emulate double with floats, for compatibility
                            double curRt = AstroUtils.getDaysSince(GaiaSky.instance.time.getTime(), set.epochJd);
                            float curRt2 = (float) (curRt - (double) ((float) curRt));
                            shaderProgram.setUniformf("u_t", (float) curRt, curRt2);

                            curRt = AstroUtils.getDaysSince(GaiaSky.instance.time.getTime(), set.variabilityEpochJd);
                            shaderProgram.setUniformf("u_s", (float) curRt);

                            try {
                                curr.mesh.render(shaderProgram, GL20.GL_POINTS);
                            } catch (IllegalArgumentException e) {
                                logger.error("Render exception");
                            }
                        }
                    }
                }
            });
            shaderProgram.end();
        }
    }

    protected VertexAttribute[] buildVertexAttributes() {
        Array<VertexAttribute> attributes = new Array<>();
        attributes.add(new VertexAttribute(Usage.Position, 3, ExtShaderProgram.POSITION_ATTRIBUTE));
        attributes.add(new VertexAttribute(Usage.Tangent, 3, "a_pm"));
        attributes.add(new VertexAttribute(Usage.ColorPacked, 4, ExtShaderProgram.COLOR_ATTRIBUTE));
        attributes.add(new VertexAttribute(Usage.BiNormal, 1, "a_nVari"));
        attributes.add(new VertexAttribute(OwnUsage.VariableMagnitudes, 4, "a_vmags1"));
        attributes.add(new VertexAttribute(OwnUsage.VariableMagnitudes + 1, 4, "a_vmags2"));
        attributes.add(new VertexAttribute(OwnUsage.VariableMagnitudes + 2, 4, "a_vmags3"));
        attributes.add(new VertexAttribute(OwnUsage.VariableMagnitudes + 3, 4, "a_vmags4"));
        attributes.add(new VertexAttribute(OwnUsage.VariableMagnitudes + 4, 4, "a_vmags5"));
        attributes.add(new VertexAttribute(OwnUsage.VariableTimes, 4, "a_vtimes1"));
        attributes.add(new VertexAttribute(OwnUsage.VariableTimes + 1, 4, "a_vtimes2"));
        attributes.add(new VertexAttribute(OwnUsage.VariableTimes + 2, 4, "a_vtimes3"));
        attributes.add(new VertexAttribute(OwnUsage.VariableTimes + 3, 4, "a_vtimes4"));
        attributes.add(new VertexAttribute(OwnUsage.VariableTimes + 4, 4, "a_vtimes5"));

        VertexAttribute[] array = new VertexAttribute[attributes.size];
        for (int i = 0; i < attributes.size; i++)
            array[i] = attributes.get(i);
        return array;
    }

    protected void setInGpu(IRenderable renderable, boolean state) {
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

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        switch (event) {
        case STAR_BASE_LEVEL_CMD -> opacityLimits[0] = (float) data[0];
        case GPU_DISPOSE_VARIABLE_GROUP -> {
            IRenderable renderable = (IRenderable) source;
            int offset = getOffset(renderable);
            clearMeshData(offset);
            inGpu.remove(renderable);
        }
        case BILLBOARD_TEXTURE_IDX_CMD -> GaiaSky.postRunnable(() -> setStarTexture(Settings.settings.scene.star.getStarTexture()));
        default -> {
        }
        }
    }

}
