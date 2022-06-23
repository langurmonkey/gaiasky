package gaiasky.scene.system.render.draw;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.RenderGroup;
import gaiasky.render.api.IRenderable;
import gaiasky.render.system.AbstractRenderSystem;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.*;
import gaiasky.scene.entity.ParticleUtils;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.particle.IParticleRecord;
import gaiasky.util.Constants;
import gaiasky.util.Settings;
import gaiasky.util.comp.DistanceEntityComparator;
import gaiasky.util.gdx.mesh.IntMesh;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.math.Vector3d;

/**
 * Renders billboards (camera-oriented quads), optionally with a global texture. If a texture is not provided,
 * it is expected that the provided shader program does the rendering procedurally, or
 * the object itself binds its own texture.
 */
public class BillboardRenderer extends AbstractRenderSystem implements IObserver {

    private IntMesh mesh;
    private Texture billboardTexture;
    private final ComponentType componentType;

    // Render metadata
    protected float thpointTimesFovfactor;
    protected float thupOverFovfactor;
    protected float thdownOverFovfactor;
    protected float fovFactor;

    private final ParticleUtils utils;

    private final Vector3 F31;
    private final Vector3d D31;

    public BillboardRenderer(RenderGroup rg, float[] alphas, ExtShaderProgram[] programs, String texturePath, ComponentType componentType, float w, float h, boolean starTextureListener) {
        super(rg, alphas, programs);
        this.componentType = componentType;
        init(texturePath, w, h, starTextureListener);

        this.utils = new ParticleUtils();

        this.F31 = new Vector3();
        this.D31 = new Vector3d();

        initRenderAttributes();
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
    public BillboardRenderer(RenderGroup rg, float[] alphas, ExtShaderProgram[] shaderPrograms, String texturePath, ComponentType componentType, boolean starTextureListener) {
        this(rg, alphas, shaderPrograms, texturePath, componentType, 2, 2, starTextureListener);
    }

    private void initRenderAttributes() {
        if (GaiaSky.instance != null) {
            fovFactor = GaiaSky.instance.getCameraManager().getFovFactor();
        } else {
            fovFactor = 1f;
        }
        Settings settings = Settings.settings;
        thpointTimesFovfactor = (float) settings.scene.star.threshold.point;
        thupOverFovfactor = (float) Constants.THRESHOLD_UP / fovFactor;
        thdownOverFovfactor = (float) Constants.THRESHOLD_DOWN / fovFactor;
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
    public void renderStud(Array<IRenderable> renderables, ICamera camera, double t) {
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
        var base = Mapper.base.get(entity);
        var body = Mapper.body.get(entity);
        var graph = Mapper.graph.get(entity);
        var celestial = Mapper.celestial.get(entity);

        float alpha = getAlpha(base.ct);

        if (Mapper.tagBillboardGalaxy.has(entity)) {
            /*
             *  BILLBOARD GALAXIES
             */
            var scaffolding = Mapper.modelScaffolding.get(entity);
            float size = (float) (getRenderSizeBillboard(camera, body, scaffolding) / Constants.DISTANCE_SCALE_FACTOR);

            Vector3 aux = F31;
            shader.setUniformf("u_pos", graph.translation.put(aux));
            shader.setUniformf("u_size", size);

            shader.setUniformf("u_color", celestial.colorPale[0], celestial.colorPale[1], celestial.colorPale[2], alpha);
            shader.setUniformf("u_alpha", alpha * base.opacity);
            shader.setUniformf("u_distance", (float) body.distToCamera);
            shader.setUniformf("u_apparent_angle", (float) body.viewAngleApparent);
            shader.setUniformf("u_time", (float) GaiaSky.instance.getT() / 5f);

            shader.setUniformf("u_radius", size);

            // Render mesh
            mesh.render(shader, GL20.GL_TRIANGLES, 0, 6);

        } else if (Mapper.starSet.has(entity)) {
            /*
             *  STARS IN SETS
             */
            var set = Mapper.starSet.get(entity);
            var desc = Mapper.datasetDescription.get(entity);
            var highlight = Mapper.highlight.get(entity);

            // Star set
            double thPointTimesFovFactor = Settings.settings.scene.star.threshold.point * camera.getFovFactor();
            double thUpOverFovFactor = Constants.THRESHOLD_UP / camera.getFovFactor();
            double thDownOverFovFactor = Constants.THRESHOLD_DOWN / camera.getFovFactor();
            double innerRad = 0.006 + Settings.settings.scene.star.pointSize * 0.008;
            alpha = alpha * base.opacity;
            float fovFactor = camera.getFovFactor();

            // GENERAL UNIFORMS
            shader.setUniformf("u_thpoint", (float) thPointTimesFovFactor);
            // Light glow always disabled with star groups
            shader.setUniformi("u_lightScattering", 0);
            shader.setUniformf("u_inner_rad", (float) innerRad);

            // RENDER ACTUAL STARS
            boolean focusRendered = false;
            int n = Math.min(Settings.settings.scene.star.group.numBillboard, set.pointData.size());
            for (int i = 0; i < n; i++) {
                renderCloseupStar(set, highlight, desc, set.active[i], fovFactor, set.cPosD, shader, mesh, thPointTimesFovFactor, thUpOverFovFactor, thDownOverFovFactor, alpha);
                focusRendered = focusRendered || set.active[i] == set.focusIndex;
            }
            if (set.focus != null && !focusRendered) {
                renderCloseupStar(set, highlight, desc, set.focusIndex, fovFactor, set.cPosD, shader, mesh, thPointTimesFovFactor, thUpOverFovFactor, thDownOverFovFactor, alpha);
            }
        } else if (celestial != null) {
            /*
             *  REGULAR STARS, PLANETS, SATELLITES, BILLBOARDS and SSOs
             */
            var sa = Mapper.sa.get(entity);
            var extra = Mapper.extra.get(entity);
            var scaffolding = Mapper.modelScaffolding.get(entity);
            boolean isStar = Mapper.hip.has(entity);
            boolean isModel = !isStar && Mapper.model.has(entity);

            final float fuzzySize = getRenderSizeCelestial(camera, entity, body, sa, scaffolding, extra);
            final float radius = (float) (extra != null ? extra.radius : (body.size / 2d) * scaffolding.sizeScaleFactor);

            Vector3 billboardPosition = graph.translation.put(F31);
            if (isModel) {
                // Bring it a tad closer to the camera to prevent occlusion with orbit.
                // Only for models.
                float len = billboardPosition.len();
                billboardPosition.nor().scl(len * 0.99f);
            }
            shader.setUniformf("u_pos", billboardPosition);
            shader.setUniformf("u_size", fuzzySize);

            // Models use the regular color
            float[] color = isModel ? body.color : celestial.colorPale;

            // Alpha channel:
            // - particles: alpha * opacity
            // - models:    alpha * (1 - fadeOpacity)
            float a = scaffolding != null ? alpha * (1f - scaffolding.fadeOpacity) : alpha * base.opacity;
            shader.setUniformf("u_color", color[0], color[1], color[2], a);
            shader.setUniformf("u_inner_rad", (float) celestial.innerRad);
            shader.setUniformf("u_distance", (float) body.distToCamera);
            shader.setUniformf("u_apparent_angle", (float) body.viewAngleApparent);
            shader.setUniformf("u_thpoint", (float) sa.thresholdPoint * camera.getFovFactor());

            // Whether light scattering is enabled or not
            shader.setUniformi("u_lightScattering", (isStar && GaiaSky.instance.getPostProcessor().isLightScatterEnabled()) ? 1 : 0);

            shader.setUniformf("u_radius", radius);

            // Render the mesh
            mesh.render(shader, GL20.GL_TRIANGLES, 0, 6);
        } else if(Mapper.cluster.has(entity)) {
            /*
             * STAR CLUSTERS
             */
            var cluster = Mapper.cluster.get(entity);
            // Bind texture
            if (cluster.clusterTex != null) {
                cluster.clusterTex.bind(0);
                shader.setUniformi("u_texture0", 0);
            }

            float fa = (1 - cluster.fadeAlpha) * 0.6f;

            Vector3 aux = F31;
            shader.setUniformf("u_pos", graph.translation.put(aux));
            shader.setUniformf("u_size", body.size);
            shader.setUniformf("u_color", body.color[0] * fa, body.color[1] * fa, body.color[2] * fa, body.color[3] * alpha * base.opacity * 6.5f);
            // Sprite.render
            mesh.render(shader, GL20.GL_TRIANGLES, 0, 6);
        }
    }

    public float getRenderSizeBillboard(ICamera camera, Body body, ModelScaffolding scaffolding) {
        return body.size * Settings.settings.scene.star.brightness * scaffolding.billboardSizeFactor;
    }

    public float getRenderSizeCelestial(ICamera camera, Entity entity, Body body, SolidAngle sa, ModelScaffolding scaffolding, ParticleExtra extra) {
        if (extra != null) {
            // Stars, particles
            extra.computedSize = body.size;
            if (body.viewAngle > thdownOverFovfactor) {
                double dist = body.distToCamera;
                if (body.viewAngle > thupOverFovfactor) {
                    dist = (float) extra.radius / Constants.THRESHOLD_UP;
                }
                extra.computedSize *= (dist / extra.radius) * Constants.THRESHOLD_DOWN;
            }

            extra.computedSize *= Settings.settings.scene.star.pointSize * 0.2f;
            return (float) ((extra.computedSize / Constants.DISTANCE_SCALE_FACTOR) * extra.primitiveRenderScale);
        } else if(Mapper.fade.has(entity)) {
            // Regular billboards
            return getRenderSizeBillboard(camera, body, scaffolding);
        } else {
            // Models
            float thAngleQuad = (float) sa.thresholdQuad * camera.getFovFactor();
            double size = 0f;
            if (body.viewAngle >= sa.thresholdPoint * camera.getFovFactor()) {
                size = Math.tan(thAngleQuad) * body.distToCamera * scaffolding.billboardSizeFactor;
            }
            return (float) size;
        }
    }

    private final Color c = new Color();

    private void renderCloseupStar(StarSet set, Highlight highlight, DatasetDescription desc, int idx, float fovFactor, Vector3d cPosD, ExtShaderProgram shader, IntMesh mesh, double thPointTimesFovFactor, double thUpOverFovFactor, double thDownOverFovFactor, float alpha) {
        if (utils.filter(idx, set, desc) && set.isVisible(idx)) {
            IParticleRecord star = set.pointData.get(idx);
            double varScl = utils.getVariableSizeScaling(set, idx);

            double sizeOriginal = set.getSize(idx);
            double size = sizeOriginal * varScl;
            double radius = size * Constants.STAR_SIZE_FACTOR;
            Vector3d starPos = set.fetchPosition(star, cPosD, D31, set.currDeltaYears);
            double distToCamera = starPos.len();
            double viewAngle = (sizeOriginal * Constants.STAR_SIZE_FACTOR / distToCamera) / fovFactor;

            Color.abgr8888ToColor(c, utils.getColor(idx, set, highlight));
            if (viewAngle >= thPointTimesFovFactor) {
                double fuzzySize = getRenderSizeStarSet(sizeOriginal, radius, distToCamera, viewAngle, thDownOverFovFactor, thUpOverFovFactor);

                Vector3 pos = starPos.put(F31);
                shader.setUniformf("u_pos", pos);
                shader.setUniformf("u_size", (float) fuzzySize);

                shader.setUniformf("u_color", c.r, c.g, c.b, alpha);
                shader.setUniformf("u_distance", (float) distToCamera);
                shader.setUniformf("u_apparent_angle", (float) (viewAngle * Settings.settings.scene.star.pointSize));
                shader.setUniformf("u_radius", (float) radius);

                // Sprite.render
                mesh.render(shader, GL20.GL_TRIANGLES, 0, 6);

            }
        }
    }

    public double getRenderSizeStarSet(double size, double radius, double distToCamera, double viewAngle, double thDown, double thUp) {
        double computedSize = size;
        if (viewAngle > thDown) {
            double dist = distToCamera;
            if (viewAngle > thUp) {
                dist = radius / Constants.THRESHOLD_UP;
            }
            computedSize = (size * (dist / radius) * Constants.THRESHOLD_DOWN);
        }
        // Change the factor at the end here to control the stray light of stars
        computedSize *= Settings.settings.scene.star.pointSize * 0.4;

        return computedSize;
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        if (event == Event.BILLBOARD_TEXTURE_IDX_CMD) {
            GaiaSky.postRunnable(() -> setStarTexture(Settings.settings.scene.star.getStarTexture()));
        } else if (event == Event.FOV_CHANGE_NOTIFICATION) {
            fovFactor = (Float) data[1];
            thupOverFovfactor = (float) Constants.THRESHOLD_UP / fovFactor;
            thdownOverFovfactor = (float) Constants.THRESHOLD_DOWN / fovFactor;
        }
    }
}
