/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.loaders.shared.geometry;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectMap.Entry;
import gaiasky.util.gdx.mesh.IntMesh;
import gaiasky.util.gdx.model.IntMeshPart;
import gaiasky.util.gdx.model.IntNode;
import gaiasky.util.gdx.model.IntNodePart;
import gaiasky.util.gdx.model.gltf.data.data.GLTFAccessor;
import gaiasky.util.gdx.model.gltf.data.data.GLTFBufferView;
import gaiasky.util.gdx.model.gltf.data.geometry.GLTFMesh;
import gaiasky.util.gdx.model.gltf.data.geometry.GLTFPrimitive;
import gaiasky.util.gdx.model.gltf.loaders.blender.BlenderShapeKeys;
import gaiasky.util.gdx.model.gltf.loaders.exceptions.GLTFIllegalException;
import gaiasky.util.gdx.model.gltf.loaders.exceptions.GLTFUnsupportedException;
import gaiasky.util.gdx.model.gltf.loaders.shared.GLTFTypes;
import gaiasky.util.gdx.model.gltf.loaders.shared.data.DataResolver;
import gaiasky.util.gdx.model.gltf.loaders.shared.material.MaterialLoader;
import gaiasky.util.gdx.model.gltf.scene3d.attributes.PBRVertexAttributes;
import gaiasky.util.gdx.model.gltf.scene3d.model.NodePartPlus;
import gaiasky.util.gdx.model.gltf.scene3d.model.NodePlus;
import gaiasky.util.gdx.model.gltf.scene3d.model.WeightVector;
import gaiasky.util.gdx.shader.Material;
import gaiasky.util.gdx.shader.attribute.TextureAttribute;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class MeshLoader {

    private final ObjectMap<GLTFMesh, Array<IntNodePart>> meshMap = new ObjectMap<>();
    private final Array<IntMesh> meshes = new Array<>();

    public void load(IntNode node, GLTFMesh glMesh, DataResolver dataResolver, MaterialLoader materialLoader) {
        ((NodePlus) node).morphTargetNames = BlenderShapeKeys.parse(glMesh);

        Array<IntNodePart> parts = meshMap.get(glMesh);
        if (parts == null) {
            parts = new Array<>();

            for (GLTFPrimitive primitive : glMesh.primitives) {

                final int glPrimitiveType = GLTFTypes.mapPrimitiveMode(primitive.mode);

                // material
                Material material;
                if (primitive.material != null) {
                    material = materialLoader.get(primitive.material);
                } else {
                    material = materialLoader.getDefaultMaterial();
                }

                // vertices
                Array<VertexAttribute> vertexAttributes = new Array<VertexAttribute>();
                Array<GLTFAccessor> glAccessors = new Array<GLTFAccessor>();

                Array<int[]> bonesIndices = new Array<int[]>();
                Array<float[]> bonesWeights = new Array<float[]>();

                boolean hasNormals = false;
                boolean hasTangent = false;

                for (Entry<String, Integer> attribute : primitive.attributes) {
                    String attributeName = attribute.key;
                    int accessorId = attribute.value;
                    GLTFAccessor accessor = dataResolver.getAccessor(accessorId);
                    boolean rawAttribute = true;

                    if (attributeName.equals("POSITION")) {
                        if (!(GLTFTypes.TYPE_VEC3.equals(accessor.type) && accessor.componentType == GLTFTypes.C_FLOAT))
                            throw new GLTFIllegalException("illegal position attribute format");
                        vertexAttributes.add(VertexAttribute.Position());
                    } else if (attributeName.equals("NORMAL")) {
                        if (!(GLTFTypes.TYPE_VEC3.equals(accessor.type) && accessor.componentType == GLTFTypes.C_FLOAT))
                            throw new GLTFIllegalException("illegal normal attribute format");
                        vertexAttributes.add(VertexAttribute.Normal());
                        hasNormals = true;
                    } else if (attributeName.equals("TANGENT")) {
                        if (!(GLTFTypes.TYPE_VEC4.equals(accessor.type) && accessor.componentType == GLTFTypes.C_FLOAT))
                            throw new GLTFIllegalException("illegal tangent attribute format");
                        vertexAttributes.add(new VertexAttribute(Usage.Tangent, 4, ShaderProgram.TANGENT_ATTRIBUTE));
                        hasTangent = true;
                    } else if (attributeName.startsWith("TEXCOORD_")) {
                        if (!GLTFTypes.TYPE_VEC2.equals(accessor.type))
                            throw new GLTFIllegalException("illegal texture coordinate attribute type : " + accessor.type);
                        if (accessor.componentType == GLTFTypes.C_UBYTE)
                            throw new GLTFUnsupportedException("unsigned byte texture coordinate attribute not supported");
                        if (accessor.componentType == GLTFTypes.C_USHORT)
                            throw new GLTFUnsupportedException("unsigned short texture coordinate attribute not supported");
                        if (accessor.componentType != GLTFTypes.C_FLOAT)
                            throw new GLTFIllegalException("illegal texture coordinate component type : " + accessor.componentType);
                        int unit = parseAttributeUnit(attributeName);
                        vertexAttributes.add(VertexAttribute.TexCoords(unit));
                    } else if (attributeName.startsWith("COLOR_")) {
                        int unit = parseAttributeUnit(attributeName);
                        String alias = unit > 0 ? ShaderProgram.COLOR_ATTRIBUTE + unit : ShaderProgram.COLOR_ATTRIBUTE;
                        if (GLTFTypes.TYPE_VEC4.equals(accessor.type)) {
                            if (GLTFTypes.C_FLOAT == accessor.componentType) {
                                vertexAttributes.add(new VertexAttribute(Usage.ColorUnpacked, 4, GL20.GL_FLOAT, false, alias));
                            } else if (GLTFTypes.C_USHORT == accessor.componentType) {
                                vertexAttributes.add(new VertexAttribute(Usage.ColorUnpacked, 4, GL20.GL_UNSIGNED_SHORT, true, alias));
                            } else if (GLTFTypes.C_UBYTE == accessor.componentType) {
                                vertexAttributes.add(new VertexAttribute(Usage.ColorUnpacked, 4, GL20.GL_UNSIGNED_BYTE, true, alias));
                            } else {
                                throw new GLTFIllegalException("illegal color attribute component type: " + accessor.type);
                            }
                        } else if (GLTFTypes.TYPE_VEC3.equals(accessor.type)) {
                            if (GLTFTypes.C_FLOAT == accessor.componentType) {
                                vertexAttributes.add(new VertexAttribute(Usage.ColorUnpacked, 3, GL20.GL_FLOAT, false, alias));
                            } else if (GLTFTypes.C_USHORT == accessor.componentType) {
                                throw new GLTFUnsupportedException("RGB unsigned short color attribute not supported");
                            } else if (GLTFTypes.C_UBYTE == accessor.componentType) {
                                throw new GLTFUnsupportedException("RGB unsigned byte color attribute not supported");
                            } else {
                                throw new GLTFIllegalException("illegal color attribute component type: " + accessor.type);
                            }
                        } else {
                            throw new GLTFIllegalException("illegal color attribute type: " + accessor.type);
                        }

                    } else if (attributeName.startsWith("WEIGHTS_")) {
                        rawAttribute = false;

                        if (!GLTFTypes.TYPE_VEC4.equals(accessor.type)) {
                            throw new GLTFIllegalException("illegal weight attribute type: " + accessor.type);
                        }

                        int unit = parseAttributeUnit(attributeName);
                        if (unit >= bonesWeights.size) bonesWeights.setSize(unit + 1);

                        if (accessor.componentType == GLTFTypes.C_FLOAT) {
                            bonesWeights.set(unit, dataResolver.readBufferFloat(accessorId));
                        } else if (accessor.componentType == GLTFTypes.C_USHORT) {
                            bonesWeights.set(unit, dataResolver.readBufferUShortAsFloat(accessorId));
                        } else if (accessor.componentType == GLTFTypes.C_UBYTE) {
                            bonesWeights.set(unit, dataResolver.readBufferUByteAsFloat(accessorId));
                        } else {
                            throw new GLTFIllegalException("illegal weight attribute type: " + accessor.componentType);
                        }
                    } else if (attributeName.startsWith("JOINTS_")) {
                        rawAttribute = false;

                        if (!GLTFTypes.TYPE_VEC4.equals(accessor.type)) {
                            throw new GLTFIllegalException("illegal joints attribute type: " + accessor.type);
                        }

                        int unit = parseAttributeUnit(attributeName);
                        if (unit >= bonesIndices.size) bonesIndices.setSize(unit + 1);

                        if (accessor.componentType == GLTFTypes.C_UBYTE) { // unsigned byte
                            bonesIndices.set(unit, dataResolver.readBufferUByte(accessorId));
                        } else if (accessor.componentType == GLTFTypes.C_USHORT) { // unsigned short
                            bonesIndices.set(unit, dataResolver.readBufferUShort(accessorId));
                        } else {
                            throw new GLTFIllegalException("illegal type for joints: " + accessor.componentType);
                        }
                    } else if (attributeName.startsWith("_")) {
                        Gdx.app.error("GLTF", "skip unsupported custom attribute: " + attributeName);
                    } else {
                        throw new GLTFIllegalException("illegal attribute type " + attributeName);
                    }

                    if (rawAttribute) {
                        glAccessors.add(accessor);
                    }
                }

                // morph targets
                if (primitive.targets != null) {
                    int morphTargetCount = primitive.targets.size;
                    ((NodePlus) node).weights = new WeightVector(morphTargetCount);

                    for (int t = 0; t < primitive.targets.size; t++) {
                        int unit = t;
                        for (Entry<String, Integer> attribute : primitive.targets.get(t)) {
                            String attributeName = attribute.key;
                            int accessorId = attribute.value.intValue();
                            GLTFAccessor accessor = dataResolver.getAccessor(accessorId);
                            glAccessors.add(accessor);

                            if (attributeName.equals("POSITION")) {
                                if (!(GLTFTypes.TYPE_VEC3.equals(accessor.type) && accessor.componentType == GLTFTypes.C_FLOAT))
                                    throw new GLTFIllegalException("illegal morph target position attribute format");
                                vertexAttributes.add(new VertexAttribute(PBRVertexAttributes.Usage.PositionTarget, 3, ShaderProgram.POSITION_ATTRIBUTE + unit, unit));
                            } else if (attributeName.equals("NORMAL")) {
                                if (!(GLTFTypes.TYPE_VEC3.equals(accessor.type) && accessor.componentType == GLTFTypes.C_FLOAT))
                                    throw new GLTFIllegalException("illegal morph target normal attribute format");
                                vertexAttributes.add(new VertexAttribute(PBRVertexAttributes.Usage.NormalTarget, 3, ShaderProgram.NORMAL_ATTRIBUTE + unit, unit));
                            } else if (attributeName.equals("TANGENT")) {
                                if (!(GLTFTypes.TYPE_VEC3.equals(accessor.type) && accessor.componentType == GLTFTypes.C_FLOAT))
                                    throw new GLTFIllegalException("illegal morph target tangent attribute format");
                                vertexAttributes.add(new VertexAttribute(PBRVertexAttributes.Usage.TangentTarget, 3, ShaderProgram.TANGENT_ATTRIBUTE + unit, unit));
                            } else {
                                throw new GLTFIllegalException("illegal morph target attribute type " + attributeName);
                            }
                        }
                    }

                }

                int bSize = bonesIndices.size * 4;

                Array<VertexAttribute> bonesAttributes = new Array<VertexAttribute>();
                for (int b = 0; b < bSize; b++) {
                    VertexAttribute boneAttribute = VertexAttribute.BoneWeight(b);
                    vertexAttributes.add(boneAttribute);
                    bonesAttributes.add(boneAttribute);
                }

                // add missing vertex attributes (normals and tangent)
                boolean computeNormals = false;
                boolean computeTangents = false;
                VertexAttribute normalMapUVs = null;
                if (glPrimitiveType == GL20.GL_TRIANGLES) {
                    if (!hasNormals) {
                        vertexAttributes.add(VertexAttribute.Normal());
                        glAccessors.add(null);
                        computeNormals = true;
                    }
                    if (!hasTangent) {
                        // tangent is only needed when normal map is used
                        TextureAttribute normalMap = material.get(TextureAttribute.class, TextureAttribute.Normal);
                        if (normalMap != null) {
                            vertexAttributes.add(new VertexAttribute(Usage.Tangent, 4, ShaderProgram.TANGENT_ATTRIBUTE));
                            glAccessors.add(null);
                            computeTangents = true;
                            for (VertexAttribute attribute : vertexAttributes) {
                                if (attribute.usage == Usage.TextureCoordinates && attribute.unit == normalMap.uvIndex) {
                                    normalMapUVs = attribute;
                                }
                            }
                            if (normalMapUVs == null) throw new GLTFIllegalException("UVs not found for normal map");
                        }
                    }
                }

                VertexAttributes attributesGroup = new VertexAttributes(vertexAttributes.toArray(VertexAttribute.class));

                int vertexFloats = attributesGroup.vertexSize / 4;

                int maxVertices = glAccessors.first().count;

                float[] vertices = new float[maxVertices * vertexFloats];

                for (int b = 0; b < bSize; b++) {
                    VertexAttribute boneAttribute = bonesAttributes.get(b);
                    for (int i = 0; i < maxVertices; i++) {
                        vertices[i * vertexFloats + boneAttribute.offset / 4] = bonesIndices.get(b / 4)[i * 4 + b % 4];
                        vertices[i * vertexFloats + boneAttribute.offset / 4 + 1] = bonesWeights.get(b / 4)[i * 4 + b % 4];
                    }
                }

                for (int i = 0; i < glAccessors.size; i++) {
                    GLTFAccessor glAccessor = glAccessors.get(i);
                    VertexAttribute attribute = vertexAttributes.get(i);


                    if (glAccessor == null) continue;

                    if (glAccessor.bufferView == null) {
                        throw new GLTFIllegalException("bufferView is null (mesh compression ?)");
                    }

                    GLTFBufferView glBufferView = dataResolver.getBufferView(glAccessor.bufferView);

                    FloatBuffer floatBuffer = dataResolver.getBufferFloat(glAccessor);

                    int attributeFloats = GLTFTypes.accessorStrideSize(glAccessor) / 4;

                    // buffer can be interleaved, so vertex stride may be different than vertex size
                    int floatStride = glBufferView.byteStride == null ? attributeFloats : glBufferView.byteStride / 4;

                    for (int j = 0; j < glAccessor.count; j++) {

                        floatBuffer.position(j * floatStride);

                        int vIndex = j * vertexFloats + attribute.offset / 4;

                        floatBuffer.get(vertices, vIndex, attributeFloats);
                    }
                }

                // indices
                if (primitive.indices != null) {

                    GLTFAccessor indicesAccessor = dataResolver.getAccessor(primitive.indices);

                    if (!indicesAccessor.type.equals(GLTFTypes.TYPE_SCALAR)) {
                        throw new GLTFIllegalException("indices accessor must be SCALAR but was " + indicesAccessor.type);
                    }

                    int maxIndices = indicesAccessor.count;

                    switch (indicesAccessor.componentType) {
                        case GLTFTypes.C_UINT -> {
                            int[] indices = new int[maxIndices];
                            dataResolver.getBufferInt(indicesAccessor).get(indices);
                            generateParts(node, parts, material, glMesh.name, vertices, maxVertices, indices, attributesGroup, glPrimitiveType, computeNormals, computeTangents, normalMapUVs);
                        }
                        case GLTFTypes.C_USHORT, GLTFTypes.C_SHORT -> {
                            short[] indicesAux = new short[maxIndices];
                            dataResolver.getBufferShort(indicesAccessor).get(indicesAux);
                            int[] indices = new int[indicesAux.length];
                            for (int i = 0; i < indicesAux.length; i++) {
                                indices[i] = indicesAux[i];
                            }
                            generateParts(node, parts, material, glMesh.name, vertices, maxVertices, indices, attributesGroup, glPrimitiveType, computeNormals, computeTangents, normalMapUVs);
                        }
                        case GLTFTypes.C_UBYTE -> {
                            int[] indices = new int[maxIndices];
                            ByteBuffer byteBuffer = dataResolver.getBufferByte(indicesAccessor);
                            for (int i = 0; i < maxIndices; i++) {
                                indices[i] = byteBuffer.get() & 0xFFFF;
                            }
                            generateParts(node, parts, material, glMesh.name, vertices, maxVertices, indices, attributesGroup, glPrimitiveType, computeNormals, computeTangents, normalMapUVs);
                        }
                        default ->
                                throw new GLTFIllegalException("illegal componentType " + indicesAccessor.componentType);
                    }
                } else {
                    // non indexed mesh
                    generateParts(node, parts, material, glMesh.name, vertices, maxVertices, null, attributesGroup, glPrimitiveType, computeNormals, computeTangents, normalMapUVs);
                }
            }
            meshMap.put(glMesh, parts);
        }
        node.parts.addAll(parts);
    }

    private void generateParts(IntNode node, Array<IntNodePart> parts, Material material, String id, float[] vertices, int vertexCount, int[] indices, VertexAttributes attributesGroup, int glPrimitiveType, boolean computeNormals, boolean computeTangents, VertexAttribute normalMapUVs) {

        // skip empty meshes
        if (vertices.length == 0 || (indices != null && indices.length == 0)) {
            return;
        }

        if (computeNormals || computeTangents) {
            if (computeNormals && computeTangents)
                Gdx.app.log("GLTF", "compute normals and tangents for primitive " + id);
            else if (computeTangents) Gdx.app.log("GLTF", "compute tangents for primitive " + id);
            else Gdx.app.log("GLTF", "compute normals for primitive " + id);
            MeshTangentSpaceGenerator.computeTangentSpace(vertices, indices, attributesGroup, computeNormals, computeTangents, normalMapUVs);
        }

        IntMesh mesh = new IntMesh(true, vertexCount, indices == null ? 0 : indices.length, attributesGroup);
        meshes.add(mesh);
        mesh.setVertices(vertices);

        if (indices != null) {
            mesh.setIndices(indices);
        }

        int len = indices == null ? vertexCount : indices.length;

        IntMeshPart meshPart = new IntMeshPart(id, mesh, 0, len, glPrimitiveType);


        NodePartPlus nodePart = new NodePartPlus();
        nodePart.morphTargets = ((NodePlus) node).weights;
        nodePart.meshPart = meshPart;
        nodePart.material = material;
        parts.add(nodePart);

    }

    private int parseAttributeUnit(String attributeName) {
        int lastUnderscoreIndex = attributeName.lastIndexOf('_');
        try {
            return Integer.parseInt(attributeName.substring(lastUnderscoreIndex + 1));
        } catch (NumberFormatException e) {
            throw new GLTFIllegalException("illegal attribute name " + attributeName);
        }
    }

    public Array<? extends IntMesh> getMeshes() {
        return meshes;
    }

}
