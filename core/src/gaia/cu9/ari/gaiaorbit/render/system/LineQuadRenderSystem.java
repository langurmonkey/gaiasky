package gaia.cu9.ari.gaiaorbit.render.system;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

import gaia.cu9.ari.gaiaorbit.render.ILineRenderable;
import gaia.cu9.ari.gaiaorbit.render.IRenderable;
import gaia.cu9.ari.gaiaorbit.scenegraph.Particle;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode.RenderGroup;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.math.MathUtilsd;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;

/**
 * Renders lines as Polyline Quadstrips (Polyboards).
 * Slower but higher quality.
 * @author tsagrista
 *
 */
public class LineQuadRenderSystem extends LineRenderSystem {
    private MeshDataExt currext;
    private Array<double[]> provisionalLines;
    private Array<Line> provLines;
    private LineArraySorter sorter;
    private Pool<double[]> dpool;

    private class MeshDataExt extends MeshData {
        int uvOffset;
        int indexIdx;
        int maxIndices;
        short[] indices;

        public void clear() {
            super.clear();
            indexIdx = 0;
        }
    }

    private class Line {
        public float r, g, b, a;
        public double widthAngleTan;
        public double[][] points;
        public double[] dists;

        public Line() {
            super();
        }

    }

    Vector3d line, camdir0, camdir1, camdir15, point, vec;
    final static double widthAngle = Math.toRadians(0.1);
    final static double widthAngleTan = Math.tan(widthAngle);
    public LineQuadRenderSystem(RenderGroup rg, float[] alphas, ShaderProgram[] shaders) {
        super(rg, alphas, shaders);
        dpool = new DPool(INI_DPOOL_SIZE, MAX_DPOOL_SIZE, 14);
        provisionalLines = new Array<double[]>();
        provLines = new Array<Line>();
        sorter = new LineArraySorter(12);
        glType = GL20.GL_TRIANGLES;
        line = new Vector3d();
        camdir0 = new Vector3d();
        camdir1 = new Vector3d();
        camdir15 = new Vector3d();
        point = new Vector3d();
        vec = new Vector3d();
    }

    @Override
    protected void initVertices() {
        meshes = new MeshDataExt[1000];
        initVertices(meshIdx++);
    }

    private void initVertices(int index) {
        if (meshes[index] == null) {
            currext = new MeshDataExt();
            meshes[index] = currext;
            curr = currext;

            maxVertices = MAX_VERTICES;
            currext.maxIndices = maxVertices + maxVertices / 2;

            VertexAttribute[] attribs = buildVertexAttributes();
            currext.mesh = new Mesh(false, maxVertices, currext.maxIndices, attribs);

            currext.indices = new short[currext.maxIndices];
            currext.vertexSize = currext.mesh.getVertexAttributes().vertexSize / 4;
            currext.vertices = new float[maxVertices * currext.vertexSize];

            currext.colorOffset = currext.mesh.getVertexAttribute(Usage.ColorPacked) != null ? currext.mesh.getVertexAttribute(Usage.ColorPacked).offset / 4 : 0;
            currext.uvOffset = currext.mesh.getVertexAttribute(Usage.TextureCoordinates) != null ? currext.mesh.getVertexAttribute(Usage.TextureCoordinates).offset / 4 : 0;
        } else {
            currext = (MeshDataExt) meshes[index];
            curr = currext;
        }
    }

    protected VertexAttribute[] buildVertexAttributes() {
        Array<VertexAttribute> attribs = new Array<VertexAttribute>();
        attribs.add(new VertexAttribute(Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE));
        attribs.add(new VertexAttribute(Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE));
        attribs.add(new VertexAttribute(Usage.TextureCoordinates, 2, "a_uv"));

        VertexAttribute[] array = new VertexAttribute[attribs.size];
        for (int i = 0; i < attribs.size; i++)
            array[i] = attribs.get(i);
        return array;
    }

