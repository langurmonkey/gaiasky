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
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;

import java.util.Random;

public class MWModelRenderSystem extends ImmediateRenderSystem implements IObserver {
    public static final boolean oit = false;
    private boolean UPDATE_POINTS = true;

    private Vector3 aux3f1;
    private int additionalOffset;

    private MeshData dust, bulge, stars, hii, gas;

    private Random rand = new Random(24601);

    public MWModelRenderSystem(RenderGroup rg, float[] alphas, ShaderProgram[] starShaders) {
        super(rg, alphas, starShaders);
        aux3f1 = new Vector3();
    }

    @Override
    protected void initShaderProgram() {
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

    private void initMesh(MeshData md, int nvertices) {

        VertexAttribute[] attribs = buildVertexAttributes();
        md.mesh = new IntMesh(false, nvertices, 0, attribs);

        md.vertexSize = md.mesh.getVertexAttributes().vertexSize / 4;
        md.colorOffset = md.mesh.getVertexAttribute(Usage.ColorPacked) != null ? md.mesh.getVertexAttribute(Usage.ColorPacked).offset / 4 : 0;
        additionalOffset = md.mesh.getVertexAttribute(Usage.Generic) != null ? md.mesh.getVertexAttribute(Usage.Generic).offset / 4 : 0;
    }

    private Vector3d computeCentre(Array<? extends ParticleBean> arr) {
        Vector3d c = new Vector3d();
        for (int i = 0; i < arr.size; i++) {
            ParticleBean pb = arr.get(i);
            c.add(pb.data[0], pb.data[1], pb.data[2]);
        }
        c.scl(1d / (double) arr.size);
        return c;
    }

    private MeshData getMeshData(Array<? extends ParticleBean> data) {
        return getMeshData(data, null);
    }

    private MeshData getMeshData(Array<? extends ParticleBean> data, ColorGenerator cg) {
        float hiDpiScaleFactor = GlobalConf.SCALE_FACTOR;

        MeshData md = new MeshData();
        initMesh(md, data.size);

        ensureTempVertsSize(data.size * md.vertexSize);
        for (ParticleBean star : data) {
            // COLOR
            float[] col = star.data.length >= 7 ? new float[] { (float) star.data[4], (float) star.data[5], (float) star.data[6] } : cg.generateColor();
            col[0] = MathUtilsd.clamp(col[0], 0f, 1f);
            col[1] = MathUtilsd.clamp(col[1], 0f, 1f);
            col[2] = MathUtilsd.clamp(col[2], 0f, 1f);
            tempVerts[md.vertexIdx + md.colorOffset] = Color.toFloatBits(col[0], col[1], col[2], 1f);

            // SIZE
            double starSize = star.data[3];
            tempVerts[md.vertexIdx + additionalOffset] = (float) (starSize * hiDpiScaleFactor);
            tempVerts[md.vertexIdx + additionalOffset + 1] = 0;

            // POSITION
            aux3f1.set((float) star.data[0], (float) star.data[1], (float) star.data[2]);
            final int idx = md.vertexIdx;
            tempVerts[idx] = aux3f1.x;
            tempVerts[idx + 1] = aux3f1.y;
            tempVerts[idx + 2] = aux3f1.z;

            md.vertexIdx += md.vertexSize;

        }
        md.mesh.setVertices(tempVerts, 0, md.vertexIdx);
        return md;
    }

    private void streamToGpu(MilkyWay mw) {
        StarColorGenerator scg = new StarColorGenerator();

        /* BULGE */
        bulge = getMeshData(mw.bulgeData, scg);

        /* STARS */
        stars = getMeshData(mw.starData, scg);

        /* HII */
        hii = getMeshData(mw.hiiData);

        /* GAS */
        gas = getMeshData(mw.gasData);

        /* DUST */
        dust = getMeshData(mw.dustData, new DustColorGenerator());
    }

    @Override
    public void renderStud(Array<IRenderable> renderables, ICamera camera, double t) {
        renderStudNoOit(renderables, camera, t);
    }

    public void renderStudNoOit(Array<IRenderable> renderables, ICamera camera, double t) {
        if (renderables.size > 0) {
            MilkyWay mw = (MilkyWay) renderables.get(0);

            /**
             * PARTICLES RENDERER
             */
            if (UPDATE_POINTS) {
                streamToGpu(mw);
                // Put flag down
                UPDATE_POINTS = false;
            }
            float alpha = getAlpha(mw);
            if (alpha > 0) {
                /**
                 * PARTICLE RENDERER
                 */
                // Enable gl_PointCoord
                Gdx.gl20.glEnable(34913);
                // Enable point sizes
                Gdx.gl20.glEnable(0x8642);

                ShaderProgram shaderProgram = getShaderProgram();

                shaderProgram.begin();
                shaderProgram.setUniformMatrix("u_projModelView", camera.getCamera().combined);
                shaderProgram.setUniformMatrix("u_view", camera.getCamera().view);
                shaderProgram.setUniformMatrix("u_projection", camera.getCamera().projection);

                shaderProgram.setUniformf("u_camPos", camera.getCurrent().getPos().put(aux3f1));
                shaderProgram.setUniformf("u_alpha", mw.opacity * alpha);
                shaderProgram.setUniformf("u_ar", GlobalConf.program.STEREOSCOPIC_MODE && (GlobalConf.program.STEREO_PROFILE != StereoProfile.HD_3DTV_HORIZONTAL && GlobalConf.program.STEREO_PROFILE != StereoProfile.ANAGLYPHIC) ? 0.5f : 1f);
                // Relativistic effects
                addEffectsUniforms(shaderProgram, camera);

                // Additive blending
                Gdx.gl20.glEnable(GL20.GL_DEPTH_TEST);
                Gdx.gl20.glEnable(GL20.GL_BLEND);

                // DUST - depth enabled - depth writes
                Gdx.gl20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
                Gdx.gl20.glDepthMask(true);

                shaderProgram.setUniformf("u_sizeFactor", 3f);
                shaderProgram.setUniformf("u_intensity", 1f);
                dust.mesh.render(shaderProgram, ShapeType.Point.getGlType());
                shaderProgram.end();

                // BULGE + STARS + HII + GAS - depth enabled - no depth writes
                Gdx.gl20.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE);
                shaderProgram.begin();
                Gdx.gl20.glDepthMask(false);

                // Bulge
                shaderProgram.setUniformf("u_sizeFactor", 2f);
                shaderProgram.setUniformf("u_intensity", 0.2f);
                bulge.mesh.render(shaderProgram, ShapeType.Point.getGlType());

                // Stars
                shaderProgram.setUniformf("u_sizeFactor", 0.8f);
                shaderProgram.setUniformf("u_intensity", 0.2f);
                stars.mesh.render(shaderProgram, ShapeType.Point.getGlType());

                // HII
                shaderProgram.setUniformf("u_sizeFactor", 0.8f);
                shaderProgram.setUniformf("u_intensity", 0.8f);
                hii.mesh.render(shaderProgram, ShapeType.Point.getGlType());

                // Gas
                shaderProgram.setUniformf("u_sizeFactor", 1.4f);
                shaderProgram.setUniformf("u_intensity", 0.25f);
                gas.mesh.render(shaderProgram, ShapeType.Point.getGlType());
                shaderProgram.end();
            }
        }

    }

    protected VertexAttribute[] buildVertexAttributes() {
        Array<VertexAttribute> attribs = new Array<>();
        attribs.add(new VertexAttribute(Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE));
        attribs.add(new VertexAttribute(Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE));
        attribs.add(new VertexAttribute(Usage.Generic, 4, "a_additional"));

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

    private class StarColorGenerator implements ColorGenerator{
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
            float r = (float) Math.abs(StdRandom.uniform() * 0.1);
            return new float[]{r, r, r};
        }
    }
}
