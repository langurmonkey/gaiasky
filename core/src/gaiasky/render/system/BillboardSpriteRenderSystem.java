/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.system;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.render.IQuadRenderable;
import gaiasky.render.IRenderable;
import gaiasky.render.SceneGraphRenderer.RenderGroup;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.DecalUtils;
import gaiasky.util.comp.DistToCameraComparator;
import gaiasky.util.gdx.mesh.IntMesh;
import gaiasky.util.gdx.shader.ExtShaderProgram;

import java.util.List;

public class BillboardSpriteRenderSystem extends AbstractRenderSystem {

    private IntMesh mesh;
    private Quaternion quaternion;
    private final int ctIndex;

    public BillboardSpriteRenderSystem(RenderGroup rg, float[] alphas, ExtShaderProgram[] programs, int ctIndex, float w, float h) {
        super(rg, alphas, programs);
        this.ctIndex = ctIndex;
        init(w, h);
    }

    /**
     * Creates a new billboard quad render component
     * 
     * @param rg
     *            The render group
     * @param alphas
     *            The alphas list
     * @param programs
     *            The shader programs to render the quad with
     * @param ctIndex
     *            The component type index
     */
    public BillboardSpriteRenderSystem(RenderGroup rg, float[] alphas, ExtShaderProgram[] programs, int ctIndex) {
        this(rg, alphas, programs, ctIndex, 2, 2);
    }

    private void init(float w, float h) {
        // Init comparator
        comp = new DistToCameraComparator<>();
        // Init vertices
        float[] vertices = new float[20];
        fillVertices(vertices, w, h);

        // We wont need indices if we use GL_TRIANGLE_FAN to draw our quad
        // TRIANGLE_FAN will draw the verts in this order: 0, 1, 2; 0, 2, 3
        mesh = new IntMesh(true, 4, 6, new VertexAttribute(Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE), new VertexAttribute(Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE), new VertexAttribute(Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"));

        mesh.setVertices(vertices, 0, vertices.length);
        mesh.getIndicesBuffer().position(0);
        mesh.getIndicesBuffer().limit(6);

        int[] indices = new int[] { 0, 1, 2, 0, 2, 3 };
        mesh.setIndices(indices);

        quaternion = new Quaternion();
        aux = new Vector3();

    }

    private void fillVertices(float[] vertices, float w, float h) {
        float x = w / 2;
        float y = h / 2;
        float width = -w;
        float height = -h;
        final float fx2 = x + width;
        final float fy2 = y + height;
        final float u = 0;
        final float v = 1;
        final float u2 = 1;
        final float v2 = 0;

        float color = Color.WHITE.toFloatBits();

        int idx = 0;
        vertices[idx++] = x;
        vertices[idx++] = y;
        vertices[idx++] = color;
        vertices[idx++] = u;
        vertices[idx++] = v;

        vertices[idx++] = x;
        vertices[idx++] = fy2;
        vertices[idx++] = color;
        vertices[idx++] = u;
        vertices[idx++] = v2;

        vertices[idx++] = fx2;
        vertices[idx++] = fy2;
        vertices[idx++] = color;
        vertices[idx++] = u2;
        vertices[idx++] = v2;

        vertices[idx++] = fx2;
        vertices[idx++] = y;
        vertices[idx++] = color;
        vertices[idx++] = u2;
        vertices[idx++] = v;
    }

    @Override
    public void renderStud(Array<IRenderable> renderables, ICamera camera, double t) {
        if ((ctIndex < 0 || alphas[ctIndex] != 0)) {
            renderables.sort(comp);

            // Calculate billobard rotation quaternion ONCE
            DecalUtils.setBillboardRotation(quaternion, camera.getCamera().direction, camera.getCamera().up);

            ExtShaderProgram shaderProgram = getShaderProgram();

            shaderProgram.begin();

            // General uniforms
            shaderProgram.setUniformMatrix("u_projTrans", camera.getCamera().combined);
            shaderProgram.setUniformf("u_quaternion", quaternion.x, quaternion.y, quaternion.z, quaternion.w);

            // Rel, grav, z-buffer
            addEffectsUniforms(shaderProgram, camera);

            // Global uniforms
            shaderProgram.setUniformf("u_time", (float) t);

            renderables.forEach(r -> {
                IQuadRenderable s = (IQuadRenderable) r;
                s.render(shaderProgram, getAlpha(s), mesh, camera);
            });
            shaderProgram.end();
        }

    }

}
