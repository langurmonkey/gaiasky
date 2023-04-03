/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */
package gaiasky.util.gdx.contrib.postprocess.utils;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import gaiasky.util.gdx.loader.WarpMeshReader.WarpMesh;

public class FullscreenMesh {
    private final Mesh mesh;
    private final int w;
    private final int h;

    public FullscreenMesh(float[] p, int w, int h) {
        this.w = w;
        this.h = h;
        this.mesh = createFullscreenMesh(p, w, h);
    }

    public FullscreenMesh(WarpMesh warpMesh) {
        this.w = warpMesh.nx;
        this.h = warpMesh.ny;
        this.mesh = createFullscreenMesh(warpMesh);
    }

    public void dispose() {
        mesh.dispose();
    }

    /** Renders the quad with the specified shader program. */
    public void render(ShaderProgram program) {
        mesh.render(program, GL20.GL_TRIANGLES, 0, (w - 1) * (h - 1) * 6);
    }

    private Mesh createFullscreenMesh(float[] p, int w, int h) {
        float[] vertices = new float[(w - 1) * (h - 1) * 4 * 6];
        // Generate vx0, vy0, u0, v0, vx1, vy1, u1, v1, ...

        float du = 1f / (w - 1f);
        float dv = 1f / (h - 1f);
        int k = 0;
        for (int j = 0; j < h - 1; j++) {
            for (int i = 0; i < w - 1; i++) {

                int p00 = (w * j + i) * 3;
                int p10 = (w * j + i + 1) * 3;
                int p11 = (w * (j + 1) + i + 1) * 3;
                int p01 = (w * (j + 1) + i) * 3;

                float u = i / (w - 1f);
                float v = j / (h - 1f);

                // V00
                vertices[k] = p[p00] * 2f - 1f;
                vertices[k + 1] = p[p00 + 1] * 2f - 1f;
                vertices[k + 2] = u;
                vertices[k + 3] = v;
                vertices[k + 4] = 1;
                // V10
                vertices[k + 4] = p[p10] * 2f - 1f;
                vertices[k + 5] = p[p10 + 1] * 2f - 1f;
                vertices[k + 6] = (u + du);
                vertices[k + 7] = v;
                // V11
                vertices[k + 8] = p[p11] * 2f - 1f;
                vertices[k + 9] = p[p11 + 1] * 2f - 1f;
                vertices[k + 10] = (u + du);
                vertices[k + 11] = (v + dv);

                // V00
                vertices[k + 12] = p[p00] * 2f - 1f;
                vertices[k + 13] = p[p00 + 1] * 2f - 1f;
                vertices[k + 14] = u;
                vertices[k + 15] = v;
                // V11
                vertices[k + 16] = p[p11] * 2f - 1f;
                vertices[k + 17] = p[p11 + 1] * 2f - 1f;
                vertices[k + 18] = (u + du);
                vertices[k + 19] = (v + dv);
                // V01
                vertices[k + 20] = p[p01] * 2f - 1f;
                vertices[k + 21] = p[p01 + 1] * 2f - 1f;
                vertices[k + 22] = u;
                vertices[k + 23] = (v + dv);

                k += 24;
            }
        }
        Mesh mesh = new Mesh(true, (w - 1) * (h - 1) * 6, 0, new VertexAttribute(Usage.Position, 2, "a_position"), new VertexAttribute(Usage.TextureCoordinates, 2, "a_texCoord0"));
        mesh.setVertices(vertices);
        return mesh;
    }

    private Mesh createFullscreenMesh(WarpMesh warp) {
        float[] vertices = new float[(w - 1) * (h - 1) * 5 * 6];
        int k = 0;
        float ar = (float) w / (float) h;
        for (int j = 0; j < h - 1; j++) {
            for (int i = 0; i < w - 1; i++) {
                var p00 = warp.nodes.get(w * j + i);
                var p10 = warp.nodes.get(w * j + i + 1);
                var p11 = warp.nodes.get(w * (j + 1) + i + 1);
                var p01 = warp.nodes.get(w * (j + 1) + i);

                // V00
                vertices[k] = p00[0] / ar;
                vertices[k + 1] = p00[1];
                vertices[k + 2] = p00[2];
                vertices[k + 3] = p00[3];
                vertices[k + 4] = p00[4];
                // V10
                vertices[k + 5] = p10[0] / ar;
                vertices[k + 6] = p10[1];
                vertices[k + 7] = p10[2];
                vertices[k + 8] = p10[3];
                vertices[k + 9] = p10[4];
                // V11
                vertices[k + 10] = p11[0] / ar;
                vertices[k + 11] = p11[1];
                vertices[k + 12] = p11[2];
                vertices[k + 13] = p11[3];
                vertices[k + 14] = p11[4];

                // V00
                vertices[k + 15] = p00[0] / ar;
                vertices[k + 16] = p00[1];
                vertices[k + 17] = p00[2];
                vertices[k + 18] = p00[3];
                vertices[k + 19] = p00[4];
                // V11
                vertices[k + 20] = p11[0] / ar;
                vertices[k + 21] = p11[1];
                vertices[k + 22] = p11[2];
                vertices[k + 23] = p11[3];
                vertices[k + 24] = p11[4];
                // V01
                vertices[k + 25] = p01[0] / ar;
                vertices[k + 26] = p01[1];
                vertices[k + 27] = p01[2];
                vertices[k + 28] = p01[3];
                vertices[k + 29] = p01[4];

                k += 30;
            }
        }
        Mesh mesh = new Mesh(true, (w - 1) * (h - 1) * 6, 0, new VertexAttribute(Usage.Position, 2, "a_position"), new VertexAttribute(Usage.TextureCoordinates, 2, "a_texCoord0"), new VertexAttribute(Usage.Generic, 1, "a_intensity"));
        mesh.setVertices(vertices);
        return mesh;
    }

}
