/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.system;

import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import gaiasky.event.IObserver;
import gaiasky.render.RenderGroup;
import gaiasky.render.api.IRenderable;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.util.gdx.IcoSphereCreator;
import gaiasky.util.gdx.ModelCreator.IFace;
import gaiasky.util.gdx.mesh.IntMesh;
import gaiasky.util.gdx.shader.ExtShaderProgram;

import java.util.List;

public abstract class InstancedRenderSystem extends ImmediateModeRenderSystem implements IObserver {

    /**
     * Auxiliary array that holds vertices temporarily
     * This buffer will be used with divisor=1, so each instance
     * gets one
     **/
    protected float[] tempInstanceAttribs;

    /** Number of vertices in the model/mesh. **/
    protected int numModelVertices;
    /** Size of each vertex in the model/mesh. **/
    protected int modelVertexSize;

    /** Name of the model to use. **/
    protected final ModelType modelType;

    public enum ModelType {
        QUAD(RenderGroup.PARTICLE_GROUP_EXT),
        UVSPHERE(RenderGroup.PARTICLE_GROUP_EXT_SPHERE),
        ICOSPHERE(RenderGroup.PARTICLE_GROUP_EXT_SPHERE);

        public final RenderGroup rg;
        ModelType(RenderGroup rg) {
            this.rg = rg;
        }
        public boolean isQuad() {
            return this.equals(QUAD);
        }
        public boolean isUVSphere() {
            return this.equals(UVSPHERE);
        }
        public boolean isIcoSphere() {
            return this.equals(ICOSPHERE);
        }
    }

    public InstancedRenderSystem(SceneRenderer sceneRenderer,
                                 RenderGroup rg,
                                 float[] alphas,
                                 ExtShaderProgram[] shaders,
                                 ModelType modelType) {
        super(sceneRenderer, rg, alphas, shaders);
        this.modelType = modelType;
        initVerticesDelayed();
    }

    public InstancedRenderSystem(SceneRenderer sceneRenderer,
                                 RenderGroup rg,
                                 float[] alphas,
                                 ExtShaderProgram[] shaders) {
        this(sceneRenderer, rg, alphas, shaders, ModelType.QUAD);
    }

    protected void initVerticesDelayed() {
        meshes = new Array<>();
        initializeModel();
    }

