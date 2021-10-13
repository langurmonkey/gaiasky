/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.system;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.render.IRenderable;
import gaiasky.render.SceneGraphRenderer.RenderGroup;
import gaiasky.scenegraph.StarGroup;
import gaiasky.scenegraph.camera.CameraManager;
import gaiasky.scenegraph.camera.FovCamera;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.particle.IParticleRecord;
import gaiasky.util.Constants;
import gaiasky.util.Pair;
import gaiasky.util.Settings;
import gaiasky.util.color.Colormap;
import gaiasky.util.comp.DistToCameraComparator;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.gdx.mesh.IntMesh;
import gaiasky.util.gdx.shader.ExtShaderProgram;

/**
 * Contains some common code to all point cloud renderers.
 */
public abstract class PointCloudTriRenderSystem extends ImmediateRenderSystem implements IObserver {

    // Positions per vertex index
    protected Pair<Float, Float>[] vertPos;
    // UV coordinates per vertex index (0,1,2,4)
    protected Pair<Float, Float>[] vertUV;

    public PointCloudTriRenderSystem(RenderGroup rg, float[] alphas, ExtShaderProgram[] shaders) {
        super(rg, alphas, shaders);

        vertPos = new Pair[4];
        vertPos[0] = new Pair<>(1f, 1f);
        vertPos[1] = new Pair<>(1f, -1f);
        vertPos[2] = new Pair<>(-1f, -1f);
        vertPos[3] = new Pair<>(-1f, 1f);

        vertUV = new Pair[4];
        vertUV[0] = new Pair<>(1f, 1f);
        vertUV[1] = new Pair<>(1f, 0f);
        vertUV[2] = new Pair<>(0f, 0f);
        vertUV[3] = new Pair<>(0f, 1f);
    }

    @Override
    protected void initVertices() {
        meshes = new Array<>();
    }

    protected abstract void globalUniforms(ExtShaderProgram shaderProgram, ICamera camera);

    protected abstract void renderObject(ExtShaderProgram shaderProgram, IRenderable r);

    @Override
    public void renderStud(Array<IRenderable> renderables, ICamera camera, double t) {
        if (renderables.size > 0) {
            ExtShaderProgram shaderProgram = getShaderProgram();
            float starPointSize = Settings.settings.scene.star.pointSize * 0.2f;

            shaderProgram.begin();
            // Global uniforms
            globalUniforms(shaderProgram, camera);
            // Render
            renderables.forEach((r) -> renderObject(shaderProgram, r));
            shaderProgram.end();
        }
    }

    protected void index(int idx) {
        tempIndices[curr.indexIdx++] = idx;
    }

    /**
     * Adds the required vertex attributes for this renderer to the given list
     * @param attributes The list of attributes
     */
    protected abstract void addVertexAttributes(Array<VertexAttribute> attributes);

    /**
     * Builds the vertex attributes array and returns it
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
     * @param curr The current mesh data
     */
    protected abstract void offsets(MeshData curr);

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



}
