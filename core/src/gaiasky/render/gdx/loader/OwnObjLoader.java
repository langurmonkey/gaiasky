/*
 * Copyright (c) 2023-2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.loader;

import com.badlogic.gdx.Gdx;
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
import gaiasky.render.gdx.model.data.*;
import gaiasky.util.Logger;
import gaiasky.render.gdx.loader.is.InputStreamProvider;
import gaiasky.render.gdx.loader.is.RegularInputStreamProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

public class OwnObjLoader extends IntModelLoader<OwnObjLoader.ObjLoaderParameters> {
    private static final Logger.Log logger = Logger.getLogger(OwnObjLoader.class);

    public static boolean logWarning;
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

    @Override
    public IntModelData loadModelData(FileHandle file, ObjLoaderParameters parameters) {
        return loadModelData(file, parameters != null && parameters.flipV, true);
    }

    protected IntModelData loadModelData(FileHandle file, boolean flipV) {
        return loadModelData(file, flipV, false);
    }

    protected IntModelData loadModelData(FileHandle file, boolean flipV, boolean computeNormals) {
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
            logger.error(e);
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

        // Compute smooth vertex normals for groups that have faces but no
        // normals in the file, if requested.
        if (computeNormals)
            for (Group group : groups)
                if (!group.hasNorms && group.numFaces > 0) {
                    computeSmoothNormals(group);
                    // The mesh will contain normals, so the group must be
                    // marked accordingly for the vertex expansion below.
                    group.hasNorms = true;
                }

        // Get number of objects/groups remaining after removing empty ones
        int numGroups = groups.size;

        IntModelData data = new IntModelData();

        for (int g = 0; g < numGroups; g++) {
            Group group = groups.get(g);
            IntArray faces = group.faces;
            int numElements = faces.size;
            int numFaces = group.numFaces;
            boolean hasNorms = group.hasNorms;
            boolean hasUVs = group.hasUVs;
            // Normals computed in-loader (no vn entries in the file) are
            // indexed by vertex index, not by a normal index in the face.
            float[] computedNormals = group.computedNormals;

            float[] finalVerts = new float[(numFaces * 3) * (3 + (hasNorms ? 3 : 0) + (hasUVs ? 2 : 0))];

            for (int i = 0, vi = 0; i < numElements; ) {
                int vertIdx = faces.get(i++);
                int vertIndex = vertIdx * 3;
                finalVerts[vi++] = verts.get(vertIndex++);
                finalVerts[vi++] = verts.get(vertIndex++);
                finalVerts[vi++] = verts.get(vertIndex);
                if (hasNorms) {
                    if (computedNormals != null) {
                        // Smooth normals: use the vertex index directly.
                        int normIndex = vertIdx * 3;
                        finalVerts[vi++] = computedNormals[normIndex++];
                        finalVerts[vi++] = computedNormals[normIndex++];
                        finalVerts[vi++] = computedNormals[normIndex];
                    } else {
                        int normIndex = faces.get(i++) * 3;
                        finalVerts[vi++] = norms.get(normIndex++);
                        finalVerts[vi++] = norms.get(normIndex++);
                        finalVerts[vi++] = norms.get(normIndex);
                    }
                }
                if (hasUVs) {
                    int uvIndex = faces.get(i++) * 2;
                    finalVerts[vi++] = uvs.get(uvIndex++);
                    finalVerts[vi++] = uvs.get(uvIndex);
                }
            }

            int numIndices = numFaces * 3 >= Integer.MAX_VALUE ? 0 : numFaces * 3;
            int[] finalIndices = new int[numIndices];
            // If there are too many vertices in a mesh, we can't use indices.
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
            mesh.attributes = attributes.toArray(VertexAttribute[]::new);
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

    /**
     * Computes smooth (per-vertex, angle-accumulated) normals for a group
     * that contains faces but no normals in the OBJ file. The result is
     * stored in {@link Group#computedNormals}, indexed by vertex index
     * (3 floats per vertex), so it can be read during vertex expansion.
     * <p>
     * The face stride depends on the face layout produced during parsing:
     * each face corner consumes 1 index for the position, plus 1 more for
     * the normal (never present here) and/or the UV, if the group has UVs.
     */
    private void computeSmoothNormals(Group group) {
        final int numVerts = verts.size / 3;
        final float[] accumulated = new float[numVerts * 3];

        final IntArray faces = group.faces;
        final int stride = group.hasUVs ? 2 : 1;
        final int numFaces = group.numFaces;

        final Vector3 a = new Vector3();
        final Vector3 b = new Vector3();
        final Vector3 c = new Vector3();
        final Vector3 ab = new Vector3();
        final Vector3 ac = new Vector3();
        final Vector3 fn = new Vector3();

        for (int f = 0; f < numFaces; f++) {
            final int base = f * 3 * stride;

            final int ia = faces.get(base) * 3;
            final int ib = faces.get(base + stride) * 3;
            final int ic = faces.get(base + 2 * stride) * 3;

            a.set(verts.get(ia), verts.get(ia + 1), verts.get(ia + 2));
            b.set(verts.get(ib), verts.get(ib + 1), verts.get(ib + 2));
            c.set(verts.get(ic), verts.get(ic + 1), verts.get(ic + 2));

            // Face normal (not normalized — its length equals twice the
            // triangle area, which weights the contribution by area).
            ab.set(b).sub(a);
            ac.set(c).sub(a);
            fn.set(ab).crs(ac);

            // Skip degenerate faces.
            if (fn.isZero())
                continue;

            accumulated[ia] += fn.x;
            accumulated[ia + 1] += fn.y;
            accumulated[ia + 2] += fn.z;
            accumulated[ib] += fn.x;
            accumulated[ib + 1] += fn.y;
            accumulated[ib + 2] += fn.z;
            accumulated[ic] += fn.x;
            accumulated[ic + 1] += fn.y;
            accumulated[ic + 2] += fn.z;
        }

        // Normalize accumulated normals; fall back to the normalized
        // position for vertices that belong to degenerate faces only.
        for (int i = 0; i < numVerts; i++) {
            final int idx = i * 3;
            float x = accumulated[idx];
            float y = accumulated[idx + 1];
            float z = accumulated[idx + 2];
            final float len2 = x * x + y * y + z * z;
            if (len2 > 0f) {
                final float invLen = 1f / (float) Math.sqrt(len2);
                accumulated[idx] = x * invLen;
                accumulated[idx + 1] = y * invLen;
                accumulated[idx + 2] = z * invLen;
            } else {
                // Fallback: use the normalized vertex position.
                final float px = verts.get(idx);
                final float py = verts.get(idx + 1);
                final float pz = verts.get(idx + 2);
                final float plen2 = px * px + py * py + pz * pz;
                if (plen2 > 0f) {
                    final float invLen = 1f / (float) Math.sqrt(plen2);
                    accumulated[idx] = px * invLen;
                    accumulated[idx + 1] = py * invLen;
                    accumulated[idx + 2] = pz * invLen;
                } else {
                    accumulated[idx + 1] = 1f;
                }
            }
        }

        group.computedNormals = accumulated;
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
        int idx = Integer.parseInt(index);
        if (idx < 0)
            return size + idx;
        else
            return idx - 1;
    }

    public static class ObjLoaderParameters extends IntModelLoader.IntModelParameters {
        public boolean flipV;

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
        /** Smooth vertex normals computed in the loader (indexed by vertex, 3 floats per vertex). Null if not computed. */
        float[] computedNormals;

        Group(String name) {
            this.name = name;
            this.faces = new IntArray(200);
            this.numFaces = 0;
            this.materialName = "default";
        }
    }
}
