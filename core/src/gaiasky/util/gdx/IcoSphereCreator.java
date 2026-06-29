/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.IntArray;
import gaiasky.util.Logger;
import gaiasky.util.math.StdRandom;
import net.jafama.FastMath;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IcoSphereCreator extends ModelCreator {
    private static final Logger.Log logger = Logger.getLogger(IcoSphereCreator.class);

    private Map<Long, Integer> middlePointIndexCache;

    Vector3 aux1 = new Vector3(), aux2 = new Vector3();

    public IcoSphereCreator() {
        super();
        this.name = "Icosphere";
    }

    public static void main(String[] args) {
        boolean flipNormals = false;
        IcoSphereCreator isc = new IcoSphereCreator();
        int recursion = 6;
        isc.create(1, recursion, flipNormals, 0.005f);
        try {
            File file = File.createTempFile("icosphere_" + recursion + "_", ".obj");
            OutputStream os = new FileOutputStream(file);
            isc.dumpObj(os);
            os.flush();
            os.close();
            System.out.println("Vertices: " + isc.vertices.size());
            System.out.println("Normals: " + isc.normals.size());
            System.out.println("Faces: " + isc.faces.size());
            System.out.println("Model written in: " + file.getAbsolutePath());
        } catch (IOException e) {
            logger.error(e);
        }
    }

    /**
     * Adds a vertex and its UV mapping.
     *
     * @param p      The point.
     * @param radius The radius.
     *
     * @return Vertex index.
     */
    protected int vertex(Vector3 p,
                         float radius) {
        return vertex(p, radius, 0f);
    }

    /**
     * Adds a vertex and its UV mapping.
     *
     * @param p      The point.
     * @param radius The radius.
     * @param jitter Jitter to apply to the position.
     *
     * @return Vertex index.
     */
    protected int vertex(Vector3 p,
                         float radius,
                         float jitter) {
        addJitter(p, jitter);
        p.nor();

        addUV(p);
        // Vertex is p times the radius
        vertices.add(p.scl(radius + getJitter(jitter)));

        // Normal.
        var normal = p.cpy().nor();
        normals.add(normal);

        // Tangent.
        aux1.set(normal).crs(Vector3.Z);
        aux2.set(normal).crs(Vector3.Y);
        var tangent = aux2;
        if (aux1.len() > aux2.len()) {
            tangent = aux1;
        }
        tangent = tangent.cpy().nor();
        tangents.add(tangent);

        // Binormal.
        var binormal = aux1.set(normal).crs(tangent).cpy().nor();
        binormals.add(binormal);

        return index++;
    }

    /**
     * Implements the spherical UV mapping
     *
     * @param p The normalized point
     */
    protected void addUV(Vector3 p) {
        // UV
        float u = 0.5f + (float) (Math.atan2(p.z, p.y) / (Math.PI * 2.0));
        float v = 0.5f - (float) (Math.asin(p.x) / FastMath.PI);

        if (p.equals(new Vector3(1, 0, 0))) {
            u = 0.5f;
            v = 1f;
        }
        if (p.equals(new Vector3(-1, 0, 0))) {
            u = 0.5f;
            v = 0f;
        }

        uv.add(new Vector2(u, v));
    }

    private void addNormals() {
        for (IFace face : faces) {
            face.setNormals(face.v()[0], face.v()[1], face.v()[2]);
            face.setBinormals(face.v()[0], face.v()[1], face.v()[2]);
            face.setTangents(face.v()[0], face.v()[1], face.v()[2]);
        }
    }

    // return index of point in the middle of p1 and p2
    private int getMiddlePoint(int p1,
                               int p2,
                               float radius,
                               float jitter) {
        // first check if we have it already
        boolean firstIsSmaller = p1 < p2;
        long smallerIndex = firstIsSmaller ? p1 : p2;
        long greaterIndex = firstIsSmaller ? p2 : p1;
        Long key = (smallerIndex << 32) + greaterIndex;

        if (this.middlePointIndexCache.containsKey(key)) {
            return middlePointIndexCache.get(key);
        }

        // not in cache, calculate it
        Vector3 point1 = this.vertices.get(p1 - 1);
        Vector3 point2 = this.vertices.get(p2 - 1);
        Vector3 middle = new Vector3((point1.x + point2.x) / 2.0f, (point1.y + point2.y) / 2.0f, (point1.z + point2.z) / 2.0f);

        addJitter(middle, jitter);
        middle.nor();

        // add vertex makes sure point is on unit sphere
        int i = vertex(middle, radius + getJitter(jitter));

        // store it, return index
        this.middlePointIndexCache.put(key, i);
        return i;
    }

    private void addJitter(Vector3 v,
                           float jitter) {
        // Add jitter to x, y, and z.
        v.x = v.x + (float) StdRandom.gaussian(0.0, jitter);
        v.y = v.y + (float) StdRandom.gaussian(0.0, jitter);
        v.z = v.z + (float) StdRandom.gaussian(0.0, jitter);
    }

    private float getJitter(float jitter) {
        return (float) StdRandom.gaussian(0.0, jitter);
    }

    private IntArray detectWrappedUVCoordinates() {
        IntArray indices = new IntArray();
        for (int i = faces.size() - 1; i >= 0; i--) {
            IFace face = faces.get(i);

            Vector3 texA = new Vector3(uv.get(face.v()[0] - 1), 0);
            Vector3 texB = new Vector3(uv.get(face.v()[1] - 1), 0);
            Vector3 texC = new Vector3(uv.get(face.v()[2] - 1), 0);
            Vector3 a = texB.cpy().sub(texA);
            Vector3 b = texC.cpy().sub(texA);
            Vector3 texNormal = a.crs(b);
            if (texNormal.z < 0)
                indices.add(i);
        }
        return indices;
    }

    public IcoSphereCreator create(float radius,
                                   int recursionLevel) {
        return create(radius, recursionLevel, false, 0f);
    }

    public IcoSphereCreator create(float radius,
                                   int recursionLevel,
                                   float jitter) {
        return create(radius, recursionLevel, false, jitter);
    }

    /**
     * Creates an ico-sphere.
     *
     * @param radius      The radius of the sphere.
     * @param divisions   The number of divisions, it must be bigger than 0.
     * @param flipNormals Whether to flip normals or not.
     * @param jitter      Jitter to add to the vertex positions. In [0,1].
     *
     * @return This creator
     */
    public IcoSphereCreator create(float radius,
                                   int divisions,
                                   boolean flipNormals,
                                   float jitter) {
        return create(radius, divisions, flipNormals, false, jitter);
    }

    /**
     * Creates an ico-sphere.
     *
     * @param radius      The radius of the sphere.
     * @param divisions   The number of divisions, it must be bigger than 0.
     * @param flipNormals Whether to flip normals or not.
     * @param hardEdges   Whether to use smoothLighting (all vertices in a face have a
     *                    different normal) or not.
     *
     * @return This creator
     */
    public IcoSphereCreator create(float radius,
                                   int divisions,
                                   boolean flipNormals,
                                   boolean hardEdges,
                                   float jitter) {
        if (divisions < 1)
            throw new AssertionError("Recursion level must be greater than 0");
        this.flipNormals = flipNormals;
        this.hardEdges = hardEdges;
        this.middlePointIndexCache = new HashMap<>();

        // create 12 vertices of a icosahedron
        float t = (float) ((1.0 + FastMath.sqrt(5.0)) / 2.0);

        vertex(new Vector3(-1, t, 0), radius, jitter);
        vertex(new Vector3(1, t, 0), radius, jitter);
        vertex(new Vector3(-1, -t, 0), radius, jitter);
        vertex(new Vector3(1, -t, 0), radius, jitter);

        vertex(new Vector3(0, -1, t), radius, jitter);
        vertex(new Vector3(0, 1, t), radius, jitter);
        vertex(new Vector3(0, -1, -t), radius, jitter);
        vertex(new Vector3(0, 1, -t), radius, jitter);

        vertex(new Vector3(t, 0, -1), radius, jitter);
        vertex(new Vector3(t, 0, 1), radius, jitter);
        vertex(new Vector3(-t, 0, -1), radius, jitter);
        vertex(new Vector3(-t, 0, 1), radius, jitter);

        // create 20 triangles of the icosahedron
        List<IFace> faces = new ArrayList<>();

        // 5 faces around point 0
        addFace(faces, flipNormals, 1, 12, 6);
        addFace(faces, flipNormals, 1, 6, 2);
        addFace(faces, flipNormals, 1, 2, 8);
        addFace(faces, flipNormals, 1, 8, 11);
        addFace(faces, flipNormals, 1, 11, 12);

        // 5 adjacent faces
        addFace(faces, flipNormals, 2, 6, 10);
        addFace(faces, flipNormals, 6, 12, 5);
        addFace(faces, flipNormals, 12, 11, 3);
        addFace(faces, flipNormals, 11, 8, 7);
        addFace(faces, flipNormals, 8, 2, 9);

        // 5 faces around point 3
        addFace(faces, flipNormals, 4, 10, 5);
        addFace(faces, flipNormals, 4, 5, 3);
        addFace(faces, flipNormals, 4, 3, 7);
        addFace(faces, flipNormals, 4, 7, 9);
        addFace(faces, flipNormals, 4, 9, 10);

        // 5 adjacent faces
        addFace(faces, flipNormals, 5, 10, 6);
        addFace(faces, flipNormals, 3, 5, 12);
        addFace(faces, flipNormals, 7, 3, 11);
        addFace(faces, flipNormals, 9, 7, 8);
        addFace(faces, flipNormals, 10, 9, 2);

        // refine triangles
        for (int i = 1; i < divisions; i++) {
            List<IFace> faces2 = new ArrayList<>();
            for (IFace face : faces) {
                // replace triangle by 4 triangles
                int a = getMiddlePoint(face.v()[0], face.v()[1], radius, jitter);
                int b = getMiddlePoint(face.v()[1], face.v()[2], radius, jitter);
                int c = getMiddlePoint(face.v()[2], face.v()[0], radius, jitter);

                addFace(faces2, flipNormals, face.v()[0], a, c);
                addFace(faces2, flipNormals, face.v()[1], b, a);
                addFace(faces2, flipNormals, face.v()[2], c, b);
                addFace(faces2, flipNormals, a, b, c);
            }
            faces = faces2;
        }
        this.faces = faces;

        addNormals();

        // Repair seam
        //repairTextureWrapSeam();

        return this;
    }
}