    @Override
    protected void initVertices() {
        // Empty.
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
        if (modelType.isQuad()) {
            // Construct custom billboard with two triangles.

            // Model has 6 vertices, 3 for each triangle in the quad.
            numModelVertices = 6;
            // Each vertex has 4 values, xy and uv.
            modelVertexSize = 4;

            // Fill in divisor0 vertices (vertex position and UV)
            ensureTempVertsSize(numModelVertices * modelVertexSize);
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
        } else if (modelType.isUVSphere()) {
            final float width = 1f;
            final float height = 1f;
            final float depth = 1f;
            final int divisionsU = 30;
            final int divisionsV = 15;
            final float angleUFrom = 0f;
            final float angleUTo = 360f;
            final float angleVFrom = 0f;
            final float angleVTo = 180f;
            final float hw = width * 0.5f;
            final float hh = height * 0.5f;
            final float hd = depth * 0.5f;
            final float auo = MathUtils.degreesToRadians * angleUFrom;
            final float stepU = (MathUtils.degreesToRadians * (angleUTo - angleUFrom)) / divisionsU;
            final float avo = MathUtils.degreesToRadians * angleVFrom;
            final float stepV = (MathUtils.degreesToRadians * (angleVTo - angleVFrom)) / divisionsV;
            final float us = 1f / divisionsU;
            final float vs = 1f / divisionsV;
            float angleU;
            float angleV;
            numModelVertices = divisionsU * divisionsV * 4;
            modelVertexSize = 3;

            // Copy to temp verts.
            ensureTempVertsSize(numModelVertices * modelVertexSize);

            int i = 0;
            // Horizontal.
            for (int iv = 0; iv < divisionsV; iv++) {
                angleV = avo + stepV * iv;
                float t = MathUtils.sin(angleV);
                float h = MathUtils.cos(angleV) * hh;
                for (int iu = 0; iu < divisionsU; iu++) {
                    // ORIGINAL
                    angleU = auo + stepU * iu;
                    // Position.
                    tempVerts[i++] = MathUtils.cos(angleU) * hw * t;
                    tempVerts[i++] = h;
                    tempVerts[i++] = MathUtils.sin(angleU) * hd * t;

                    // RIGHT
                    angleU = auo + stepU * (iu + 1);
                    // Position.
                    tempVerts[i++] = MathUtils.cos(angleU) * hw * t;
                    tempVerts[i++] = h;
                    tempVerts[i++] = MathUtils.sin(angleU) * hd * t;
                }
            }

            // Vertical.
            for (int iv = 0; iv < divisionsV; iv++) {
                for (int iu = 0; iu < divisionsU; iu++) {
                    // ORIGINAL
                    angleV = avo + stepV * iv;
                    float t = MathUtils.sin(angleV);
                    float h = MathUtils.cos(angleV) * hh;
                    angleU = auo + stepU * iu;
                    // Position.
                    tempVerts[i++] = MathUtils.cos(angleU) * hw * t;
                    tempVerts[i++] = h;
                    tempVerts[i++] = MathUtils.sin(angleU) * hd * t;

                    // RIGHT
                    angleV = avo + stepV * (iv + 1);
                    t = MathUtils.sin(angleV);
                    h = MathUtils.cos(angleV) * hh;
                    angleU = auo + stepU * (iu);
                    // Position.
                    tempVerts[i++] = MathUtils.cos(angleU) * hw * t;
                    tempVerts[i++] = h;
                    tempVerts[i++] = MathUtils.sin(angleU) * hd * t;
                }
            }
        } else if(modelType.isIcoSphere()) {
            var ico = new IcoSphereCreator();
            ico.create(1, 3);
            int nFaces = ico.faces.size();
            var v = ico.vertices;

            numModelVertices = nFaces * 6;
            modelVertexSize = 3;

            // Copy to temp verts.
            ensureTempVertsSize(numModelVertices * modelVertexSize);

            int i = 0;
            for (IFace face : ico.faces) {
                // Get vertices.
                var v1 = v.get(face.v()[0] - 1);
                var v2 = v.get(face.v()[1] - 1);
                var v3 = v.get(face.v()[2] - 1);

                // V1-V2
                tempVerts[i++] = v1.x;
                tempVerts[i++] = v1.y;
                tempVerts[i++] = v1.z;
                tempVerts[i++] = v2.x;
                tempVerts[i++] = v2.y;
                tempVerts[i++] = v2.z;

                // V2-V3
                tempVerts[i++] = v2.x;
                tempVerts[i++] = v2.y;
                tempVerts[i++] = v2.z;
                tempVerts[i++] = v3.x;
                tempVerts[i++] = v3.y;
                tempVerts[i++] = v3.z;

                // V3-V1
                tempVerts[i++] = v3.x;
                tempVerts[i++] = v3.y;
                tempVerts[i++] = v3.z;
                tempVerts[i++] = v1.x;
                tempVerts[i++] = v1.y;
                tempVerts[i++] = v1.z;
            }
        }
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
     * Builds the vertex attributes with divisor=0 array and returns it.
     * The content of divisor-0 attributes is updated in each iteration of the vertex shader.
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
     * The content of divisor-1 attributes is updated for each instance.
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
    protected int addMeshData(int maxVerts0,
                              int maxVerts1) {
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

    protected void preRenderObjects(ExtShaderProgram shaderProgram,
                                    ICamera camera) {
        // Empty, override if needed
    }

    protected void renderObject(ExtShaderProgram shaderProgram,
                                IRenderable renderable) {
        // Empty, override if needed
    }

    protected void postRenderObjects(ExtShaderProgram shaderProgram,
                                     ICamera camera) {
        // Empty, override if needed
    }

    @Override
    public void renderStud(List<IRenderable> renderables,
                           ICamera camera,
                           double t) {
        if (renderables.size() > 0) {
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
