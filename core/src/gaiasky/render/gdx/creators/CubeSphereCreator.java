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

import java.util.ArrayList;
public class CubeSphereCreator extends ModelCreator {

    public CubeSphereCreator() {
        super();
        this.name = "CubeSphere";
    }

    /**
     * Creates a cube-sphere model.
     *
     * @param radius    The radius of the sphere.
     * @param divisions The number of subdivisions per face edge (must be &gt; 0).
     * @return This creator.
     */
    public CubeSphereCreator create(float radius, int divisions) {
        return create(radius, divisions, false);
    }

    /**
     * Creates a cube-sphere model. Start with a cube where each face contains
     * {@code divisions x divisions} quads, then project each vertex onto the
     * sphere by normalizing its position and scaling by the radius.
     * <p>
     * UVs are computed from the final spherical direction (longitude/colatitude),
     * using the same convention as {@link SphereCreator} (u=1 at longitude 0,
     * decreasing eastward; v=0 at the north pole, 1 at the south pole). This
     * makes any standard equirectangular texture (e.g. a whole-Earth map) line
     * up correctly, and lets a UV-sphere and cube-sphere share the same texture.
     * <p>
     * Because a flat square face can span more than 180 degrees of longitude
     * around the poles, three of the six faces (-X, +Y, -Y) cross the
     * longitude seam (u wraps from ~1 back to ~0). Vertices along that seam
     * are duplicated with u shifted by +-1 so no quad stretches across the
     * whole texture.
     *
     * @param radius      The radius of the sphere.
     * @param divisions   The number of subdivisions per face edge (must be &gt; 0).
     * @param flipNormals Whether to reverse triangle winding (flip normals).
     * @return This creator.
     */
    public CubeSphereCreator create(float radius, int divisions, boolean flipNormals) {
        if (divisions < 1)
            throw new AssertionError("Divisions must be > 0");

        this.flipNormals = flipNormals;
        this.vertices = new ArrayList<>();
        this.normals = new ArrayList<>();
        this.binormals = new ArrayList<>();
        this.tangents = new ArrayList<>();
        this.uv = new ArrayList<>();
        this.faces = new ArrayList<>();
        this.index = 1;

        // Face definitions: { axis, sign, uComp, vComp }
        // Each row describes one face of the unit cube [-1, 1]^3:
        //   axis   = the major axis (0=x, 1=y, 2=z)
        //   sign   = +1 or -1 for that axis
        //   uComp  = the axis used for the local U direction
        //   vComp  = the axis used for the local V direction
        int[][] faceDefs = {
                { 0, 1, 1, 2 },  // +X : x = +1, u=y, v=z   — cross(y,z)=+x, outward=+x ✓
                { 0, -1, 2, 1 }, // -X : x = -1, u=z, v=y   — cross(z,y)=-x, outward=-x ✓
                { 1, 1, 2, 0 },  // +Y : y = +1, u=z, v=x   — cross(z,x)=+y, outward=+y ✓
                { 1, -1, 0, 2 }, // -Y : y = -1, u=x, v=z   — cross(x,z)=-y, outward=-y ✓
                { 2, 1, 0, 1 },  // +Z : z = +1, u=x, v=y   — cross(x,y)=+z, outward=+z ✓
                { 2, -1, 1, 0 }  // -Z : z = -1, u=y, v=x   — cross(y,x)=-z, outward=-z ✓
        };

        for (int f = 0; f < 6; f++) {
            int axis = faceDefs[f][0];
            int sign = faceDefs[f][1];
            int uComp = faceDefs[f][2];
            int vComp = faceDefs[f][3];

            int[][] gridIdx = new int[divisions + 1][divisions + 1];
            Vector3[][] posGrid = new Vector3[divisions + 1][divisions + 1];
            Vector3[][] normalGrid = new Vector3[divisions + 1][divisions + 1];
            float[][] uGrid = new float[divisions + 1][divisions + 1];
            float[][] vGrid = new float[divisions + 1][divisions + 1];
            float[] pos = new float[3];

            for (int j = 0; j <= divisions; j++) {
                for (int i = 0; i <= divisions; i++) {
                    // Map grid cell (i, j) to cube face coordinates in [-1, 1].
                    float uc = -1.0f + 2.0f * i / divisions;
                    float vc = -1.0f + 2.0f * j / divisions;

                    pos[axis] = sign;
                    pos[uComp] = uc;
                    pos[vComp] = vc;

                    Vector3 cubePos = new Vector3(pos[0], pos[1], pos[2]);

                    // Project onto sphere (normalize and scale by radius).
                    Vector3 spherePos = cubePos.cpy().nor().scl(radius);
                    Vector3 normal = spherePos.cpy().nor();

                    // UV from the final spherical direction, matching SphereCreator:
                    // longitude in the x-z plane, colatitude measured from +Y.
                    float lon = (float) Math.atan2(normal.z, normal.x); // (-pi, pi]
                    if (lon < 0) lon += MathUtils.PI2;                  // -> [0, 2pi)
                    float colat = (float) Math.acos(MathUtils.clamp(normal.y, -1f, 1f)); // [0, pi]

                    float u = 1f - lon / MathUtils.PI2;
                    float v = colat / MathUtils.PI;

                    posGrid[j][i] = spherePos;
                    normalGrid[j][i] = normal;
                    uGrid[j][i] = u;
                    vGrid[j][i] = v;

                    gridIdx[j][i] = addVertex(spherePos, normal, u, v);
                }
            }

            // Create two triangles per quad, duplicating any vertex whose u
            // would otherwise wrap around the seam within this quad.
            for (int j = 0; j < divisions; j++) {
                for (int i = 0; i < divisions; i++) {
                    float uRef = uGrid[j][i];
                    int i00 = gridIdx[j][i];
                    int i10 = fixSeam(gridIdx[j][i + 1], uGrid[j][i + 1], uRef, posGrid[j][i + 1], normalGrid[j][i + 1], vGrid[j][i + 1]);
                    int i01 = fixSeam(gridIdx[j + 1][i], uGrid[j + 1][i], uRef, posGrid[j + 1][i], normalGrid[j + 1][i], vGrid[j + 1][i]);
                    int i11 = fixSeam(gridIdx[j + 1][i + 1], uGrid[j + 1][i + 1], uRef, posGrid[j + 1][i + 1], normalGrid[j + 1][i + 1], vGrid[j + 1][i + 1]);

                    addFace(faces, flipNormals, i00, i10, i11);
                    addFace(faces, flipNormals, i00, i11, i01);
                }
            }
        }

        // Copy vertex indices as normal/tangent/binormal indices (smooth shading).
        for (IFace face : faces) {
            face.setNormals(face.v()[0], face.v()[1], face.v()[2]);
            face.setBinormals(face.v()[0], face.v()[1], face.v()[2]);
            face.setTangents(face.v()[0], face.v()[1], face.v()[2]);
        }

        return this;
    }

    /**
     * Appends a new vertex (with identity tangent/binormal, per the tessellation
     * shader convention used elsewhere in this class) and returns its index.
     */
    private int addVertex(Vector3 position, Vector3 normal, float u, float v) {
        vertices.add(position);
        normals.add(normal);
        tangents.add(new Vector3(1.0f, 0.0f, 0.0f));
        binormals.add(new Vector3(0.0f, 1.0f, 0.0f));
        uv.add(new Vector2(u, v));
        return index++;
    }

    /**
     * If this corner's u is more than half a texture-width away from the quad's
     * reference u, it means the quad straddles the longitude seam. Rather than
     * mutate the shared grid vertex (which other, non-seam-crossing quads still
     * need with its original u), create a duplicate vertex shifted by +-1 in u
     * so this quad's own interpolation stays continuous.
     */
    private int fixSeam(int originalIndex, float u, float uRef, Vector3 position, Vector3 normal, float v) {
        float diff = u - uRef;
        if (diff > 0.5f) {
            return addVertex(position, normal, u - 1f, v);
        } else if (diff < -0.5f) {
            return addVertex(position, normal, u + 1f, v);
        }
        return originalIndex;
    }
}