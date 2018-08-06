package gaia.cu9.ari.gaiaorbit.render.system;

import org.lwjgl.opengl.GL11;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Array;

import gaia.cu9.ari.gaiaorbit.data.orbit.PolylineData;
import gaia.cu9.ari.gaiaorbit.render.IGPULineRenderable;
import gaia.cu9.ari.gaiaorbit.render.IRenderable;
import gaia.cu9.ari.gaiaorbit.scenegraph.Gaia;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode.RenderGroup;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;

/**
 * Sends all the lines to the GPU at once in a VBO.
 * 
 * @author tsagrista
 *
 */
public class LineGPURenderSystem extends ImmediateRenderSystem {
    protected ICamera camera;
    protected int glType;

    /** Hopefully we won't have more than 1000000 orbits at once **/
    private final int N_MESHES = 1000000;

    public LineGPURenderSystem(RenderGroup rg, float[] alphas, ShaderProgram[] shaders) {
        super(rg, alphas, shaders);
        glType = GL20.GL_LINE_STRIP;
    }

    @Override
    protected void initShaderProgram() {
    }

    @Override
    protected void initVertices() {
        meshes = new MeshData[N_MESHES];
    }

    /**
     * Adds a new mesh data to the meshes list and increases the mesh data index
     * 
     * @param nVertices
     *            The max number of vertices this mesh data can hold
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
            Logger.error(this.getClass().getSimpleName(), "No more free meshes!");
            return -1;
        }

        curr = new MeshData();
        meshes[mdi] = curr;

        maxVertices = nVertices + 1;

        VertexAttribute[] attribs = buildVertexAttributes();
        curr.mesh = new Mesh(false, maxVertices, 0, attribs);

        curr.vertices = new float[maxVertices * (curr.mesh.getVertexAttributes().vertexSize / 4)];
        curr.vertexSize = curr.mesh.getVertexAttributes().vertexSize / 4;
        curr.colorOffset = curr.mesh.getVertexAttribute(Usage.ColorPacked) != null ? curr.mesh.getVertexAttribute(Usage.ColorPacked).offset / 4 : 0;
        return mdi;
    }

    /**
     * Clears the mesh data at the index i
     * 
     * @param i
     *            The index
     */
    public void clearMeshData(int i) {
        assert i >= 0 && i < meshes.length : "Mesh data index out of bounds: " + i + " (n meshes = " + N_MESHES + ")";

        MeshData md = meshes[i];

        if (md != null && md.mesh != null) {
            md.mesh.dispose();

            meshes[i] = null;
        }
    }

    @Override
    public void renderStud(Array<IRenderable> renderables, ICamera camera, double t) {
        // Enable GL_LINE_SMOOTH
        Gdx.gl20.glEnable(GL11.GL_LINE_SMOOTH);
        Gdx.gl.glHint(GL20.GL_NICEST, GL11.GL_LINE_SMOOTH_HINT);
        // Enable GL_LINE_WIDTH
        Gdx.gl20.glEnable(GL20.GL_LINE_WIDTH);
        Gdx.gl20.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl20.glEnable(GL20.GL_BLEND);
        Gdx.gl20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        this.camera = camera;
        int size = renderables.size;

        for (int i = 0; i < size; i++) {
            IGPULineRenderable renderable = (IGPULineRenderable) renderables.get(i);

            /**
             * ADD LINES
             */
            if (!renderable.inGpu()) {
                PolylineData od = renderable.getPolyline();
                int npoints = od.getNumPoints();

                // Initialize or fetch mesh data
                if (!renderable.hasMeshData()) {
                    renderable.setOffset(addMeshData(npoints));
                } else {
                    curr = meshes[renderable.getOffset()];
                    // Check we still have capacity, otherwise, reinitialize.
                    if (curr.numVertices != od.getNumPoints()) {
                        curr.clear();
                        curr.mesh.dispose();
                        meshes[renderable.getOffset()] = null;
                        renderable.setOffset(addMeshData(npoints));
                    }
                }

                float[] cc = renderable.getColor();
                for (int point_i = 0; point_i < npoints; point_i++) {
                    color(cc[0], cc[1], cc[2], 1.0);
                    vertex((float) od.getX(point_i), (float) od.getY(point_i), (float) od.getZ(point_i));
                }
                // Close loop
                color(cc[0], cc[1], cc[2], 1.0);
                vertex((float) od.getX(0), (float) od.getY(0), (float) od.getZ(0));

                renderable.setCount(npoints * curr.vertexSize);
                curr.mesh.setVertices(curr.vertices, 0, renderable.getCount());
                renderable.setInGpu(true);
            }
            curr = meshes[renderable.getOffset()];

            /**
             * RENDER
             */

            ShaderProgram shaderProgram = getShaderProgram();

            shaderProgram.begin();

            // Regular
            Gdx.gl.glLineWidth(renderable.getLineWidth() * GlobalConf.SCALE_FACTOR);
            shaderProgram.setUniformMatrix("u_worldTransform", renderable.getLocalTransform());
            shaderProgram.setUniformMatrix("u_projModelView", camera.getCamera().combined);
            shaderProgram.setUniformf("u_alpha", (float) (renderable.getAlpha()) * getAlpha(renderable));
            if (renderable.getParent().name.equals("Gaia")) {
                Vector3d ppos = ((Gaia) renderable.getParent()).unrotatedPos;
                shaderProgram.setUniformf("u_parentPos", (float) ppos.x, (float) ppos.y, (float) ppos.z);
            } else {
                shaderProgram.setUniformf("u_parentPos", 0, 0, 0);
            }

            // Relativistic effects
            addEffectsUniforms(shaderProgram, camera);

            curr.mesh.render(shaderProgram, glType);

            shaderProgram.end();
        }
    }

    protected VertexAttribute[] buildVertexAttributes() {
        Array<VertexAttribute> attribs = new Array<VertexAttribute>();
        attribs.add(new VertexAttribute(Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE));
        attribs.add(new VertexAttribute(Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE));

        VertexAttribute[] array = new VertexAttribute[attribs.size];
        for (int i = 0; i < attribs.size; i++)
            array[i] = attribs.get(i);
        return array;
    }

}
