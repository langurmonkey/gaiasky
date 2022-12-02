/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer.FrameBufferBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.RenderAssets;
import gaiasky.render.RenderGroup;
import gaiasky.render.RenderingContext;
import gaiasky.render.api.IPostProcessor.PostProcessBean;
import gaiasky.render.api.IRenderMode;
import gaiasky.render.api.IRenderable;
import gaiasky.render.api.ISceneRenderer;
import gaiasky.render.process.*;
import gaiasky.render.system.AbstractRenderSystem;
import gaiasky.render.system.AbstractRenderSystem.RenderSystemRunnable;
import gaiasky.render.system.IRenderSystem;
import gaiasky.render.system.LightPositionUpdater;
import gaiasky.render.system.LineRenderSystem;
import gaiasky.scene.Mapper;
import gaiasky.scene.camera.CameraManager.CameraMode;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.Render;
import gaiasky.scene.entity.EntityUtils;
import gaiasky.scene.system.render.draw.*;
import gaiasky.scene.system.render.draw.model.ModelEntityRenderSystem;
import gaiasky.util.Constants;
import gaiasky.util.GlobalResources;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.Settings.PointCloudMode;
import gaiasky.util.gdx.IntModelBatch;
import gaiasky.util.gdx.contrib.postprocess.utils.PingPongBuffer;
import gaiasky.util.gdx.contrib.utils.GaiaSkyFrameBuffer;
import gaiasky.util.math.Intersectord;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;
import gaiasky.vr.openvr.VRContext;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL40;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static gaiasky.render.RenderGroup.*;

/**
 * Initializes the render infrastructure renders the scene using different render systems.
 */
public class SceneRenderer implements ISceneRenderer, IObserver {
    private static final Log logger = Logger.getLogger(SceneRenderer.class);

    /**
     * Contains the flags representing each type's visibility
     **/
    public ComponentTypes visible;
    /**
     * Contains the last update time of each of the flags
     **/
    public long[] times;
    /**
     * Alpha values for each type.
     **/
    public float[] alphas;

    /**
     * Render lists for all render groups.
     * The front render lists contain the objects which are actually rendered in the current cycle. The back
     * render lists get updated by the update thread.
     **/
    private List<List<IRenderable>> renderLists;

    private Array<IRenderSystem> renderSystems;

    private RenderSystemRunnable depthTestR, additiveBlendR, noDepthTestR, regularBlendR, depthTestNoWritesR, noDepthWritesR, depthWritesR, clearDepthR;

    /**
     * The particular current scene graph renderer
     **/
    private IRenderMode renderMode;
    /**
     * Renderers vector, with 0 = normal, 1 = stereoscopic, 2 = FOV, 3 = cubemap
     **/
    private IRenderMode[] sgrList;
    // Indexes
    private final int SGR_DEFAULT_IDX = 0, SGR_STEREO_IDX = 1, SGR_FOV_IDX = 2, SGR_CUBEMAP_IDX = 3, SGR_OPENVR_IDX = 4;

    // Camera at light position, with same direction. For shadow mapping
    private Camera cameraLight;

    /** Contains the candidates for regular and tessellated shadow maps. **/
    private List<Entity> shadowCandidates/*, shadowCandidatesTess */;

    // Dimension 1: number of shadows, dimension 2: number of lights
    public FrameBuffer[][] shadowMapFb;

    // Dimension 1: number of shadows, dimension 2: number of lights
    private Matrix4[][] shadowMapCombined;

    /** Map containing the shadow map for each model body. **/
    public Map<Entity, Texture> smTexMap;

    /** Map containing the combined matrix for each model body. **/
    public Map<Entity, Matrix4> smCombinedMap;

    /** Contains the code to render models. **/
    private final ModelEntityRenderSystem shadowModelRenderer;

    // Light glow pre-render
    private FrameBuffer glowFb;
    private LightPositionUpdater lpu;
    /**
     * Frame buffer map. Holds frame buffers for different resolutions, usually used
     * in screenshots and frame capture.
     */
    private Map<Integer, FrameBuffer> frameBufferMap;

    private Vector3 aux1;
    private Vector3d aux1d, aux2d, aux3d;
    private Vector3b aux1b;

    // VRContext, may be null
    private final VRContext vrContext;
    private final GlobalResources globalResources;

    private List<IRenderable> stars;

    private AbstractRenderSystem billboardStarsProc;

    private final AtomicBoolean rendering;

    private final RenderAssets renderAssets;

    public SceneRenderer(final VRContext vrContext, final GlobalResources globalResources) {
        super();
        this.vrContext = vrContext;
        this.globalResources = globalResources;
        this.rendering = new AtomicBoolean(false);
        this.renderAssets = new RenderAssets(globalResources);
        this.shadowModelRenderer = new ModelEntityRenderSystem(this);
    }

