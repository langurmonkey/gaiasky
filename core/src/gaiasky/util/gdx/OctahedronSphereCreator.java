/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import java.util.*;

public class OctahedronSphereCreator extends ModelCreator {

    private Map<Long, Integer> middlePointIndexCache;

    public OctahedronSphereCreator() {
        super();
        index = 0;
    }

    /**
     * Implements the spherical UV mapping
     */
    protected void addUV(Set<Integer> seam) {
        int idx = 0;
        for (Vector3 vertex : vertices) {
            Vector3 p = new Vector3(vertex);
            p.nor();
            // UV
            float u = 0.5f + (float) (Math.atan2(p.z, p.x) / (Math.PI * 2.0));
            float v = 0.5f - (float) (Math.asin(p.y) / Math.PI);

            if (seam.contains(idx + 1))
                v = 1f;

            uv.add(new Vector2(u, v));

            idx++;
        }
    }

    private void addNormals() {
        for (IFace face : faces) {
            // Calculate normals
            if (hardEdges) {
                // Calculate face normal, shared amongst all vertices
                Vector3 a = vertices.get(face.v()[1]).cpy().sub(vertices.get(face.v()[0]));
                Vector3 b = vertices.get(face.v()[2]).cpy().sub(vertices.get(face.v()[1]));
                normals.add(a.crs(b).nor());

                // Add index to face
                int idx = normals.size();
                face.setNormals(idx, idx, idx);

            } else {
                // Just add the vertex normal
                normals.add(vertices.get(face.v()[0]).cpy().nor());
                normals.add(vertices.get(face.v()[1]).cpy().nor());
                normals.add(vertices.get(face.v()[2]).cpy().nor());

                // Add indices to face
                int idx = normals.size();
                face.setNormals(idx - 3, idx - 2, idx - 1);
            }
        }
    }

    // return index of point in the middle of p1 and p2
    private int getMiddlePoint(int p1, int p2, float radius) {

        // first check if we have it already
        boolean firstIsSmaller = p1 < p2;
        long smallerIndex = firstIsSmaller ? p1 : p2;
        long greaterIndex = firstIsSmaller ? p2 : p1;
        long key = (smallerIndex << 32) + greaterIndex;

        if (this.middlePointIndexCache.containsKey(key)) {
            return middlePointIndexCache.get(key);
        }

        // not in cache, calculate it
        Vector3 point1 = this.vertices.get(p1);
        Vector3 point2 = this.vertices.get(p2);
        Vector3 middle = new Vector3((point1.x + point2.x) / 2.0f, (point1.y + point2.y) / 2.0f, (point1.z + point2.z) / 2.0f);

        middle.nor().scl(radius);
        // add vertex makes sure point is on unit sphere
        int i = addVertex(middle);

        // store it only if not in seam, return index
        this.middlePointIndexCache.put(key, i);
        return i;
    }

    public OctahedronSphereCreator create(float radius, int divisions, boolean flipNormals, boolean hardEdges) {
        if (divisions < 0 || divisions > 6) //-V6007
            throw new AssertionError("Divisions must be in [0..6]");
        this.flipNormals = flipNormals;
        this.hardEdges = hardEdges;
        this.middlePointIndexCache = new HashMap<>();

        // +y
        addVertex(new Vector3(0, radius, 0));
        // +z
        addVertex(new Vector3(0, 0, radius));
        // +x
        addVertex(new Vector3(radius, 0, 0));
        // -z
        addVertex(new Vector3(0, 0, -radius));
        // -x
        addVertex(new Vector3(-radius, 0, 0));
        // -y
        addVertex(new Vector3(0, -radius, 0));

        // Triangles
        int[] triangles = new int[] {
                0, 1, 2,
                0, 2, 3,
                0, 3, 4,
                0, 4, 1,
                5, 2, 1,
                5, 3, 2,
                5, 4, 3,
                5, 1, 4 };

        List<IFace> faces = new ArrayList<>();

        // Add faces
        for (int f = 0; f < 8; f++) {
            addFace(faces, flipNormals, triangles[f * 3], triangles[f * 3 + 1], triangles[f * 3 + 2]);
        }

        // refine triangles
        for (int division = 0; division < divisions; division++) {
            List<IFace> faces2 = new ArrayList<>();
            for (IFace face : faces) {
                // replace triangle by 4 triangles
                int f0 = face.v()[0];
                int f1 = face.v()[1];
                int f2 = face.v()[2];
                int a = getMiddlePoint(f0, f1, radius);
                int b = getMiddlePoint(f1, f2, radius);
                int c = getMiddlePoint(f2, f0, radius);

                addFace(faces2, flipNormals, face.v()[0], a, c);
                addFace(faces2, flipNormals, face.v()[1], b, a);
                addFace(faces2, flipNormals, face.v()[2], c, b);
                addFace(faces2, flipNormals, a, b, c);
            }
            faces = faces2;
        }
        this.faces = faces;

        addNormals();
        //addUV(seam);

        return this;
    }
}
