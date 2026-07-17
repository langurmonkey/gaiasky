/*
 * Copyright (c) 2023-2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.creators;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import gaiasky.util.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OctahedronSphereCreator extends ModelCreator {
    private static final Logger.Log logger = Logger.getLogger(OctahedronSphereCreator.class);

    private Map<Long, Integer> middlePointIndexCache;

    public OctahedronSphereCreator() {
        super();
        index = 0;
    }

    private int addVertexFull(Vector3 p, float radius) {
        p.nor().scl(radius);
        Vector3 normal = p.cpy().nor();

        // Spherical UV, based on the UNIT normal (not the radius-scaled position).
        float lon = (float) Math.atan2(normal.z, normal.x);
        if (lon < 0) lon += MathUtils.PI2;
        float colat = (float) Math.acos(MathUtils.clamp(normal.y, -1f, 1f));
        float u = 1f - lon / MathUtils.PI2;
        float v = colat / MathUtils.PI;
        uv.add(new Vector2(u, v));

        vertices.add(p);
        normals.add(normal);

        Vector3 up = new Vector3(0f, 1f, 0f);
        Vector3 tangent = new Vector3(up).crs(normal).nor();
        if (tangent.isZero(0.0001f)) tangent.set(1f, 0f, 0f);
        Vector3 binormal = new Vector3(tangent).crs(normal).nor();
        tangents.add(tangent);
        binormals.add(binormal);

        return index++;
    }

    /** Duplicates a vertex's geometry (position/normal/tangent/binormal) with a new UV. */
    private int addVertexUV(int sourceIndex, float u, float v) {
        vertices.add(vertices.get(sourceIndex));
        normals.add(normals.get(sourceIndex));
        tangents.add(tangents.get(sourceIndex));
        binormals.add(binormals.get(sourceIndex));
        uv.add(new Vector2(u, v));
        return index++;
    }

    /**
     * Post-process pass, mirroring {@link CubeSphereCreator}'s fixSeam: any
     * triangle whose vertices' u values wrap around the longitude seam gets
     * seam vertices duplicated with u shifted by +-1. Additionally, since the
     * octahedron sphere has real pole vertices shared by several faces (unlike
     * the cube sphere), each face's pole vertex gets its own duplicated UV
     * with u set to the average of the face's other two vertices, avoiding
     * texture pinching at the poles.
     */
    private void fixUVSeamsAndPoles() {
        for (IFace face : faces) {
            int[] v = face.v();
            float[] us = new float[3];
            float[] vs = new float[3];
            for (int k = 0; k < 3; k++) {
                us[k] = uv.get(v[k]).x;
                vs[k] = uv.get(v[k]).y;
            }

            // Fix poles first (v ~ 0 or v ~ 1): recompute u from the other two vertices.
            for (int k = 0; k < 3; k++) {
                if (vs[k] < 1e-4f || vs[k] > 1f - 1e-4f) {
                    int a = (k + 1) % 3;
                    int b = (k + 2) % 3;
                    float ua = us[a];
                    float ub = us[b];
                    if (Math.abs(ua - ub) > 0.5f) {
                        if (ua < ub) ua += 1f; else ub += 1f;
                    }
                    float avgU = (ua + ub) / 2f;
                    if (avgU >= 1f) avgU -= 1f;
                    us[k] = avgU;
                    v[k] = addVertexUV(v[k], avgU, vs[k]);
                }
            }

            // Fix longitude seam: reference against vertex 0.
            float uRef = us[0];
            for (int k = 1; k < 3; k++) {
                float diff = us[k] - uRef;
                if (diff > 0.5f) {
                    v[k] = addVertexUV(v[k], us[k] - 1f, vs[k]);
                } else if (diff < -0.5f) {
                    v[k] = addVertexUV(v[k], us[k] + 1f, vs[k]);
                }
            }
        }
    }

    private void addNormals() {
        for (IFace face : faces) {
            face.setNormals(face.v()[0], face.v()[1], face.v()[2]);
            face.setBinormals(face.v()[0], face.v()[1], face.v()[2]);
            face.setTangents(face.v()[0], face.v()[1], face.v()[2]);
        }
    }

    private int getMiddlePoint(int p1, int p2, float radius) {
        boolean firstIsSmaller = p1 < p2;
        long smallerIndex = firstIsSmaller ? p1 : p2;
        long greaterIndex = firstIsSmaller ? p2 : p1;
        long key = (smallerIndex << 32) + greaterIndex;

        if (this.middlePointIndexCache.containsKey(key)) {
            return middlePointIndexCache.get(key);
        }

        Vector3 point1 = this.vertices.get(p1);
        Vector3 point2 = this.vertices.get(p2);
        Vector3 middle = new Vector3((point1.x + point2.x) / 2.0f, (point1.y + point2.y) / 2.0f, (point1.z + point2.z) / 2.0f);

        int i = addVertexFull(middle, radius);

        this.middlePointIndexCache.put(key, i);
        return i;
    }

    public OctahedronSphereCreator create(float radius, int divisions, boolean flipNormals, boolean hardEdges) {
        if(divisions <= 0) {
            divisions = 1;
            logger.warn("Octahedron sphere divisions must be in [0,9]: " + divisions);
        }
        if (divisions > 9) {
            divisions = 9;
            logger.warn("Octahedron sphere divisions must be in [0,9]: " + divisions);
        }

        this.flipNormals = flipNormals;
        this.hardEdges = hardEdges;
        this.middlePointIndexCache = new HashMap<>();

        addVertexFull(new Vector3(0, 1, 0), radius);
        addVertexFull(new Vector3(0, 0, 1), radius);
        addVertexFull(new Vector3(1, 0, 0), radius);
        addVertexFull(new Vector3(0, 0, -1), radius);
        addVertexFull(new Vector3(-1, 0, 0), radius);
        addVertexFull(new Vector3(0, -1, 0), radius);

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

        for (int f = 0; f < 8; f++) {
            addFace(faces, flipNormals, triangles[f * 3], triangles[f * 3 + 1], triangles[f * 3 + 2]);
        }

        for (int division = 0; division < divisions; division++) {
            List<IFace> faces2 = new ArrayList<>();
            for (IFace face : faces) {
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

        fixUVSeamsAndPoles();
        addNormals();

        return this;
    }
}