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

public class FullscreenMesh {
    private final Mesh mesh;
    private final int w;
    private final int h;

    private float[] verts;

    public FullscreenMesh(float[] p, int w, int h) {
        this.w = w;
        this.h = h;
        this.mesh = createFullscreenMesh(p, w, h);
    }

    public void dispose() {
        mesh.dispose();
    }

    /** Renders the quad with the specified shader program. */
    public void render(ShaderProgram program) {
        mesh.render(program, GL20.GL_TRIANGLES, 0, (w - 1) * (h - 1) * 6);
    }

    private Mesh createFullscreenMesh(float[] p, int w, int h) {
        verts = new float[(w - 1) * (h - 1) * 4 * 6];
        // Genreate vx0, vy0, u0, v0, vx1, vy1, u1, v1, ...

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
                verts[k + 0] = p[p00 + 0] * 2f - 1f;
                verts[k + 1] = p[p00 + 1] * 2f - 1f;
                verts[k + 2] = u;
                verts[k + 3] = v;
                // V10
                verts[k + 4] = p[p10 + 0] * 2f - 1f;
                verts[k + 5] = p[p10 + 1] * 2f - 1f;
                verts[k + 6] = (u + du);
                verts[k + 7] = v;
                // V11
                verts[k + 8] = p[p11 + 0] * 2f - 1f;
                verts[k + 9] = p[p11 + 1] * 2f - 1f;
                verts[k + 10] = (u + du);
                verts[k + 11] = (v + dv);

                // V00
                verts[k + 12] = p[p00 + 0] * 2f - 1f;
                verts[k + 13] = p[p00 + 1] * 2f - 1f;
                verts[k + 14] = u;
                verts[k + 15] = v;
                // V11
                verts[k + 16] = p[p11 + 0] * 2f - 1f;
                verts[k + 17] = p[p11 + 1] * 2f - 1f;
                verts[k + 18] = (u + du);
                verts[k + 19] = (v + dv);
                // V01
                verts[k + 20] = p[p01 + 0] * 2f - 1f;
                verts[k + 21] = p[p01 + 1] * 2f - 1f;
                verts[k + 22] = u;
                verts[k + 23] = (v + dv);

                k += 24;
            }
        }
        Mesh mesh = new Mesh(true, (w - 1) * (h - 1) * 6, 0, new VertexAttribute(Usage.Position, 2, "a_position"), new VertexAttribute(Usage.TextureCoordinates, 2, "a_texCoord0"));
        mesh.setVertices(verts);
        return mesh;
    }

}
