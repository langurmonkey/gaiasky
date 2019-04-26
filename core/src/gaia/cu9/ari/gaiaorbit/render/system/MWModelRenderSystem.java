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
import gaia.cu9.ari.gaiaorbit.util.Constants;
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

    private MeshData dust, bulge, stars, hii;

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

    private float[] genStarColor() {
        float r = (float) rand.nextGaussian() * 0.15f;
        if (rand.nextInt(2) == 0) {
            // Blue/white star
            return new float[] { 0.95f - r, 0.8f - r, 0.6f};
        } else {
            // Red/white star
            return new float[] { 0.95f, 0.8f - r, 0.6f - r };
        }
    }

    private MeshData getMeshData(MilkyWay mw, Array<? extends ParticleBean> data, int nFactor) {
        return getMeshData(mw, data, nFactor, 100, 1, 0.5f);
    }
    private MeshData getMeshData(MilkyWay mw, Array<? extends ParticleBean> data, int nFactor, float maxDisplacementPc, float starSizeFactor, float intensity) {
        Vector3 center = mw.getPosition().toVector3();
        float hiDpiScaleFactor = GlobalConf.SCALE_FACTOR;

        // A few parsecs of displacement
        float maxDisplacement = (float) (maxDisplacementPc * Constants.PC_TO_U);
        if(nFactor <= 1)
            maxDisplacement = 0;

        MeshData md = new MeshData();
        initMesh(md, data.size * nFactor);

        ensureTempVertsSize(data.size * nFactor * md.vertexSize);
        for (ParticleBean star : data) {
            for (int i = 0; i < nFactor; i++) {
                // VERTEX
                float offsetX = (float) rand.nextGaussian() * maxDisplacement;
                float offsetY = (float) rand.nextGaussian() * maxDisplacement;
                float offsetZ = (float) rand.nextGaussian() * maxDisplacement;
                aux3f1.set((float) star.data[0] + offsetX, (float) star.data[1] + offsetY, (float) star.data[2] + offsetZ);
                double distanceCenter = aux3f1.sub(center).len() / (mw.getRadius() * 2f);

                float[] col = star.data.length >= 7 ? new float[]{(float) star.data[4], (float) star.data[5], (float) star.data[6]} :genStarColor();

                if (distanceCenter < 0.5f) {
                    float add = (float) MathUtilsd.clamp(0.5f - distanceCenter, 0f, 1f) * 0.5f;
                    col[0] = col[0] + add;
                    col[1] = col[1] + add;
                    col[2] = col[2] + add;
                }

                col[0] = MathUtilsd.clamp(col[0], 0f, 1f);
                col[1] = MathUtilsd.clamp(col[1], 0f, 1f);
                col[2] = MathUtilsd.clamp(col[2], 0f, 1f);

                // COLOR
                tempVerts[md.vertexIdx + md.colorOffset] = Color.toFloatBits(col[0], col[1], col[2], intensity);

                // SIZE
                double starSize = star.data[3] * starSizeFactor;
                tempVerts[md.vertexIdx + additionalOffset] = (float) (starSize * hiDpiScaleFactor);
                tempVerts[md.vertexIdx + additionalOffset + 1] = 0;

                // cb.transform.getTranslationf(aux);
                // POSITION
                aux3f1.set((float) star.data[0] + offsetX, (float) star.data[1] + offsetY, (float) star.data[2] + offsetZ);
                final int idx = md.vertexIdx;
                tempVerts[idx] = aux3f1.x;
                tempVerts[idx + 1] = aux3f1.y;
                tempVerts[idx + 2] = aux3f1.z;

                md.vertexIdx += md.vertexSize;
            }
        }
        md.mesh.setVertices(tempVerts, 0, md.vertexIdx);
        return md;
    }

    private void streamToGpu(MilkyWay mw) {
        /* BULGE */
        bulge = getMeshData(mw, mw.bulgeData, 1, 100, 1.5f, 0.3f);

        /* STARS */
        stars = getMeshData(mw, mw.starData, 1, 100, 1, 0.2f);

        /* HII */
        hii = getMeshData(mw, mw.hiiData, 1, 100, 1f, 0.3f);

        /* DUST */
        // This factor increases the number of particles
        int nFactor = 1;
        float maxDisplacement = (float) (50d * Constants.PC_TO_U);
        if(nFactor <= 1)
            maxDisplacement = 0;

        dust = new MeshData();
        initMesh(dust, mw.dustData.size * nFactor);
        ensureTempVertsSize(mw.dustData.size * nFactor * dust.vertexSize);
        for (ParticleBean p : mw.dustData) {
            for (int i = 0; i < nFactor; i++) {
                // COLOR
                float r = (float) Math.abs(StdRandom.uniform() * 0.15);
                float[] col = new float[] { r, r, r, 1.0f };

                tempVerts[dust.vertexIdx + dust.colorOffset] = Color.toFloatBits(col[0], col[1], col[2], col[3]);

                // SIZE
                double starSize = p.data[3] * 1.5 + Math.abs(rand.nextGaussian());
                tempVerts[dust.vertexIdx + additionalOffset] = (float) (starSize * GlobalConf.SCALE_FACTOR);
                tempVerts[dust.vertexIdx + additionalOffset + 1] = 1;

                // POSITION
                double offsetX = (rand.nextGaussian() * maxDisplacement);
                double offsetY = (rand.nextGaussian() * maxDisplacement);
                double offsetZ = (rand.nextGaussian() * maxDisplacement);
                aux3f1.set((float) (p.data[0] + offsetX), (float) (p.data[1] + offsetY), (float) (p.data[2] + offsetZ));
                final int idx = dust.vertexIdx;
                tempVerts[idx] = aux3f1.x;
                tempVerts[idx + 1] = aux3f1.y;
                tempVerts[idx + 2] = aux3f1.z;

                dust.vertexIdx += dust.vertexSize;
            }

        }
        dust.mesh.setVertices(tempVerts, 0, dust.vertexIdx);
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

                // DUST - depth enabled - no depth writes
                Gdx.gl20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
                Gdx.gl20.glDepthMask(true);
                dust.mesh.render(shaderProgram, ShapeType.Point.getGlType());
                shaderProgram.end();

                // BULGE + STARS + HII - depth enabled - depth writes
                Gdx.gl20.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE);
                shaderProgram.begin();
                Gdx.gl20.glDepthMask(false);
                bulge.mesh.render(shaderProgram, ShapeType.Point.getGlType());
                stars.mesh.render(shaderProgram, ShapeType.Point.getGlType());
                hii.mesh.render(shaderProgram, ShapeType.Point.getGlType());
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

}
