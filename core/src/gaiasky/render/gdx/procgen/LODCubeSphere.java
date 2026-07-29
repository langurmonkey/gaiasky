/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.procgen;

import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.render.gdx.IntIntMeshBuilder;
import gaiasky.render.gdx.mesh.IntMesh;
import gaiasky.scene.camera.ICamera;
import gaiasky.util.Bits;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.Vector3D;
import gaiasky.util.math.Vector3Q;
import org.lwjgl.opengl.GL20;

/**
 * A unit LOD cube-sphere, where each cube face is a quadtree.
 * <p>
 * The {@link #builder} lives in this object. Use {@link #begin(int)}
 * to start building, then call {@link Quadtree#buildMesh(int, float)} on visible leaf
 * nodes, and finally {@link #end()} to produce the {@link IntMesh}.
 */
public class LODCubeSphere {

    // Auxiliary vectors (reused to avoid allocation).
    private final Vector3 aux1 = new Vector3();
    private final Vector3 aux2 = new Vector3();
    private final Vector3Q auxQ = new Vector3Q();

    // Scratch vectors for sphere-space computation in buildMesh().
    public final Vector3 scratchPos = new Vector3();
    public final Vector3 scratchNormal = new Vector3();
    public final Vector3 scratchTangent = new Vector3();
    public final Vector3 scratchBinormal = new Vector3();
    public final Vector2 scratchUV = new Vector2();

    /** The vertex attributes. **/
    private final Bits attributes;
    /** The single mesh builder owned by this LODCubeSphere. **/
    private final IntIntMeshBuilder builder;

    /** Faces of the cube-sphere, [top, bottom, left, right, front, back]. **/
    final Quadtree[] faces;

    /** Minimum tree depth, defaults to 0 for top-level. **/
    final int minDepth = 0;
    /** Maximum tree depth. **/
    final int maxDepth;

    /** Visible leaves list, cleared at the start of each traverse() call. **/
    public final Array<Quadtree> visibleLeaves = new Array<>(256);

    public LODCubeSphere(int maxDepth,
                         boolean fullInit) {
        this.maxDepth = maxDepth;

        // Face definitions: { axis, sign, uComp, vComp }
        // Each row describes one face of the unit cube [-1, 1]^3:
        //   axis   = the major axis (0=x, 1=y, 2=z)
        //   sign   = +1 or -1 for that axis
        //   uComp  = the axis used for the local U direction
        //   vComp  = the axis used for the local V direction
        int[][] faceDefs = {
                {0, 1, 1, 2},  // +X : x = +1, u=y, v=z   — cross(y,z)=+x, outward=+x
                {0, -1, 2, 1}, // -X : x = -1, u=z, v=y   — cross(z,y)=-x, outward=-x
                {1, 1, 2, 0},  // +Y : y = +1, u=z, v=x   — cross(z,x)=+y, outward=+y
                {1, -1, 0, 2}, // -Y : y = -1, u=x, v=z   — cross(x,z)=-y, outward=-y
                {2, 1, 0, 1},  // +Z : z = +1, u=x, v=y   — cross(x,y)=+z, outward=+z
                {2, -1, 1, 0}  // -Z : z = -1, u=y, v=x   — cross(y,x)=-z, outward=-z
        };


        // Faces
        faces = new Quadtree[6];
        for (int f = 0; f < 6; f++) {
            int axis = faceDefs[f][0];
            int sign = faceDefs[f][1];
            int uComp = faceDefs[f][2];
            int vComp = faceDefs[f][3];

            double[] pos0 = new double[3];
            pos0[axis] = sign;
            pos0[uComp] = -1;
            pos0[vComp] = -1;

            double[] pos1 = new double[3];
            pos1[axis] = sign;
            pos1[uComp] = 1;
            pos1[vComp] = -1;

            double[] pos2 = new double[3];
            pos2[axis] = sign;
            pos2[uComp] = 1;
            pos2[vComp] = 1;

            double[] pos3 = new double[3];
            pos3[axis] = sign;
            pos3[uComp] = -1;
            pos3[vComp] = 1;

            faces[f] = new Quadtree(this, 0, pos0, pos1, pos2, pos3);
            if (fullInit) {
                faces[f].initialize(maxDepth);
            }
        }

        attributes = Bits.indices(VertexAttributes.Usage.Position,
                                  VertexAttributes.Usage.Normal,
                                  VertexAttributes.Usage.Tangent,
                                  VertexAttributes.Usage.BiNormal,
                                  VertexAttributes.Usage.TextureCoordinates);
        builder = new IntIntMeshBuilder();
    }

