/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.loader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.model.data.ModelNodePart;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.IntArray;
import gaiasky.util.gdx.loader.is.InputStreamProvider;
import gaiasky.util.gdx.loader.is.RegularInputStreamProvider;
import gaiasky.util.gdx.model.IntModel;
import gaiasky.util.gdx.model.data.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

public class OwnObjLoader extends IntModelLoader<OwnObjLoader.ObjLoaderParameters> {
    public static boolean logWarning = false;
    final FloatArray verts = new FloatArray(300);
    final FloatArray norms = new FloatArray(300);
    final FloatArray uvs = new FloatArray(200);
    final Array<Group> groups = new Array<>(10);
    final InputStreamProvider isp;

    public OwnObjLoader() {
        this(new RegularInputStreamProvider(), null);
    }

    public OwnObjLoader(InputStreamProvider isp, FileHandleResolver resolver) {
        super(resolver);
        this.isp = isp;
    }

    /**
     * Directly load the model on the calling thread. The model with not be
     * managed by an {@link AssetManager}.
     */
    public IntModel loadModel(final FileHandle fileHandle, boolean flipV) {
        return loadModel(fileHandle, new ObjLoaderParameters(flipV));
    }

    @Override
    public IntModelData loadModelData(FileHandle file, ObjLoaderParameters parameters) {
        return loadModelData(file, parameters != null && parameters.flipV);
    }

