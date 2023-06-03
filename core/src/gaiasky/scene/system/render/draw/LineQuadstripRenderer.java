/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render.draw;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import gaiasky.render.RenderGroup;
import gaiasky.render.api.ILineRenderable;
import gaiasky.render.api.IRenderable;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.Render;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.gdx.mesh.IntMesh;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.Vector3d;
import net.jafama.FastMath;

import java.util.List;

public class LineQuadstripRenderer extends LinePrimitiveRenderer {
    protected static final Log logger = Logger.getLogger(LineQuadstripRenderer.class);

    protected static final int INI_DPOOL_SIZE = 1000;
    protected static final int MAX_DPOOL_SIZE = 10000;
    final static double baseWidthAngle = Math.toRadians(.13);
    final static double baseWidthAngleTan = Math.tan(baseWidthAngle);
    private final Pool<double[]> doublePool;
    private final Vector3d aux = new Vector3d();
    Vector3d line, camdir0, camdir1, camdir15, point, vec;
    private MeshDataExt currExt;
    private Array<double[]> provisionalLines;
    private boolean two = false;

    public LineQuadstripRenderer(SceneRenderer sceneRenderer, RenderGroup rg, float[] alphas, ExtShaderProgram[] shaders) {
        super(sceneRenderer, rg, alphas, shaders);
        doublePool = new DoubleArrayPool(INI_DPOOL_SIZE, MAX_DPOOL_SIZE, 12);
        provisionalLines = new Array<>();
        line = new Vector3d();
        camdir0 = new Vector3d();
        camdir1 = new Vector3d();
        camdir15 = new Vector3d();
        point = new Vector3d();
        vec = new Vector3d();
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
            currExt = new MeshDataExt();
            meshes.set(index, currExt);
            curr = currExt;

            curr.capacity = 10000;
            currExt.maxIndices = curr.capacity + curr.capacity / 2;

            VertexAttribute[] attributes = buildVertexAttributes();
            currExt.mesh = new IntMesh(false, curr.capacity, currExt.maxIndices, attributes);

            currExt.indices = new int[currExt.maxIndices];
            currExt.vertexSize = currExt.mesh.getVertexAttributes().vertexSize / 4;
            currExt.vertices = new float[curr.capacity * currExt.vertexSize];

            currExt.colorOffset = currExt.mesh.getVertexAttribute(Usage.ColorPacked) != null ? currExt.mesh.getVertexAttribute(Usage.ColorPacked).offset / 4 : 0;
            currExt.uvOffset = currExt.mesh.getVertexAttribute(Usage.TextureCoordinates) != null ? currExt.mesh.getVertexAttribute(Usage.TextureCoordinates).offset / 4 : 0;
        } else {
            currExt = (MeshDataExt) meshes.get(index);
            curr = currExt;
        }
    }

    protected VertexAttribute[] buildVertexAttributes() {
        Array<VertexAttribute> attributes = new Array<>();
        attributes.add(new VertexAttribute(Usage.Position, 3, ExtShaderProgram.POSITION_ATTRIBUTE));
        attributes.add(new VertexAttribute(Usage.ColorPacked, 4, ExtShaderProgram.COLOR_ATTRIBUTE));
        attributes.add(new VertexAttribute(Usage.TextureCoordinates, 2, "a_uv"));

        VertexAttribute[] array = new VertexAttribute[attributes.size];
        for (int i = 0; i < attributes.size; i++)
            array[i] = attributes.get(i);
        return array;
    }

    public void uv(float u, float v) {
        currExt.vertices[currExt.vertexIdx + currExt.uvOffset] = u;
        currExt.vertices[currExt.vertexIdx + currExt.uvOffset + 1] = v;
    }

    public void breakLine() {
        two = false;
    }

    public void addPoint(ILineRenderable lr, double x, double y, double z, float r, float g, float b, float a) {
        if (!two) {
            aux.set(x, y, z);
            two = true;
        } else {
            addLine(lr, aux.x, aux.y, aux.z, x, y, z, r, g, b, a);
            aux.set(x, y, z);
        }
    }

    @Override
    public void addLine(ILineRenderable lr, double x0, double y0, double z0, double x1, double y1, double z1, Color c) {
        addLine(lr, x0, y0, z0, x1, y1, z1, c.toFloatBits());
    }

    @Override
    public void addLine(ILineRenderable lr, double x0, double y0, double z0, double x1, double y1, double z1, Color c0, Color c1) {
        addLine(lr, x0, y0, z0, x1, y1, z1, c0.toFloatBits(), c1.toFloatBits());
    }

    public void addLine(ILineRenderable lr, double x0, double y0, double z0, double x1, double y1, double z1, float c) {
        addLineInternal(x0, y0, z0, x1, y1, z1, c, c, lr.getLineWidth() * baseWidthAngleTan * Settings.settings.scene.lineWidth);
    }

    public void addLine(ILineRenderable lr, double x0, double y0, double z0, double x1, double y1, double z1, float c0, float c1) {
        addLineInternal(x0, y0, z0, x1, y1, z1, c0, c1, lr.getLineWidth() * baseWidthAngleTan * Settings.settings.scene.lineWidth);
    }

    @Override
    public void addLine(ILineRenderable lr, double x0, double y0, double z0, double x1, double y1, double z1, float r, float g, float b, float a) {
        addLineInternal(x0, y0, z0, x1, y1, z1, Color.toFloatBits(r, g, b, a), Color.toFloatBits(r, g, b, a), lr.getLineWidth() * baseWidthAngleTan * Settings.settings.scene.lineWidth);
    }

    @Override
    public void addLine(ILineRenderable lr, double x0, double y0, double z0, double x1, double y1, double z1, float r0, float g0, float b0, float a0, float r1, float g1, float b1, float a1) {
        addLineInternal(x0, y0, z0, x1, y1, z1, Color.toFloatBits(r0, g0, b0, a0), Color.toFloatBits(r1, g1, b1, a1), lr.getLineWidth() * baseWidthAngleTan * Settings.settings.scene.lineWidth);
    }

    private void addLineInternal(double x0, double y0, double z0, double x1, double y1, double z1, float c0, float c1, double widthAngleTan) {
        addLineInternal(x0, y0, z0, x1, y1, z1, c0, c1, widthAngleTan, true);
    }

    Color col0 = new Color();
    Color col1 = new Color();
    Color col15 = new Color();

    private void addLineInternal(double x0, double y0, double z0, double x1, double y1, double z1, float c0, float c1, double widthAngleTan, boolean rec) {
        double distToSegment = MathUtilsDouble.distancePointSegment(x0, y0, z0, x1, y1, z1, 0, 0, 0);

        double dist0 = FastMath.sqrt(x0 * x0 + y0 * y0 + z0 * z0);
        double dist1 = FastMath.sqrt(x1 * x1 + y1 * y1 + z1 * z1);

        Vector3d p15 = auxd;

        if (rec && distToSegment < dist0 && distToSegment < dist1) {
            // Projection falls in line, split line.
            MathUtilsDouble.getClosestPoint2(x0, y0, z0, x1, y1, z1, 0, 0, 0, p15);
            double px = p15.x;
            double py = p15.y;
            double pz = p15.z;

            // Mean color.
            Color.abgr8888ToColor(col0, c0);
            Color.abgr8888ToColor(col1, c1);
            col15.set((col0.r + col1.r) / 2f, (col0.g + col1.g) / 2f, (col0.b + col1.b) / 2f, (col0.a + col1.a) / 2f);
            float c15 = col15.toFloatBits();

            addLineInternal(x0, y0, z0, px, py, pz, c0, c15, widthAngleTan, true);
            addLineInternal(px, py, pz, x1, y1, z1, c15, c1, widthAngleTan, true);
        } else {
            // Add line to list
            // x0 y0 z0 x1 y1 z1 r0 g0 b0 a0 r1 g1 b1 a1 dist0 dist1 distMean
            double[] l = doublePool.obtain();
            l[0] = x0;
            l[1] = y0;
            l[2] = z0;
            l[3] = x1;
            l[4] = y1;
            l[5] = z1;
            l[6] = c0;
            l[7] = c1;
            l[8] = dist0;
            l[9] = dist1;
            l[10] = widthAngleTan;
            l[11] = (dist0 + dist1) / 2.0;
            provisionalLines.add(l);
        }
    }

    public void addLinePostproc(double x0, double y0, double z0, double x1, double y1, double z1, float c0, float c1, double dist0, double dist1, double widthTan) {

        // Check if 4 more indices fit
        if (currExt.numVertices + 4 >= curr.capacity) {
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
        color(c0);
        uv(0, 0);
        vertex((float) point.x, (float) point.y, (float) point.z);

        // P2
        point.set(x0, y0, z0).sub(camdir0);
        color(c0);
        uv(0, 1);
        vertex((float) point.x, (float) point.y, (float) point.z);

        camdir1.setLength(width1);
        // P3
        point.set(x1, y1, z1).add(camdir1);
        color(c1);
        uv(1, 0);
        vertex((float) point.x, (float) point.y, (float) point.z);

        // P4
        point.set(x1, y1, z1).sub(camdir1);
        color(c1);
        uv(1, 1);
        vertex((float) point.x, (float) point.y, (float) point.z);

        // Add indexes
        index(currExt.numVertices - 4);
        index(currExt.numVertices - 2);
        index(currExt.numVertices - 3);

        index(currExt.numVertices - 2);
        index(currExt.numVertices - 1);
        index(currExt.numVertices - 3);

    }

    private void index(int idx) {
        currExt.indices[currExt.indexIdx] = idx;
        currExt.indexIdx++;
    }

    @Override
    public void renderStud(List<IRenderable> renderables, ICamera camera, double t) {
        this.camera = camera;

        // Reset.
        meshIdx = 1;
        currExt = (MeshDataExt) meshes.get(0);
        curr = currExt;

        renderables.forEach(r -> {
            Render render = (Render) r;
            view.setEntity(render.entity);
            view.render(this, camera, getAlpha(render));
        });

        // Sort phase.
        //provisionalLines.sort(sorter);
        for (double[] l : provisionalLines)
            addLinePostproc(l[0], l[1], l[2], l[3], l[4], l[5], (float) l[6], (float) l[7], l[8], l[9], l[10]);

        ExtShaderProgram shaderProgram = getShaderProgram();

        shaderProgram.begin();
        shaderProgram.setUniformMatrix("u_projView", camera.getCamera().combined);

        // Rel, grav, z-buffer
        addEffectsUniforms(shaderProgram, camera);

        for (int i = 0; i < meshIdx; i++) {
            MeshDataExt md = (MeshDataExt) meshes.get(i);
            md.mesh.setVertices(md.vertices, 0, md.vertexIdx);
            md.mesh.setIndices(md.indices, 0, md.indexIdx);
            md.mesh.render(shaderProgram, GL20.GL_TRIANGLES);

            md.clear();
        }

        shaderProgram.end();

        // Reset mesh index and current
        int n = provisionalLines.size;
        for (int i = 0; i < n; i++)
            doublePool.free(provisionalLines.get(i));

        provisionalLines.clear();
    }

    public void dispose() {
        super.dispose();
        currExt = null;
        provisionalLines.clear();
        provisionalLines = null;

    }

    private static class MeshDataExt extends MeshData {
        int uvOffset;
        int indexIdx;
        int maxIndices;
        int[] indices;

        public void clear() {
            super.clear();
            indexIdx = 0;
        }
    }

    /**
     * Pools arrays of double-precision floating point numbers.
     */
    protected static class DoubleArrayPool extends Pool<double[]> {

        private final int poolSize;

        public DoubleArrayPool(int initialCapacity, int max, int poolSize) {
            super(initialCapacity, max);
            this.poolSize = poolSize;
        }

        @Override
        protected double[] newObject() {
            return new double[poolSize];
        }

    }

}
