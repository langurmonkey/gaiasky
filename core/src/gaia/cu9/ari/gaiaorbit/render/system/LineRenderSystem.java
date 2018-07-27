package gaia.cu9.ari.gaiaorbit.render.system;

import java.util.Comparator;

import org.lwjgl.opengl.GL11;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

import gaia.cu9.ari.gaiaorbit.render.ILineRenderable;
import gaia.cu9.ari.gaiaorbit.render.IRenderable;
import gaia.cu9.ari.gaiaorbit.scenegraph.Particle;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode.RenderGroup;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;

public class LineRenderSystem extends ImmediateRenderSystem {
    protected static final int MAX_VERTICES = 5000000;
    protected static final int INI_DPOOL_SIZE = 5000;
    protected static final int MAX_DPOOL_SIZE = 100000;
    protected ICamera camera;
    protected int glType;
    private Array<double[]> provisionalLines;
    private LineArraySorter sorter;
    private Pool<double[]> dpool;

    protected Vector3 aux2;

    protected MeshData curr_outline;

    public LineRenderSystem(RenderGroup rg, float[] alphas, ShaderProgram[] shaders) {
        super(rg, alphas, shaders);
        dpool = new DPool(INI_DPOOL_SIZE, MAX_DPOOL_SIZE, 11);
        provisionalLines = new Array<double[]>();
        sorter = new LineArraySorter(10);
        glType = GL20.GL_LINES;
        aux2 = new Vector3();
    }

    @Override
    protected void initShaderProgram() {
    }

    @Override
    protected void initVertices() {
        meshes = new MeshData[2];
        maxVertices = MAX_VERTICES;

        // ORIGINAL LINES
        curr = new MeshData();
        meshes[0] = curr;

        VertexAttribute[] attribs = buildVertexAttributes();
        curr.mesh = new Mesh(false, maxVertices, 0, attribs);

        curr.vertices = new float[maxVertices * (curr.mesh.getVertexAttributes().vertexSize / 4)];
        curr.vertexSize = curr.mesh.getVertexAttributes().vertexSize / 4;
        curr.colorOffset = curr.mesh.getVertexAttribute(Usage.ColorPacked) != null ? curr.mesh.getVertexAttribute(Usage.ColorPacked).offset / 4 : 0;
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
            ILineRenderable renderable = (ILineRenderable) renderables.get(i);
            // Regular
            Gdx.gl.glLineWidth(renderable.getLineWidth() * GlobalConf.SCALE_FACTOR);
            boolean rend = true;
            // TODO ugly hack
            if (renderable instanceof Particle && !GlobalConf.scene.PROPER_MOTION_VECTORS)
                rend = false;
            if (rend)
                renderable.render(this, camera, getAlpha(renderable));
        }

        // Sort phase
        provisionalLines.sort(sorter);
        for (double[] l : provisionalLines)
            addLinePostproc(l[0], l[1], l[2], l[3], l[4], l[5], l[6], l[7], l[8], l[9]);

        ShaderProgram shaderProgram = getShaderProgram();

        shaderProgram.begin();
        shaderProgram.setUniformMatrix("u_projModelView", camera.getCamera().combined);

        // Relativistic effects
        addEffectsUniforms(shaderProgram, camera);

        curr.mesh.setVertices(curr.vertices, 0, curr.vertexIdx);
        curr.mesh.render(shaderProgram, glType);

        shaderProgram.end();

        // CLEAR
        curr.clear();

        // Reset mesh index and current
        int n = provisionalLines.size;
        for (int i = 0; i < n; i++)
            dpool.free(provisionalLines.get(i));
        provisionalLines.clear();
    }

    public void addLine(ILineRenderable lr, double x0, double y0, double z0, double x1, double y1, double z1, Color col) {
        addLine(lr, x0, y0, z0, x1, y1, z1, col.r, col.g, col.b, col.a);
    }

    public void addLine(ILineRenderable lr, double x0, double y0, double z0, double x1, double y1, double z1, float r, float g, float b, float a) {
        double dist0 = Math.sqrt(x0 * x0 + y0 * y0 + z0 * z0);
        double dist1 = Math.sqrt(x1 * x1 + y1 * y1 + z1 * z1);
        double[] l = dpool.obtain();
        l[0] = x0;
        l[1] = y0;
        l[2] = z0;
        l[3] = x1;
        l[4] = y1;
        l[5] = z1;
        l[6] = r;
        l[7] = g;
        l[8] = b;
        l[9] = a;
        l[10] = (dist0 + dist1) / 2d;
        provisionalLines.add(l);
    }

    public void addLinePostproc(double x0, double y0, double z0, double x1, double y1, double z1, double r, double g, double b, double a) {
        color(r, g, b, a);
        vertex((float) x0, (float) y0, (float) z0);
        color(r, g, b, a);
        vertex((float) x1, (float) y1, (float) z1);
    }

    public void color_outline(float r, float g, float b, float a) {
        curr_outline.vertices[curr_outline.vertexIdx + curr_outline.colorOffset] = Color.toFloatBits(r, g, b, a);
    }

    public void vertex_outline(float x, float y, float z) {
        curr_outline.vertices[curr_outline.vertexIdx] = x;
        curr_outline.vertices[curr_outline.vertexIdx + 1] = y;
        curr_outline.vertices[curr_outline.vertexIdx + 2] = z;

        curr_outline.vertexIdx += curr_outline.vertexSize;
        curr_outline.numVertices++;
    }

    protected class LineArraySorter implements Comparator<double[]> {
        private int idx;

        public LineArraySorter(int idx) {
            this.idx = idx;
        }

        @Override
        public int compare(double[] o1, double[] o2) {
            double f = o1[idx] - o2[idx];
            if (f == 0)
                return 0;
            else if (f < 0)
                return 1;
            else
                return -1;
        }

    }

    protected class DPool extends Pool<double[]> {

        private int dsize;

        public DPool(int initialCapacity, int max, int dsize) {
            super(initialCapacity, max);
            this.dsize = dsize;
        }

        @Override
        protected double[] newObject() {
            return new double[dsize];
        }

    }

}