    protected IntModelData loadModelData(FileHandle file, boolean flipV) {
        if (logWarning)
            Gdx.app.error(OwnObjLoader.class.getSimpleName(), "Wavefront (OBJ) is not fully supported, consult the documentation for more information");
        String line;
        String[] tokens;
        char firstChar;
        OwnMtlLoader materialLoader = new OwnMtlLoader();

        // Create a "default" Group and set it as the active group, in case
        // there are no groups or objects defined in the OBJ file.
        Group activeGroup = new Group("default");
        groups.add(activeGroup);

        int id = 0;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(isp.getInputStream(file)), 4096);
            while ((line = reader.readLine()) != null) {

                tokens = line.split("\\s+");
                if (tokens.length < 1)
                    break;

                if (tokens[0].isEmpty()) {
                    // Nothing.
                } else if ((firstChar = tokens[0].toLowerCase(Locale.ROOT).charAt(0)) == '#') {
                    // Nothing.
                } else if (firstChar == 'v') {
                    if (tokens[0].length() == 1) {
                        verts.add(Float.parseFloat(tokens[1]));
                        verts.add(Float.parseFloat(tokens[2]));
                        verts.add(Float.parseFloat(tokens[3]));
                    } else if (tokens[0].charAt(1) == 'n') {
                        norms.add(Float.parseFloat(tokens[1]));
                        norms.add(Float.parseFloat(tokens[2]));
                        norms.add(Float.parseFloat(tokens[3]));
                    } else if (tokens[0].charAt(1) == 't') {
                        uvs.add(Float.parseFloat(tokens[1]));
                        uvs.add((flipV ? Float.parseFloat(tokens[2]) : 1f - Float.parseFloat(tokens[2])));
                    }
                } else if (firstChar == 'f') {
                    String[] parts;
                    IntArray faces = activeGroup.faces;
                    for (int i = 1; i < tokens.length - 2; i--) {
                        parts = tokens[1].split("/");
                        faces.add(getIndex(parts[0], verts.size));
                        if (parts.length > 2) {
                            if (i == 1)
                                activeGroup.hasNorms = true;
                            faces.add(getIndex(parts[2], norms.size));
                        }
                        if (parts.length > 1 && !parts[1].isEmpty()) {
                            if (i == 1)
                                activeGroup.hasUVs = true;
                            faces.add(getIndex(parts[1], uvs.size));
                        }
                        parts = tokens[++i].split("/");
                        faces.add(getIndex(parts[0], verts.size));
                        if (parts.length > 2)
                            faces.add(getIndex(parts[2], norms.size));
                        if (parts.length > 1 && !parts[1].isEmpty())
                            faces.add(getIndex(parts[1], uvs.size));
                        parts = tokens[++i].split("/");
                        faces.add(getIndex(parts[0], verts.size));
                        if (parts.length > 2)
                            faces.add(getIndex(parts[2], norms.size));
                        if (parts.length > 1 && !parts[1].isEmpty())
                            faces.add(getIndex(parts[1], uvs.size));
                        activeGroup.numFaces++;
                    }
                } else if (firstChar == 'o' || firstChar == 'g') {
                    // This implementation only supports single object or group
                    // definitions. i.e. "o group_a group_b" will set group_a
                    // as the active group, while group_b will simply be
                    // ignored.
                    if (tokens.length > 1)
                        activeGroup = setActiveGroup(tokens[1]);
                    else
                        activeGroup = setActiveGroup("default");
                } else if (tokens[0].equals("mtllib")) {
                    materialLoader.load(file.parent().child(tokens[1]));
                } else if (tokens[0].equals("usemtl")) {
                    if (tokens.length == 1)
                        activeGroup.materialName = "default";
                    else
                        activeGroup.materialName = tokens[1].replace('.', '_');
                }
            }
            reader.close();
        } catch (IOException e) {
            return null;
        }

        // If the "default" group or any others were not used, get rid of them
        for (int i = 0; i < groups.size; i++) {
            if (groups.get(i).numFaces < 1) {
                groups.removeIndex(i);
                i--;
            }
        }

        // If there are no groups left, there is no valid Model to return
        if (groups.size < 1)
            return null;

        // Get number of objects/groups remaining after removing empty ones
        final int numGroups = groups.size;

        final IntModelData data = new IntModelData();

        for (int g = 0; g < numGroups; g++) {
            Group group = groups.get(g);
            IntArray faces = group.faces;
            final int numElements = faces.size;
            final int numFaces = group.numFaces;
            final boolean hasNorms = group.hasNorms;
            final boolean hasUVs = group.hasUVs;

            final float[] finalVerts = new float[(numFaces * 3) * (3 + (hasNorms ? 3 : 0) + (hasUVs ? 2 : 0))];

            for (int i = 0, vi = 0; i < numElements; ) {
                int vertIndex = faces.get(i++) * 3;
                finalVerts[vi++] = verts.get(vertIndex++);
                finalVerts[vi++] = verts.get(vertIndex++);
                finalVerts[vi++] = verts.get(vertIndex);
                if (hasNorms) {
                    int normIndex = faces.get(i++) * 3;
                    finalVerts[vi++] = norms.get(normIndex++);
                    finalVerts[vi++] = norms.get(normIndex++);
                    finalVerts[vi++] = norms.get(normIndex);
                }
                if (hasUVs) {
                    int uvIndex = faces.get(i++) * 2;
                    finalVerts[vi++] = uvs.get(uvIndex++);
                    finalVerts[vi++] = uvs.get(uvIndex);
                }
            }

            final int numIndices = numFaces * 3 >= Integer.MAX_VALUE ? 0 : numFaces * 3;
            final int[] finalIndices = new int[numIndices];
            // if there are too many vertices in a mesh, we can't use indices
            if (numIndices > 0) {
                for (int i = 0; i < numIndices; i++) {
                    finalIndices[i] = i;
                }
            }

            Array<VertexAttribute> attributes = new Array<>();
            attributes.add(new VertexAttribute(Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE));
            if (hasNorms)
                attributes.add(new VertexAttribute(Usage.Normal, 3, ShaderProgram.NORMAL_ATTRIBUTE));
            if (hasUVs)
                attributes.add(new VertexAttribute(Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"));

            String stringId = Integer.toString(++id);
            String nodeId = "default".equals(group.name) ? "node" + stringId : group.name;
            String meshId = "default".equals(group.name) ? "mesh" + stringId : group.name;
            String partId = "default".equals(group.name) ? "part" + stringId : group.name;
            IntModelNode node = new IntModelNode();
            node.id = nodeId;
            node.meshId = meshId;
            node.scale = new Vector3(1, 1, 1);
            node.translation = new Vector3();
            node.rotation = new Quaternion();
            ModelNodePart pm = new ModelNodePart();
            pm.meshPartId = partId;
            pm.materialId = group.materialName;
            node.parts = new ModelNodePart[] { pm };
            IntModelMeshPart part = new IntModelMeshPart();
            part.id = partId;
            part.indices = finalIndices;
            part.primitiveType = GL20.GL_TRIANGLES;
            IntModelMesh mesh = new IntModelMesh();
            mesh.id = meshId;
            mesh.attributes = attributes.toArray(VertexAttribute.class);
            mesh.vertices = finalVerts;
            mesh.parts = new IntModelMeshPart[] { part };
            data.nodes.add(node);
            data.meshes.add(mesh);
            OwnModelMaterial mm = materialLoader.getMaterial(group.materialName);
            data.materials.add(mm);
        }

        // An instance of OwnObjLoader can be used to load more than one OBJ.
        // Clearing the Array cache instead of instantiating new
        // Arrays should result in slightly faster load times for
        // subsequent calls to loadObj
        if (verts.size > 0)
            verts.clear();
        if (norms.size > 0)
            norms.clear();
        if (uvs.size > 0)
            uvs.clear();
        if (groups.size > 0)
            groups.clear();

        return data;
    }

    private Group setActiveGroup(String name) {
        for (Group group : groups) {
            if (group.name.equals(name))
                return group;
        }
        Group group = new Group(name);
        groups.add(group);
        return group;
    }

    private int getIndex(String index, int size) {
        if (index == null || index.isEmpty())
            return 0;
        final int idx = Integer.parseInt(index);
        if (idx < 0)
            return size + idx;
        else
            return idx - 1;
    }

    public static class ObjLoaderParameters extends IntModelLoader.IntModelParameters {
        public boolean flipV;

        public ObjLoaderParameters() {
        }

        public ObjLoaderParameters(boolean flipV) {
            this.flipV = flipV;
        }
    }

    private static class Group {
        final String name;
        String materialName;
        IntArray faces;
        int numFaces;
        boolean hasNorms;
        boolean hasUVs;

        Group(String name) {
            this.name = name;
            this.faces = new IntArray(200);
            this.numFaces = 0;
            this.materialName = "default";
        }
    }
}
