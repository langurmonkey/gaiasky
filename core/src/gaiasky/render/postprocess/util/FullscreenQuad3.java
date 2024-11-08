/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.util;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public class FullscreenQuad3 {
    private static final int VERT_SIZE = 20;
    private static final float[] verts = new float[VERT_SIZE];
    private static final int X1 = 0;
    private static final int Y1 = 1;
    private static final int I1 = 2;
    private static final int U1 = 3;
    private static final int V1 = 4;
    private static final int X2 = 5;
    private static final int Y2 = 6;
    private static final int I2 = 7;
    private static final int U2 = 8;
    private static final int V2 = 9;
    private static final int X3 = 10;
    private static final int Y3 = 11;
    private static final int I3 = 12;
    private static final int U3 = 13;
    private static final int V3 = 14;
    private static final int X4 = 15;
    private static final int Y4 = 16;
    private static final int I4 = 17;
    private static final int U4 = 18;
    private static final int V4 = 19;
    private final Mesh quad;
    public FullscreenQuad3() {
        quad = createFullscreenQuad();
    }

    public void dispose() {
        quad.dispose();
    }

    /** Renders the quad with the specified shader program. */
    public void render(ShaderProgram program) {
        quad.render(program, GL20.GL_TRIANGLE_FAN, 0, 4);
    }

    private Mesh createFullscreenQuad() {
        // vertex coord
        verts[X1] = -1;
        verts[Y1] = -1;
        // index for frustum corner
        verts[I1] = 3;

        verts[X2] = 1;
        verts[Y2] = -1;
        // index for frustum corner
        verts[I2] = 2;

        verts[X3] = 1;
        verts[Y3] = 1;
        // index for frustum corner
        verts[I3] = 1;

        verts[X4] = -1;
        verts[Y4] = 1;
        // index for frustum corner
        verts[I4] = 0;

        // tex coords
        verts[U1] = 0f;
        verts[V1] = 0f;

        verts[U2] = 1f;
        verts[V2] = 0f;

        verts[U3] = 1f;
        verts[V3] = 1f;

        verts[U4] = 0f;
        verts[V4] = 1f;

        Mesh tmpMesh = new Mesh(true, 4, 0, new VertexAttribute(Usage.Position, 3, "a_position"), new VertexAttribute(Usage.TextureCoordinates, 2, "a_texCoord0"));

        tmpMesh.setVertices(verts);
        return tmpMesh;
    }
}
