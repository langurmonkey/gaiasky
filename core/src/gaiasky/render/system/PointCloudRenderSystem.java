/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.system;

import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.utils.Array;
import gaiasky.event.IObserver;
import gaiasky.render.RenderGroup;
import gaiasky.render.api.IRenderable;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.scene.camera.ICamera;
import gaiasky.util.gdx.mesh.IntMesh;
import gaiasky.util.gdx.shader.ExtShaderProgram;

import java.util.List;

/**
 * Contains some common code to all point cloud renderers and some
 * scaffolding to make life easier. Should be used by point
 * clouds that render their particles as GL_POINTS.
 */
public abstract class PointCloudRenderSystem extends ImmediateModeRenderSystem implements IObserver {

    public PointCloudRenderSystem(SceneRenderer sceneRenderer, RenderGroup rg, float[] alphas, ExtShaderProgram[] shaders) {
        super(sceneRenderer, rg, alphas, shaders);
    }

    @Override
    protected void initVertices() {
        meshes = new Array<>();
    }

    /**
     * Adds the required vertex attributes for this renderer to the given list
     *
     * @param attributes The list of attributes
     */
    protected abstract void addVertexAttributes(Array<VertexAttribute> attributes);

    /**
     * Builds the vertex attributes array and returns it
     *
     * @return The vertex attributes array
     */
    protected VertexAttribute[] buildVertexAttributes() {
        Array<VertexAttribute> attributes = new Array<>();
        addVertexAttributes(attributes);

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
    protected abstract void offsets(MeshData curr);

    /**
     * Adds a new mesh data to the meshes list and increases the mesh data index
     *
     * @param maxVerts The max number of vertices this mesh data can hold
     *
     * @return The index of the new mesh data
     */
    protected int addMeshData(int maxVerts) {
        return addMeshData(maxVerts, 0);
    }

    /**
     * Adds a new mesh data to the meshes list and increases the mesh data index
     *
     * @param maxVerts   The max number of vertices this mesh data can hold
     * @param maxIndices The maximum number of indices this mesh data can hold
     *
     * @return The index of the new mesh data
     */
    protected int addMeshData(int maxVerts, int maxIndices) {
        int mdi = createMeshData();
        curr = meshes.get(mdi);

        VertexAttribute[] attributes = buildVertexAttributes();
        curr.mesh = new IntMesh(false, maxVerts, maxIndices, attributes);

        curr.vertexSize = curr.mesh.getVertexAttributes().vertexSize / 4;

        offsets(curr);

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
    public void renderStud(List<IRenderable> renderables, ICamera camera, double t) {
        if (renderables.size() > 0) {
            ExtShaderProgram shaderProgram = getShaderProgram();

            shaderProgram.begin();
            // Pre-render
            preRenderObjects(shaderProgram, camera);
            // Render
            for (IRenderable r : renderables) {
                renderObject(shaderProgram, r);
            }
            // Post-render
            postRenderObjects(shaderProgram, camera);
            shaderProgram.end();
        }
    }

}
