/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.system;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.render.IPointRenderable;
import gaiasky.render.IRenderable;
import gaiasky.render.SceneGraphRenderer.RenderGroup;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.gdx.mesh.IntMesh;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import org.lwjgl.opengl.GL30;

public class PointRenderSystem extends ImmediateModeRenderSystem {
    protected static final Log logger = Logger.getLogger(PointRenderSystem.class);

    protected ICamera camera;
    protected int glType;
    private int sizeOffset;

    protected Vector3 aux2;

    @Override
    protected void initShaderProgram() {
        Gdx.gl.glEnable(GL30.GL_POINT_SPRITE);
        Gdx.gl.glEnable(GL30.GL_VERTEX_PROGRAM_POINT_SIZE);
    }

    public PointRenderSystem(RenderGroup rg, float[] alphas, ExtShaderProgram[] shaders) {
        super(rg, alphas, shaders, -1);
        glType = GL20.GL_POINTS;
        aux2 = new Vector3();
    }

    @Override
    protected void initVertices() {
        meshes = new Array<>();
        initVertices(meshIdx++);
    }

    private void initVertices(int index) {
        if (index >= meshes.size) {
            meshes.setSize(index + 1);
        }
        if (meshes.get(index) == null) {
            if (index > 0)
                logger.info("Capacity too small, creating new meshdata: " + curr.capacity);
            curr = new MeshData();
            meshes.set(index, curr);

            curr.capacity = 1000;

            VertexAttribute[] attribs = buildVertexAttributes();
            curr.mesh = new IntMesh(false, curr.capacity, 0, attribs);

            curr.vertexSize = curr.mesh.getVertexAttributes().vertexSize / 4;
            curr.vertices = new float[curr.capacity * curr.vertexSize];
            curr.colorOffset = curr.mesh.getVertexAttribute(Usage.ColorPacked) != null ? curr.mesh.getVertexAttribute(Usage.ColorPacked).offset / 4 : 0;
            sizeOffset = curr.mesh.getVertexAttribute(Usage.Generic).offset / 4;
        } else {
            curr = meshes.get(index);
        }
    }

    protected VertexAttribute[] buildVertexAttributes() {
        Array<VertexAttribute> attribs = new Array<>();
        attribs.add(new VertexAttribute(Usage.Position, 3, ExtShaderProgram.POSITION_ATTRIBUTE));
        attribs.add(new VertexAttribute(Usage.ColorPacked, 4, ExtShaderProgram.COLOR_ATTRIBUTE));
        attribs.add(new VertexAttribute(Usage.Generic, 1, "a_size"));

        VertexAttribute[] array = new VertexAttribute[attribs.size];
        for (int i = 0; i < attribs.size; i++)
            array[i] = attribs.get(i);
        return array;
    }

    @Override
    public void renderStud(Array<IRenderable> renderables, ICamera camera, double t) {
        this.camera = camera;

        ExtShaderProgram shaderProgram = getShaderProgram();
        shaderProgram.begin();
        shaderProgram.setUniformMatrix("u_projView", camera.getCamera().combined);
        addEffectsUniforms(shaderProgram, camera);

        renderables.forEach(r -> {
            IPointRenderable renderable = (IPointRenderable) r;
            renderable.render(this, camera, getAlpha(renderable));

            renderable.blend();
            renderable.depth();

            for (int md = 0; md < meshIdx; md++) {
                MeshData meshd = meshes.get(md);
                meshd.mesh.setVertices(meshd.vertices, 0, meshd.vertexIdx);
                meshd.mesh.render(shaderProgram, glType);

                meshd.clear();
            }
        });
        shaderProgram.end();

        // Reset indices
        meshIdx = 1;
        curr = meshes.get(0);

    }

    public void addPoint(IPointRenderable pr, double x, double y, double z, float pointSize, float r, float g, float b, float a) {
        // Check if 3 more indices fit
        if (curr.numVertices + 1 >= curr.capacity) {
            // Create new mesh data
            initVertices(meshIdx++);
        }

        color(r, g, b, a);
        size(pointSize);
        vertex((float) x, (float) y, (float) z);
    }

    public void size(float pointSize) {
        curr.vertices[curr.vertexIdx + sizeOffset] = pointSize;
    }

}
