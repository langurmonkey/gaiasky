/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.system;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import gaiasky.render.RenderGroup;
import gaiasky.render.api.IRenderable;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.util.gdx.mesh.IntMesh;
import gaiasky.util.gdx.shader.ExtShaderProgram;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class ImmediateModeRenderSystem extends AbstractRenderSystem {

    // Offset and count per renderable, if needed
    private final Map<IRenderable, Integer> offsets;
    private final Map<IRenderable, Integer> counts;
    protected int meshIdx;
    protected Array<MeshData> meshes;
    protected MeshData curr;
    // Auxiliary array that holds vertices temporarily
    protected float[] tempVerts;
    // Auxiliary array that holds indices temporarily
    protected int[] tempIndices;
    // Renderables that are already in the GPU
    protected Set<IRenderable> inGpu;

    protected ImmediateModeRenderSystem(SceneRenderer sceneRenderer, RenderGroup rg, float[] alphas, ExtShaderProgram[] programs) {
        this(sceneRenderer, rg, alphas, programs, -1);
    }

    protected ImmediateModeRenderSystem(SceneRenderer sceneRenderer, RenderGroup rg, float[] alphas, ExtShaderProgram[] programs, int tempVertsSize) {
        super(sceneRenderer, rg, alphas, programs);
        initShaderProgram();
        initVertices();
        meshIdx = 0;
        if (tempVertsSize > 0)
            tempVerts = new float[tempVertsSize];
        inGpu = new HashSet<>();
        offsets = new HashMap<>();
        counts = new HashMap<>();
    }

    /**
     * Creates a new mesh data object and adds it to the first available index
     * in the meshes array
     *
     * @return The index of the new mesh data in meshes
     */
    protected int createMeshData() {
        // look for index
        int mdi;
        for (mdi = 0; mdi < meshes.size; mdi++) {
            if (meshes.get(mdi) == null) {
                break;
            }
        }

        if (mdi >= meshes.size) {
            meshes.setSize(mdi + 1);
        }

        meshes.set(mdi, new MeshData());
        return mdi;
    }

    public void clearMeshes() {
        if (meshes != null) {
            for (int i = 0; i < meshes.size; i++) {
                clearMeshData(i);
            }
            meshes.clear();
        }
    }

    /**
     * Clears the mesh data at the index i
     *
     * @param i The index of the mesh data to remove
     */
    public void clearMeshData(int i) {
        try {
            MeshData md = meshes.get(i);
            if (md != null && md.mesh != null) {
                md.dispose();
                meshes.set(i, null);
            }
        } catch (IndexOutOfBoundsException e) {
            // Nothing
        }
    }

    protected abstract void initVertices();

    protected boolean inGpu(IRenderable renderable) {
        return inGpu != null && inGpu.contains(renderable);
    }

    protected void setInGpu(IRenderable renderable, boolean state) {
        if (inGpu != null) {
            if (state) {
                inGpu.add(renderable);
            } else {
                inGpu.remove(renderable);
            }
        }
    }

    protected void setOffset(IRenderable renderable, int offset) {
        if (offsets != null) {
            if (offset >= 0) {
                offsets.put(renderable, offset);
            } else {
                offsets.remove(renderable);
            }
        }
    }

    protected int getOffset(IRenderable renderable) {
        if (offsets != null && offsets.containsKey(renderable)) {
            return offsets.get(renderable);
        }
        return -1;
    }

    protected void setCount(IRenderable renderable, int count) {
        if (counts != null) {
            counts.put(renderable, count);
        }
    }

    protected int getCount(IRenderable renderable) {
        if (counts != null && counts.containsKey(renderable)) {
            return counts.get(renderable);
        }
        return -1;
    }

    public void dispose() {
        super.dispose();

        inGpu.clear();
        offsets.clear();
        counts.clear();

        clearMeshes();
        tempVerts = null;
        curr = null;
    }

    /**
     * This function makes sure that the tempVerts array has at least
     * the given size. After calling this function, the elements of tempVerts
     * may have been cleared.
     *
     * @param size The size to ensure
     */
    protected void ensureTempVertsSize(int size) {
        if (tempVerts == null || tempVerts.length < size) {
            tempVerts = new float[size];
        }
    }

    /**
     * This function makes sure that the tempIndices array has at least
     * the given size. After calling this function, the elements of tempIndices
     * may have been cleared.
     *
     * @param size The size to ensure
     */
    protected void ensureTempIndicesSize(int size) {
        if (tempIndices == null || tempIndices.length < size) {
            tempIndices = new int[size];
        }
    }

    public void color(Color color) {
        curr.vertices[curr.vertexIdx + curr.colorOffset] = color.toFloatBits();
    }

    public void color(float r, float g, float b, float a) {
        curr.vertices[curr.vertexIdx + curr.colorOffset] = Color.toFloatBits(r, g, b, a);
    }

    public void color(double r, double g, double b, double a) {
        curr.vertices[curr.vertexIdx + curr.colorOffset] = Color.toFloatBits((float) r, (float) g, (float) b, (float) a);
    }

    public void color(float colorBits) {
        curr.vertices[curr.vertexIdx + curr.colorOffset] = colorBits;
    }

    public void vertex(float x, float y, float z) {
        curr.vertices[curr.vertexIdx] = x;
        curr.vertices[curr.vertexIdx + 1] = y;
        curr.vertices[curr.vertexIdx + 2] = z;

        curr.vertexIdx += curr.vertexSize;
        curr.numVertices++;
    }

    public static class MeshData {

        public IntMesh mesh;

        public int colorOffset;

        public int vertexIdx;
        // Size of each vertex in number of entries in array. Multiply by array type
        // size to get vertex size in bytes.
        public int vertexSize;

        // Vertex array, this usually is just a reference to an external temp array
        public float[] vertices;
        public int instanceIdx;
        public int instanceSize;
        public float[] instance;

        public int indexIdx;
        public int numVertices;
        public int capacity;
        protected int indexVert;
        protected int[] indices;

        public void clear() {
            instanceIdx = 0;
            vertexIdx = 0;
            indexIdx = 0;
            indexVert = 0;
            numVertices = 0;
        }

        public void dispose() {
            if (mesh != null)
                mesh.dispose();
            vertices = null;
            indices = null;
        }
    }

    protected static class OwnUsage {
        public static final int Size = 512;
        public static final int NumVariablePoints = 1024;
        public static final int VariableMagnitudes = 2048;
        public static final int VariableTimes = 4096;
        public static final int ObjectPosition = 8192;
        public static final int ProperMotion = 16384;
        public static final int Additional = 20000;
        public static final int OrbitElems1 = 21000;
        public static final int OrbitElems2 = 22000;
    }

}
