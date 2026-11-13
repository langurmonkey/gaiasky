/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.loader;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import gaiasky.render.gdx.loader.is.RegularInputStreamProvider;
import gaiasky.render.gdx.model.data.IntModelData;
import gaiasky.render.gdx.model.data.IntModelMesh;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;

/**
 * A mesh with no texture coordinates cannot be textured: the vertex buffer is built without
 * {@code a_texCoord0}, the attribute keeps its default value and every fragment samples the same
 * texel. That used to make a material supplied from the dataset JSON (see
 * {@code ModelComponent.setMaterial}) useless on any model that has no {@code vt} entries, which is
 * what a shape-only OBJ looks like.
 * <p>
 * These tests pin the fallback projection, and that a file which does carry {@code vt} entries is
 * left alone.
 */
public class OwnObjLoaderTest {

    /** Octahedron: poles on the y axis, four vertices on the equator. No vt, no vn. */
    private static final String OCTAHEDRON = """
            v 0 1 0
            v 1 0 0
            v 0 0 -1
            v -1 0 0
            v 0 0 1
            v 0 -1 0
            f 1 2 3
            f 1 3 4
            f 1 4 5
            f 1 5 2
            f 6 3 2
            f 6 4 3
            f 6 5 4
            f 6 2 5
            """;

    /** The same octahedron, with vt entries and usemtl, as a textured export would have it. */
    private static final String OCTAHEDRON_WITH_UVS = """
            v 0 1 0
            v 1 0 0
            v 0 0 -1
            v -1 0 0
            v 0 0 1
            v 0 -1 0
            vt 0.25 0.25
            vt 0.50 0.25
            vt 0.75 0.25
            f 1/1 2/2 3/3
            f 1/1 3/2 4/3
            f 1/1 4/2 5/3
            f 1/1 5/2 2/3
            f 6/1 3/2 2/3
            f 6/1 4/2 3/3
            f 6/1 5/2 4/3
            f 6/1 2/2 5/3
            """;

    @Test
    public void generatesUVsWhenTheFileHasNone() throws Exception {
        IntModelData data = load(OCTAHEDRON);
        Assert.assertEquals(1, data.meshes.size);
        IntModelMesh mesh = data.meshes.get(0);

        VertexAttributes attributes = new VertexAttributes(mesh.attributes);
        VertexAttribute uv = attributes.findByUsage(Usage.TextureCoordinates);
        Assert.assertNotNull("a textured model needs texture coordinates", uv);
        Assert.assertEquals(2, uv.numComponents);

        float[] uvPairs = uvs(mesh, attributes);
        for (int i = 0; i < uvPairs.length; i += 2) {
            // v is a latitude, so it is strictly in [0, 1]; u may leave the range by a
            // whole turn, which the wrap-around repair does on purpose.
            Assert.assertTrue("v out of range: " + uvPairs[i + 1], uvPairs[i + 1] >= 0f && uvPairs[i + 1] <= 1f);
            Assert.assertTrue("u out of range: " + uvPairs[i], uvPairs[i] >= -1f && uvPairs[i] <= 2f);
        }

        // The four equator vertices cover the circle, and the poles collapse onto v = 0 and 1.
        float minV = Float.MAX_VALUE, maxV = -Float.MAX_VALUE;
        float minU = Float.MAX_VALUE, maxU = -Float.MAX_VALUE;
        for (int i = 0; i < uvPairs.length; i += 2) {
            minU = Math.min(minU, uvPairs[i]);
            maxU = Math.max(maxU, uvPairs[i]);
            minV = Math.min(minV, uvPairs[i + 1]);
            maxV = Math.max(maxV, uvPairs[i + 1]);
        }
        Assert.assertEquals(0f, minV, 1e-5f);
        Assert.assertEquals(1f, maxV, 1e-5f);
        // Half a turn between opposite equator vertices, plus the wrap repair.
        Assert.assertTrue("longitude does not span the model: " + minU + " to " + maxU, maxU - minU > 0.45f);
    }

    @Test
    public void trianglesDoNotStretchAcrossTheSeam() throws Exception {
        IntModelData data = load(OCTAHEDRON);
        IntModelMesh mesh = data.meshes.get(0);
        VertexAttributes attributes = new VertexAttributes(mesh.attributes);
        VertexAttribute uv = attributes.findByUsage(Usage.TextureCoordinates);
        float[] uvPairs = uvs(mesh, attributes);

        // Three corners per face. A face that straddles u = 0 must not have corners at both
        // 0.02 and 0.98, or the whole texture is drawn across one triangle.
        for (int f = 0; f < uvPairs.length / 6; f++) {
            float u0 = uvPairs[f * 6];
            float u1 = uvPairs[f * 6 + 2];
            float u2 = uvPairs[f * 6 + 4];
            Assert.assertTrue("seam not repaired in face " + f,
                              Math.abs(u1 - u0) <= 0.5f + 1e-4f && Math.abs(u2 - u0) <= 0.5f + 1e-4f);
        }
    }

    @Test
    public void leavesTheFileUVsAlone() throws Exception {
        IntModelData data = load(OCTAHEDRON_WITH_UVS);
        IntModelMesh mesh = data.meshes.get(0);
        VertexAttributes attributes = new VertexAttributes(mesh.attributes);
        VertexAttribute uv = attributes.findByUsage(Usage.TextureCoordinates);
        Assert.assertNotNull(uv);

        float[] uvPairs = uvs(mesh, attributes);
        // The loader stores 1 - v, because OBJ counts v upwards.
        Assert.assertEquals(0.25f, uvPairs[0], 1e-6f);
        Assert.assertEquals(0.75f, uvPairs[1], 1e-6f);
        Assert.assertEquals(0.50f, uvPairs[2], 1e-6f);
    }

    private static IntModelData load(String obj) throws Exception {
        File file = File.createTempFile("gaiasky-obj-", ".obj");
        file.deleteOnExit();
        Files.writeString(file.toPath(), obj);
        OwnObjLoader loader = new OwnObjLoader(new RegularInputStreamProvider(), null);
        OwnObjLoader.logWarning = false;
        IntModelData data = loader.loadModelData(new FileHandle(file), false, true);
        Assert.assertNotNull(data);
        return data;
    }

    /** Texture coordinates of every vertex of the mesh, as u, v pairs. */
    private static float[] uvs(IntModelMesh mesh, VertexAttributes attributes) {
        VertexAttribute uv = attributes.findByUsage(Usage.TextureCoordinates);
        int stride = attributes.vertexSize / 4;
        int offset = uv.offset / 4;
        int n = mesh.vertices.length / stride;
        float[] out = new float[n * 2];
        for (int i = 0; i < n; i++) {
            out[i * 2] = mesh.vertices[i * stride + offset];
            out[i * 2 + 1] = mesh.vertices[i * stride + offset + 1];
        }
        return out;
    }
}