    /**
     * Traverses the structure computing the visible nodes, adding them to the {@link #visibleLeaves} list.
     *
     * @param cam The camera.
     */
    public void update(ICamera cam,
                       double objectRadius,
                       Matrix4 localTransform) {
        System.out.println(visibleLeaves.size);
        visibleLeaves.clear();

        for (int f = 0; f < 6; f++) {
            traverseNode(faces[f], cam, objectRadius, localTransform);
        }
    }

    void traverseNode(Quadtree node,
                      ICamera cam,
                      double objectRadius,
                      Matrix4 localTransform) {
        // Put in world coordinates.
        var worldPosition = node.center.put(aux1).mul(localTransform);

        // Frustum cull using bounding sphere.
        if (!frustumTest(worldPosition, node.radius * objectRadius, cam)) return;

        // Determine if this node should subdivide based on screen-space error
        // We compute the distance from the camera to the node.
        double dist = worldPosition.len();
        // Screen size is the node radius over the distance.
        double screenSize = node.radius * objectRadius / dist;
        int targetDepth = computeTargetDepth(screenSize, 3f);

        if (node.depth < targetDepth) {
            // Subdivide: recurse into children
            for (int i = 0; i < 4; i++) {
                if (node.isLeaf()) {
                    node.subdivide();
                }
                traverseNode(node.children[i], cam, objectRadius, localTransform);
            }
        } else {
            // Leaf: add to visible list
            visibleLeaves.add(node);
        }
    }

    int computeTargetDepth(double screenSize,
                           double factor) {
        // The node's geometric error is roughly proportional to its angular size.
        // Convert to target depth using a logarithmic scale.
        double ratio = screenSize * factor;
        int depth = (int) (Math.log(ratio) / Math.log(2)) + minDepth;
        return MathUtilsDouble.clamp(depth, minDepth, maxDepth);
    }

    boolean frustumTest(Vector3 position,
                        double radius,
                        ICamera cam) {
        return cam.getCamera().frustum.boundsInFrustum(position, aux2.set((float) radius, (float) radius, (float) radius));
    }

    /**
     * Build meshes for all visible leaves (populated by {@link #update(ICamera, double, Matrix4)}).
     * Calls {@link Quadtree#buildMesh(int, float)} on each visible leaf.
     *
     * @param N      The number of subdivisions per edge (N &gt; 0).
     * @param radius The radius of the sphere.
     */
    public void buildVisibleMeshes(int N,
                                   float radius) {
        for (Quadtree leaf : visibleLeaves) {
            leaf.buildMesh(N, radius);
        }
    }

    /**
     * Begin building a mesh. Initializes the internal {@link IntIntMeshBuilder}, if needed, and calls
     * {@code begin(attributes, primitiveType)} on it.
     *
     * @param primitiveType Primitive type (e.g. {@link GL20#GL_TRIANGLES}).
     */
    public void begin(int primitiveType) {
        builder.begin(attributes, primitiveType);
    }

    /**
     * Finish building and return the mesh.
     *
     * @return The completed mesh.
     */
    public IntMesh end() {
        if (builder == null)
            throw new IllegalStateException("begin() must be called before end()");
        return builder.end();
    }

