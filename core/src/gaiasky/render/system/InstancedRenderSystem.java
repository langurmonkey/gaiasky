/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.system;

import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import gaiasky.event.IObserver;
import gaiasky.render.RenderGroup;
import gaiasky.render.api.IRenderable;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.ParticleSet;
import gaiasky.scene.entity.ParticleUtils;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.util.Bits;
import gaiasky.util.ModelCache;
import gaiasky.util.gdx.IcoSphereCreator;
import gaiasky.util.gdx.ModelCreator.IFace;
import gaiasky.util.gdx.mesh.IntMesh;
import gaiasky.util.gdx.model.IntModel;
import gaiasky.util.gdx.shader.ExtShaderProgram;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class InstancedRenderSystem extends ImmediateModeRenderSystem implements IObserver {

    /**
     * Instanced model list, one per particle group.
     */
    protected Array<InstancedModel> models = new Array<>(50);
    /** Particle utils instance. **/
    protected final ParticleUtils utils;

    /**
     * Holds temporary instanced model data.
     */
    public static class InstancedModel {
        // Attribute offsets.
        public int sizeOffset, particlePosOffset, properMotionOffset, textureIndexOffset, additionalOffset,
                nVariOffset, variMagsOffset, variTimesOffset, elems01Offset, elems02Offset;
        /** Number of vertices. **/
        public int numVertices;
        /** Vertex size in floats (4-bytes). **/
        public int modelVertexSize;
        /** Number of indices. **/
        public int numIndices = 0;
        public float[] vertices;
        public int[] indices;
        public float[] instanceAttributes;

        public InstancedModel(IntMesh mesh) {
            this(mesh.getNumVertices(), mesh.getVertexSize() / 4, mesh.getNumIndices());
            if (numVertices > 0)
                mesh.getVertices(vertices);
            if (numIndices > 0)
                mesh.getIndices(indices);
        }

        public InstancedModel(int numModelVertices,
                              int modelVertexSize,
                              int numIndices) {
            this.numVertices = numModelVertices;
            this.modelVertexSize = modelVertexSize;
            this.numIndices = numIndices;
            initializeLists();
        }

        public InstancedModel(int numModelVertices,
                              int modelVertexSize) {
            this.numVertices = numModelVertices;
            this.modelVertexSize = modelVertexSize;
            initializeLists();
        }

        public void ensureInstanceAttribsSize(int size) {
            if (instanceAttributes == null || instanceAttributes.length < size) {
                instanceAttributes = new float[size];
            }
        }

        protected void initializeLists() {
            int size = numVertices * modelVertexSize;
            if (vertices == null || vertices.length < size) {
                vertices = new float[size];
            }
            if (numIndices > 0 && (indices == null || indices.length < numIndices)) {
                indices = new int[numIndices];
            }
        }
    }

    protected InstancedRenderSystem(SceneRenderer sceneRenderer,
                                    RenderGroup rg,
                                    float[] alphas,
                                    ExtShaderProgram[] shaders) {
        super(sceneRenderer, rg, alphas, shaders);
        meshes = new Array<>(20);
        utils = new ParticleUtils();
    }

    @Override
    protected void initVertices() {
        // Empty.
    }

    public boolean isWireframe(int primitive) {
        return primitive >= GL30.GL_LINES && primitive <= GL30.GL_LINE_STRIP;
    }

    protected void setModel(int offset,
                            InstancedModel model) {
        if (offset >= 0) {
            models.insert(offset, model);
        }
    }

    protected InstancedModel getModel(ParticleSet set,
                                      int offset) {
        return getModel(set.model, set.modelType, set.modelParams, set.modelPrimitive, offset);
    }

    protected InstancedModel getModelQuad(int primitive,
                                          int offset) {
        return getModel(null, "quad", null, primitive, offset);
    }

    protected InstancedModel getModel(IntModel modelFile,
                                      String modelType,
                                      Map<String, Object> modelParams,
                                      int primitive,
                                      int offset) {
        if (offset >= 0 && models.size > offset && models.get(offset) != null) {
            return models.get(offset);
        }

        boolean wireframe = isWireframe(primitive);
        int primitivePlus = wireframe ? 0 : 2;
        InstancedModel model;

        // Perhaps model is already loaded?
        if (modelFile != null) {
            var mesh = modelFile.meshes.get(0);
            model = new InstancedModel(mesh);

        } else if (modelType.equalsIgnoreCase("sphere")) {
            // UV-sphere.
            final float width = 1f;
            final float height = 1f;
            final float depth = 1f;
            final int quality = 30;
            if (wireframe) {
                final float angleUFrom = 0f;
                final float angleUTo = 360f;
                final float angleVFrom = 0f;
                final float angleVTo = 180f;
                final float hw = width * 0.5f;
                final float hh = height * 0.5f;
                final float hd = depth * 0.5f;
                final float auo = MathUtils.degreesToRadians * angleUFrom;
                final float stepU = (MathUtils.degreesToRadians * (angleUTo - angleUFrom)) / quality;
                final float avo = MathUtils.degreesToRadians * angleVFrom;
                final float stepV = (MathUtils.degreesToRadians * (angleVTo - angleVFrom)) / quality;
                final float us = 1f / quality;
                final float vs = 1f / quality;
                float angleU;
                float angleV;
                model = new InstancedModel(quality * quality * 4, 3);

                int i = 0;
                // Horizontal.
                for (int iv = 0; iv < quality; iv++) {
                    angleV = avo + stepV * iv;
                    float t = MathUtils.sin(angleV);
                    float h = MathUtils.cos(angleV) * hh;
                    for (int iu = 0; iu < quality; iu++) {
                        // ORIGINAL
                        angleU = auo + stepU * iu;
                        // Position.
                        model.vertices[i++] = MathUtils.cos(angleU) * hw * t;
                        model.vertices[i++] = h;
                        model.vertices[i++] = MathUtils.sin(angleU) * hd * t;

                        // RIGHT
                        angleU = auo + stepU * (iu + 1);
                        // Position.
                        model.vertices[i++] = MathUtils.cos(angleU) * hw * t;
                        model.vertices[i++] = h;
                        model.vertices[i++] = MathUtils.sin(angleU) * hd * t;
                    }
                }

                // Vertical.
                for (int iv = 0; iv < quality; iv++) {
                    for (int iu = 0; iu < quality; iu++) {
                        // ORIGINAL
                        angleV = avo + stepV * iv;
                        float t = MathUtils.sin(angleV);
                        float h = MathUtils.cos(angleV) * hh;
                        angleU = auo + stepU * iu;
                        // Position.
                        model.vertices[i++] = MathUtils.cos(angleU) * hw * t;
                        model.vertices[i++] = h;
                        model.vertices[i++] = MathUtils.sin(angleU) * hd * t;

                        // RIGHT
                        angleV = avo + stepV * (iv + 1);
                        t = MathUtils.sin(angleV);
                        h = MathUtils.cos(angleV) * hh;
                        angleU = auo + stepU * (iu);
                        // Position.
                        model.vertices[i++] = MathUtils.cos(angleU) * hw * t;
                        model.vertices[i++] = h;
                        model.vertices[i++] = MathUtils.sin(angleU) * hd * t;
                    }
                }
            } else {
                if (modelParams == null) {
                    modelParams = new HashMap<>();
                    modelParams.put("diameter", (double) width);
                    modelParams.put("quality", (long) quality);
                }
                var modelPair = ModelCache.cache.getModel(modelType, modelParams, Bits.indices(Usage.Position, Usage.TextureCoordinates), primitive);
                var intModel = modelPair.getFirst();
                var mesh = intModel.meshes.get(0);
                model = new InstancedModel(mesh);
            }

        } else if (modelType.equalsIgnoreCase("icosphere")) {
            final long recursion = 3;
            final double diameter = 1.0;
            if (wireframe) {
                // ICO-sphere.
                var ico = new IcoSphereCreator();
                ico.create((float) (diameter / 2.0), (int) recursion);
                int nFaces = ico.faces.size();
                var v = ico.vertices;

                model = new InstancedModel(nFaces * 6, 3);

                int i = 0;
                for (IFace face : ico.faces) {
                    // Get vertices.
                    var v1 = v.get(face.v()[0] - 1);
                    var v2 = v.get(face.v()[1] - 1);
                    var v3 = v.get(face.v()[2] - 1);

                    // V1-V2
                    model.vertices[i++] = v1.x;
                    model.vertices[i++] = v1.y;
                    model.vertices[i++] = v1.z;
                    model.vertices[i++] = v2.x;
                    model.vertices[i++] = v2.y;
                    model.vertices[i++] = v2.z;

                    // V2-V3
                    model.vertices[i++] = v2.x;
                    model.vertices[i++] = v2.y;
                    model.vertices[i++] = v2.z;
                    model.vertices[i++] = v3.x;
                    model.vertices[i++] = v3.y;
                    model.vertices[i++] = v3.z;

                    // V3-V1
                    model.vertices[i++] = v3.x;
                    model.vertices[i++] = v3.y;
                    model.vertices[i++] = v3.z;
                    model.vertices[i++] = v1.x;
                    model.vertices[i++] = v1.y;
                    model.vertices[i++] = v1.z;
                }
            } else {
                if (modelParams == null) {
                    modelParams = new HashMap<>();
                    modelParams.put("recursion", recursion);
                    modelParams.put("diameter", diameter);
                }
                var modelPair = ModelCache.cache.getModel(modelType, modelParams, Bits.indices(Usage.Position, Usage.TextureCoordinates), primitive);
                var intModel = modelPair.getFirst();
                var mesh = intModel.meshes.get(0);
                model = new InstancedModel(mesh);
            }
        } else if (modelType.equalsIgnoreCase("quad")) {
            // Regular quad.
            // Construct custom billboard with two triangles.

            model = new InstancedModel(6, 2 + primitivePlus);
            int i = 0;
            // 0 (0)
            model.vertices[i++] = 1; // pos
            model.vertices[i++] = 1;
            if (!wireframe) {
                model.vertices[i++] = 1; // uv
                model.vertices[i++] = 1;
            }
            // 1 (1)
            model.vertices[i++] = 1; // pos
            model.vertices[i++] = -1;
            if (!wireframe) {
                model.vertices[i++] = 1; // uv
                model.vertices[i++] = 0;
            }
            // 2 (2)
            model.vertices[i++] = -1; // pos
            model.vertices[i++] = -1;
            if (!wireframe) {
                model.vertices[i++] = 0; // uv
                model.vertices[i++] = 0;
            }
            // 3 (2)
            model.vertices[i++] = -1; // pos
            model.vertices[i++] = -1;
            if (!wireframe) {
                model.vertices[i++] = 0; // uv
                model.vertices[i++] = 0;
            }
            // 4 (3)
            model.vertices[i++] = -1; // pos
            model.vertices[i++] = 1;
            if (!wireframe) {
                model.vertices[i++] = 0; // uv
                model.vertices[i++] = 1;
            }
            // 5 (0)
            model.vertices[i++] = 1; // pos
            model.vertices[i++] = 1;
            if (!wireframe) {
                model.vertices[i++] = 1; // uv
                model.vertices[i] = 1;
            }

        } else {
            // Load custom model.
            var modelPair = ModelCache.cache.getModel(modelType, modelParams, Bits.indices(Usage.Position, Usage.TextureCoordinates), primitive);
            var intModel = modelPair.getFirst();
            var mesh = intModel.meshes.get(0);

            model = new InstancedModel(mesh);
        }
        setModel(offset, model);
        return model;

    }

    /**
     * Adds the required vertex attributes for this renderer to the given list. These
     * attributes are added only once for all instances (divisor=0)
     *
     * @param attributes   The list of attributes with divisor=0
     * @param posArraySize Size of the position array (2 or 3).
     * @param useNormals   Add normal attributes as a vector-3.
     * @param useTexCoords Add texture coordinates attribute as a vector-2.
     */
    protected void addAttributesDivisor0(Array<VertexAttribute> attributes,
                                         int posArraySize,
                                         boolean useNormals,
                                         boolean useTexCoords) {
        // Vertex position and texture coordinates are global
        attributes.add(new VertexAttribute(Usage.Position, posArraySize, ExtShaderProgram.POSITION_ATTRIBUTE));
        if (useNormals) {
            attributes.add(new VertexAttribute(Usage.Normal, 3, ExtShaderProgram.NORMAL_ATTRIBUTE));
        }
        if (useTexCoords) {
            attributes.add(new VertexAttribute(Usage.TextureCoordinates, 2, ExtShaderProgram.TEXCOORD_ATTRIBUTE + "0"));
        }
    }

    /**
     * Adds the required vertex attributes for this renderer to the given list. These
     * attributes are added for every instance (divisor=1)
     *
     * @param attributes The list of attributes with divisor=1
     * @param primitive  The rendering primitive.
     */
    protected abstract void addAttributesDivisor1(Array<VertexAttribute> attributes,
                                                  int primitive);

    /**
     * Builds the vertex attributes with divisor=0 array and returns it.
     * The content of divisor-0 attributes is updated in each iteration of the vertex shader.
     *
     * @param posArraySize Size of the position array (2 or 3).
     * @param useNormals   Add normal attributes as a vector-3.
     * @param useTexCoords Add texture coordinates attribute as a vector-2.
     *
     * @return The vertex attributes array
     */
    protected VertexAttribute[] buildAttributesDivisor0(int posArraySize,
                                                        boolean useNormals,
                                                        boolean useTexCoords) {
        Array<VertexAttribute> attributes = new Array<>();
        addAttributesDivisor0(attributes, posArraySize, useNormals, useTexCoords);

        VertexAttribute[] array = new VertexAttribute[attributes.size];
        for (int i = 0; i < attributes.size; i++)
            array[i] = attributes.get(i);
        return array;
    }

    /**
     * Builds the vertex attributes with divisor=1 array and returns it
     * The content of divisor-1 attributes is updated for each instance.
     *
     * @param primitive The rendering primitive.
     *
     * @return The vertex attributes array.
     */
    protected VertexAttribute[] buildAttributesDivisor1(int primitive) {
        Array<VertexAttribute> attributes = new Array<>();
        addAttributesDivisor1(attributes, primitive);

        VertexAttribute[] array = new VertexAttribute[attributes.size];
        for (int i = 0; i < attributes.size; i++)
            array[i] = attributes.get(i);
        return array;
    }

    /**
     * Computes the offset for each vertex attribute. The offsets will be
     * used later in the render stage.
     *
     * @param curr  The current mesh data.
     * @param model The instanced model.
     */
    protected abstract void offsets0(MeshData curr,
                                     InstancedModel model);

    /**
     * Computes the offset for each vertex attribute in the instanced array. The offsets will be
     * used later in the render stage.
     *
     * @param curr  The current mesh data.
     * @param model The instanced model.
     */
    protected abstract void offsets1(MeshData curr,
                                     InstancedModel model);

    /**
     * Adds a new mesh data to the meshes list and increases the mesh data index.
     *
     * @param model        The instanced model instance.
     * @param maxVerts     The max number of vertices the divisor 0 mesh data can hold.
     * @param maxInstances The max number of vertices the divisor 1 mesh data can hold.
     * @param modelType    The model type string.
     * @param primitive    The rendering primitive.
     *
     * @return The index of the new mesh data.
     */
    protected int addMeshData(InstancedModel model,
                              int maxVerts,
                              int maxInstances,
                              int maxIndices,
                              String modelType,
                              int primitive) {
        return addMeshData(model, maxVerts, maxInstances, maxIndices, null, modelType, primitive);
    }

    /**
     * Adds a new mesh data to the meshes list and increases the mesh data index.
     *
     * @param model        The instanced model instance.
     * @param maxVerts     The max number of vertices the divisor 0 mesh data can hold.
     * @param maxInstances The max number of vertices the divisor 1 mesh data can hold.
     * @param modelFile    The model file, if any.
     * @param modelType    The model type string.
     * @param primitive    The rendering primitive.
     *
     * @return The index of the new mesh data.
     */
    protected int addMeshData(InstancedModel model,
                              int maxVerts,
                              int maxInstances,
                              int maxIndices,
                              String modelFile,
                              String modelType,
                              int primitive) {
        int mdi = createMeshData();
        curr = meshes.get(mdi);

        VertexAttribute[] attributes0 = buildAttributesDivisor0(modelFile == null && modelType.equalsIgnoreCase("quad") ? 2 : 3, modelFile != null,
                                                                !isWireframe(primitive));
        VertexAttribute[] attributes1 = buildAttributesDivisor1(primitive);
        curr.mesh = new IntMesh(true, maxVerts, maxInstances, maxIndices, attributes0, attributes1);

        curr.vertexSize = curr.mesh.getVertexAttributes().vertexSize / 4;
        curr.instanceSize = curr.mesh.getInstanceAttributes().vertexSize / 4;

        offsets0(curr, model);
        offsets1(curr, model);

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
        if (!renderables.isEmpty()) {
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
