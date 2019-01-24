package gaia.cu9.ari.gaiaorbit.render.system;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.render.ILineRenderable;
import gaia.cu9.ari.gaiaorbit.render.IRenderable;
import gaia.cu9.ari.gaiaorbit.scenegraph.Particle;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode.RenderGroup;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import org.lwjgl.opengl.GL11;

import java.util.Comparator;

public class LineRenderSystem extends ImmediateRenderSystem {
    protected static final int MAX_VERTICES = 5000000;
    protected static final int INI_DPOOL_SIZE = 5000;
    protected static final int MAX_DPOOL_SIZE = 100000;
    protected ICamera camera;
    protected int glType;

    protected Vector3 aux2;

    public LineRenderSystem(RenderGroup rg, float[] alphas, ShaderProgram[] shaders) {
        super(rg, alphas, shaders, -1);
        glType = GL20.GL_LINE_STRIP;
        aux2 = new Vector3();
    }

    @Override
    protected void initShaderProgram() {
    }

    @Override
    protected void initVertices() {
        meshes = new MeshData[100000];
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

        ShaderProgram shaderProgram = getShaderProgram();

        shaderProgram.begin();
        shaderProgram.setUniformMatrix("u_projModelView", camera.getCamera().combined);

        // Relativistic effects
        addEffectsUniforms(shaderProgram, camera);

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

            curr.mesh.setVertices(curr.vertices, 0, curr.vertexIdx);
            curr.mesh.render(shaderProgram, glType);

            curr.clear();
        }
        shaderProgram.end();
    }

    /**
     * Breaks current line of points
     */
    public void breakLine(){

    }

    public void addPoint(ILineRenderable lr, double x, double y, double z, float r, float g, float b, float a) {
        color(r, g, b, a);
        vertex((float) x, (float) y, (float) z);
    }

    public void addLine(ILineRenderable lr, double x0, double y0, double z0, double x1, double y1, double z1, Color col) {
        addLinePostproc(x0, y0, z0, x1, y1, z1, col.r, col.g, col.b, col.a);
    }

    public void addLine(ILineRenderable lr, double x0, double y0, double z0, double x1, double y1, double z1, float r, float g, float b, float a) {
        addLinePostproc(x0, y0, z0, x1, y1, z1, r, g, b, a);
    }

    public void addLinePostproc(double x0, double y0, double z0, double x1, double y1, double z1, double r, double g, double b, double a) {
        color(r, g, b, a);
        vertex((float) x0, (float) y0, (float) z0);
        color(r, g, b, a);
        vertex((float) x1, (float) y1, (float) z1);
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

}
