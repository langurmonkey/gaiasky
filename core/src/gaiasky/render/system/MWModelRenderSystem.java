/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.system;

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
import gaiasky.event.EventManager;
import gaiasky.event.Event;
import gaiasky.event.IObserver;
import gaiasky.render.IRenderable;
import gaiasky.render.SceneGraphRenderer.RenderGroup;
import gaiasky.scenegraph.MilkyWay;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.particle.IParticleRecord;
import gaiasky.util.Constants;
import gaiasky.util.GlobalResources;
import gaiasky.util.Settings;
import gaiasky.util.Settings.GraphicsQuality;
import gaiasky.util.gdx.mesh.IntMesh;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.math.StdRandom;
import gaiasky.util.tree.LoadStatus;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MWModelRenderSystem extends PointCloudTriRenderSystem implements IObserver {
    private static final String texFolder = "data/galaxy/sprites/";

    private final Vector3 aux3f1;
    private MeshData dust, bulge, stars, hii, gas;
    private GpuData dustA, bulgeA, starsA, hiiA, gasA;

    private AtomicBoolean reloadDataFlag = new AtomicBoolean(false);

    private TextureArray ta;
    // Max sizes for dust, star, bulge, gas and hii
    private final float[] maxSizes;

    private enum PType {
        DUST(0, new int[] { 3, 5, 7 }),
        STAR(1, new int[] { 0, 1 }),
        BULGE(2, new int[] { 0, 1 }),
        GAS(3, new int[] { 0, 1, 2, 3, 4, 5, 6, 7 }),
        HII(4, new int[] { 2, 3, 4, 5, 6, 7 });

        // The particle type id
        public int id;
        // The layers it can use
        public int[] layers;
        // The modulus to skip particles, usually 0
        public int modulus;

        PType(int id, int[] layers, int modulus) {
            this.id = id;
            this.layers = layers;
            this.modulus = modulus;
        }

        PType(int id, int[] layers) {
            this(id, layers, 0);
        }

    }

    public MWModelRenderSystem(RenderGroup rg, float[] alphas, ExtShaderProgram[] starShaders) {
        super(rg, alphas, starShaders);
        aux3f1 = new Vector3();
        this.maxSizes = new float[PType.values().length];
        initializeMaxSizes(Settings.settings.graphics.quality);
        EventManager.instance.subscribe(this, Event.GRAPHICS_QUALITY_UPDATED);
    }

    /**
     * Initializes the maximum size per component regarding the given graphics quality
     *
     * @param gq The graphics quality
     */
    private void initializeMaxSizes(GraphicsQuality gq) {
        if (gq.isUltra()) {
            this.maxSizes[PType.DUST.ordinal()] = (float) Math.tan(Math.toRadians(30f));
            this.maxSizes[PType.STAR.ordinal()] = (float) Math.tan(Math.toRadians(0.1f));
            this.maxSizes[PType.BULGE.ordinal()] = (float) Math.tan(Math.toRadians(0.15f));
            this.maxSizes[PType.GAS.ordinal()] = (float) Math.tan(Math.toRadians(10f));
            this.maxSizes[PType.HII.ordinal()] = (float) Math.tan(Math.toRadians(8f));
        } else if (gq.isHigh()) {
            this.maxSizes[PType.DUST.ordinal()] = (float) Math.tan(Math.toRadians(20f));
            this.maxSizes[PType.STAR.ordinal()] = (float) Math.tan(Math.toRadians(0.1f));
            this.maxSizes[PType.BULGE.ordinal()] = (float) Math.tan(Math.toRadians(0.1f));
            this.maxSizes[PType.GAS.ordinal()] = (float) Math.tan(Math.toRadians(8f));
            this.maxSizes[PType.HII.ordinal()] = (float) Math.tan(Math.toRadians(2f));
        } else if (gq.isNormal()) {
            this.maxSizes[PType.DUST.ordinal()] = (float) Math.tan(Math.toRadians(13f));
            this.maxSizes[PType.STAR.ordinal()] = (float) Math.tan(Math.toRadians(0.1f));
            this.maxSizes[PType.BULGE.ordinal()] = (float) Math.tan(Math.toRadians(0.1f));
            this.maxSizes[PType.GAS.ordinal()] = (float) Math.tan(Math.toRadians(2.3f));
            this.maxSizes[PType.HII.ordinal()] = (float) Math.tan(Math.toRadians(1.5f));
        } else if (gq.isLow()) {
            this.maxSizes[PType.DUST.ordinal()] = (float) Math.tan(Math.toRadians(13f));
            this.maxSizes[PType.STAR.ordinal()] = (float) Math.tan(Math.toRadians(0.1f));
            this.maxSizes[PType.BULGE.ordinal()] = (float) Math.tan(Math.toRadians(0.1f));
            this.maxSizes[PType.GAS.ordinal()] = (float) Math.tan(Math.toRadians(2f));
            this.maxSizes[PType.HII.ordinal()] = (float) Math.tan(Math.toRadians(1.5f));
        }
    }

    @Override
    protected void initShaderProgram() {

        for (ExtShaderProgram shaderProgram : programs) {
            shaderProgram.begin();
            shaderProgram.setUniformf("u_pointAlphaMin", 0.1f);
            shaderProgram.setUniformf("u_pointAlphaMax", 1.0f);
            shaderProgram.end();
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
        ta = new TextureArray(true, Format.RGBA8888, s00, s01, d00, d01, d02, d03, d04, d05);
        ta.setFilter(TextureFilter.Linear, TextureFilter.Linear);

        for (ExtShaderProgram shaderProgram : programs) {
            shaderProgram.begin();
            ta.bind(0);
            shaderProgram.setUniformi("u_textures", 0);
        }

    }

    private FileHandle unpack(String texName, GraphicsQuality gq) {
        return Settings.settings.data.dataFileHandle(GlobalResources.unpackAssetPath(texFolder + texName, gq));
    }

    private void disposeTextureArray() {
        ta.dispose();
    }

    private void disposeMeshes() {
        if (dust != null)
            dust.dispose();
        dust = null;
        dustA = null;
        if (bulge != null)
            bulge.dispose();
        bulge = null;
        bulgeA = null;
        if (stars != null)
            stars.dispose();
        stars = null;
        starsA = null;
        if (hii != null)
            hii.dispose();
        hii = null;
        hiiA = null;
        if (gas != null)
            gas.dispose();
        gas = null;
        gasA = null;
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

    private MeshData toMeshData(GpuData ad, MeshData md) {
        if (ad != null && ad.vertices != null) {
            if (md != null) {
                md.dispose();
            }
            md = new MeshData();
            VertexAttribute[] attributes = buildVertexAttributes();
            md.mesh = new IntMesh(true, ad.vertices.length / 6, ad.indices.length, attributes);
            md.vertexSize = md.mesh.getVertexAttributes().vertexSize / 4;
            md.colorOffset = md.mesh.getVertexAttribute(Usage.ColorPacked) != null ? md.mesh.getVertexAttribute(Usage.ColorPacked).offset / 4 : 0;
            md.vertexIdx = ad.vertexIdx;
            md.mesh.setVertices(ad.vertices, 0, ad.vertices.length);
            md.mesh.setIndices(ad.indices, 0, ad.indices.length);

            ad.vertices = null;
            return md;
        }
        return null;
    }

    /**
     * Converts a given list of particle records to GPU data
     * 0 - dust
     * 1 - star
     * 2 - bulge
     * 3 - gas
     * 4 - hii
     *
     * @param data List with the particle records
     * @param cg   The color generator
     * @param type The type
     *
     * @return The GPU data object
     */
    private GpuData convertDataToGpu(List<IParticleRecord> data, ColorGenerator cg, PType type) {
        GpuData ad = new GpuData();

        // vert_pos + col + uv + obj_pos + additional
        int vertexSize = 2 + 1 + 2 + 3 + 3;
        // Offsets
        int posOffset = 0;
        int colorOffset = 2;
        int uvOffset = 3;
        int particlePosOffset = 5;
        int additionalOffset = 8;
        ad.vertices = new float[data.size() * vertexSize * 4];
        ad.indices = new int[data.size() * 6];

        int nLayers = type.layers.length;

        int i = 0;
        for (IParticleRecord particle : data) {
            if (type.modulus == 0 || i % type.modulus == 0) {
                int layer = StdRandom.uniform(nLayers);
                for (int vert = 0; vert < 4; vert++) {
                    // Vertex POSITION
                    ad.vertices[ad.vertexIdx + posOffset] = vertPos[vert].getFirst();
                    ad.vertices[ad.vertexIdx + posOffset + 1] = vertPos[vert].getSecond();

                    // COLOR
                    double[] doubleData = particle.rawDoubleData();
                    float[] col = doubleData.length >= 7 ? new float[] { (float) doubleData[4], (float) doubleData[5], (float) doubleData[6] } : cg.generateColor();
                    col[0] = MathUtilsd.clamp(col[0], 0f, 1f);
                    col[1] = MathUtilsd.clamp(col[1], 0f, 1f);
                    col[2] = MathUtilsd.clamp(col[2], 0f, 1f);
                    ad.vertices[ad.vertexIdx + colorOffset] = Color.toFloatBits(col[0], col[1], col[2], 1f);

                    // UV coordinates
                    ad.vertices[ad.vertexIdx + uvOffset] = vertUV[vert].getFirst();
                    ad.vertices[ad.vertexIdx + uvOffset + 1] = vertUV[vert].getSecond();

                    // SIZE, TYPE, TEX LAYER
                    double starSize = particle.size();
                    ad.vertices[ad.vertexIdx + additionalOffset] = (float) starSize;
                    ad.vertices[ad.vertexIdx + additionalOffset + 1] = (float) type.id;
                    ad.vertices[ad.vertexIdx + additionalOffset + 2] = (float) type.layers[layer];

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

    private void convertDataToGpuFormat(MilkyWay mw) {
        logger.info("Converting galaxy data to VRAM format");
        StarColorGenerator scg = new StarColorGenerator();
        bulgeA = convertDataToGpu(mw.bulgeData, scg, PType.BULGE);
        starsA = convertDataToGpu(mw.starData, scg, PType.STAR);
        hiiA = convertDataToGpu(mw.hiiData, scg, PType.HII);
        gasA = convertDataToGpu(mw.gasData, scg, PType.GAS);
        dustA = convertDataToGpu(mw.dustData, new DustColorGenerator(), PType.DUST);
    }

    private void streamToGpu() {
        logger.info("Streaming galaxy to GPU");
        bulge = toMeshData(bulgeA, bulge);
        bulgeA = null;

        stars = toMeshData(starsA, stars);
        starsA = null;

        hii = toMeshData(hiiA, hii);
        hiiA = null;

        gas = toMeshData(gasA, gas);
        gasA = null;

        dust = toMeshData(dustA, dust);
        dustA = null;
    }

    @Override
    public void renderStud(Array<IRenderable> renderables, ICamera camera, double t) {
        if (renderables.size > 0) {
            MilkyWay mw = (MilkyWay) renderables.get(0);

            if (reloadDataFlag.get()) {
                mw.status = LoadStatus.NOT_LOADED;
                reloadDataFlag.set(false);
            }

            switch (mw.status) {
            case NOT_LOADED:
                // PRELOAD
                mw.status = LoadStatus.LOADING;
                Thread loader = new Thread(() -> {
                    convertDataToGpuFormat(mw);
                    mw.status = LoadStatus.READY;
                });
                loader.start();
                break;
            case READY:
                // TO GPU
                streamToGpu();
                mw.status = LoadStatus.LOADED;
                break;
            case LOADED:
                // RENDER
                float alpha = getAlpha(mw);
                if (alpha > 0) {
                    ExtShaderProgram shaderProgram = getShaderProgram();

                    shaderProgram.begin();

                    shaderProgram.setUniformMatrix("u_projView", camera.getCamera().combined);
                    shaderProgram.setUniformf("u_camPos", camera.getPos().put(aux3f1));
                    shaderProgram.setUniformf("u_alpha", mw.opacity * alpha);
                    shaderProgram.setUniformf("u_edges", mw.getFadeIn().y, mw.getFadeOut().y);
                    double pointScaleFactor = 1.8e7;

                    // Rel, grav, z-buffer
                    addEffectsUniforms(shaderProgram, camera);

                    // General settings for all
                    Gdx.gl20.glEnable(GL20.GL_DEPTH_TEST);
                    Gdx.gl20.glEnable(GL20.GL_BLEND);

                    // PART 1: DUST - depth enabled - depth writes
                    Gdx.gl20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
                    Gdx.gl20.glDepthMask(true);

                    //  Dust
                    shaderProgram.setUniformf("u_maxPointSize", maxSizes[PType.DUST.ordinal()]);
                    shaderProgram.setUniformf("u_sizeFactor", (float) (3e2 * pointScaleFactor));
                    shaderProgram.setUniformf("u_intensity", 2.0f);
                    dust.mesh.render(shaderProgram, GL20.GL_TRIANGLES);

                    // PART2: BULGE + STARS + HII + GAS - depth enabled - no depth writes
                    Gdx.gl20.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE);
                    Gdx.gl20.glDepthMask(false);

                    // HII
                    shaderProgram.setUniformf("u_maxPointSize", maxSizes[PType.HII.ordinal()]);
                    shaderProgram.setUniformf("u_sizeFactor", (float) (1e2 * pointScaleFactor));
                    shaderProgram.setUniformf("u_intensity", 0.5f);
                    hii.mesh.render(shaderProgram, GL20.GL_TRIANGLES);

                    // Gas
                    shaderProgram.setUniformf("u_maxPointSize", maxSizes[PType.GAS.ordinal()]);
                    shaderProgram.setUniformf("u_sizeFactor", (float) (4e1 * pointScaleFactor));
                    shaderProgram.setUniformf("u_intensity", 0.5f);
                    gas.mesh.render(shaderProgram, GL20.GL_TRIANGLES);

                    // Bulge
                    shaderProgram.setUniformf("u_maxPointSize", maxSizes[PType.BULGE.ordinal()]);
                    shaderProgram.setUniformf("u_sizeFactor", (float) (3e1f * pointScaleFactor));
                    shaderProgram.setUniformf("u_intensity", 10f);
                    bulge.mesh.render(shaderProgram, GL20.GL_TRIANGLES);

                    // Stars
                    shaderProgram.setUniformf("u_maxPointSize", maxSizes[PType.STAR.ordinal()]);
                    shaderProgram.setUniformf("u_sizeFactor", (float) (.2e1 * pointScaleFactor));
                    shaderProgram.setUniformf("u_intensity", 8f);
                    stars.mesh.render(shaderProgram, GL20.GL_TRIANGLES);

                    shaderProgram.end();

                }
                break;
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
    public void notify(final Event event, Object source, final Object... data) {
        if (event == Event.GRAPHICS_QUALITY_UPDATED) {
            GraphicsQuality gq = (GraphicsQuality) data[0];
            GaiaSky.postRunnable(() -> {
                // Dispose textures and meshes
                dispose();

                // Initialize textures
                initializeTextureArray(gq);

                // Initialize maximum sizes
                initializeMaxSizes(gq);

                // Mark data for reload
                reloadDataFlag.set(true);

            });
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
}