    @Override
    public void initialize(AssetManager manager) {
        // Frame buffer map
        frameBufferMap = new HashMap<>();
        // Initialize the render assets.
        renderAssets.initialize(manager);

        stars = new ArrayList<>();

        renderSystems = new Array<>();

        // Initialize the runnables.
        noDepthTestR = (renderSystem, renderList, camera) -> {
            Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
            Gdx.gl.glDepthMask(false);
        };
        depthTestR = (renderSystem, renderList, camera) -> {
            Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
            Gdx.gl.glDepthMask(true);
        };
        depthTestNoWritesR = (renderSystem, renderList, camera) -> {
            Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
            Gdx.gl.glDepthMask(false);
        };
        noDepthWritesR = (renderSystem, renderList, camera) -> Gdx.gl.glDepthMask(false);
        depthWritesR = (renderSystem, renderList, camera) -> Gdx.gl.glDepthMask(true);
        additiveBlendR = (renderSystem, renderList, camera) -> {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            GL40.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE);
        };
        regularBlendR = (renderSystem, renderList, camera) -> {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            GL40.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        };
        clearDepthR = (renderSystem, renderList, camera) -> Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);

        if (Settings.settings.scene.renderer.shadow.active) {
            // Shadow map camera
            cameraLight = new PerspectiveCamera(0.5f, Settings.settings.scene.renderer.shadow.resolution, Settings.settings.scene.renderer.shadow.resolution);

            // Aux vectors
            aux1 = new Vector3();
            aux1d = new Vector3d();
            aux2d = new Vector3d();
            aux3d = new Vector3d();
            aux1b = new Vector3b();

            // Build frame buffers and arrays
            buildShadowMapData();
        }

        if (Settings.settings.postprocess.lightGlow.active) {
            buildGlowData();
        }
    }

    public void doneLoading(final AssetManager manager) {
        // Prepare render assets
        renderAssets.doneLoading(manager);

        // Initialize render lists
        RenderGroup[] renderGroups = values();
        renderLists = (new ArrayList<>(renderGroups.length));
        for (int i = 0; i < renderGroups.length; i++) {
            renderLists.add((new ArrayList<>(20)));
        }

        // Set reference
        visible = new ComponentTypes();
        final ComponentType[] types = ComponentType.values();
        for (int i = 0; i < Settings.settings.scene.visibility.size(); i++) {
            if (Settings.settings.scene.visibility.get(types[i].toString())) {
                visible.set(ComponentType.values()[i].ordinal());
            }
        }
        // Invisible are always visible :_D
        visible.set(ComponentType.Invisible.ordinal());

        ComponentType[] comps = ComponentType.values();
        times = new long[comps.length];
        alphas = new float[comps.length];
        for (int i = 0; i < comps.length; i++) {
            times[i] = -20000L;
            alphas[i] = 0f;
        }

        /*
         * INITIALIZE SGRs
         */
        sgrList = new IRenderMode[5];
        sgrList[SGR_DEFAULT_IDX] = new RenderModeMain();
        sgrList[SGR_STEREO_IDX] = new RenderModeStereoscopic(globalResources.getSpriteBatch());
        sgrList[SGR_FOV_IDX] = new RenderModeFov();
        sgrList[SGR_CUBEMAP_IDX] = new RenderModeCubemapProjections();
        sgrList[SGR_OPENVR_IDX] = new RenderModeOpenVR(GaiaSky.instance.scene, vrContext, globalResources.getSpriteBatchVR());
        renderMode = null;

        /*
         * ======= INITIALIZE RENDER SYSTEMS =======
         */
        final PointCloudMode pcm = Settings.settings.scene.renderer.pointCloud;

        // SINGLE STAR POINTS
        AbstractRenderSystem singlePointProc = new SinglePointRenderer(this, POINT_STAR, alphas, renderAssets.starPointShaders, ComponentType.Stars);
        singlePointProc.addPreRunnables(additiveBlendR, noDepthTestR);

        // SKYBOX - (MW panorama, CMWB)
        AbstractRenderSystem skyboxProc = new ModelRenderer(this, SKYBOX, alphas, renderAssets.mbSkybox);

        // MODEL BACKGROUND - (MW panorama, CMWB)
        AbstractRenderSystem modelBackgroundProc = new ModelRenderer(this, MODEL_BG, alphas, renderAssets.mbVertexDiffuse);

        // MODEL GRID - (Ecl, Eq, Gal grids)
        AbstractRenderSystem modelGridsProc = new ModelRenderer(this, MODEL_VERT_GRID, alphas, renderAssets.mbVertexLightingGrid);
        modelGridsProc.addPostRunnables(clearDepthR);

        // RECURSIVE GRID
        AbstractRenderSystem modelRecGridProc = new ModelRenderer(this, MODEL_VERT_RECGRID, alphas, renderAssets.mbVertexLightingRecGrid);
        modelRecGridProc.addPreRunnables(regularBlendR, depthTestR);

        // ANNOTATIONS - (grids)
        AbstractRenderSystem annotationsProc = new TextRenderer(this, FONT_ANNOTATION, alphas, renderAssets.spriteBatch, null, null, renderAssets.font2d, null);
        annotationsProc.addPreRunnables(regularBlendR, noDepthTestR);
        annotationsProc.addPostRunnables(clearDepthR);

        // BILLBOARD STARS
        billboardStarsProc = new BillboardRenderer(this, BILLBOARD_STAR, alphas, renderAssets.starBillboardShaders, Settings.settings.scene.star.getStarTexture(), ComponentType.Stars, true);
        billboardStarsProc.addPreRunnables(additiveBlendR, noDepthTestR);
        lpu = new LightPositionUpdater();
        billboardStarsProc.addPostRunnables(lpu);

        // BILLBOARD GALAXIES
        AbstractRenderSystem billboardGalaxiesProc = new BillboardRenderer(this, BILLBOARD_GAL, alphas, renderAssets.galShaders, Constants.DATA_LOCATION_TOKEN + "tex/base/static.jpg", ComponentType.Galaxies, false);
        billboardGalaxiesProc.addPreRunnables(additiveBlendR, depthTestR, noDepthWritesR);

        // BILLBOARD SPRITES
        AbstractRenderSystem billboardSpritesProc = new BillboardRenderer(this, BILLBOARD_SPRITE, alphas, renderAssets.spriteShaders, null, ComponentType.Clusters, false);
        billboardSpritesProc.addPreRunnables(additiveBlendR, depthTestNoWritesR);

        // LINES CPU
        AbstractRenderSystem lineProc = getLineRenderSystem();

        // LINES GPU
        AbstractRenderSystem lineGpuProc = new PrimitiveVertexRenderSystem<>(this, LINE_GPU, alphas, renderAssets.lineGpuShaders, true);
        lineGpuProc.addPreRunnables(additiveBlendR, depthTestR, noDepthWritesR);

        // POINTS CPU
        AbstractRenderSystem pointProc = new PointPrimitiveRenderSystem(this, POINT, alphas, renderAssets.pointShaders);

        // POINTS GPU
        AbstractRenderSystem pointGpuProc = new PrimitiveVertexRenderSystem<>(this, POINT_GPU, alphas, renderAssets.lineGpuShaders, false);
        pointGpuProc.addPreRunnables(regularBlendR, depthTestR);

        // MODELS DUST AND MESH
        AbstractRenderSystem modelMeshOpaqueProc = new ModelRenderer(this, MODEL_PIX_DUST, alphas, renderAssets.mbPixelLightingDust);
        AbstractRenderSystem modelMeshAdditiveProc = new ModelRenderer(this, MODEL_VERT_ADDITIVE, alphas, renderAssets.mbVertexLightingAdditive);
        // MODEL PER-PIXEL-LIGHTING EARLY
        AbstractRenderSystem modelPerPixelLightingEarly = new ModelRenderer(this, MODEL_PIX_EARLY, alphas, renderAssets.mbPixelLighting);
        // MODEL PER-VERTEX-LIGHTING EARLY
        AbstractRenderSystem modelPerVertexLightingEarly = new ModelRenderer(this, MODEL_VERT_EARLY, alphas, renderAssets.mbVertexLighting);

        // MODEL DIFFUSE
        AbstractRenderSystem modelMeshDiffuse = new ModelRenderer(this, MODEL_DIFFUSE, alphas, renderAssets.mbVertexDiffuse);

        // MODEL PER-PIXEL-LIGHTING
        AbstractRenderSystem modelPerPixelLighting = new ModelRenderer(this, MODEL_PIX, alphas, renderAssets.mbPixelLighting);

        // MODEL PER-PIXEL-LIGHTING-TESSELLATION
        AbstractRenderSystem modelPerPixelLightingTess = new TessellationRenderer(this, MODEL_PIX_TESS, alphas, renderAssets.mbPixelLightingTessellation);
        modelPerPixelLightingTess.addPreRunnables(regularBlendR, depthTestR);

        // MODEL BEAM
        AbstractRenderSystem modelBeamProc = new ModelRenderer(this, MODEL_VERT_BEAM, alphas, renderAssets.mbVertexLightingBeam);

        // GALAXY
        AbstractRenderSystem billboardSetProc = new BillboardSetRenderer(this, BILLBOARD_GROUP, alphas, renderAssets.billboardGroupShaders);

        // PARTICLE GROUP
        AbstractRenderSystem particleGroupProc = switch (pcm) {
            case TRIANGLES -> new ParticleSetRenderer(this, PARTICLE_GROUP, alphas, renderAssets.particleGroupShaders);
            case TRIANGLES_INSTANCED -> new ParticleSetInstancedRenderer(this, PARTICLE_GROUP, alphas, renderAssets.particleGroupShaders);
            case POINTS -> new ParticleSetPointRenderer(this, PARTICLE_GROUP, alphas, renderAssets.particleGroupShaders);
        };
        particleGroupProc.addPreRunnables(additiveBlendR, depthTestR, noDepthWritesR);
        particleGroupProc.addPostRunnables(regularBlendR, depthWritesR);

        // STAR GROUP
        AbstractRenderSystem starGroupProc = switch (pcm) {
            case TRIANGLES -> new StarSetRenderer(this, STAR_GROUP, alphas, renderAssets.starGroupShaders);
            case TRIANGLES_INSTANCED -> new StarSetInstancedRenderer(this, STAR_GROUP, alphas, renderAssets.starGroupShaders);
            case POINTS -> new StarSetPointRenderer(this, STAR_GROUP, alphas, renderAssets.starGroupShaders);
        };
        starGroupProc.addPreRunnables(additiveBlendR, depthTestR, noDepthWritesR);
        starGroupProc.addPostRunnables(regularBlendR, depthWritesR);

        // VARIABLE GROUP
        AbstractRenderSystem variableGroupProc = switch (pcm) {
            case TRIANGLES -> new VariableSetRenderer(this, VARIABLE_GROUP, alphas, renderAssets.variableGroupShaders);
            case TRIANGLES_INSTANCED -> new VariableSetInstancedRenderer(this, VARIABLE_GROUP, alphas, renderAssets.variableGroupShaders);
            case POINTS -> new VariableSetPointRenderer(this, VARIABLE_GROUP, alphas, renderAssets.variableGroupShaders);
        };
        variableGroupProc.addPreRunnables(additiveBlendR, depthTestR, noDepthWritesR);
        variableGroupProc.addPostRunnables(regularBlendR, depthWritesR);

        // ORBITAL ELEMENTS PARTICLES
        AbstractRenderSystem elementsProc = new ElementsRenderer(this, ORBITAL_ELEMENTS_PARTICLE, alphas, renderAssets.orbitElemShaders);
        elementsProc.addPreRunnables(additiveBlendR, depthTestR, noDepthWritesR);
        elementsProc.addPostRunnables(regularBlendR, depthWritesR);

        // ORBITAL ELEMENTS GROUP
        AbstractRenderSystem elementsSetProc = new ElementsSetRenderer(this, ORBITAL_ELEMENTS_GROUP, alphas, renderAssets.orbitElemShaders);
        elementsSetProc.addPreRunnables(additiveBlendR, depthTestR, noDepthWritesR);
        elementsSetProc.addPostRunnables(regularBlendR, depthWritesR);

        // MODEL STARS
        AbstractRenderSystem modelStarsProc = new ModelRenderer(this, MODEL_VERT_STAR, alphas, renderAssets.mbVertexLightingStarSurface);

        // LABELS
        AbstractRenderSystem labelsProc = new TextRenderer(this, FONT_LABEL, alphas, renderAssets.fontBatch, renderAssets.distanceFieldFontShader, renderAssets.font3d, renderAssets.font2d, renderAssets.fontTitles);

        // BILLBOARD SSO
        AbstractRenderSystem billboardSSOProc = new BillboardRenderer(this, BILLBOARD_SSO, alphas, renderAssets.starBillboardShaders, Constants.DATA_LOCATION_TOKEN + "tex/base/sso.png", null, false);
        billboardSSOProc.addPreRunnables(additiveBlendR, depthTestNoWritesR);

        // MODEL ATMOSPHERE
        AbstractRenderSystem modelAtmProc = new ModelRenderer(this, MODEL_ATM, alphas, renderAssets.mbAtmosphere) {
            @Override
            public float getAlpha(IRenderable s) {
                return alphas[ComponentType.Atmospheres.ordinal()] * (float) Math.pow(alphas[s.getComponentType().getFirstOrdinal()], 2);
            }

            @Override
            protected boolean mustRender() {
                return alphas[ComponentType.Atmospheres.ordinal()] * alphas[ComponentType.Planets.ordinal()] > 0;
            }
        };

        // MODEL CLOUDS
        AbstractRenderSystem modelCloudProc = new ModelRenderer(this, MODEL_CLOUD, alphas, renderAssets.mbCloud);

        // PARTICLE EFFECTS
        AbstractRenderSystem particleEffectsProc = new ParticleEffectsRenderer(this, null, alphas, renderAssets.particleEffectShaders);
        particleEffectsProc.addPreRunnables(additiveBlendR, noDepthTestR);
        particleEffectsProc.addPostRunnables(regularBlendR);

        // SHAPES
        AbstractRenderSystem shapeProc = new ShapeRenderer(this, SHAPE, alphas, globalResources.getShapeShader());
        shapeProc.addPreRunnables(regularBlendR, depthTestR);


        /* ===============================
         * ADD RENDER SYSTEMS TO PROCESSOR
         * =============================== */

        // Background stuff
        addRenderSystem(skyboxProc);
        addRenderSystem(modelBackgroundProc);
        addRenderSystem(modelGridsProc);
        addRenderSystem(singlePointProc);
        addRenderSystem(annotationsProc);

        // Opaque meshes
        addRenderSystem(modelMeshOpaqueProc);
        addRenderSystem(modelPerPixelLightingEarly);
        addRenderSystem(modelPerVertexLightingEarly);

        // Billboard groups
        addRenderSystem(billboardSetProc);

        // Billboard stars
        addRenderSystem(billboardStarsProc);

        // Star and particle sets
        addRenderSystem(particleGroupProc);
        addRenderSystem(starGroupProc);
        addRenderSystem(variableGroupProc);
        addRenderSystem(elementsProc);
        addRenderSystem(elementsSetProc);

        // Diffuse meshes
        addRenderSystem(modelMeshDiffuse);

        // Generic per-pixel lighting models
        addRenderSystem(modelPerPixelLighting);
        addRenderSystem(modelPerPixelLightingTess);
        addRenderSystem(modelBeamProc);
        addRenderSystem(modelStarsProc);

        // Labels
        addRenderSystem(labelsProc);

        // Galaxy & nebulae billboards, recursive grid
        addRenderSystem(billboardSpritesProc);
        addRenderSystem(billboardGalaxiesProc);
        addRenderSystem(modelRecGridProc);

        // Primitives
        addRenderSystem(pointProc);
        addRenderSystem(pointGpuProc);

        // Lines
        addRenderSystem(lineProc);
        addRenderSystem(lineGpuProc);

        // Billboards SSO
        addRenderSystem(billboardSSOProc);

        // Special models
        addRenderSystem(modelAtmProc);
        addRenderSystem(modelCloudProc);

        addRenderSystem(shapeProc);
        addRenderSystem(particleEffectsProc);

        // Additive meshes
        addRenderSystem(modelMeshAdditiveProc);

        // INIT GL STATE
        GL30.glClampColor(GL30.GL_CLAMP_READ_COLOR, GL30.GL_FALSE);
        GL30.glClampColor(GL30.GL_CLAMP_VERTEX_COLOR, GL30.GL_FALSE);
        GL30.glClampColor(GL30.GL_CLAMP_FRAGMENT_COLOR, GL30.GL_FALSE);

        // Enable sRGB if needed.
        if (Settings.settings.graphics.useSRGB && !Settings.settings.program.safeMode) {
            Gdx.gl.glEnable(GL30.GL_FRAMEBUFFER_SRGB);
        }

        EventManager.instance.subscribe(this, Event.TOGGLE_VISIBILITY_CMD, Event.LINE_RENDERER_UPDATE, Event.STEREOSCOPIC_CMD, Event.CAMERA_MODE_CMD, Event.CUBEMAP_CMD, Event.REBUILD_SHADOW_MAP_DATA_CMD, Event.LIGHT_GLOW_CMD);

    }

    public synchronized void setRendering(boolean rendering) {
        this.rendering.set(rendering);
    }

    public List<List<IRenderable>> renderListsFront() {
        return renderLists;
    }

    public List<List<IRenderable>> renderListsBack() {
        return renderLists;
    }

    private void addRenderSystem(IRenderSystem renderSystem) {
        if (!renderSystems.contains(renderSystem, true)) {
            renderSystems.add(renderSystem);
        }
    }

    private void initRenderMode(ICamera camera) {
        if (Settings.settings.runtime.openVr) {
            // Using Steam OpenVR renderer
            renderMode = sgrList[SGR_OPENVR_IDX];
        } else if (camera.getNCameras() > 1) {
            // FOV mode
            renderMode = sgrList[SGR_FOV_IDX];
        } else if (Settings.settings.program.modeStereo.active) {
            // Stereoscopic mode
            renderMode = sgrList[SGR_STEREO_IDX];
        } else if (Settings.settings.program.modeCubemap.active) {
            // 360 mode: cube map -> equirectangular map
            renderMode = sgrList[SGR_CUBEMAP_IDX];
        } else {
            // Default mode
            renderMode = sgrList[SGR_DEFAULT_IDX];
        }
    }

    Array<Entity> controllers = new Array<>();
    ModelEntityRenderSystem renderObject = new ModelEntityRenderSystem(this);

    private void renderModel(IRenderable r, IntModelBatch batch, float alpha, boolean shadow) {
        if (r instanceof Render) {
            Render render = (Render) r;
            if (Mapper.model.has(render.entity)) {
                renderObject.renderOpaque(render.entity, batch, alpha, shadow);
            }
        }

    }

    public void renderGlowPass(ICamera camera, FrameBuffer frameBuffer) {
        if (frameBuffer == null) {
            frameBuffer = glowFb;
        }
        if (Settings.settings.postprocess.lightGlow.active && frameBuffer != null) {
            // Get all billboard stars
            List<IRenderable> billboardStars = renderLists.get(BILLBOARD_STAR.ordinal());

            stars.clear();
            for (IRenderable st : billboardStars) {
                if (st instanceof Render) {
                    if (Mapper.hip.has(((Render) st).getEntity())) {
                        stars.add(st);
                    }
                }
            }

            // Get all models
            List<IRenderable> models = renderLists.get(MODEL_PIX.ordinal());
            List<IRenderable> modelsTess = renderLists.get(MODEL_PIX_TESS.ordinal());

            // VR controllers
            if (Settings.settings.runtime.openVr) {
                RenderModeOpenVR sgrVR = (RenderModeOpenVR) sgrList[SGR_OPENVR_IDX];
                if (vrContext != null) {
                    for (Entity m : sgrVR.controllerObjects) {
                        var render = Mapper.render.get(m);
                        if (!models.contains(render))
                            controllers.add(m);
                    }
                }
            }

            frameBuffer.begin();
            Gdx.gl.glEnable(GL30.GL_DEPTH_TEST);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

            // Render billboard stars
            billboardStarsProc.render(stars, camera, 0, null);

            // Render models
            renderAssets.mbPixelLightingOpaque.begin(camera.getCamera());
            for (IRenderable model : models) {
                renderModel(model, renderAssets.mbPixelLightingOpaque, 1, false);
            }
            renderAssets.mbPixelLightingOpaque.end();

            // Render tessellated models
            if (modelsTess.size() > 0) {
                renderAssets.mbPixelLightingOpaqueTessellation.begin(camera.getCamera());
                for (IRenderable model : modelsTess) {
                    renderModel(model, renderAssets.mbPixelLightingOpaqueTessellation, 1, false);
                }
                renderAssets.mbPixelLightingOpaqueTessellation.end();
            }
            //}

            // Set texture to updater
            if (lpu != null) {
                lpu.setGlowTexture(frameBuffer.getColorBufferTexture());
            }

            frameBuffer.end();

        }

    }

    private void addCandidates(List<IRenderable> models, List<Entity> candidates) {
        if (candidates != null) {
            candidates.clear();
            int num = 0;
            for (int i = 0; i < models.size(); i++) {
                Render render = (Render) models.get(i);
                var scaffolding = Mapper.modelScaffolding.get(render.entity);
                if (scaffolding != null) {
                    if (scaffolding.isShadow()) {
                        candidates.add(num, render.entity);
                        scaffolding.shadow = 0;
                        num++;
                        if (num == Settings.settings.scene.renderer.shadow.number)
                            break;
                    }
                }
            }
        }
    }

    public void clearScreen() {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
    }

    private void renderShadowMapCandidates(List<Entity> candidates, int shadowNRender, ICamera camera) {
        int i = 0;
        int j = 0;
        // Normal bodies
        for (Entity candidate : candidates) {
            var body = Mapper.body.get(candidate);
            var model = Mapper.model.get(candidate);
            var scaffolding = Mapper.modelScaffolding.get(candidate);

            Vector3 camDir = aux1.set(model.model.directional(0).direction);
            // Direction is that of the light
            cameraLight.direction.set(camDir);

            double radius = (body.size / 2.0) * scaffolding.sizeScaleFactor;
            // Distance from camera to object, radius * sv[0]
            double distance = radius * scaffolding.shadowMapValues[0];
            // Position, factor of radius
            Vector3b objPos = EntityUtils.getAbsolutePosition(candidate, aux1b);
            objPos.sub(camera.getPos()).sub(camDir.nor().scl((float) distance));
            objPos.put(cameraLight.position);
            // Up is perpendicular to dir
            if (cameraLight.direction.y != 0 || cameraLight.direction.z != 0)
                aux1.set(1, 0, 0);
            else
                aux1.set(0, 1, 0);
            cameraLight.up.set(cameraLight.direction).crs(aux1);

            // Near is sv[1]*radius before the object
            cameraLight.near = (float) (distance - radius * scaffolding.shadowMapValues[1]);
            // Far is sv[2]*radius after the object
            cameraLight.far = (float) (distance + radius * scaffolding.shadowMapValues[2]);

            // Update cam
            cameraLight.update(false);

            // Render model depth map to frame buffer
            shadowMapFb[i][j].begin();
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

            // No tessellation
            renderAssets.mbPixelLightingDepth.begin(cameraLight);
            shadowModelRenderer.render(candidate, renderAssets.mbPixelLightingDepth, camera, 1, 0, null, RenderGroup.MODEL_PIX, true);
            renderAssets.mbPixelLightingDepth.end();

            // Save frame buffer and combined matrix
            scaffolding.shadow = shadowNRender;
            shadowMapCombined[i][j].set(cameraLight.combined);
            smCombinedMap.put(candidate, shadowMapCombined[i][j]);
            smTexMap.put(candidate, shadowMapFb[i][j].getColorBufferTexture());

            shadowMapFb[i][j].end();
            i++;
        }
    }

    private void renderShadowMapCandidatesTess(Array<Entity> candidates, int shadowNRender, ICamera camera, RenderingContext rc) {
        int i = 0;
        int j = 0;
        // Normal bodies
        for (Entity candidate : candidates) {
            var body = Mapper.body.get(candidate);
            var model = Mapper.model.get(candidate);
            var scaffolding = Mapper.modelScaffolding.get(candidate);

            double radius = (body.size / 2.0) * scaffolding.sizeScaleFactor;
            // Only render when camera very close to surface
            if (body.distToCamera < radius * 1.1) {
                scaffolding.shadow = shadowNRender;

                Vector3 shadowCameraDir = aux1.set(model.model.directional(0).direction);

                // Shadow camera direction is that of the light
                cameraLight.direction.set(shadowCameraDir);

                Vector3 shadowCamDir = aux1.set(model.model.directional(0).direction);
                // Direction is that of the light
                cameraLight.direction.set(shadowCamDir);

                // Distance from camera to object, radius * sv[0]
                float distance = (float) (radius * scaffolding.shadowMapValues[0] * 0.01);
                // Position, factor of radius
                Vector3b objPos = EntityUtils.getAbsolutePosition(candidate, aux1b);
                Vector3b camPos = camera.getPos();
                Vector3d camDir = aux3d.set(camera.getDirection()).nor().scl(100 * Constants.KM_TO_U);
                boolean intersect = Intersectord.checkIntersectSegmentSphere(camPos.tov3d(), aux3d.set(camPos).add(camDir), objPos.put(aux1d), radius);
                if (intersect) {
                    // Use height
                    camDir.nor().scl(body.distToCamera - radius);
                }
                Vector3d objCam = aux2d.set(camPos).sub(objPos).nor().scl(-(body.distToCamera - radius)).add(camDir);

                objCam.add(shadowCamDir.nor().scl(-distance));
                objCam.put(cameraLight.position);

                // Shadow camera up is perpendicular to dir
                if (cameraLight.direction.y != 0 || cameraLight.direction.z != 0)
                    aux1.set(1, 0, 0);
                else
                    aux1.set(0, 1, 0);
                cameraLight.up.set(cameraLight.direction).crs(aux1);

                // Near is sv[1]*radius before the object
                cameraLight.near = distance * 0.98f;
                // Far is sv[2]*radius after the object
                cameraLight.far = distance * 1.02f;

                // Update cam
                cameraLight.update(false);

                // Render model depth map to frame buffer
                shadowMapFb[i][j].begin();
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

                // Tessellation
                renderAssets.mbPixelLightingDepthTessellation.begin(cameraLight);
                shadowModelRenderer.render(candidate, renderAssets.mbPixelLightingDepthTessellation, camera, 1, 0, rc, RenderGroup.MODEL_PIX, true);
                renderAssets.mbPixelLightingDepthTessellation.end();

                // Save frame buffer and combined matrix
                scaffolding.shadow = shadowNRender;
                shadowMapCombined[i][j].set(cameraLight.combined);
                smCombinedMap.put(candidate, shadowMapCombined[i][j]);
                smTexMap.put(candidate, shadowMapFb[i][j].getColorBufferTexture());

                shadowMapFb[i][j].end();
                i++;
            } else {
                scaffolding.shadow = -1;
            }
        }
    }

    private void renderShadowMap(ICamera camera) {
        if (Settings.settings.scene.renderer.shadow.active) {
            /*
             * Shadow mapping here?
             * <ul>
             * <li>Extract model bodies (front)</li>
             * <li>Work out light direction</li>
             * <li>Set orthographic camera at set distance from bodies,
             * direction of light, clip planes</li>
             * <li>Render depth map to frame buffer (fb)</li>
             * <li>Send frame buffer texture in to ModelBatchRenderSystem along
             * with light position, direction, clip planes and light camera
             * combined matrix</li>
             * <li>Compare real distance from light to texture sample, render
             * shadow if different</li>
             * </ul>
             */
            List<IRenderable> models = renderLists.get(MODEL_PIX.ordinal());
            List<IRenderable> modelsTess = renderLists.get(MODEL_PIX_TESS.ordinal());
            models.sort(Comparator.comparingDouble(IRenderable::getDistToCamera));

            int shadowNRender = (Settings.settings.program.modeStereo.active || Settings.settings.runtime.openVr) ? 2 : Settings.settings.program.modeCubemap.active ? 6 : 1;

            if (shadowMapFb != null && smCombinedMap != null) {
                addCandidates(models, shadowCandidates);
                //addCandidates(modelsTess, shadowCandidatesTess);

                // Clear maps
                smTexMap.clear();
                smCombinedMap.clear();

                renderShadowMapCandidates(shadowCandidates, shadowNRender, camera);
                //renderShadowMapCandidatesTess(shadowCandidatesTess, shadowNRender, camera);
            }
        }
    }

    public void render(final ICamera camera, final double t, final int rw, final int rh, final int tw, final int th, final FrameBuffer fb, final PostProcessBean ppb) {
        if (rendering.get()) {
            if (renderMode == null)
                initRenderMode(camera);

            // Shadow maps are the same for all
            renderShadowMap(camera);

            // In stereo and cubemap modes, the glow pass is rendered in the SGR itself
            if (!Settings.settings.program.modeStereo.active && !Settings.settings.program.modeCubemap.active && !Settings.settings.runtime.openVr) {
                renderGlowPass(camera, glowFb);
            }

            renderMode.render(this, camera, t, rw, rh, tw, th, fb, ppb);
        }
    }

    @Override
    public IRenderMode getRenderProcess() {
        return renderMode;
    }

    @Override
    public FrameBuffer getGlowFrameBuffer() {
        return glowFb;
    }

    /**
     * Renders the scene.
     *
     * @param camera        The camera to use.
     * @param t             The time in seconds since the start.
     * @param renderContext The render context.
     */
    public void renderScene(ICamera camera, double t, RenderingContext renderContext) {
        try {
            // Update time difference since last update
            for (ComponentType ct : ComponentType.values()) {
                alphas[ct.ordinal()] = calculateAlpha(ct, t);
            }

            int size = renderSystems.size;
            for (int i = 0; i < size; i++) {
                IRenderSystem process = renderSystems.get(i);
                // If we have no render group, this means all the info is already in
                // the render system. No lists needed
                if (process.getRenderGroup() != null) {
                    List<IRenderable> l = renderLists.get(process.getRenderGroup().ordinal());
                    process.render(l, camera, t, renderContext);
                } else {
                    process.render(null, camera, t, renderContext);
                }
            }
        } catch (Exception e) {
            logger.error(e);
        }

    }

    /**
     * Renders all the systems which are of the given class.
     *
     * @param camera        The camera to use.
     * @param t             The time in seconds since the start.
     * @param renderContext The render context.
     * @param systemClass   The class.
     */
    protected void renderSystem(ICamera camera, double t, RenderingContext renderContext, Class<? extends IRenderSystem> systemClass) {
        // Update time difference since last update
        for (ComponentType ct : ComponentType.values()) {
            alphas[ct.ordinal()] = calculateAlpha(ct, t);
        }

        int size = renderSystems.size;
        for (int i = 0; i < size; i++) {
            IRenderSystem process = renderSystems.get(i);
            if (systemClass.isInstance(process)) {
                // If we have no render group, this means all the info is already in
                // the render system. No lists needed
                if (process.getRenderGroup() != null) {
                    List<IRenderable> l = renderLists.get(process.getRenderGroup().ordinal());
                    process.render(l, camera, t, renderContext);
                } else {
                    process.render(null, camera, t, renderContext);
                }
            }
        }
    }

    private boolean isInstance(IRenderSystem process, Class<?>... systemClasses) {
        for (Class<?> systemClass : systemClasses) {
            if (systemClass.isInstance(process))
                return true;
        }
        return false;
    }

    /**
     * Renders all the systems which are of one of the given classes.
     *
     * @param camera        The camera to use.
     * @param t             The time in seconds since the start.
     * @param renderContext The render context.
     * @param systemClasses The classes.
     */
    protected void renderSystems(ICamera camera, double t, RenderingContext renderContext, Class<?>... systemClasses) {
        // Update time difference since last update
        for (ComponentType ct : ComponentType.values()) {
            alphas[ct.ordinal()] = calculateAlpha(ct, t);
        }

        int size = renderSystems.size;
        for (int i = 0; i < size; i++) {
            IRenderSystem process = renderSystems.get(i);
            if (isInstance(process, systemClasses)) {
                // If we have no render group, this means all the info is already in
                // the render system. No lists needed
                if (process.getRenderGroup() != null) {
                    List<IRenderable> l = renderLists.get(process.getRenderGroup().ordinal());
                    process.render(l, camera, t, renderContext);
                } else {
                    process.render(null, camera, t, renderContext);
                }
            }
        }
    }

    /**
     * This must be called when all the rendering for the current frame has
     * finished.
     */
    public void swapRenderLists() {
        // Clear lists to get them ready for update pass.
        for (RenderGroup rg : values()) {
            renderLists.get(rg.ordinal()).clear();
        }
    }

    /**
     * Checks if a given component type is on
     *
     * @param comp The component
     *
     * @return Whether the component is on
     */
    public boolean isOn(ComponentType comp) {
        return visible.get(comp.ordinal()) || alphas[comp.ordinal()] > 0;
    }

    /**
     * Checks if the component types are all on
     *
     * @param comp The components
     *
     * @return Whether the components are all on
     */
    public boolean allOn(ComponentTypes comp) {
        boolean allOn = comp.length() == 0 || comp.allSetLike(visible);

        if (!allOn) {
            allOn = true;
            for (int i = comp.nextSetBit(0); i >= 0; i = comp.nextSetBit(i + 1)) {
                // operate on index i here
                allOn = allOn && alphas[i] > 0;
                if (i == Integer.MAX_VALUE) {
                    break; // or (i+1) would overflow
                }
            }
        }
        return allOn;
    }

    /**
     * Computes the alpha of this component types by multiplying the alphas
     * of all components
     *
     * @param comp The components
     *
     * @return The alpha value
     */
    public float alpha(ComponentTypes comp) {
        float alpha = 1;

        for (int i = comp.nextSetBit(0); i >= 0; i = comp.nextSetBit(i + 1)) {
            // operate on index i here
            alpha *= alphas[i];
        }
        return alpha;
    }

    public boolean isOn(int ordinal) {
        return visible.get(ordinal) || alphas[ordinal] > 0;
    }

    @Override
    public void notify(Event event, Object source, final Object... data) {
        switch (event) {
        case TOGGLE_VISIBILITY_CMD -> {
            ComponentType ct = ComponentType.getFromKey((String) data[0]);
            if (ct != null) {
                int idx = ct.ordinal();
                if (data.length == 2) {
                    // We have the boolean
                    boolean currentVisibility = visible.get(ct.ordinal());
                    boolean newVisibility = (boolean) data[1];
                    if (currentVisibility != newVisibility) {
                        // Only update if visibility different
                        if (newVisibility)
                            visible.set(ct.ordinal());
                        else
                            visible.clear(ct.ordinal());
                        times[idx] = (long) (GaiaSky.instance.getT() * 1000f);
                    }
                } else {
                    // Only toggle
                    visible.flip(ct.ordinal());
                    times[idx] = (long) (GaiaSky.instance.getT() * 1000f);
                }
            }
        }
        case LINE_RENDERER_UPDATE -> GaiaSky.postRunnable(this::updateLineRenderSystem);
        case STEREOSCOPIC_CMD -> {
            boolean stereo = (Boolean) data[0];
            if (stereo)
                renderMode = sgrList[SGR_STEREO_IDX];
            else {
                if (Settings.settings.runtime.openVr)
                    renderMode = sgrList[SGR_OPENVR_IDX];
                else
                    renderMode = sgrList[SGR_DEFAULT_IDX];
            }
        }
        case CUBEMAP_CMD -> {
            boolean cubemap = (Boolean) data[0] && !Settings.settings.runtime.openVr;
            if (cubemap) {
                renderMode = sgrList[SGR_CUBEMAP_IDX];
            } else {
                if (Settings.settings.runtime.openVr)
                    renderMode = sgrList[SGR_OPENVR_IDX];
                else
                    renderMode = sgrList[SGR_DEFAULT_IDX];
            }
        }
        case CAMERA_MODE_CMD -> {
            CameraMode cm = (CameraMode) data[0];
            if (cm.isGaiaFov())
                renderMode = sgrList[SGR_FOV_IDX];
            else {
                if (Settings.settings.runtime.openVr)
                    renderMode = sgrList[SGR_OPENVR_IDX];
                else if (Settings.settings.program.modeStereo.active)
                    renderMode = sgrList[SGR_STEREO_IDX];
                else if (Settings.settings.program.modeCubemap.active)
                    renderMode = sgrList[SGR_CUBEMAP_IDX];
                else
                    renderMode = sgrList[SGR_DEFAULT_IDX];

            }
        }
        case REBUILD_SHADOW_MAP_DATA_CMD -> buildShadowMapData();
        case LIGHT_GLOW_CMD -> {
            boolean glow = (Boolean) data[0];
            if (glow) {
                buildGlowData();
            }
        }
        default -> {
        }
        }
    }

    /**
     * Computes the alpha for the given component type.
     *
     * @param type The component type.
     * @param t    The current time in seconds.
     *
     * @return The alpha value.
     */
    private float calculateAlpha(ComponentType type, double t) {
        int ordinal = type.ordinal();
        long diff = (long) (t * 1000f) - times[ordinal];
        if (diff > Settings.settings.scene.fadeMs) {
            if (visible.get(ordinal)) {
                alphas[ordinal] = 1;
            } else {
                alphas[ordinal] = 0;
            }
            return alphas[ordinal];
        } else {
            return visible.get(ordinal) ? MathUtilsd.lint(diff, 0, Settings.settings.scene.fadeMs, 0, 1) : MathUtilsd.lint(diff, 0, Settings.settings.scene.fadeMs, 1, 0);
        }
    }

    /**
     * Resizes the scene renderer and all its sub-renderers and assets, but not the render systems.
     *
     * @param tw New target (screen) width.
     * @param th New target (screen) height.
     * @param rw New render buffer width.
     * @param rh New render buffer height.
     */
    public void resize(final int tw, final int th, final int rw, final int rh) {
        resize(tw, th, rw, rh, false);
    }

    /**
     * Resizes the scene renderer and all its sub-renderers and assets.
     *
     * @param tw              New target (screen) width.
     * @param th              New target (screen) height.
     * @param rw              New render buffer width.
     * @param rh              New render buffer height.
     * @param resizeRenderSys Also resize all render systems.
     */
    public void resize(final int tw, final int th, final int rw, final int rh, boolean resizeRenderSys) {
        if (resizeRenderSys) {
            resizeRenderSystems(tw, th, rw, rh);
        }

        for (IRenderMode sgr : sgrList) {
            sgr.resize(rw, rh, tw, th);
        }
    }

    /**
     * Resizes the render systems of this renderer.
     *
     * @param tw New target (screen) width.
     * @param th New target (screen) height.
     * @param rw New render buffer width.
     * @param rh New render buffer height.
     */
    public void resizeRenderSystems(final int tw, final int th, final int rw, final int rh) {
        for (IRenderSystem rendSys : renderSystems) {
            rendSys.resize(tw, th);
        }
    }

    public void dispose() {
        // Dispose render systems
        if (renderSystems != null) {
            for (IRenderSystem rendSys : renderSystems) {
                rendSys.dispose();
            }
            renderSystems.clear();
        }

        // Dispose SGRs
        if (sgrList != null) {
            for (IRenderMode sgr : sgrList) {
                if (sgr != null)
                    sgr.dispose();
            }
            sgrList = null;
        }
    }

    /**
     * Builds the shadow map data; frame buffers, arrays, etc.
     */
    private void buildShadowMapData() {
        if (shadowMapFb != null) {
            for (FrameBuffer[] frameBufferArray : shadowMapFb)
                for (FrameBuffer fb : frameBufferArray) {
                    if (fb != null)
                        fb.dispose();
                }
        }

        // Shadow map frame buffer
        shadowMapFb = new FrameBuffer[Settings.settings.scene.renderer.shadow.number][Constants.N_DIR_LIGHTS];
        // Shadow map combined matrices
        shadowMapCombined = new Matrix4[Settings.settings.scene.renderer.shadow.number][Constants.N_DIR_LIGHTS];
        // Init
        for (int i = 0; i < Settings.settings.scene.renderer.shadow.number; i++) {
            for (int j = 0; j < 1; j++) {
                shadowMapFb[i][j] = new FrameBuffer(Format.RGBA8888, Settings.settings.scene.renderer.shadow.resolution, Settings.settings.scene.renderer.shadow.resolution, true);
                shadowMapCombined[i][j] = new Matrix4();
            }
        }
        if (smTexMap == null)
            smTexMap = new HashMap<>();
        smTexMap.clear();

        if (smCombinedMap == null)
            smCombinedMap = new HashMap<>();
        smCombinedMap.clear();

        if (shadowCandidates == null) {
            shadowCandidates = new ArrayList<>(Settings.settings.scene.renderer.shadow.number);
            //shadowCandidatesTess = new ArrayList<>(Settings.settings.scene.renderer.shadow.number);
        }
        shadowCandidates.clear();
        //shadowCandidatesTess.clear();
    }

    private void buildGlowData() {
        if (glowFb == null) {
            FrameBufferBuilder fbb = new FrameBufferBuilder(1920, 1080);
            fbb.addBasicColorTextureAttachment(Format.RGBA8888);
            fbb.addBasicDepthRenderBuffer();
            glowFb = new GaiaSkyFrameBuffer(fbb, 0, 1);
        }
    }

    public void updateLineRenderSystem() {
        LineRenderSystem current = null;
        for (IRenderSystem proc : renderSystems) {
            if (proc instanceof LineRenderSystem) {
                current = (LineRenderSystem) proc;
            }
        }
        final int idx = renderSystems.indexOf(current, true);
        if (current != null && ((current instanceof LineQuadstripRenderer && Settings.settings.scene.renderer.isNormalLineRenderer()) || (!(current instanceof LineQuadstripRenderer) && !Settings.settings.scene.renderer.isNormalLineRenderer()))) {
            renderSystems.removeIndex(idx);
            AbstractRenderSystem lineSys = getLineRenderSystem();
            renderSystems.insert(idx, lineSys);
            current.dispose();
        }
    }

    private AbstractRenderSystem getLineRenderSystem() {
        AbstractRenderSystem sys;
        if (Settings.settings.scene.renderer.isNormalLineRenderer()) {
            // Normal
            sys = new LinePrimitiveRenderer(this, LINE, alphas, renderAssets.lineShaders);
            sys.addPreRunnables(regularBlendR, depthTestR, noDepthWritesR);
        } else {
            // Quad
            sys = new LineQuadstripRenderer(this, LINE, alphas, renderAssets.lineQuadShaders);
            sys.addPreRunnables(additiveBlendR, depthTestR, noDepthWritesR);
        }
        return sys;
    }

    /**
     * Resets the render flags for all systems.
     */
    public void resetRenderSystemFlags() {
        for (IRenderSystem system : renderSystems) {
            if (system instanceof AbstractRenderSystem) {
                ((AbstractRenderSystem) system).resetFlags();
            }
        }
    }

    public FrameBuffer getFrameBuffer(final int w, final int h) {
        final int key = getKey(w, h);
        if (!frameBufferMap.containsKey(key)) {
            final FrameBuffer fb = PingPongBuffer.createMainFrameBuffer(w, h, true, true, true, true, Format.RGB888, true);
            frameBufferMap.put(key, fb);
        }
        return frameBufferMap.get(key);
    }

    private int getKey(final int w, final int h) {
        return 31 * h + w;
    }
}