    /**
     * <p>A quadtree, which parts 2D space into 4 parts on subdivision.</p>
     * <p>Each quadtree has 4 vertices (p_i) and the given indices for the children:</p>
     *
     * <pre>
     *     p0              p1
     *       ┌──────┬──────┐
     *       │      │      │
     *       │  0   │  1   │
     *       │      │      │
     *       ├──────┼──────┤
     *       │      │      │
     *       │  2   │  3   │
     *       │      │      │
     *       └──────┴──────┘
     *     p2              p3
     * </pre>
     */
    public static class Quadtree {
        /** The parent LODCubeSphere, which owns the mesh builder. **/
        final LODCubeSphere parent;
        /** Bounds of the quadtree, in the flat cube. **/
        final Vector3D p0, p1, p2, p3;
        /** Central point. **/
        final Vector3D center;
        /** Depth of this node in the tree structure. **/
        final int depth;
        /** Radius of this node, given by the distance of its points to the center of the patch. **/
        final double radius;

        /** Array of children, as in [tl, tr, bl, br]. **/
        final Quadtree[] children = new Quadtree[4];

        public Quadtree(LODCubeSphere parent,
                        int depth,
                        double[] p0,
                        double[] p1,
                        double[] p2,
                        double[] p3) {

            this(parent,
                 depth,
                 new Vector3D(p0),
                 new Vector3D(p1),
                 new Vector3D(p2),
                 new Vector3D(p3));
        }

        public Quadtree(LODCubeSphere parent,
                        int depth,
                        Vector3D p0,
                        Vector3D p1,
                        Vector3D p2,
                        Vector3D p3) {

            this.depth = depth;
            this.parent = parent;
            this.p0 = p0;
            this.p1 = p1;
            this.p2 = p2;
            this.p3 = p3;
            this.center = middlePoint(p0, p3);
            this.radius = p0.dst(center);

        }

        /**
         * Spawns cells in this node down to the given level, fully.
         *
         * @param depth The depth to initialize.
         */
        public void initialize(int depth) {
            if (isLeaf() && this.depth < depth) {
                subdivide();
                for (var ch : children) {
                    ch.initialize(depth);
                }
            }
        }

        /**
         * Check if this node has any children or if it is a leaf node.
         *
         * @return True if the node has no children.
         */
        public boolean isLeaf() {
            return children[0] == null && children[1] == null && children[2] == null && children[3] == null;
        }

        /**
         * Subdivide the current quadtree into 4 children.
         * This operation only succeeds if this is a leaf node. Otherwise, the node is already subdivided and nothing happens.
         */
        public void subdivide() {
            if (isLeaf() && depth < parent.maxDepth) {
                var p01 = middlePoint(p0, p1);
                var p02 = middlePoint(p0, p2);
                var p13 = middlePoint(p1, p3);
                var p23 = middlePoint(p2, p3);

                // Top-left
                children[0] = new Quadtree(parent,
                                           depth + 1,
                                           p0.values(),
                                           p01.values(),
                                           p02.values(),
                                           center.values());

                // Top-right
                children[1] = new Quadtree(parent,
                                           depth + 1,
                                           p01.values(),
                                           p1.values(),
                                           center.values(),
                                           p13.values());

                // Bottom-left
                children[2] = new Quadtree(parent,
                                           depth + 1,
                                           p02.values(),
                                           center.values(),
                                           p2.values(),
                                           p23.values());

                // Bottom-right
                children[3] = new Quadtree(parent,
                                           depth + 1,
                                           center.values(),
                                           p13.values(),
                                           p23.values(),
                                           p3.values());
            }
        }

        /**
         * Remove the children of this node, making it into a leaf node.
         */
        public void undivide() {
            if (!isLeaf()) {
                children[0] = children[1] = children[2] = children[3] = null;
            }
        }

        private Vector3D middlePoint(Vector3D p,
                                     Vector3D q) {
            Vector3D result = new Vector3D();
            return result.set(p).add(q).scl(0.5);
        }

        /**
         * Map the point p on the cube to the surface of the cube-sphere.
         *
         * @param p The point to map. This method transforms the components of the point.
         */
        static void map(Vector3D p) {
            double x = p.x, y = p.y, z = p.z;
            double x2 = x * x, y2 = y * y, z2 = z * z;
            double sx = x * Math.sqrt(1.0 - y2 / 2.0 - z2 / 2.0 + y2 * z2 / 3.0);
            double sy = y * Math.sqrt(1.0 - x2 / 2.0 - z2 / 2.0 + x2 * z2 / 3.0);
            double sz = z * Math.sqrt(1.0 - x2 / 2.0 - y2 / 2.0 + x2 * y2 / 3.0);
            p.set(sx, sy, sz);
        }

