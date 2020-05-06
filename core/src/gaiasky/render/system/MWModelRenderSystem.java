/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.system;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.render.IRenderable;
import gaiasky.scenegraph.MilkyWay;
import gaiasky.scenegraph.ParticleGroup.ParticleBean;
import gaiasky.scenegraph.SceneGraphNode.RenderGroup;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.GlobalConf;
import gaiasky.util.GlobalConf.SceneConf.GraphicsQuality;
import gaiasky.util.GlobalResources;
import gaiasky.util.gdx.mesh.IntMesh;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.math.StdRandom;
import gaiasky.util.tree.LoadStatus;
import org.lwjgl.opengl.GL30;

import java.util.List;

public class MWModelRenderSystem extends ImmediateRenderSystem implements IObserver {
    private static final String texFolder = "data/galaxy/sprites/";

    private Vector3 aux3f1;
    private MeshData dust, bulge, stars, hii, gas;
    private GpuData dustA, bulgeA, starsA, hiiA, gasA;

    private TextureArray ta;
    // Max sizes for dust, star, bulge, gas and hii
    private float[] maxSizes;

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
        initializeMaxSizes(GlobalConf.scene.GRAPHICS_QUALITY);
        EventManager.instance.subscribe(this, Events.GRAPHICS_QUALITY_UPDATED);
    }

    /**
     * Initializes the maximum size per component regarding the given graphics quality
     * @param gq The graphics quality
     */
    private void initializeMaxSizes(GraphicsQuality gq) {
        if (gq.isUltra()) {
            this.maxSizes[PType.DUST.ordinal()] = 4000f;
            this.maxSizes[PType.STAR.ordinal()] = 150f;
            this.maxSizes[PType.BULGE.ordinal()] = 300f;
            this.maxSizes[PType.GAS.ordinal()] = 4000f;
            this.maxSizes[PType.HII.ordinal()] = 4000f;
        } else if (gq.isHigh()) {
            this.maxSizes[PType.DUST.ordinal()] = 1000f;
            this.maxSizes[PType.STAR.ordinal()] = 20f;
            this.maxSizes[PType.BULGE.ordinal()] = 250f;
            this.maxSizes[PType.GAS.ordinal()] = 1200f;
            this.maxSizes[PType.HII.ordinal()] = 400f;
        } else if (gq.isNormal()) {
            this.maxSizes[PType.DUST.ordinal()] = 60f;
            this.maxSizes[PType.STAR.ordinal()] = 10f;
            this.maxSizes[PType.BULGE.ordinal()] = 60f;
            this.maxSizes[PType.GAS.ordinal()] = 120f;
            this.maxSizes[PType.HII.ordinal()] = 70f;
        } else if (gq.isLow()) {
            this.maxSizes[PType.DUST.ordinal()] = 50f;
            this.maxSizes[PType.STAR.ordinal()] = 10f;
            this.maxSizes[PType.BULGE.ordinal()] = 50f;
            this.maxSizes[PType.GAS.ordinal()] = 100f;
            this.maxSizes[PType.HII.ordinal()] = 60f;
        }
    }

    @Override
    protected void initShaderProgram() {
        Gdx.gl.glEnable(GL30.GL_POINT_SPRITE);
        Gdx.gl.glEnable(GL30.GL_VERTEX_PROGRAM_POINT_SIZE);

        for (ExtShaderProgram shaderProgram : programs) {
            shaderProgram.begin();
            shaderProgram.setUniformf("u_pointAlphaMin", 0.1f);
            shaderProgram.setUniformf("u_pointAlphaMax", 1.0f);
            shaderProgram.end();
        }
        initializeTextureArray(GlobalConf.scene.GRAPHICS_QUALITY);
    }

    private void initializeTextureArray(GraphicsQuality gq) {
        // Create TextureArray with 8 layers
        FileHandle s00 = unpack("star-00*.png", gq);
        FileHandle s01 = unpack("star-01*.png", gq);
        FileHandle d00 = unpack("dust-00*.png", gq);
        FileHandle d01 = unpack("dust-01*.png", gq);
        FileHandle d02 = unpack("dust-02*.png", gq);
        FileHandle d03 = unpack("dust-03*.png", gq);
        FileHandle d04 = unpack("dust-04*.png", gq);
        FileHandle d05 = unpack("dust-05*.png", gq);
        ta = new TextureArray(true, Pixmap.Format.RGBA8888, s00, s01, d00, d01, d02, d03, d04, d05);
        ta.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }

    private FileHandle unpack(String texName, GraphicsQuality gq) {
        return GlobalConf.data.dataFileHandle(GlobalResources.unpackTexName(texFolder + texName, gq));
    }

    private void disposeTextureArray() {
        ta.dispose();
    }

    @Override
    public void dispose() {
        super.dispose();
        disposeTextureArray();
    }

    @Override
    protected void initVertices() {
    }

    private MeshData toMeshData(GpuData ad) {
        if (ad != null && ad.vertices != null) {
            MeshData md = new MeshData();
            VertexAttribute[] attribs = buildVertexAttributes();
            md.mesh = new IntMesh(true, ad.vertices.length / 6, 0, attribs);
            md.vertexSize = md.mesh.getVertexAttributes().vertexSize / 4;
            md.colorOffset = md.mesh.getVertexAttribute(Usage.ColorPacked) != null ? md.mesh.getVertexAttribute(Usage.ColorPacked).offset / 4 : 0;
            md.vertexIdx = ad.vertexIdx;
            md.mesh.setVertices(ad.vertices, 0, md.vertexIdx);

            ad.vertices = null;
            return md;
        }
        return null;
    }

    /**
     * 0 - dust
     * 1 - star
     * 2 - bulge
     * 3 - gas
     * 4 - hii
     *
     * @param data
     * @param cg
     * @param type The type
     * @return
     */
    private GpuData convertDataToGpu(List<? extends ParticleBean> data, ColorGenerator cg, PType type) {
        GpuData ad = new GpuData();

        int vertexSize = 3 + 1 + 3;
        int colorOffset = 3;
        int additionalOffset = 4;
        ad.vertices = new float[data.size() * vertexSize];

        int nLayers = type.layers.length;

        int i = 0;
        for (ParticleBean star : data) {
            if (type.modulus == 0 || i % type.modulus == 0) {
                // COLOR
                float[] col = star.data.length >= 7 ? new float[] { (float) star.data[4], (float) star.data[5], (float) star.data[6] } : cg.generateColor();
                col[0] = MathUtilsd.clamp(col[0], 0f, 1f);
                col[1] = MathUtilsd.clamp(col[1], 0f, 1f);
                col[2] = MathUtilsd.clamp(col[2], 0f, 1f);
                ad.vertices[ad.vertexIdx + colorOffset] = Color.toFloatBits(col[0], col[1], col[2], 1f);

                // SIZE, TYPE, TEX LAYER
                double starSize = star.data[3];
                ad.vertices[ad.vertexIdx + additionalOffset] = (float) (starSize * GlobalConf.UI_SCALE_FACTOR);
                ad.vertices[ad.vertexIdx + additionalOffset + 1] = (float) type.id;
                ad.vertices[ad.vertexIdx + additionalOffset + 2] = (float) type.layers[StdRandom.uniform(nLayers)];

                // POSITION
                aux3f1.set((float) star.data[0], (float) star.data[1], (float) star.data[2]);
                final int idx = ad.vertexIdx;
                ad.vertices[idx] = aux3f1.x;
                ad.vertices[idx + 1] = aux3f1.y;
                ad.vertices[idx + 2] = aux3f1.z;

                ad.vertexIdx += vertexSize;
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
        bulge = toMeshData(bulgeA);
        bulgeA = null;

        stars = toMeshData(starsA);
        starsA = null;

        hii = toMeshData(hiiA);
        hiiA = null;

        gas = toMeshData(gasA);
        gasA = null;

        dust = toMeshData(dustA);
        dustA = null;
    }

    @Override
    public void renderStud(Array<IRenderable> renderables, ICamera camera, double t) {
        if (renderables.size > 0) {
            MilkyWay mw = (MilkyWay) renderables.get(0);

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
                    double fovf = camera.getFovFactor();

                    shaderProgram.begin();

                    ta.bind(0);
                    shaderProgram.setUniformi("u_textures", 0);

                    shaderProgram.setUniformMatrix("u_projModelView", camera.getCamera().combined);
                    shaderProgram.setUniformf("u_camPos", camera.getCurrent().getPos().put(aux3f1));
                    shaderProgram.setUniformf("u_alpha", mw.opacity * alpha);
                    shaderProgram.setUniformf("u_ar", GlobalConf.program.isStereoHalfWidth() ? 2f : 1f);
                    shaderProgram.setUniformf("u_edges", mw.getFadeIn().y, mw.getFadeOut().y);

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
                    shaderProgram.setUniformf("u_sizeFactor", (float) (1.6e13 / fovf));
                    shaderProgram.setUniformf("u_intensity", 2.2f);
                    dust.mesh.render(shaderProgram, ShapeType.Point.getGlType());

                    // PART2: BULGE + STARS + HII + GAS - depth enabled - no depth writes
                    Gdx.gl20.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE);
                    Gdx.gl20.glDepthMask(false);

                    // HII
                    shaderProgram.setUniformf("u_maxPointSize", maxSizes[PType.HII.ordinal()]);
                    shaderProgram.setUniformf("u_sizeFactor", (float) (7e11 / fovf));
                    shaderProgram.setUniformf("u_intensity", 1.2f);
                    hii.mesh.render(shaderProgram, ShapeType.Point.getGlType());

                    // Gas
                    shaderProgram.setUniformf("u_maxPointSize", maxSizes[PType.GAS.ordinal()]);
                    shaderProgram.setUniformf("u_sizeFactor", (float) (1.9e12 / fovf));
                    shaderProgram.setUniformf("u_intensity", 0.8f);
                    gas.mesh.render(shaderProgram, ShapeType.Point.getGlType());

                    // Bulge
                    shaderProgram.setUniformf("u_maxPointSize", maxSizes[PType.BULGE.ordinal()]);
                    shaderProgram.setUniformf("u_sizeFactor", (float) (2e12 / fovf));
                    shaderProgram.setUniformf("u_intensity", 0.5f);
                    bulge.mesh.render(shaderProgram, ShapeType.Point.getGlType());

                    // Stars
                    shaderProgram.setUniformf("u_maxPointSize", maxSizes[PType.STAR.ordinal()]);
                    shaderProgram.setUniformf("u_sizeFactor", (float) (0.3e11 / fovf));
                    shaderProgram.setUniformf("u_intensity", 2.5f);
                    stars.mesh.render(shaderProgram, ShapeType.Point.getGlType());

                    shaderProgram.end();
                }
                break;
            }
        }

    }

    protected VertexAttribute[] buildVertexAttributes() {
        Array<VertexAttribute> attribs = new Array<>();
        attribs.add(new VertexAttribute(Usage.Position, 3, ExtShaderProgram.POSITION_ATTRIBUTE));
        attribs.add(new VertexAttribute(Usage.ColorPacked, 4, ExtShaderProgram.COLOR_ATTRIBUTE));
        attribs.add(new VertexAttribute(Usage.Generic, 3, "a_additional"));

        VertexAttribute[] array = new VertexAttribute[attribs.size];
        for (int i = 0; i < attribs.size; i++)
            array[i] = attribs.get(i);
        return array;
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case GRAPHICS_QUALITY_UPDATED:
            GraphicsQuality gq = (GraphicsQuality) data[0];
            GaiaSky.postRunnable(() -> {
                disposeTextureArray();
                initializeTextureArray(gq);
                initializeMaxSizes(gq);
            });
            break;
        default:
            break;
        }
    }

    private interface ColorGenerator {
        float[] generateColor();
    }

    private class StarColorGenerator implements ColorGenerator {
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

    private class DustColorGenerator implements ColorGenerator {
        @Override
        public float[] generateColor() {
            float r = (float) Math.abs(StdRandom.uniform() * 0.19);
            return new float[] { r, r, r };
        }
    }

    private class GpuData {
        float[] vertices;
        int vertexIdx;
    }
}
