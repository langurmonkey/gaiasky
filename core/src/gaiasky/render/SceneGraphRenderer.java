/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render;

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
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.api.IPostProcessor.PostProcessBean;
import gaiasky.render.api.IRenderMode;
import gaiasky.render.api.IRenderable;
import gaiasky.render.api.ISceneRenderer;
import gaiasky.render.process.*;
import gaiasky.render.system.*;
import gaiasky.render.system.AbstractRenderSystem.RenderSystemRunnable;
import gaiasky.scenegraph.ModelBody;
import gaiasky.scenegraph.Star;
import gaiasky.scenegraph.VRDeviceModel;
import gaiasky.scenegraph.camera.CameraManager.CameraMode;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.Constants;
import gaiasky.util.GlobalResources;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.Settings.PointCloudMode;
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
public class SceneGraphRenderer implements ISceneRenderer, IObserver {
    private static final Log logger = Logger.getLogger(SceneGraphRenderer.class);

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
    private Array<Array<IRenderable>> renderLists;

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
    private List<ModelBody> shadowCandidates, shadowCandidatesTess;
    // Dimension 1: number of shadows, dimension 2: number of lights
    public FrameBuffer[][] shadowMapFb;
    // Dimension 1: number of shadows, dimension 2: number of lights
    private Matrix4[][] shadowMapCombined;
    /** Map containing the shadow map for each model body. **/
    public Map<ModelBody, Texture> smTexMap;
    /** Map containing the combined matrix for each model body. **/
    public Map<ModelBody, Matrix4> smCombinedMap;

    // Light glow pre-render
    private FrameBuffer glowFb;
    private LightPositionUpdater lpu;

    private Vector3 aux1;
    private Vector3d aux1d, aux2d, aux3d;
    private Vector3b aux1b;

    // VRContext, may be null
    private final VRContext vrContext;
    private final GlobalResources globalResources;

    private Array<IRenderable> stars;

    private AbstractRenderSystem billboardStarsProc;

    private AtomicBoolean rendering;

    private RenderAssets renderAssets;

    public SceneGraphRenderer(final VRContext vrContext, final GlobalResources globalResources) {
        super();
        this.vrContext = vrContext;
        this.globalResources = globalResources;
        this.rendering = new AtomicBoolean(false);
        this.renderAssets = new RenderAssets(globalResources);
    }


    @Override
    public void initialize(AssetManager manager) {
        // Initialize the render assets.
        renderAssets.initialize(manager);

        stars = new Array<>();

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

        if (Settings.settings.postprocess.lightGlow) {
            buildGlowData();
        }
    }

