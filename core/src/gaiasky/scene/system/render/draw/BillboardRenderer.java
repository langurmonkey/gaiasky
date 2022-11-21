package gaiasky.scene.system.render.draw;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.math.Vector3;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.RenderGroup;
import gaiasky.render.api.IRenderable;
import gaiasky.render.system.AbstractRenderSystem;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.Render;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.scene.system.render.draw.billboard.BillboardEntityRenderSystem;
import gaiasky.scene.view.BillboardView;
import gaiasky.util.Settings;
import gaiasky.util.comp.DistanceEntityComparator;
import gaiasky.util.gdx.mesh.IntMesh;
import gaiasky.util.gdx.shader.ExtShaderProgram;

import java.util.List;

/**
 * Renders billboards (camera-oriented quads), optionally with a global texture. If a texture is not provided,
 * it is expected that the provided shader program does the rendering procedurally, or
 * the object itself binds its own texture.
 */
public class BillboardRenderer extends AbstractRenderSystem implements IObserver {

    private IntMesh mesh;
    private Texture billboardTexture;
    private final ComponentType componentType;

    private final BillboardView view;
    private final BillboardEntityRenderSystem renderSystem;

    // Render metadata

    public BillboardRenderer(SceneRenderer sceneRenderer, RenderGroup rg, float[] alphas, ExtShaderProgram[] programs, String texturePath, ComponentType componentType, float w, float h, boolean starTextureListener) {
        super(sceneRenderer, rg, alphas, programs);
        this.componentType = componentType;
        init(texturePath, w, h, starTextureListener);

        this.view = new BillboardView();
        this.renderSystem = new BillboardEntityRenderSystem();
    }

    /**
     * Creates a new billboard quad render component.
     *
     * @param rg                  The render group.
     * @param alphas              The alphas list.
     * @param shaderPrograms      The shader programs to render the quad with.
     * @param texturePath         The path to the texture to use for the billboards.
     * @param componentType       The component type.
     * @param starTextureListener Whether to listen for star texture setting changes.
     */
    public BillboardRenderer(SceneRenderer sceneRenderer, RenderGroup rg, float[] alphas, ExtShaderProgram[] shaderPrograms, String texturePath, ComponentType componentType, boolean starTextureListener) {
        this(sceneRenderer, rg, alphas, shaderPrograms, texturePath, componentType, 2, 2, starTextureListener);
    }

    private void init(String tex0, float w, float h, boolean starTextureListener) {
        setStarTexture(tex0);

        // Init comparator
        comp = new DistanceEntityComparator<>();
        // Init vertices
        float[] vertices = new float[20];
        fillVertices(vertices, w, h);

        // We won't need indices if we use GL_TRIANGLE_FAN to draw our quad
        // TRIANGLE_FAN will draw the vertices in this order: 0, 1, 2; 0, 2, 3
        mesh = new IntMesh(true, 4, 6,
                new VertexAttribute[] {
                        new VertexAttribute(Usage.Position, 2, ExtShaderProgram.POSITION_ATTRIBUTE),
                        new VertexAttribute(Usage.ColorPacked, 4, ExtShaderProgram.COLOR_ATTRIBUTE),
                        new VertexAttribute(Usage.TextureCoordinates, 2, ExtShaderProgram.TEXCOORD_ATTRIBUTE + "0") });

        mesh.setVertices(vertices, 0, vertices.length);
        mesh.getIndicesBuffer().position(0);
        mesh.getIndicesBuffer().limit(6);

        int[] indices = new int[] { 0, 1, 2, 0, 2, 3 };
        mesh.setIndices(indices);

        auxf = new Vector3();

        if (starTextureListener) {
            EventManager.instance.subscribe(this, Event.BILLBOARD_TEXTURE_IDX_CMD);
        }
    }

    public void setStarTexture(String texturePath) {
        if (texturePath != null) {
            billboardTexture = new Texture(Settings.settings.data.dataFileHandle(texturePath), true);
            billboardTexture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        }
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
    public void renderStud(List<IRenderable> renderables, ICamera camera, double t) {
        if ((componentType == null || alphas[componentType.ordinal()] != 0)) {
            renderables.sort(comp);

            ExtShaderProgram shaderProgram = getShaderProgram();

            shaderProgram.begin();

            if (billboardTexture != null) {
                billboardTexture.bind(0);
                shaderProgram.setUniformi("u_texture0", 0);
            }

            // Global uniforms
            shaderProgram.setUniformMatrix("u_projView", camera.getCamera().combined);
            shaderProgram.setUniformf("u_time", (float) t);

            // Rel, grav, z-buffer
            addEffectsUniforms(shaderProgram, camera);

            // Render each sprite
            renderables.forEach(r -> {
                Entity entity = ((Render) r).entity;
                render(entity, shaderProgram, mesh, camera);
            });
            shaderProgram.end();
        }
    }

    /**
     * Billboard quad render, for planets, stars, sso and sets.
     */
    public void render(Entity entity, ExtShaderProgram shader, IntMesh mesh, ICamera camera) {
        view.setEntity(entity);
        float alpha = getAlpha(view.base.ct);

        // Render using consumer.
        view.billboard.renderConsumer.apply(renderSystem, view, alpha, shader, mesh, camera);
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        if (event == Event.BILLBOARD_TEXTURE_IDX_CMD) {
            GaiaSky.postRunnable(() -> setStarTexture(Settings.settings.scene.star.getStarTexture()));
        }
    }
}
