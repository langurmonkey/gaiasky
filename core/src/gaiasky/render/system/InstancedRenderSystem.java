/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.system;

import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.utils.Array;
import gaiasky.event.IObserver;
import gaiasky.render.IRenderable;
import gaiasky.render.SceneGraphRenderer.RenderGroup;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.gdx.mesh.IntMesh;
import gaiasky.util.gdx.shader.ExtShaderProgram;

/**
 * Common code to all render systems that render quads with GPU instancing.
 */
public abstract class InstancedRenderSystem extends ImmediateModeRenderSystem implements IObserver {

    // Auxiliary array that holds vertices temporarily
    // This buffer will be used with divisor=1, so each instance
    // gets one
    protected float[] tempInstanceAttribs;

    public InstancedRenderSystem(RenderGroup rg, float[] alphas, ExtShaderProgram[] shaders) {
        super(rg, alphas, shaders);
    }

    @Override
    protected void initVertices() {
        meshes = new Array<>();
        initializeModel();
    }

    protected void ensureInstanceAttribsSize(int size) {
        if (tempInstanceAttribs == null || tempInstanceAttribs.length < size) {
            tempInstanceAttribs = new float[size];
        }
    }

    // By default, the model is initialized as two triangles (6 vertices)
    // with texture coordinates (uv).
    // This method fills in the tempVerts array.
    protected void initializeModel() {
        // Fill in divisor0 vertices (vertex position and UV)
        ensureTempVertsSize(6 * 4);
        int i = 0;
        // 0 (0)
        tempVerts[i++] = 1; // pos
        tempVerts[i++] = 1;
        tempVerts[i++] = 1; // uv
        tempVerts[i++] = 1;
        // 1 (1)
        tempVerts[i++] = 1; // pos
        tempVerts[i++] = -1;
        tempVerts[i++] = 1; // uv
        tempVerts[i++] = 0;
        // 2 (2)
        tempVerts[i++] = -1; // pos
        tempVerts[i++] = -1;
        tempVerts[i++] = 0; // uv
        tempVerts[i++] = 0;
        // 3 (2)
        tempVerts[i++] = -1; // pos
        tempVerts[i++] = -1;
        tempVerts[i++] = 0; // uv
        tempVerts[i++] = 0;
        // 4 (3)
        tempVerts[i++] = -1; // pos
        tempVerts[i++] = 1;
        tempVerts[i++] = 0; // uv
        tempVerts[i++] = 1;
        // 5 (0)
        tempVerts[i++] = 1; // pos
        tempVerts[i++] = 1;
        tempVerts[i++] = 1; // uv
        tempVerts[i] = 1;
    }

    /**
     * Adds the required vertex attributes for this renderer to the given list. These
     * attributes are added only once for all instances (divisor=0)
     *
     * @param attributes The list of attributes with divisor=0
     */
    protected abstract void addAttributesDivisor0(Array<VertexAttribute> attributes);

    /**
     * Adds the required vertex attributes for this renderer to the given list. These
     * attributes are added for every instance (divisor=1)
     *
     * @param attributes The list of attributes with divisor=1
     */
    protected abstract void addAttributesDivisor1(Array<VertexAttribute> attributes);

    /**
     * Builds the vertex attributes with divisor=0 array and returns it
     *
     * @return The vertex attributes array
     */
    protected VertexAttribute[] buildAttributesDivisor0() {
        Array<VertexAttribute> attributes = new Array<>();
        addAttributesDivisor0(attributes);

        VertexAttribute[] array = new VertexAttribute[attributes.size];
        for (int i = 0; i < attributes.size; i++)
            array[i] = attributes.get(i);
        return array;
    }

    /**
     * Builds the vertex attributes with divisor=1 array and returns it
     *
     * @return The vertex attributes array
     */
    protected VertexAttribute[] buildAttributesDivisor1() {
        Array<VertexAttribute> attributes = new Array<>();
        addAttributesDivisor1(attributes);

        VertexAttribute[] array = new VertexAttribute[attributes.size];
        for (int i = 0; i < attributes.size; i++)
            array[i] = attributes.get(i);
        return array;
    }

    /**
     * Computes the offset for each vertex attribute. The offsets will be
     * used later in the render stage.
     *
     * @param curr The current mesh data
     */
    protected abstract void offsets0(MeshData curr);

    /**
     * Computes the offset for each vertex attribute in the instanced array. The offsets will be
     * used later in the render stage.
     *
     * @param curr The current mesh data
     */
    protected abstract void offsets1(MeshData curr);

    /**
     * Adds a new mesh data to the meshes list and increases the mesh data index
     *
     * @param maxVerts0 The max number of vertices the divisor 0 mesh data can hold
     * @param maxVerts1 The max number of vertices the divisor 1 mesh data can hold
     *
     * @return The index of the new mesh data
     */
    protected int addMeshData(int maxVerts0, int maxVerts1) {
        int mdi = createMeshData();
        curr = meshes.get(mdi);

        VertexAttribute[] attributes0 = buildAttributesDivisor0();
        VertexAttribute[] attributes1 = buildAttributesDivisor1();
        curr.mesh = new IntMesh(true, maxVerts0, maxVerts1, attributes0, attributes1);

        curr.vertexSize = curr.mesh.getVertexAttributes().vertexSize / 4;
        curr.instanceSize = curr.mesh.getInstanceAttributes().vertexSize / 4;

        offsets0(curr);
        offsets1(curr);

        return mdi;
    }

    protected void preRenderObjects(ExtShaderProgram shaderProgram, ICamera camera) {
        // Empty, override if needed
    }

    protected void renderObject(ExtShaderProgram shaderProgram, IRenderable renderable) {
        // Empty, override if needed
    }

    protected void postRenderObjects(ExtShaderProgram shaderProgram, ICamera camera) {
        // Empty, override if needed
    }

    @Override
    public void renderStud(Array<IRenderable> renderables, ICamera camera, double t) {
        if (renderables.size > 0) {
            ExtShaderProgram shaderProgram = getShaderProgram();

            shaderProgram.begin();
            // Pre-render
            preRenderObjects(shaderProgram, camera);
            // Render
            renderables.forEach((r) -> renderObject(shaderProgram, r));
            // Post-render
            postRenderObjects(shaderProgram, camera);
            shaderProgram.end();
        }
    }

}