    public void uv(float u, float v) {
        currext.vertices[currext.vertexIdx + currext.uvOffset] = u;
        currext.vertices[currext.vertexIdx + currext.uvOffset + 1] = v;
    }

    @Override
    public void addLine(ILineRenderable lr, double x0, double y0, double z0, double x1, double y1, double z1, Color c) {
        addLine(lr, x0, y0, z0, x1, y1, z1, c.r, c.g, c.b, c.a);
    }

    @Override
    public void addLine(ILineRenderable lr, double x0, double y0, double z0, double x1, double y1, double z1, float r, float g, float b, float a) {
        addLineInternal(x0, y0, z0, x1, y1, z1, r, g, b, a, lr.getLineWidth() * widthAngleTan);
    }

    private void addLineInternal(double x0, double y0, double z0, double x1, double y1, double z1, float r, float g, float b, float a, double widthAngleTan) {
        addLineInternal(x0, y0, z0, x1, y1, z1, r, g, b, a, widthAngleTan, true);
    }

    private void addLineInternal(double x0, double y0, double z0, double x1, double y1, double z1, float r, float g, float b, float a, double widthAngleTan, boolean rec) {
        double distToSegment = MathUtilsd.distancePointSegment(x0, y0, z0, x1, y1, z1, 0, 0, 0);

        double dist0 = Math.sqrt(x0 * x0 + y0 * y0 + z0 * z0);
        double dist1 = Math.sqrt(x1 * x1 + y1 * y1 + z1 * z1);

        Vector3d p15 = null;

        if (rec && distToSegment < dist0 && distToSegment < dist1) {
            // Projection falls in line, split line
            p15 = MathUtilsd.getClosestPoint2(x0, y0, z0, x1, y1, z1, 0, 0, 0);

            addLineInternal(x0, y0, z0, p15.x, p15.y, p15.z, r, g, b, a, widthAngleTan, true);
            addLineInternal(p15.x, p15.y, p15.z, x1, y1, z1, r, g, b, a, widthAngleTan, true);
        } else {
            // Add line to list
            // x0 y0 z0 x1 y1 z1 r g b a dist0 dist1 distMean
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
            l[10] = dist0;
            l[11] = dist1;
            l[12] = (dist0 + dist1) / 2d;
            l[13] = widthAngleTan;
            provisionalLines.add(l);
        }
    }

    public void addLinePostproc(Line l) {
        int npoints = l.points.length;
        // Check if npoints more indices fit
        if (currext.numVertices + npoints > shortLimit)
            initVertices(meshIdx++);

        for (int i = 1; i < npoints; i++) {
            if (i == 1) {
                // Line from 0 to 1
                line.set(l.points[1][0] - l.points[0][0], l.points[1][1] - l.points[0][1], l.points[1][2] - l.points[0][2]);
            } else if (i == npoints - 1) {
                // Line from npoints-1 to npoints
                line.set(l.points[npoints - 1][0] - l.points[npoints - 2][0], l.points[npoints - 1][1] - l.points[npoints - 2][1], l.points[npoints - 1][2] - l.points[npoints - 2][2]);
            } else {
                // Line from i-1 to i+1
                line.set(l.points[i + 1][0] - l.points[i - 1][0], l.points[i + 1][1] - l.points[i - 1][1], l.points[i + 1][2] - l.points[i - 1][2]);
            }
            camdir0.set(l.points[i]);
            camdir0.crs(line);
            camdir0.setLength(l.widthAngleTan * l.dists[i] * camera.getFovFactor());

            // P1
            point.set(l.points[i]).add(camdir0);
            color(l.r, l.g, l.b, l.a);
            uv(i / (npoints - 1), 0);
            vertex((float) point.x, (float) point.y, (float) point.z);

            // P2
            point.set(l.points[i]).sub(camdir0);
            color(l.r, l.g, l.b, l.a);
            uv(i / (npoints - 1), 1);
            vertex((float) point.x, (float) point.y, (float) point.z);

            // Indices
            if (i > 1) {
                index((short) (currext.numVertices - 4));
                index((short) (currext.numVertices - 2));
                index((short) (currext.numVertices - 3));

                index((short) (currext.numVertices - 2));
                index((short) (currext.numVertices - 1));
                index((short) (currext.numVertices - 3));
            }

        }
    }

