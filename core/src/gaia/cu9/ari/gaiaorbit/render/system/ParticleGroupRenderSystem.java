package gaia.cu9.ari.gaiaorbit.render.system;

import java.util.Random;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.render.IRenderable;
import gaia.cu9.ari.gaiaorbit.scenegraph.ParticleGroup;
import gaia.cu9.ari.gaiaorbit.scenegraph.ParticleGroup.ParticleBean;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode.RenderGroup;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf.ProgramConf.StereoProfile;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.comp.DistToCameraComparator;

public class ParticleGroupRenderSystem extends ImmediateRenderSystem implements IObserver {
    private final int N_MESHES = 50;
    Vector3 aux1;
    int additionalOffset, pmOffset;
    Random rand;

    public ParticleGroupRenderSystem(RenderGroup rg, float[] alphas, ShaderProgram[] shaders) {
        super(rg, alphas, shaders, 1500000);
        comp = new DistToCameraComparator<IRenderable>();
        rand = new Random(123);
        aux1 = new Vector3();
        EventManager.instance.subscribe(this, Events.DISPOSE_PARTICLE_GROUP_GPU_MESH);
    }

    @Override
    protected void initShaderProgram() {
    }

    @Override
    protected void initVertices() {
        /** STARS **/
        meshes = new MeshData[N_MESHES];
    }

    /**
     * Adds a new mesh data to the meshes list and increases the mesh data index
     * 
     * @param nVertices The max number of vertices this mesh data can hold
     * @return The index of the new mesh data
     */
    private int addMeshData(int nVertices) {
        // look for index
        int mdi;
        for (mdi = 0; mdi < N_MESHES; mdi++) {
            if (meshes[mdi] == null) {
                break;
            }
        }

        if (mdi >= N_MESHES) {
            logger.error("No more free meshes!");
            return -1;
        }

        curr = new MeshData();
        meshes[mdi] = curr;

        maxVertices = nVertices;

        VertexAttribute[] attribs = buildVertexAttributes();
        curr.mesh = new Mesh(false, maxVertices, 0, attribs);

        curr.vertexSize = curr.mesh.getVertexAttributes().vertexSize / 4;
        curr.colorOffset = curr.mesh.getVertexAttribute(Usage.ColorPacked) != null ? curr.mesh.getVertexAttribute(Usage.ColorPacked).offset / 4 : 0;
        pmOffset = curr.mesh.getVertexAttribute(Usage.Tangent) != null ? curr.mesh.getVertexAttribute(Usage.Tangent).offset / 4 : 0;
        additionalOffset = curr.mesh.getVertexAttribute(Usage.Generic) != null ? curr.mesh.getVertexAttribute(Usage.Generic).offset / 4 : 0;
        return mdi;
    }

    /**
     * Clears the mesh data at the index i
     * 
     * @param i The index
     */
    public void clearMeshData(int i) {
        assert i >= 0 && i < meshes.length : "Mesh data index out of bounds: " + i + " (n meshes = " + N_MESHES + ")";

        MeshData md = meshes[i];

        if (md != null && md.mesh != null) {
            md.mesh.dispose();
            md.vertices = null;
            md.indices = null;
            meshes[i] = null;
        }
    }

    @Override
    public void renderStud(Array<IRenderable> renderables, ICamera camera, double t) {
        if (renderables.size > 0) {
            for (IRenderable renderable : renderables) {
                ParticleGroup particleGroup = (ParticleGroup) renderable;
                curr = meshes[particleGroup.offset];
                /**
                 * GROUP RENDER
                 */
                if (!particleGroup.inGpu) {
                    particleGroup.offset = addMeshData(particleGroup.size());

                    checkRequiredVerticesSize(particleGroup.size() * curr.vertexSize);
                    curr.vertices = vertices;

                    for (ParticleBean pb : particleGroup.data()) {
                        double[] p = pb.data;
                        // COLOR
                        float[] c = particleGroup.cc;
                        curr.vertices[curr.vertexIdx + curr.colorOffset] = Color.toFloatBits(c[0], c[1], c[2], c[3]);

                        // SIZE
                        curr.vertices[curr.vertexIdx + additionalOffset] = particleGroup.size + (float) (rand.nextGaussian() * particleGroup.size / 4d);

                        // cb.transform.getTranslationf(aux);
                        // POSITION
                        final int idx = curr.vertexIdx;
                        curr.vertices[idx] = (float) p[0];
                        curr.vertices[idx + 1] = (float) p[1];
                        curr.vertices[idx + 2] = (float) p[2];

                        curr.vertexIdx += curr.vertexSize;
                    }
                    particleGroup.count = particleGroup.size() * curr.vertexSize;
                    curr.mesh.setVertices(curr.vertices, 0, particleGroup.count);
                    curr.vertices = null;

                    particleGroup.inGpu = true;

                }

                if (curr != null) {
                    /**
                     * PARTICLE RENDERER
                     */
                    // Enable gl_PointCoord
                    Gdx.gl20.glEnable(34913);
                    // Enable point sizes
                    Gdx.gl20.glEnable(0x8642);

                    // Additive blending
                    Gdx.gl20.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE);

                    ShaderProgram shaderProgram = getShaderProgram();

                    shaderProgram.begin();
                    shaderProgram.setUniformMatrix("u_projModelView", camera.getCamera().combined);
                    shaderProgram.setUniformf("u_camPos", camera.getCurrent().getPos().put(aux1));
                    shaderProgram.setUniformf("u_alpha", particleGroup.opacity * alphas[particleGroup.ct.getFirstOrdinal()]);
                    shaderProgram.setUniformf("u_ar", GlobalConf.program.STEREOSCOPIC_MODE && (GlobalConf.program.STEREO_PROFILE != StereoProfile.HD_3DTV_HORIZONTAL && GlobalConf.program.STEREO_PROFILE != StereoProfile.ANAGLYPHIC) ? 0.5f : 1f);
                    shaderProgram.setUniformf("u_profileDecay", particleGroup.profileDecay);
                    shaderProgram.setUniformf("u_sizeFactor", rc.scaleFactor);

                    // Relativistic effects
                    addEffectsUniforms(shaderProgram, camera);

                    curr.mesh.render(shaderProgram, ShapeType.Point.getGlType());
                    shaderProgram.end();

                    // Restore
                    Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
                }
            }
        }

    }

    protected VertexAttribute[] buildVertexAttributes() {
        Array<VertexAttribute> attribs = new Array<VertexAttribute>();
        attribs.add(new VertexAttribute(Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE));
        attribs.add(new VertexAttribute(Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE));
        attribs.add(new VertexAttribute(Usage.Generic, 1, "a_size"));

        VertexAttribute[] array = new VertexAttribute[attribs.size];
        for (int i = 0; i < attribs.size; i++)
            array[i] = attribs.get(i);
        return array;
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case DISPOSE_PARTICLE_GROUP_GPU_MESH:
            Integer meshIdx = (Integer) data[0];
            clearMeshData(meshIdx);
            break;

        }
    }

}
