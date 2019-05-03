/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.render.system;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.render.IRenderable;
import gaia.cu9.ari.gaiaorbit.scenegraph.MilkyWay;
import gaia.cu9.ari.gaiaorbit.scenegraph.ParticleGroup.ParticleBean;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode.RenderGroup;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf.ProgramConf.StereoProfile;
import gaia.cu9.ari.gaiaorbit.util.gdx.mesh.IntMesh;
import gaia.cu9.ari.gaiaorbit.util.math.MathUtilsd;
import gaia.cu9.ari.gaiaorbit.util.math.StdRandom;
import gaia.cu9.ari.gaiaorbit.util.tree.LoadStatus;
import org.lwjgl.opengl.GL30;

public class MWModelRenderSystem extends ImmediateRenderSystem implements IObserver {
    private Vector3 aux3f1;

    private MeshData dust, bulge, stars, hii, gas;
    private GpuData dustA, bulgeA, starsA, hiiA, gasA;

    public MWModelRenderSystem(RenderGroup rg, float[] alphas, ShaderProgram[] starShaders) {
        super(rg, alphas, starShaders);
        aux3f1 = new Vector3();
    }

    @Override
    protected void initShaderProgram() {
        Gdx.gl.glEnable(GL30.GL_POINT_SPRITE);
        Gdx.gl.glEnable(GL30.GL_VERTEX_PROGRAM_POINT_SIZE);

        for (ShaderProgram shaderProgram : programs) {
            shaderProgram.begin();
            shaderProgram.setUniformf("u_pointAlphaMin", 0.1f);
            shaderProgram.setUniformf("u_pointAlphaMax", 1.0f);
            shaderProgram.end();
        }
    }

    @Override
    protected void initVertices() {
    }

    private MeshData toMeshData(GpuData ad){
        if(ad != null && ad.vertices != null){
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

    private GpuData convertDataToGpu(Array<? extends ParticleBean> data, ColorGenerator cg) {
        float hiDpiScaleFactor = GlobalConf.SCALE_FACTOR;

        GpuData ad = new GpuData();

        int vertexSize = 6;
        int colorOffset = 3;
        int additionalOffset = 4;
        ad.vertices = new float[data.size * vertexSize];

        for (ParticleBean star : data) {
            // COLOR
            float[] col = star.data.length >= 7 ? new float[] { (float) star.data[4], (float) star.data[5], (float) star.data[6] } : cg.generateColor();
            col[0] = MathUtilsd.clamp(col[0], 0f, 1f);
            col[1] = MathUtilsd.clamp(col[1], 0f, 1f);
            col[2] = MathUtilsd.clamp(col[2], 0f, 1f);
            ad.vertices[ad.vertexIdx + colorOffset] = Color.toFloatBits(col[0], col[1], col[2], 1f);

            // SIZE
            double starSize = star.data[3];
            ad.vertices[ad.vertexIdx + additionalOffset] = (float) (starSize * hiDpiScaleFactor);
            ad.vertices[ad.vertexIdx + additionalOffset + 1] = 0;

            // POSITION
            aux3f1.set((float) star.data[0], (float) star.data[1], (float) star.data[2]);
            final int idx = ad.vertexIdx;
            ad.vertices[idx] = aux3f1.x;
            ad.vertices[idx + 1] = aux3f1.y;
            ad.vertices[idx + 2] = aux3f1.z;

            ad.vertexIdx += vertexSize;

        }
        return ad;
    }

    private void convertDataToGpuFormat(MilkyWay mw) {
        logger.info("Converting galaxy data to VRAM format");
        StarColorGenerator scg = new StarColorGenerator();
        bulgeA = convertDataToGpu(mw.bulgeData, scg);
        starsA = convertDataToGpu(mw.starData, scg);
        hiiA = convertDataToGpu(mw.hiiData, scg);
        gasA = convertDataToGpu(mw.gasData, scg);
        dustA = convertDataToGpu(mw.dustData, new DustColorGenerator());
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
                    ShaderProgram shaderProgram = getShaderProgram();

                    shaderProgram.begin();

                    shaderProgram.setUniformMatrix("u_projModelView", camera.getCamera().combined);
                    shaderProgram.setUniformf("u_camPos", camera.getCurrent().getPos().put(aux3f1));
                    shaderProgram.setUniformf("u_alpha", mw.opacity * alpha);
                    shaderProgram.setUniformf("u_ar", GlobalConf.program.STEREOSCOPIC_MODE && (GlobalConf.program.STEREO_PROFILE != StereoProfile.HD_3DTV_HORIZONTAL && GlobalConf.program.STEREO_PROFILE != StereoProfile.ANAGLYPHIC) ? 0.5f : 1f);
                    // Relativistic effects
                    addEffectsUniforms(shaderProgram, camera);

                    // General settings for all
                    Gdx.gl20.glEnable(GL20.GL_DEPTH_TEST);
                    Gdx.gl20.glEnable(GL20.GL_BLEND);

                    // PART 1: DUST - depth enabled - depth writes
                    Gdx.gl20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
                    Gdx.gl20.glDepthMask(true);

                    //  Dust
                    shaderProgram.setUniformf("u_sizeFactor", 3f);
                    shaderProgram.setUniformf("u_intensity", 1f);
                    dust.mesh.render(shaderProgram, ShapeType.Point.getGlType());

                    // PART2: BULGE + STARS + HII + GAS - depth enabled - no depth writes
                    Gdx.gl20.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE);
                    Gdx.gl20.glDepthMask(false);

                    // Bulge
                    shaderProgram.setUniformf("u_sizeFactor", 2.2f);
                    shaderProgram.setUniformf("u_intensity", 0.3f);
                    bulge.mesh.render(shaderProgram, ShapeType.Point.getGlType());

                    // Stars
                    shaderProgram.setUniformf("u_sizeFactor", 0.5f);
                    shaderProgram.setUniformf("u_intensity", 0.6f);
                    stars.mesh.render(shaderProgram, ShapeType.Point.getGlType());

                    // HII
                    shaderProgram.setUniformf("u_sizeFactor", 0.8f);
                    shaderProgram.setUniformf("u_intensity", 0.8f);
                    hii.mesh.render(shaderProgram, ShapeType.Point.getGlType());

                    // Gas
                    shaderProgram.setUniformf("u_sizeFactor", 1.2f);
                    shaderProgram.setUniformf("u_intensity", 0.6f);
                    gas.mesh.render(shaderProgram, ShapeType.Point.getGlType());

                    shaderProgram.end();
                }
                break;
            }
        }

    }

    protected VertexAttribute[] buildVertexAttributes() {
        Array<VertexAttribute> attribs = new Array<>();
        attribs.add(new VertexAttribute(Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE));
        attribs.add(new VertexAttribute(Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE));
        attribs.add(new VertexAttribute(Usage.Generic, 2, "a_additional"));

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
