/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.render.system;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.render.IPointRenderable;
import gaia.cu9.ari.gaiaorbit.render.IRenderable;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode.RenderGroup;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;

public class PointRenderSystem extends ImmediateRenderSystem {
    protected static final int N_MESHDATA = 100;
    protected ICamera camera;
    protected int glType;
    private int sizeOffset;

    protected Vector3 aux2;

    private Runnable[] depth, blend;

    public PointRenderSystem(RenderGroup rg, float[] alphas, ShaderProgram[] shaders) {
        super(rg, alphas, shaders, -1);
        glType = GL20.GL_POINTS;
        aux2 = new Vector3();
        depth = new Runnable[N_MESHDATA];
        blend = new Runnable[N_MESHDATA];
    }

    @Override
    protected void initShaderProgram() {
    }

    @Override
    protected void initVertices() {
        meshes = new Array<>();

        // ORIGINAL POINTS
        meshes.add(newMeshData());
        curr = meshes.get(0);

        meshIdx = 0;
    }

    private MeshData newMeshData() {

        MeshData md = new MeshData();

        VertexAttribute[] attribs = buildVertexAttributes();
        md.mesh = new Mesh(false, 10000, 0, attribs);

        md.vertices = new float[10000 * (md.mesh.getVertexAttributes().vertexSize / 4)];
        md.vertexSize = md.mesh.getVertexAttributes().vertexSize / 4;
        md.colorOffset = md.mesh.getVertexAttribute(Usage.ColorPacked) != null ? md.mesh.getVertexAttribute(Usage.ColorPacked).offset / 4 : 0;
        sizeOffset = md.mesh.getVertexAttribute(Usage.Generic).offset / 4;
        return md;

    }

    protected VertexAttribute[] buildVertexAttributes() {
        Array<VertexAttribute> attribs = new Array<>();
        attribs.add(new VertexAttribute(Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE));
        attribs.add(new VertexAttribute(Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE));
        attribs.add(new VertexAttribute(Usage.Generic, 1, "a_size"));

        VertexAttribute[] array = new VertexAttribute[attribs.size];
        for (int i = 0; i < attribs.size; i++)
            array[i] = attribs.get(i);
        return array;
    }

    @Override
    public void renderStud(Array<IRenderable> renderables, ICamera camera, double t) {
        this.camera = camera;
        int size = renderables.size;

        ShaderProgram shaderProgram = getShaderProgram();
        shaderProgram.begin();
        shaderProgram.setUniformMatrix("u_projModelView", camera.getCamera().combined);
        addEffectsUniforms(shaderProgram, camera);

        for (int i = 0; i < size; i++) {
            IPointRenderable renderable = (IPointRenderable) renderables.get(i);
            renderable.render(this, camera, getAlpha(renderable));

            renderable.blend();
            renderable.depth();

            curr.mesh.setVertices(curr.vertices, 0, curr.vertexIdx);
            curr.mesh.render(shaderProgram, glType);

            // Clear for next cycle
            curr.clear();
        }
        shaderProgram.end();

    }

    private void run(Runnable[] r, int i) {
        if (i >= 0 && i < r.length && r[i] != null)
            r[i].run();
    }

    public void addPoint(IPointRenderable pr, double x, double y, double z, float pointSize, Color col) {
        addPoint(pr, x, y, z, pointSize, col.r, col.g, col.b, col.a);
    }

    public void addPoint(IPointRenderable pr, double x, double y, double z, float pointSize, float r, float g, float b, float a) {
        color(r, g, b, a);
        size(pointSize);
        vertex((float) x, (float) y, (float) z);
    }

    public void size(float pointSize) {
        curr.vertices[curr.vertexIdx + sizeOffset] = pointSize;
    }

    public void addBlend(Runnable b) {
        blend[meshIdx] = b;
    }

    public void addDepth(Runnable d) {
        depth[meshIdx] = d;
    }
}