        /**
         * Build a mesh for this quadtree node with an NxN grid of vertices.
         * <p>
         * Each vertex position is bilinearly interpolated within the quad's bounds
         * on the cube face, then mapped to the sphere via {@link #map(Vector3D)}.
         * Normals, UV coordinates (spherical projection), tangents (westward/U direction),
         * and binormals (southward/V direction) are computed from the mapped position,
         * matching the conventions of {@link gaiasky.render.gdx.creators.CubeSphereCreator}.
         * <p>
         * This method uses the parent {@link LODCubeSphere#builder} to add geometry.
         * Call {@link LODCubeSphere#begin(int)} before, and
         * {@link LODCubeSphere#end()} after building all visible leaf nodes.
         *
         * @param N      The number of subdivisions per edge (N &gt; 0). Produces an NxN grid of quads.
         * @param radius The radius of the sphere.
         */
        public void buildMesh(int N,
                              float radius) {
            if (N < 1)
                throw new IllegalArgumentException("N must be > 0");

            // Grid of vertex indices.
            int[][] gridIdx = new int[N + 1][N + 1];

            // Scratch vectors reused per vertex to avoid allocation.
            Vector3D posD = new Vector3D();
            Vector3D top = new Vector3D();
            Vector3D bot = new Vector3D();
            Vector3D tmpD = new Vector3D();

            LODCubeSphere parent = this.parent;
            Vector3 pos = parent.scratchPos;
            Vector3 normal = parent.scratchNormal;
            Vector3 tangent = parent.scratchTangent;
            Vector3 binormal = parent.scratchBinormal;
            Vector2 uv = parent.scratchUV;

            for (int j = 0; j <= N; j++) {
                for (int i = 0; i <= N; i++) {
                    float u = (float) i / N; // 0..1, p0→p1
                    float v = (float) j / N; // 0..1, p0→p2

                    // Bilinear interpolation on the cube face:
                    //   top = lerp(p0, p1, u)
                    //   bot = lerp(p2, p3, u)
                    //   pos = lerp(top, bot, v)
                    top.set(p0).scl(1.0 - u).add(tmpD.set(p1).scl(u));
                    bot.set(p2).scl(1.0 - u).add(tmpD.set(p3).scl(u));
                    posD.set(top).scl(1.0 - v).add(tmpD.set(bot).scl(v));

                    // Map cube position to sphere.
                    map(posD);

                    // Scale by radius.
                    pos.set((float) posD.x * radius,
                            (float) posD.y * radius,
                            (float) posD.z * radius);

                    // Normal = normalized sphere position.
                    normal.set(pos).nor();

                    // UV from spherical projection (matching CubeSphereCreator).
                    float lon = (float) Math.atan2(normal.z, normal.x);
                    if (lon < 0) lon += MathUtils.PI2;
                    float colat = (float) Math.acos(MathUtils.clamp(normal.y, -1f, 1f));
                    uv.set(1f - lon / MathUtils.PI2, colat / MathUtils.PI);

                    // Tangent = westward (U direction) = cross(Y, normal).
                    tangent.set(Vector3.Y).crs(normal);
                    if (tangent.isZero(0.0001f))
                        tangent.set(1f, 0f, 0f);
                    else
                        tangent.nor();

                    // Binormal = southward (V direction) = cross(tangent, normal).
                    binormal.set(tangent).crs(normal).nor();

                    // Add vertex.
                    int idx = parent.builder.vertex(pos, normal, tangent, binormal, null, uv);
                    gridIdx[j][i] = idx;
                }
            }

            // Create two triangles per quad.
            for (int j = 0; j < N; j++) {
                for (int i = 0; i < N; i++) {
                    int i00 = gridIdx[j][i];
                    int i10 = gridIdx[j][i + 1];
                    int i01 = gridIdx[j + 1][i];
                    int i11 = gridIdx[j + 1][i + 1];
                    parent.builder.triangle(i00, i10, i11);
                    parent.builder.triangle(i00, i11, i01);
                }
            }
        }
    }
}