    public void addLinePostproc(double x0, double y0, double z0, double x1, double y1, double z1, double r, double g, double b, double a, double dist0, double dist1, double widthTan) {

        // Check if 3 more indices fit
        if (currext.numVertices + 3 >= shortLimit) {
            // We need to open a new MeshDataExt!
            initVertices(meshIdx++);
        }

        // Projection falls outside line
        double width0 = widthTan * dist0 * camera.getFovFactor();
        double width1 = widthTan * dist1 * camera.getFovFactor();

        line.set(x1 - x0, y1 - y0, z1 - z0);

        camdir0.set(x0, y0, z0);
        camdir1.set(x1, y1, z1);

        // Camdir0 and 1 will contain the perpendicular to camdir and line
        camdir0.crs(line);
        camdir1.crs(line);

        camdir0.setLength(width0);
        // P1
        point.set(x0, y0, z0).add(camdir0);
        color(r, g, b, a);
        uv(0, 0);
        vertex((float) point.x, (float) point.y, (float) point.z);

        // P2
        point.set(x0, y0, z0).sub(camdir0);
        color(r, g, b, a);
        uv(0, 1);
        vertex((float) point.x, (float) point.y, (float) point.z);

        camdir1.setLength(width1);
        // P3
        point.set(x1, y1, z1).add(camdir1);
        color(r, g, b, a);
        uv(1, 0);
        vertex((float) point.x, (float) point.y, (float) point.z);

        // P4
        point.set(x1, y1, z1).sub(camdir1);
        color(r, g, b, a);
        uv(1, 1);
        vertex((float) point.x, (float) point.y, (float) point.z);

        // Add indexes
        index((short) (currext.numVertices - 4));
        index((short) (currext.numVertices - 2));
        index((short) (currext.numVertices - 3));

        index((short) (currext.numVertices - 2));
        index((short) (currext.numVertices - 1));
        index((short) (currext.numVertices - 3));

    }

    private void index(short idx) {
        currext.indices[currext.indexIdx] = idx;
        currext.indexIdx++;
    }

    @Override
    public void renderStud(Array<IRenderable> renderables, ICamera camera, double t) {
        this.camera = camera;

        int size = renderables.size;
        for (int i = 0; i < size; i++) {
            ILineRenderable renderable = (ILineRenderable) renderables.get(i);
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
            addLinePostproc(l[0], l[1], l[2], l[3], l[4], l[5], l[6], l[7], l[8], l[9], l[10], l[11], l[13]);

        for (Line l : provLines)
            addLinePostproc(l);

        ShaderProgram shaderProgram = getShaderProgram();

        shaderProgram.begin();
        shaderProgram.setUniformMatrix("u_projModelView", camera.getCamera().combined);

        // Relativistic effects
        addEffectsUniforms(shaderProgram, camera);

        for (int i = 0; i < meshIdx; i++) {
            MeshDataExt md = (MeshDataExt) meshes[i];
            md.mesh.setVertices(md.vertices, 0, md.vertexIdx);
            md.mesh.setIndices(md.indices, 0, md.indexIdx);
            md.mesh.render(shaderProgram, glType);

            md.clear();
        }

        shaderProgram.end();

        // Reset mesh index and current
        meshIdx = 1;
        currext = (MeshDataExt) meshes[0];
        curr = currext;
        int n = provisionalLines.size;
        for (int i = 0; i < n; i++)
            dpool.free(provisionalLines.get(i));
        provisionalLines.clear();

        // Reset mesh index, current and lines
        meshIdx = 1;
        currext = (MeshDataExt) meshes[0];
        curr = currext;
        n = provLines.size;
        //for (int i = 0; i < n; i++)
        //    lpool.free(provLines.get(i));
        provLines.clear();
    }

}
