/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.system;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.event.EventManager;
import gaiasky.event.Event;
import gaiasky.event.IObserver;
import gaiasky.render.IQuadRenderable;
import gaiasky.render.IRenderable;
import gaiasky.render.SceneGraphRenderer.RenderGroup;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.Settings;
import gaiasky.util.comp.DistToCameraComparator;
import gaiasky.util.gdx.mesh.IntMesh;
import gaiasky.util.gdx.shader.ExtShaderProgram;

public class BillboardStarRenderSystem extends AbstractRenderSystem implements IObserver {

    private IntMesh mesh;
    private Texture texture0;
    private final int ctIndex;

    public BillboardStarRenderSystem(RenderGroup rg, float[] alphas, ExtShaderProgram[] programs, String tex0, int ctIndex, float w, float h) {
        super(rg, alphas, programs);
        this.ctIndex = ctIndex;
        init(tex0, w, h);
    }

    /**
     * Creates a new billboard quad render component.
     *
     * @param rg             The render group.
     * @param alphas         The alphas list.
     * @param shaderPrograms The shader programs to render the quad with.
     */
    public BillboardStarRenderSystem(RenderGroup rg, float[] alphas, ExtShaderProgram[] shaderPrograms, String tex0, int ctIndex) {
        this(rg, alphas, shaderPrograms, tex0, ctIndex, 2, 2);
    }

    private void init(String tex0, float w, float h) {
        setStarTexture(tex0);

        // Init comparator
        comp = new DistToCameraComparator<>();
        // Init vertices
        float[] vertices = new float[20];
        fillVertices(vertices, w, h);

        // We won't need indices if we use GL_TRIANGLE_FAN to draw our quad
        // TRIANGLE_FAN will draw the vertices in this order: 0, 1, 2; 0, 2, 3
        mesh = new IntMesh(true, 4, 6,
                new VertexAttribute[]{
                new VertexAttribute(Usage.Position, 2, ExtShaderProgram.POSITION_ATTRIBUTE),
                new VertexAttribute(Usage.ColorPacked, 4, ExtShaderProgram.COLOR_ATTRIBUTE),
                new VertexAttribute(Usage.TextureCoordinates, 2, ExtShaderProgram.TEXCOORD_ATTRIBUTE + "0")});

        mesh.setVertices(vertices, 0, vertices.length);
        mesh.getIndicesBuffer().position(0);
        mesh.getIndicesBuffer().limit(6);

        int[] indices = new int[] { 0, 1, 2, 0, 2, 3 };
        mesh.setIndices(indices);

        aux = new Vector3();

        EventManager.instance.subscribe(this, Event.STAR_TEXTURE_IDX_CMD);
    }

    public void setStarTexture(String tex0) {
        texture0 = new Texture(Settings.settings.data.dataFileHandle(tex0), true);
        texture0.setFilter(TextureFilter.Linear, TextureFilter.Linear);
    }

    private void fillVertices(float[] vertices, float w, float h) {
        float x = w / 2;
        float y = h / 2;
        float width = -w;
        float height = -h;
        final float fx2 = x + width;
        final float fy2 = y + height;
        final float u = 1;
        final float v = 1;
        final float u2 = 0;
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
        vertices[idx] = v;
    }

    @Override
    public void renderStud(Array<IRenderable> renderables, ICamera camera, double t) {
        if ((ctIndex < 0 || alphas[ctIndex] != 0)) {
            renderables.sort(comp);

            ExtShaderProgram shaderProgram = getShaderProgram();

            shaderProgram.begin();

            if (texture0 != null) {
                texture0.bind(0);
                shaderProgram.setUniformi("u_texture0", 0);
            }

            // General uniforms
            shaderProgram.setUniformMatrix("u_projView", camera.getCamera().combined);

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

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        if (event == Event.STAR_TEXTURE_IDX_CMD) {
            GaiaSky.postRunnable(() -> setStarTexture(Settings.settings.scene.star.getStarTexture()));
        }
    }
}
