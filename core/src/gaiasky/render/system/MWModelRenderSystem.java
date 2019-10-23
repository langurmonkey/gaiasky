/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.system;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.render.IRenderable;
import gaiasky.scenegraph.MilkyWay;
import gaiasky.scenegraph.ParticleGroup.ParticleBean;
import gaiasky.scenegraph.SceneGraphNode.RenderGroup;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.Constants;
import gaiasky.util.GlobalConf;
import gaiasky.util.GlobalConf.SceneConf.GraphicsQuality;
import gaiasky.util.gdx.mesh.IntMesh;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.math.StdRandom;
import gaiasky.util.tree.LoadStatus;
import org.lwjgl.opengl.GL30;

public class MWModelRenderSystem extends ImmediateRenderSystem implements IObserver {
    private Vector3 aux3f1;

    private MeshData dust, bulge, stars, hii, gas;
    private GpuData dustA, bulgeA, starsA, hiiA, gasA;

    private TextureArray ta;

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

        PType(int id, int[] layers) {
            this.id = id;
            this.layers = layers;
        }

    }

    public MWModelRenderSystem(RenderGroup rg, float[] alphas, ExtShaderProgram[] starShaders) {
        super(rg, alphas, starShaders);
        aux3f1 = new Vector3();
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

        // Create TextureArray with 8 layers
        ta = new TextureArray(true, Pixmap.Format.RGBA8888, GlobalConf.data.dataFileHandle("data/tex/base/mw-sprites/star-00.png"), GlobalConf.data.dataFileHandle("data/tex/base/mw-sprites/star-01.png"), GlobalConf.data.dataFileHandle("data/tex/base/mw-sprites/dust-00.png"), GlobalConf.data.dataFileHandle("data/tex/base/mw-sprites/dust-01.png"), GlobalConf.data.dataFileHandle("data/tex/base/mw-sprites/dust-02.png"), GlobalConf.data.dataFileHandle("data/tex/base/mw-sprites/dust-03.png"), GlobalConf.data.dataFileHandle("data/tex/base/mw-sprites/dust-04.png"), GlobalConf.data.dataFileHandle("data/tex/base/mw-sprites/dust-05.png"));
        ta.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

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
    private GpuData convertDataToGpu(Array<? extends ParticleBean> data, ColorGenerator cg, PType type) {
        GraphicsQuality gq = GlobalConf.scene.GRAPHICS_QUALITY;
        int modulus;
        switch (gq) {
        case LOW:
            // Every second out
            modulus = 2;
            break;
        case NORMAL:
            // Every fourth out
            modulus = 4;
            break;
        case HIGH:
        case ULTRA:
        default:
            // All of them
            modulus = 0;
            break;
        }
        if (type == PType.DUST) {
            modulus = 50;
        }

        float hiDpiScaleFactor = GlobalConf.UI_SCALE_FACTOR;

        GpuData ad = new GpuData();

        int vertexSize = 3 + 1 + 3;
        int colorOffset = 3;
        int additionalOffset = 4;
        ad.vertices = new float[data.size * vertexSize];

        int nLayers = type.layers.length;

        int i = 0;
        for (ParticleBean star : data) {
            if (modulus == 0 || i % modulus == 0) {
                // COLOR
                float[] col = star.data.length >= 7 ? new float[] { (float) star.data[4], (float) star.data[5], (float) star.data[6] } : cg.generateColor();
                col[0] = MathUtilsd.clamp(col[0], 0f, 1f);
                col[1] = MathUtilsd.clamp(col[1], 0f, 1f);
                col[2] = MathUtilsd.clamp(col[2], 0f, 1f);
                ad.vertices[ad.vertexIdx + colorOffset] = Color.toFloatBits(col[0], col[1], col[2], 1f);

                // SIZE, TYPE, TEX LAYER
                double starSize = star.data[3];
                ad.vertices[ad.vertexIdx + additionalOffset] = (float) (starSize * hiDpiScaleFactor);
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
                    shaderProgram.setUniformf("u_sizeFactor", (float) (4.5e13 * Constants.DISTANCE_SCALE_FACTOR));
                    shaderProgram.setUniformf("u_intensity", 1.3f);
                    dust.mesh.render(shaderProgram, ShapeType.Point.getGlType());

                    // PART2: BULGE + STARS + HII + GAS - depth enabled - no depth writes
                    Gdx.gl20.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE);
                    Gdx.gl20.glDepthMask(false);

                    // HII
                    shaderProgram.setUniformf("u_sizeFactor", (float) (6e11 * Constants.DISTANCE_SCALE_FACTOR));
                    shaderProgram.setUniformf("u_intensity", 0.6f);
                    hii.mesh.render(shaderProgram, ShapeType.Point.getGlType());

                    // Gas
                    shaderProgram.setUniformf("u_sizeFactor", (float) (2e12 * Constants.DISTANCE_SCALE_FACTOR));
                    shaderProgram.setUniformf("u_intensity", 0.6f);
                    gas.mesh.render(shaderProgram, ShapeType.Point.getGlType());

                    // Bulge
                    shaderProgram.setUniformf("u_sizeFactor", (float) (5e11 * Constants.DISTANCE_SCALE_FACTOR));
                    shaderProgram.setUniformf("u_intensity", 0.8f);
                    bulge.mesh.render(shaderProgram, ShapeType.Point.getGlType());

                    // Stars
                    shaderProgram.setUniformf("u_sizeFactor", (float) (1e11 * Constants.DISTANCE_SCALE_FACTOR));
                    shaderProgram.setUniformf("u_intensity", 1f);
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
            float r = (float) Math.abs(StdRandom.uniform() * 0.15);
            return new float[] { r, r, r };
        }
    }

    private class GpuData {
        float[] vertices;
        int vertexIdx;
    }
}