    public void doneLoading(final AssetManager manager) {
        // Prepare render assets
        renderAssets.doneLoading(manager);

        // Initialize render lists
        RenderGroup[] renderGroups = values();
        renderLists = new Array<>(false, renderGroups.length);
        for (int i = 0; i < renderGroups.length; i++) {
            renderLists.add(new Array<>(false, 20));
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
        sgrList[SGR_OPENVR_IDX] = new RenderModeOpenVR(vrContext, globalResources.getSpriteBatchVR());
        renderMode = null;

        /*
         * ======= INITIALIZE RENDER SYSTEMS =======
         */

        final PointCloudMode pcm = Settings.settings.scene.renderer.pointCloud;

        // SINGLE STAR POINTS
        AbstractRenderSystem singlePointProc = new StarPointRenderSystem(POINT_STAR, alphas, renderAssets.starPointShaders, ComponentType.Stars);
        singlePointProc.addPreRunnables(additiveBlendR, noDepthTestR);

        // SKYBOX - (MW panorama, CMWB)
        AbstractRenderSystem skyboxProc = new ModelBatchRenderSystem(SKYBOX, alphas, renderAssets.mbSkybox);

        // MODEL BACKGROUND - (MW panorama, CMWB)
        AbstractRenderSystem modelBackgroundProc = new ModelBatchRenderSystem(MODEL_BG, alphas, renderAssets.mbVertexDiffuse);

        // MODEL GRID - (Ecl, Eq, Gal grids)
        AbstractRenderSystem modelGridsProc = new ModelBatchRenderSystem(MODEL_VERT_GRID, alphas, renderAssets.mbVertexLightingGrid);
        modelGridsProc.addPostRunnables(clearDepthR);
        // RECURSIVE GRID
        AbstractRenderSystem modelRecGridProc = new ModelBatchRenderSystem(MODEL_VERT_RECGRID, alphas, renderAssets.mbVertexLightingRecGrid);
        modelRecGridProc.addPreRunnables(regularBlendR, depthTestR);

        // ANNOTATIONS - (grids)
        AbstractRenderSystem annotationsProc = new FontRenderSystem(FONT_ANNOTATION, alphas, renderAssets.spriteBatch, null, null, renderAssets.font2d, null);
        annotationsProc.addPreRunnables(regularBlendR, noDepthTestR);
        annotationsProc.addPostRunnables(clearDepthR);

        // BILLBOARD STARS
        billboardStarsProc = new BillboardRenderSystem(BILLBOARD_STAR, alphas, renderAssets.starBillboardShaders, Settings.settings.scene.star.getStarTexture(), ComponentType.Stars, true);
        billboardStarsProc.addPreRunnables(additiveBlendR, noDepthTestR);
        lpu = new LightPositionUpdater();
        billboardStarsProc.addPostRunnables(lpu);

        // BILLBOARD GALAXIES
        AbstractRenderSystem billboardGalaxiesProc = new BillboardRenderSystem(BILLBOARD_GAL, alphas, renderAssets.galShaders, "data/tex/base/static.jpg", ComponentType.Galaxies, false);
        billboardGalaxiesProc.addPreRunnables(additiveBlendR, depthTestR, noDepthWritesR);

        // BILLBOARD SPRITES
        AbstractRenderSystem billboardSpritesProc = new BillboardRenderSystem(BILLBOARD_SPRITE, alphas, renderAssets.spriteShaders, null, ComponentType.Clusters, false);
        billboardSpritesProc.addPreRunnables(additiveBlendR, depthTestNoWritesR);

        // LINES CPU
        AbstractRenderSystem lineProc = getLineRenderSystem();

        // LINES GPU
        AbstractRenderSystem lineGpuProc = new VertGPURenderSystem<>(LINE_GPU, alphas, renderAssets.lineGpuShaders, true);
        lineGpuProc.addPreRunnables(additiveBlendR, depthTestR, noDepthWritesR);

        // POINTS CPU
        AbstractRenderSystem pointProc = new PointRenderSystem(POINT, alphas, renderAssets.pointShaders);

        // POINTS GPU
        AbstractRenderSystem pointGpuProc = new VertGPURenderSystem<>(POINT_GPU, alphas, renderAssets.lineGpuShaders, false);
        pointGpuProc.addPreRunnables(regularBlendR, depthTestR);

        // MODELS DUST AND MESH
        AbstractRenderSystem modelMeshOpaqueProc = new ModelBatchRenderSystem(MODEL_PIX_DUST, alphas, renderAssets.mbPixelLightingDust);
        AbstractRenderSystem modelMeshAdditiveProc = new ModelBatchRenderSystem(MODEL_VERT_ADDITIVE, alphas, renderAssets.mbVertexLightingAdditive);
        // MODEL PER-PIXEL-LIGHTING EARLY
        AbstractRenderSystem modelPerPixelLightingEarly = new ModelBatchRenderSystem(MODEL_PIX_EARLY, alphas, renderAssets.mbPixelLighting);
        // MODEL PER-VERTEX-LIGHTING EARLY
        AbstractRenderSystem modelPerVertexLightingEarly = new ModelBatchRenderSystem(MODEL_VERT_EARLY, alphas, renderAssets.mbVertexLighting);

        // MODEL DIFFUSE
        AbstractRenderSystem modelMeshDiffuse = new ModelBatchRenderSystem(MODEL_DIFFUSE, alphas, renderAssets.mbVertexDiffuse);

        // MODEL PER-PIXEL-LIGHTING
        AbstractRenderSystem modelPerPixelLighting = new ModelBatchRenderSystem(MODEL_PIX, alphas, renderAssets.mbPixelLighting);

        // MODEL PER-PIXEL-LIGHTING-TESSELLATION
        AbstractRenderSystem modelPerPixelLightingTess = new ModelBatchTessellationRenderSystem(MODEL_PIX_TESS, alphas, renderAssets.mbPixelLightingTessellation);
        modelPerPixelLightingTess.addPreRunnables(regularBlendR, depthTestR);

        // MODEL BEAM
        AbstractRenderSystem modelBeamProc = new ModelBatchRenderSystem(MODEL_VERT_BEAM, alphas, renderAssets.mbVertexLightingBeam);

        // GALAXY
        BillboardGroupRenderSystem billboardGroupRenderSystem = new BillboardGroupRenderSystem(BILLBOARD_GROUP, alphas, renderAssets.billboardGroupShaders);

        // PARTICLE EFFECTS
        AbstractRenderSystem particleEffectsProc = new ParticleEffectsRenderSystem(null, alphas, renderAssets.particleEffectShaders);
        particleEffectsProc.addPreRunnables(additiveBlendR, noDepthTestR);
        particleEffectsProc.addPostRunnables(regularBlendR);

        // PARTICLE GROUP
        AbstractRenderSystem particleGroupProc = switch (pcm) {
            case TRIANGLES -> new ParticleGroupRenderSystem(PARTICLE_GROUP, alphas, renderAssets.particleGroupShaders);
            case TRIANGLES_INSTANCED -> new ParticleGroupInstRenderSystem(PARTICLE_GROUP, alphas, renderAssets.particleGroupShaders);
            case POINTS -> new ParticleGroupPointRenderSystem(PARTICLE_GROUP, alphas, renderAssets.particleGroupShaders);
        };
        particleGroupProc.addPreRunnables(additiveBlendR, depthTestR, noDepthWritesR);
        particleGroupProc.addPostRunnables(regularBlendR, depthWritesR);

        // STAR GROUP
        AbstractRenderSystem starGroupProc = switch (pcm) {
            case TRIANGLES -> new StarGroupRenderSystem(STAR_GROUP, alphas, renderAssets.starGroupShaders);
            case TRIANGLES_INSTANCED -> new StarGroupInstRenderSystem(STAR_GROUP, alphas, renderAssets.starGroupShaders);
            case POINTS -> new StarGroupPointRenderSystem(STAR_GROUP, alphas, renderAssets.starGroupShaders);
        };
        starGroupProc.addPreRunnables(additiveBlendR, depthTestR, noDepthWritesR);
        starGroupProc.addPostRunnables(regularBlendR, depthWritesR);

        // VARIABLE GROUP
        AbstractRenderSystem variableGroupProc = switch (pcm) {
            case TRIANGLES -> new VariableGroupRenderSystem(VARIABLE_GROUP, alphas, renderAssets.variableGroupShaders);
            case TRIANGLES_INSTANCED -> new VariableGroupInstRenderSystem(VARIABLE_GROUP, alphas, renderAssets.variableGroupShaders);
            case POINTS -> new VariableGroupPointRenderSystem(VARIABLE_GROUP, alphas, renderAssets.variableGroupShaders);
        };
        variableGroupProc.addPreRunnables(additiveBlendR, depthTestR, noDepthWritesR);
        variableGroupProc.addPostRunnables(regularBlendR, depthWritesR);

        // ORBITAL ELEMENTS PARTICLES
        AbstractRenderSystem orbitElemParticlesProc = new OrbitalElementsParticlesRenderSystem(ORBITAL_ELEMENTS_PARTICLE, alphas, renderAssets.orbitElemShaders);
        orbitElemParticlesProc.addPreRunnables(additiveBlendR, depthTestR, noDepthWritesR);
        orbitElemParticlesProc.addPostRunnables(regularBlendR, depthWritesR);

        // ORBITAL ELEMENTS GROUP
        AbstractRenderSystem orbitElemGroupProc = new OrbitalElementsGroupRenderSystem(ORBITAL_ELEMENTS_GROUP, alphas, renderAssets.orbitElemShaders);
        orbitElemGroupProc.addPreRunnables(additiveBlendR, depthTestR, noDepthWritesR);
        orbitElemGroupProc.addPostRunnables(regularBlendR, depthWritesR);

        // MODEL STARS
        AbstractRenderSystem modelStarsProc = new ModelBatchRenderSystem(MODEL_VERT_STAR, alphas, renderAssets.mbVertexLightingStarSurface);

        // LABELS
        AbstractRenderSystem labelsProc = new FontRenderSystem(FONT_LABEL, alphas, renderAssets.fontBatch, renderAssets.distanceFieldFontShader, renderAssets.font3d, renderAssets.font2d, renderAssets.fontTitles);

        // BILLBOARD SSO
        AbstractRenderSystem billboardSSOProc = new BillboardRenderSystem(BILLBOARD_SSO, alphas, renderAssets.starBillboardShaders, "data/tex/base/sso.png", null, false);
        billboardSSOProc.addPreRunnables(additiveBlendR, depthTestNoWritesR);

        // MODEL ATMOSPHERE
        AbstractRenderSystem modelAtmProc = new ModelBatchRenderSystem(MODEL_ATM, alphas, renderAssets.mbAtmosphere) {
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
        AbstractRenderSystem modelCloudProc = new ModelBatchRenderSystem(MODEL_CLOUD, alphas, renderAssets.mbCloud);

        // SHAPES
        AbstractRenderSystem shapeProc = new ShapeRenderSystem(SHAPE, alphas, globalResources.getShapeShader());
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
        addRenderSystem(billboardGroupRenderSystem);

        // Billboard stars
        addRenderSystem(billboardStarsProc);

        // Stars, particles
        addRenderSystem(particleGroupProc);
        addRenderSystem(starGroupProc);
        addRenderSystem(variableGroupProc);
        addRenderSystem(orbitElemParticlesProc);
        addRenderSystem(orbitElemGroupProc);

        // Diffuse meshes
        addRenderSystem(modelMeshDiffuse);

        // Generic per-pixel lighting models
        addRenderSystem(modelPerPixelLighting);
        addRenderSystem(modelPerPixelLightingTess);
        addRenderSystem(modelBeamProc);

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
        addRenderSystem(modelStarsProc);
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

        EventManager.instance.subscribe(this, Event.TOGGLE_VISIBILITY_CMD, Event.LINE_RENDERER_UPDATE, Event.STEREOSCOPIC_CMD, Event.CAMERA_MODE_CMD, Event.CUBEMAP_CMD, Event.REBUILD_SHADOW_MAP_DATA_CMD, Event.LIGHT_SCATTERING_CMD);

    }

    public synchronized void setRendering(boolean rendering) {
        this.rendering.set(rendering);
    }

    public Array<Array<IRenderable>> renderListsFront() {
        return renderLists;
    }
    public Array<Array<IRenderable>> renderListsBack() {
        return renderLists;
    }

    private void addRenderSystem(IRenderSystem renderSystem) {
        if (!renderSystems.contains(renderSystem, true)) {
            renderSystems.add(renderSystem);
        }
    }

    private void initSGR(ICamera camera) {
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

    Array<VRDeviceModel> controllers = new Array<>();

    public void renderGlowPass(ICamera camera, FrameBuffer frameBuffer) {
        if (frameBuffer == null) {
            frameBuffer = glowFb;
        }
        if (Settings.settings.postprocess.lightGlow && frameBuffer != null) {
            // Get all billboard stars
            Array<IRenderable> billboardStars = renderLists.get(BILLBOARD_STAR.ordinal());

            stars.clear();
            for (IRenderable st : billboardStars) {
                if (st instanceof Star) {
                    stars.add(st);
                }
            }

            // Get all models
            Array<IRenderable> models = renderLists.get(MODEL_PIX.ordinal());
            Array<IRenderable> modelsTess = renderLists.get(MODEL_PIX_TESS.ordinal());

            // VR controllers
            if (Settings.settings.runtime.openVr) {
                RenderModeOpenVR sgrVR = (RenderModeOpenVR) sgrList[SGR_OPENVR_IDX];
                if (vrContext != null) {
                    for (VRDeviceModel m : sgrVR.controllerObjects) {
                        if (!models.contains(m, true))
                            controllers.add(m);
                    }
                }
            }

            frameBuffer.begin();
            Gdx.gl.glEnable(GL30.GL_DEPTH_TEST);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

            //if (!Settings.settings.program.CUBEMAP360_MODE) {
            // Render billboard stars
            billboardStarsProc.render(stars, camera, 0, null);

            // Render models
            renderAssets.mbPixelLightingOpaque.begin(camera.getCamera());
            for (IRenderable model : models) {
                if (model instanceof ModelBody) {
                    ModelBody mb = (ModelBody) model;
                    mb.render(renderAssets.mbPixelLightingOpaque, RenderGroup.MODEL_PIX, 1, 0, false);
                }
            }
            renderAssets.mbPixelLightingOpaque.end();

            // Render tessellated models
            if (modelsTess.size > 0) {
                renderAssets.mbPixelLightingOpaqueTessellation.begin(camera.getCamera());
                for (IRenderable model : modelsTess) {
                    if (model instanceof ModelBody) {
                        ModelBody mb = (ModelBody) model;
                        mb.render(renderAssets.mbPixelLightingOpaqueTessellation, RenderGroup.MODEL_PIX, 1, 0, false);
                    }
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

    private void addCandidates(Array<IRenderable> models, List<ModelBody> candidates) {
        if (candidates != null) {
            candidates.clear();
            int num = 0;
            for (int i = 0; i < models.size; i++) {
                if (models.get(i) instanceof ModelBody) {
                    ModelBody mr = (ModelBody) models.get(i);
                    if (mr.isShadow()) {
                        candidates.add(num, mr);
                        mr.shadow = 0;
                        num++;
                        if (num == Settings.settings.scene.renderer.shadow.number)
                            break;
                    }
                }
            }
        }
    }

    private void renderShadowMapCandidates(List<ModelBody> candidates, int shadowNRender, ICamera camera) {
        int i = 0;
        int j = 0;
        // Normal bodies
        for (ModelBody candidate : candidates) {

            Vector3 camDir = aux1.set(candidate.mc.directional(0).direction);
            // Direction is that of the light
            cameraLight.direction.set(camDir);

            double radius = candidate.getRadius();
            // Distance from camera to object, radius * sv[0]
            double distance = radius * candidate.shadowMapValues[0];
            // Position, factor of radius
            candidate.getAbsolutePosition(aux1b);
            aux1b.sub(camera.getPos()).sub(camDir.nor().scl((float) distance));
            aux1b.put(cameraLight.position);
            // Up is perpendicular to dir
            if (cameraLight.direction.y != 0 || cameraLight.direction.z != 0)
                aux1.set(1, 0, 0);
            else
                aux1.set(0, 1, 0);
            cameraLight.up.set(cameraLight.direction).crs(aux1);

            // Near is sv[1]*radius before the object
            cameraLight.near = (float) (distance - radius * candidate.shadowMapValues[1]);
            // Far is sv[2]*radius after the object
            cameraLight.far = (float) (distance + radius * candidate.shadowMapValues[2]);

            // Update cam
            cameraLight.update(false);

            // Render model depth map to frame buffer
            shadowMapFb[i][j].begin();
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

            // No tessellation
            renderAssets.mbPixelLightingDepth.begin(cameraLight);
            candidate.render(renderAssets.mbPixelLightingDepth, 1, 0, null, RenderGroup.MODEL_PIX);
            renderAssets.mbPixelLightingDepth.end();

            // Save frame buffer and combined matrix
            candidate.shadow = shadowNRender;
            shadowMapCombined[i][j].set(cameraLight.combined);
            smCombinedMap.put(candidate, shadowMapCombined[i][j]);
            smTexMap.put(candidate, shadowMapFb[i][j].getColorBufferTexture());

            shadowMapFb[i][j].end();
            i++;
        }
    }

    private void renderShadowMapCandidatesTess(Array<ModelBody> candidates, int shadowNRender, ICamera camera, RenderingContext rc) {
        int i = 0;
        int j = 0;
        // Normal bodies
        for (ModelBody candidate : candidates) {
            double radius = candidate.getRadius();
            // Only render when camera very close to surface
            if (candidate.distToCamera < radius * 1.1) {
                candidate.shadow = shadowNRender;

                Vector3 shadowCameraDir = aux1.set(candidate.mc.directional(0).direction);

                // Shadow camera direction is that of the light
                cameraLight.direction.set(shadowCameraDir);

                Vector3 shadowCamDir = aux1.set(candidate.mc.directional(0).direction);
                // Direction is that of the light
                cameraLight.direction.set(shadowCamDir);

                // Distance from camera to object, radius * sv[0]
                float distance = (float) (radius * candidate.shadowMapValues[0] * 0.01);
                // Position, factor of radius
                Vector3b objPos = candidate.getAbsolutePosition(aux1b);
                Vector3b camPos = camera.getPos();
                Vector3d camDir = aux3d.set(camera.getDirection()).nor().scl(100 * Constants.KM_TO_U);
                boolean intersect = Intersectord.checkIntersectSegmentSphere(camPos.tov3d(), aux3d.set(camPos).add(camDir), objPos.put(aux1d), radius);
                if (intersect) {
                    // Use height
                    camDir.nor().scl(candidate.distToCamera - radius);
                }
                Vector3d objCam = aux2d.set(camPos).sub(objPos).nor().scl(-(candidate.distToCamera - radius)).add(camDir);

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
                candidate.render(renderAssets.mbPixelLightingDepthTessellation, 1, 0, rc, RenderGroup.MODEL_PIX);
                renderAssets.mbPixelLightingDepthTessellation.end();

                // Save frame buffer and combined matrix
                candidate.shadow = shadowNRender;
                shadowMapCombined[i][j].set(cameraLight.combined);
                smCombinedMap.put(candidate, shadowMapCombined[i][j]);
                smTexMap.put(candidate, shadowMapFb[i][j].getColorBufferTexture());

                shadowMapFb[i][j].end();
                i++;
            } else {
                candidate.shadow = -1;
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
            Array<IRenderable> models = renderLists.get(MODEL_PIX.ordinal());
            Array<IRenderable> modelsTess = renderLists.get(MODEL_PIX_TESS.ordinal());
            models.sort(Comparator.comparingDouble(IRenderable::getDistToCamera));

            int shadowNRender = (Settings.settings.program.modeStereo.active || Settings.settings.runtime.openVr) ? 2 : Settings.settings.program.modeCubemap.active ? 6 : 1;

            if (shadowMapFb != null && smCombinedMap != null) {
                addCandidates(models, shadowCandidates);
                addCandidates(modelsTess, shadowCandidatesTess);

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
                initSGR(camera);

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
                    Array<IRenderable> l = renderLists.get(process.getRenderGroup().ordinal());
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
                    Array<IRenderable> l = renderLists.get(process.getRenderGroup().ordinal());
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
                    Array<IRenderable> l = renderLists.get(process.getRenderGroup().ordinal());
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

    public boolean isOn(int ordinal) {
        return visible.get(ordinal) || alphas[ordinal] > 0;
    }

    @Override
    public void notify(Event event, Object source, final Object... data) {
        switch (event) {
        case TOGGLE_VISIBILITY_CMD:
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
            break;
        case LINE_RENDERER_UPDATE:
            GaiaSky.postRunnable(this::updateLineRenderSystem);
            break;
        case STEREOSCOPIC_CMD:
            boolean stereo = (Boolean) data[0];
            if (stereo)
                renderMode = sgrList[SGR_STEREO_IDX];
            else {
                if (Settings.settings.runtime.openVr)
                    renderMode = sgrList[SGR_OPENVR_IDX];
                else
                    renderMode = sgrList[SGR_DEFAULT_IDX];
            }
            break;
        case CUBEMAP_CMD:
            boolean cubemap = (Boolean) data[0] && !Settings.settings.runtime.openVr;
            if (cubemap) {
                renderMode = sgrList[SGR_CUBEMAP_IDX];
            } else {
                if (Settings.settings.runtime.openVr)
                    renderMode = sgrList[SGR_OPENVR_IDX];
                else
                    renderMode = sgrList[SGR_DEFAULT_IDX];
            }
            break;
        case CAMERA_MODE_CMD:
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
            break;
        case REBUILD_SHADOW_MAP_DATA_CMD:
            buildShadowMapData();
            break;
        case LIGHT_SCATTERING_CMD:
            boolean glow = (Boolean) data[0];
            if (glow) {
                buildGlowData();
            }
            break;
        default:
            break;
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

    public void resize(final int w, final int h, final int rw, final int rh) {
        resize(w, h, rw, rh, false);
    }

    public void resize(final int w, final int h, final int rw, final int rh, boolean resizeRenderSys) {
        if (resizeRenderSys)
            resizeRenderSystems(w, h, rw, rh);

        for (IRenderMode sgr : sgrList) {
            sgr.resize(w, h);
        }
    }

    public void resizeRenderSystems(final int w, final int h, final int rw, final int rh) {
        for (IRenderSystem rendSys : renderSystems) {
            rendSys.resize(w, h);
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
            shadowMapFb = null;
        }
        shadowMapCombined = null;

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
            shadowCandidatesTess = new ArrayList<>(Settings.settings.scene.renderer.shadow.number);
        }
        shadowCandidates.clear();
        shadowCandidatesTess.clear();
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
        if (current != null && ((current instanceof LineQuadRenderSystem && Settings.settings.scene.renderer.isNormalLineRenderer()) || (!(current instanceof LineQuadRenderSystem) && !Settings.settings.scene.renderer.isNormalLineRenderer()))) {
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
            sys = new LineRenderSystem(LINE, alphas, renderAssets.lineShaders);
            sys.addPreRunnables(regularBlendR, depthTestR, noDepthWritesR);
        } else {
            // Quad
            sys = new LineQuadRenderSystem(LINE, alphas, renderAssets.lineQuadShaders);
            sys.addPreRunnables(additiveBlendR, depthTestR, noDepthWritesR);
        }
        return sys;
    }

}
