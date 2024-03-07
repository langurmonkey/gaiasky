/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public abstract class ModelCreator {
    public String name;
    public List<Vector3> vertices;
    public List<Vector3> normals;
    public List<Vector3> binormals;
    public List<Vector3> tangents;
    public List<Vector2> uv;
    public List<IFace> faces;
    protected int index;
    protected boolean flipNormals;
    protected boolean hardEdges;
    protected ModelCreator() {
        this.vertices = new ArrayList<>();
        this.normals = new ArrayList<>();
        this.binormals = new ArrayList<>();
        this.tangents = new ArrayList<>();
        this.uv = new ArrayList<>();
        this.faces = new ArrayList<>();
        this.index = 1;
    }

    protected void addFace(List<IFace> faces, boolean flipNormals, int... v) {
        if (flipNormals) {
            faces.add(new Face(flip(v, 1)));
        } else {
            faces.add(new Face(v));
        }
    }

    protected int[] flip(int[] v, int startIndex) {
        for (int i = startIndex; i < v.length / 2; i++) {
            int temp = v[i];
            v[i] = v[v.length - i + startIndex - 1];
            v[v.length - i + startIndex - 1] = temp;
        }
        return v;
    }

    /**
     * Adds a vertex.
     *
     * @param p The point.
     *
     * @return The index of this vertex.
     */
    protected int addVertex(Vector3 p) {
        vertices.add(p);
        return index++;
    }

    /**
     * Exports the model to the .obj (Wavefront) format in the given output stream.
     *
     * @param os The output stream.
     */
    public void dumpObj(OutputStream os) throws IOException {
        DecimalFormat nf = new DecimalFormat("########0.000000");
        OutputStreamWriter osw = new OutputStreamWriter(os);
        osw.append("# Created by ").append(this.getClass().getSimpleName()).append(" - ARI - ZAH - Heidelberg Universitat\n");
        osw.append("o ").append(name).append("\n");
        // Write vertices
        for (Vector3 vertex : vertices) {
            osw.append("v ").append(nf.format(vertex.x)).append(" ").append(nf.format(vertex.y)).append(" ").append(nf.format(vertex.z)).append("\n");
        }

        // Write vertex normals
        for (Vector3 vertex : normals) {
            osw.append("vn ").append(nf.format(vertex.x)).append(" ").append(nf.format(vertex.y)).append(" ").append(nf.format(vertex.z)).append("\n");
        }

        //osw.append("s 1\n");

        // Write faces
        for (IFace face : faces) {
            // All vertices of a face share the same normal
            osw.append("f ");
            int[] v = face.v();
            for (int i = 0; i < v.length; i++) {
                osw.append(idx(face.v()[i], face.n()[i]));
                if (i != v.length - 1) {
                    osw.append(" ");
                }
            }
            osw.append("\n");
        }

        osw.flush();
        osw.close();
    }

    /**
     * Constructs the face string for the given vertex.
     *
     * @param vi The vertex index.
     * @param ni The normal index
     *
     * @return The face string
     */
    private String idx(int vi, int ni) {
        return vi + "//" + ni;
    }

    public interface IFace {
        int[] v();

        int[] n();
        int[] b();
        int[] t();

        void setNormals(int... n);
        void setBinormals(int... n);
        void setTangents(int... n);
    }

    /**
     * Contains the index info for a face.
     */
    public class Face implements IFace {
        /** This stores the indices for both the vertices and the UV coordinates **/
        public int[] v;

        /** This stores the indices for the normals **/
        public int[] n;
        /** This stores the indices for the tangents **/
        public int[] t;
        /** This stores the indices for the binormals **/
        public int[] b;

        /**
         * Constructs a face with the indices of the vertices.
         *
         * @param v Indices of the vertices.
         */
        public Face(int... v) {
            this.v = v;
        }

        /**
         * Sets the normal indices.
         *
         * @param n Indices of the normals.
         */
        public void setNormals(int... n) {
            this.n = n;
        }
        /**
         * Sets the binormal indices.
         *
         * @param n Indices of the binormals.
         */
        public void setBinormals(int... n) {
            this.b = n;
        }
        /**
         * Sets the tangent indices.
         *
         * @param n Indices of the tangents.
         */
        public void setTangents(int... n) {
            this.t = n;
        }

        @Override
        public int[] v() {
            return v;
        }

        @Override
        public int[] n() {
            return n;
        }

        @Override
        public int[] t() {
            return t;
        }

        @Override
        public int[] b() {
            return b;
        }
    }
}
