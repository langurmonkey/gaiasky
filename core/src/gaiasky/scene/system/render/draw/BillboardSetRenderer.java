/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render.draw;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.TextureArray;
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
import gaiasky.render.system.PointCloudTriRenderSystem;
import gaiasky.scene.Mapper;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.Base;
import gaiasky.scene.component.BillboardSet;
import gaiasky.scene.component.Render;
import gaiasky.scene.record.BillboardDataset;
import gaiasky.scene.record.BillboardDataset.ParticleType;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.util.Constants;
import gaiasky.util.GlobalResources;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.Settings.GraphicsQuality;
import gaiasky.util.gdx.mesh.IntMesh;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.StdRandom;
import gaiasky.util.tree.LoadStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BillboardSetRenderer extends PointCloudTriRenderSystem implements IObserver {
    protected static final Log logger = Logger.getLogger(BillboardSetRenderer.class);

    private static final String texFolder = Constants.DATA_LOCATION_TOKEN + "galaxy/sprites/";

    private final Vector3 aux3f1;

    private final Map<Render, MeshDataWrap[]> meshes;
    private final Map<Render, GpuData[]> gpus;
    private final Map<Render, Integer> loadIndices;

    private TextureArray ta;

    private final ColorGenerator starColorGenerator;
    private final ColorGenerator dustColorGenerator;

    public BillboardSetRenderer(SceneRenderer sceneRenderer,
                                RenderGroup rg,
                                float[] alphas,
                                ExtShaderProgram[] shaders) {
        super(sceneRenderer, rg, alphas, shaders);
        aux3f1 = new Vector3();

        starColorGenerator = new StarColorGenerator();
        dustColorGenerator = new DustColorGenerator();

        meshes = new HashMap<>();
        gpus = new HashMap<>();
        loadIndices = new HashMap<>();

        EventManager.instance.subscribe(this, Event.GPU_DISPOSE_BILLBOARD_DATASET);
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
        initializeTextureArray(Settings.settings.graphics.quality);
    }

    private void initializeTextureArray(GraphicsQuality gq) {
        // Create TextureArray with 8 layers
        FileHandle s00 = unpack("star-00" + Constants.STAR_SUBSTITUTE + ".png", gq);
        FileHandle s01 = unpack("star-01" + Constants.STAR_SUBSTITUTE + ".png", gq);

        FileHandle d00 = unpack("dust-00" + Constants.STAR_SUBSTITUTE + ".png", gq);
        FileHandle d01 = unpack("dust-01" + Constants.STAR_SUBSTITUTE + ".png", gq);
        FileHandle d02 = unpack("dust-02" + Constants.STAR_SUBSTITUTE + ".png", gq);
        FileHandle d03 = unpack("dust-03" + Constants.STAR_SUBSTITUTE + ".png", gq);
        FileHandle d04 = unpack("dust-04" + Constants.STAR_SUBSTITUTE + ".png", gq);
        FileHandle d05 = unpack("dust-05" + Constants.STAR_SUBSTITUTE + ".png", gq);

        if (ta == null) {
            ta = new TextureArray(true, Format.RGBA8888, s00, s01, d00, d01, d02, d03, d04, d05);
            ta.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        }
    }

    private FileHandle unpack(String texName,
                              GraphicsQuality gq) {
        return Settings.settings.data.dataFileHandle(GlobalResources.unpackAssetPath(texFolder + texName, gq));
    }

    private void disposeTextureArray() {
        if (ta != null) {
            ta.dispose();
        }
    }

    private void disposeMeshes(Render key) {
        if (meshes != null && meshes.containsKey(key)) {
            MeshDataWrap[] m = meshes.get(key);
            if (m != null && m.length > 0) {
                for (MeshDataWrap meshDataWrap : m) {
                    if (meshDataWrap != null && meshDataWrap.meshData != null) {
                        meshDataWrap.meshData.dispose();
                    }
                }
                meshes.remove(key);
                gpus.remove(key);
            }
        }
    }

    private void disposeMeshes() {
        Set<Render> keys = meshes.keySet();
        for (Render key : keys) {
            disposeMeshes(key);
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        disposeMeshes();
        disposeTextureArray();
    }

    @Override
    protected void initVertices() {
    }

    private MeshDataWrap toMeshData(GpuData ad,
                                    MeshDataWrap mdw) {
        if (ad != null && ad.vertices != null) {
            if (mdw != null && mdw.meshData != null) {
                mdw.meshData.dispose();
            }
            mdw = new MeshDataWrap();
            MeshData md = new MeshData();
            VertexAttribute[] attributes = buildVertexAttributes();
            md.mesh = new IntMesh(true, ad.vertices.length / 6, ad.indices.length, attributes);
            md.vertexSize = md.mesh.getVertexAttributes().vertexSize / 4;
            md.colorOffset = md.mesh.getVertexAttribute(Usage.ColorPacked) != null ? md.mesh.getVertexAttribute(Usage.ColorPacked).offset / 4 : 0;
            md.vertexIdx = ad.vertexIdx;
            md.mesh.setVertices(ad.vertices, 0, ad.vertices.length);
            md.mesh.setIndices(ad.indices, 0, ad.indices.length);

            mdw.meshData = md;
            mdw.dataset = ad.dataset;

            ad.vertices = null;
            ad.indices = null;
            return mdw;
        }
        return null;
    }

    /**
     * Converts a given list of particle records to GPU data.
     *
     * @param bd The billboard dataset.
     * @param cg The color generator.
     *
     * @return The GPU data object.
     */
    private GpuData convertDataToGpu(BillboardDataset bd,
                                     ColorGenerator cg) {
        StdRandom.setSeed(11447799L);
        GpuData ad = new GpuData();
        // Dataset
        ad.dataset = bd;

        // vert_pos + col + uv + obj_pos + additional
        int vertexSize = 2 + 1 + 2 + 3 + 3;
        // Offsets
        int posOffset = 0;
        int colorOffset = 2;
        int uvOffset = 3;
        int particlePosOffset = 5;
        int additionalOffset = 8;
        List<IParticleRecord> data = bd.data;
        ad.vertices = new float[data.size() * vertexSize * 4];
        ad.indices = new int[data.size() * 6];

        int nLayers = bd.layers.length;

        int i = 0;
        for (IParticleRecord particle : data) {
            if (bd.modulus == 0 || i % bd.modulus == 0) {
                int layer = StdRandom.uniform(nLayers);
                for (int vert = 0; vert < 4; vert++) {
                    // Vertex POSITION
                    ad.vertices[ad.vertexIdx + posOffset] = vertPos[vert].getFirst();
                    ad.vertices[ad.vertexIdx + posOffset + 1] = vertPos[vert].getSecond();

                    // COLOR
                    double[] doubleData = particle.rawDoubleData();
                    float[] col = doubleData.length >= 7 ? new float[] { (float) doubleData[4], (float) doubleData[5], (float) doubleData[6] } : cg.generateColor();
                    col[0] = MathUtilsDouble.clamp(col[0], 0f, 1f);
                    col[1] = MathUtilsDouble.clamp(col[1], 0f, 1f);
                    col[2] = MathUtilsDouble.clamp(col[2], 0f, 1f);
                    ad.vertices[ad.vertexIdx + colorOffset] = Color.toFloatBits(col[0], col[1], col[2], 1f);

                    // UV coordinates
                    ad.vertices[ad.vertexIdx + uvOffset] = vertUV[vert].getFirst();
                    ad.vertices[ad.vertexIdx + uvOffset + 1] = vertUV[vert].getSecond();

                    // SIZE, TYPE, TEX LAYER
                    double starSize = particle.size();
                    ad.vertices[ad.vertexIdx + additionalOffset] = (float) starSize;
                    ad.vertices[ad.vertexIdx + additionalOffset + 1] = (float) bd.type.ordinal();
                    ad.vertices[ad.vertexIdx + additionalOffset + 2] = (float) bd.layers[layer];

                    // OBJECT POSITION
                    final int idx = ad.vertexIdx;
                    ad.vertices[idx + particlePosOffset] = (float) particle.x();
                    ad.vertices[idx + particlePosOffset + 1] = (float) particle.y();
                    ad.vertices[idx + particlePosOffset + 2] = (float) particle.z();

                    ad.vertexIdx += vertexSize;
                    ad.numVertices++;
                }
                ad.quadIndices();
            }
            i++;
        }
        return ad;
    }

    private ColorGenerator getColorGenerator(final ParticleType type) {
        return type == ParticleType.DUST ? dustColorGenerator : starColorGenerator;
    }

    /**
     * Creates the GPU data objects for a given dataset provider and stores them.
     *
     * @param render The render object.
     * @param base   The base component.
     * @param set    The billboard set component.
     */
    private void convertDataToGpuFormat(Render render,
                                        Base base,
                                        BillboardSet set) {
        logger.info("Converting billboard data to VRAM format: " + base.getLocalizedName());
        BillboardDataset[] datasets = set.datasets;
        GpuData[] g = new GpuData[datasets.length];
        for (int i = 0; i < g.length; i++) {
            g[i] = convertDataToGpu(datasets[i], getColorGenerator(datasets[i].type));
        }
        gpus.put(render, g);
    }

    /**
     * Converts the GPU data objects for the given dataset provider to mesh data
     * objects, and streams them to the GPU, one at a time (in each call to this method).
     * When all objects have been converted and streamed, this method returns true.
     *
     * @param render The render object.
     * @param base   The base component.
     *
     * @return True if all data is already in the GPU.
     */
    private boolean streamToGpu(Render render,
                                Base base) {
        GpuData[] g = gpus.get(render);
        if (g != null) {
            int index;
            if (!loadIndices.containsKey(render)) {
                loadIndices.put(render, 0);
                index = 0;
            } else {
                index = loadIndices.get(render) + 1;
                loadIndices.put(render, index);
            }

            MeshDataWrap[] m;
            if (!meshes.containsKey(render)) {
                m = new MeshDataWrap[g.length];
                meshes.put(render, m);
            } else {
                m = meshes.get(render);
            }

            if (index >= m.length) {
                // Done!
                gpus.remove(render);
                return true;
            } else {
                logger.info(String.format("Streaming dataset to GPU (%d/%d): %s", index + 1, m.length, base.getLocalizedName()));
                m[index] = toMeshData(g[index], m[index]);
                return false;
            }
        }
        return false;
    }

    @Override
    public void renderStud(List<IRenderable> renderables,
                           ICamera camera,
                           double t) {
        for (IRenderable renderable : renderables) {
            Render render = (Render) renderable;
            var base = Mapper.base.get(render.entity);
            var set = Mapper.billboardSet.get(render.entity);

            switch (set.status.get()) {
            case NOT_LOADED -> {
                // PRELOAD
                set.setStatus(LoadStatus.LOADING);
                GaiaSky.instance.getExecutorService().execute(() -> {
                    convertDataToGpuFormat(render, base, set);
                    set.setStatus(LoadStatus.READY);
                });
            }
            case READY -> {
                // TO GPU, one component at a time.
                if (streamToGpu(render, base)) {
                    set.setStatus(LoadStatus.LOADED);
                }
            }
            case LOADED -> {
                // RENDER
                float alpha = getAlpha(renderable);
                if (alpha > 0) {
                    var fade = Mapper.fade.get(render.entity);

                    ExtShaderProgram shaderProgram = getShaderProgram();

                    shaderProgram.begin();

                    // Global uniforms.
                    if (ta != null) {
                        ta.bind(2400);
                        shaderProgram.setUniformi("u_textures", 2400);
                    }
                    shaderProgram.setUniformMatrix("u_projView", camera.getCamera().combined);
                    shaderProgram.setUniformf("u_camPos", camera.getPos().put(aux3f1));
                    addCameraUpCubemapMode(shaderProgram, camera);
                    shaderProgram.setUniformf("u_alpha", renderable.getOpacity() * alpha);
                    shaderProgram.setUniformf("u_edges", (float) fade.fadeIn.y, (float) fade.fadeOut.y);
                    double pointScaleFactor = 1.8e7;

                    // Rel, grav, z-buffer
                    addEffectsUniforms(shaderProgram, camera);

                    int qualityIndex = Settings.settings.graphics.quality.ordinal();

                    // General settings for all
                    Gdx.gl20.glEnable(GL20.GL_DEPTH_TEST);
                    Gdx.gl20.glEnable(GL20.GL_BLEND);

                    MeshDataWrap[] m = meshes.get(render);
                    for (MeshDataWrap meshDataWrap : m) {
                        MeshData meshData = meshDataWrap.meshData;
                        BillboardDataset dataset = meshDataWrap.dataset;
                        // Blend mode
                        switch (dataset.blending) {
                        case ALPHA -> Gdx.gl20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
                        case ADDITIVE -> Gdx.gl20.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE);
                        }
                        // Depth mask
                        Gdx.gl20.glDepthMask(dataset.depthMask);

                        // Specific uniforms
                        shaderProgram.setUniformf("u_maxPointSize", (float) dataset.maxSizes[qualityIndex]);
                        shaderProgram.setUniformf("u_sizeFactor", (float) (dataset.size * pointScaleFactor));
                        shaderProgram.setUniformf("u_intensity", dataset.intensity);

                        // Render mesh
                        meshData.mesh.render(shaderProgram, GL20.GL_TRIANGLES);
                    }
                    shaderProgram.end();
                }
            }
            }
        }
    }

    protected void addVertexAttributes(Array<VertexAttribute> attributes) {
        attributes.add(new VertexAttribute(Usage.Position, 2, ExtShaderProgram.POSITION_ATTRIBUTE));
        attributes.add(new VertexAttribute(Usage.ColorPacked, 4, ExtShaderProgram.COLOR_ATTRIBUTE));
        attributes.add(new VertexAttribute(Usage.TextureCoordinates, 2, ExtShaderProgram.TEXCOORD_ATTRIBUTE));
        attributes.add(new VertexAttribute(OwnUsage.ObjectPosition, 3, "a_particlePos"));
        attributes.add(new VertexAttribute(OwnUsage.Additional, 3, "a_additional"));
    }

    @Override
    protected void offsets(MeshData curr) {
        // Empty, do not use mesh data
    }

    @Override
    public void notify(final Event event,
                       Object source,
                       final Object... data) {
        if (event == Event.GPU_DISPOSE_BILLBOARD_DATASET) {
            if (source instanceof Render) {
                disposeMeshes((Render) source);
            }
        }
    }

    private interface ColorGenerator {
        float[] generateColor();
    }

    private static class StarColorGenerator implements ColorGenerator {
        public float[] generateColor() {
            float r = (float) StdRandom.gaussian() * 0.15f;
            if (StdRandom.uniform(2) == 0) {
                // Blue/white star
                return new float[] { 0.95f - r, 0.8f - r, 0.6f };
            } else {
                // Red/white star
                return new float[] { 0.95f, 0.8f - r, 0.6f - r };
            }
        }
    }

    private static class DustColorGenerator implements ColorGenerator {
        @Override
        public float[] generateColor() {
            float r = (float) Math.abs(StdRandom.uniform() * 0.19);
            return new float[] { r, r, r };
        }
    }

    private static class GpuData {
        float[] vertices;
        int[] indices;
        int vertexIdx;
        int indexIdx;
        int numVertices;
        BillboardDataset dataset;

        public void quadIndices() {
            index(numVertices - 4);
            index(numVertices - 3);
            index(numVertices - 2);

            index(numVertices - 2);
            index(numVertices - 1);
            index(numVertices - 4);
        }

        private void index(int idx) {
            indices[indexIdx++] = idx;
        }
    }

    private static class MeshDataWrap {
        public MeshData meshData;
        public BillboardDataset dataset;
    }
}
