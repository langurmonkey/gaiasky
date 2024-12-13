/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.render.*;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.api.IPostProcessor.PostProcessBean;
import gaiasky.render.api.IRenderMode;
import gaiasky.render.api.IRenderable;
import gaiasky.render.api.ISceneRenderer;
import gaiasky.render.postprocess.util.PingPongBuffer;
import gaiasky.render.process.RenderModeCubemapProjections;
import gaiasky.render.process.RenderModeMain;
import gaiasky.render.process.RenderModeOpenXR;
import gaiasky.render.process.RenderModeStereoscopic;
import gaiasky.render.system.AbstractRenderSystem;
import gaiasky.render.system.AbstractRenderSystem.RenderSystemRunnable;
import gaiasky.render.system.IRenderSystem;
import gaiasky.scene.Mapper;
import gaiasky.scene.camera.CameraManager.CameraMode;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.Render;
import gaiasky.scene.system.render.draw.*;
import gaiasky.scene.system.render.draw.model.ModelEntityRenderSystem;
import gaiasky.scene.system.render.pass.*;
import gaiasky.util.Constants;
import gaiasky.util.GlobalResources;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.Settings.PointCloudMode;
import gaiasky.util.gdx.IntModelBatch;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.vr.openxr.XrDriver;
import gaiasky.vr.openxr.input.XrControllerDevice;
import net.jafama.FastMath;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL40;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static gaiasky.render.RenderGroup.*;

/**
 * Coordinates and manages all rendering operations in Gaia Sky.
 */
public class SceneRenderer implements ISceneRenderer, IObserver {
    private static final Log logger = Logger.getLogger(SceneRenderer.class);
    // Indexes
    private final int SGR_DEFAULT_IDX = 0, SGR_STEREO_IDX = 1, SGR_CUBEMAP_IDX = 2, SGR_OPENXR_IDX = 3;
    // The OpenXR driver. This is null if we are not in VR.
    private final XrDriver xrDriver;
    private final GlobalResources globalResources;
    private final AtomicBoolean rendering;
    private final RenderAssets renderAssets;
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
    private final ModelEntityRenderSystem modelEntityRenderSystem = new ModelEntityRenderSystem(this);
    /**
     * Render lists for all render groups.
     * The front render lists contain the objects which are actually rendered in the current cycle. The back
     * render lists get updated by the update thread.
     **/
    private List<List<IRenderable>> renderLists;
    private Map<RenderGroup, IRenderSystem> renderSystems;
    private List<RenderGroup> renderGroups;
    private RenderSystemRunnable depthTestR, additiveBlendR, noDepthTestR, regularBlendR, depthTestNoWritesR, noDepthWritesR, depthWritesR, clearDepthR;
    /**
     * The particular current scene graph renderer
     **/
    private IRenderMode renderMode;
    /**
     * Renderers vector, with 0 = normal, 1 = stereoscopic, 2 = FOV, 3 = cubemap
     **/
    private IRenderMode[] sgrList;
    /**
     * Frame buffer map. Holds frame buffers for different resolutions, usually used
     * in screenshots and frame capture.
     */
    private Map<Integer, FrameBuffer> frameBufferMap;

    private final ShadowMapRenderPass shadowMapPass;
    private final LightGlowRenderPass lightGlowPass;

    private final List<RenderPass> renderPasses;

    /**
     * Render groups with autonomous systems. These are systems that do not need renderables.
     */
    private final RenderGroup[] autonomousGroups = new RenderGroup[]{PARTICLE_EFFECTS};

    public SceneRenderer(final XrDriver xrDriver, final GlobalResources globalResources) {
        super();
        this.xrDriver = xrDriver;
        this.globalResources = globalResources;
        this.rendering = new AtomicBoolean(false);
        this.renderAssets = new RenderAssets(globalResources);

        this.shadowMapPass = new ShadowMapRenderPass(this);
        CascadedShadowMapRenderPass cascadedShadowMapRenderPass = new CascadedShadowMapRenderPass(this);
        this.lightGlowPass = new LightGlowRenderPass(this);
        SVTRenderPass svtPass = new SVTRenderPass(this);

        this.shadowMapPass.setCondition(() -> Settings.settings.scene.renderer.shadow.active);
        cascadedShadowMapRenderPass.setCondition(() -> Settings.settings.scene.renderer.shadow.active);

        cascadedShadowMapRenderPass.setEnabled(false);

        this.renderPasses = new ArrayList<>();
        this.renderPasses.add(shadowMapPass);
        this.renderPasses.add(cascadedShadowMapRenderPass);
        this.renderPasses.add(lightGlowPass);
        this.renderPasses.add(svtPass);
    }

    @Override
    public void initialize(AssetManager manager) {
        // Frame buffer map
        frameBufferMap = new HashMap<>();

        // Initialize the render assets.
        renderAssets.initialize(manager);

        renderSystems = new HashMap<>();
        // Render groups are sorted according to
        renderGroups = new ArrayList<>();
        renderGroups.addAll(Arrays.asList(values()));
        renderGroups.sort(Comparator.comparingInt(rg -> rg.priority));

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

        for (var renderPass : renderPasses) {
            renderPass.initialize();
        }
    }

    public void doneLoading(final AssetManager manager) {
        // Prepare render assets.
        renderAssets.doneLoading(manager);

        // Render passes.
        for (var renderPass : renderPasses) {
            renderPass.doneLoading(manager);
        }

        // Initialize render lists.
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
        sgrList = new IRenderMode[4];
        sgrList[SGR_DEFAULT_IDX] = new RenderModeMain();
        sgrList[SGR_STEREO_IDX] = new RenderModeStereoscopic(globalResources.getSpriteBatch());
        sgrList[SGR_CUBEMAP_IDX] = new RenderModeCubemapProjections();
        sgrList[SGR_OPENXR_IDX] = new RenderModeOpenXR(GaiaSky.instance.scene, xrDriver, globalResources.getExtSpriteBatch());
        renderMode = null;

        // INIT GL STATE
        GL30.glClampColor(GL30.GL_CLAMP_READ_COLOR, GL30.GL_FALSE);

        EventManager.instance.subscribe(this, Event.TOGGLE_VISIBILITY_CMD, Event.LINE_RENDERER_UPDATE, Event.STEREOSCOPIC_CMD, Event.CAMERA_MODE_CMD, Event.CUBEMAP_CMD, Event.REBUILD_SHADOW_MAP_DATA_CMD, Event.LIGHT_GLOW_CMD);

    }

    private AbstractRenderSystem initializeRenderSystem(final RenderGroup rg) {
        AbstractRenderSystem system = null;
        switch (rg) {
            case SKYBOX -> // SKYBOX - (MW panorama, CMWB)
                    system = new ModelRenderer(this, SKYBOX, alphas, renderAssets.mbSkybox);
            case MODEL_BG -> // MODEL BACKGROUND - (MW panorama, CMWB)
                    system = new ModelRenderer(this, MODEL_BG, alphas, renderAssets.mbVertexDiffuse);
            case POINT_STAR -> {
                // SINGLE STAR POINTS
                system = new SingleStarQuadRenderer(this, POINT_STAR, alphas, renderAssets.starGroupShaders, ComponentType.Stars);
                system.addPreRunnables(additiveBlendR, noDepthTestR);
            }
            case MODEL_VERT_GRID -> {
                // MODEL GRID - (Ecl, Eq, Gal grids)
                system = new ModelRenderer(this, MODEL_VERT_GRID, alphas, renderAssets.mbVertexLightingGrid);
                system.addPostRunnables(clearDepthR);
            }
            case MODEL_VERT_RECGRID -> {
                // RECURSIVE GRID
                system = new ModelRenderer(this, MODEL_VERT_RECGRID, alphas, renderAssets.mbVertexLightingRecGrid);
                system.addPreRunnables(regularBlendR, depthTestR);
            }
            case FONT_ANNOTATION -> {
                // ANNOTATIONS - (grids)
                system = new TextRenderer(this, FONT_ANNOTATION, alphas, renderAssets.spriteBatch, null, null, renderAssets.font2d, null);
                system.addPreRunnables(regularBlendR, noDepthTestR);
                system.addPostRunnables(clearDepthR);
            }
            case LINE -> system = getLineCPURenderSystem();
            case LINE_GPU -> system = getLineGPURenderSystem();
            case POINT -> system = new PointPrimitiveRenderSystem(this, POINT, alphas, renderAssets.pointShaders);
            case POINT_GPU -> {
                system = new PrimitiveVertexRenderSystem<>(this, POINT_GPU, alphas, renderAssets.primitiveGpuShaders, false);
                system.addPreRunnables(regularBlendR, depthTestR);
            }
            case MODEL_PIX_DUST -> // MODELS DUST AND MESH
                    system = new ModelRenderer(this, MODEL_PIX_DUST, alphas, renderAssets.mbPixelLightingDust);
            case MODEL_VERT_ADDITIVE -> system = new ModelRenderer(this, MODEL_VERT_ADDITIVE, alphas, renderAssets.mbVertexLightingAdditive);
            case MODEL_PIX_EARLY -> // MODEL PER-PIXEL-LIGHTING EARLY
                    system = new ModelRenderer(this, MODEL_PIX_EARLY, alphas, renderAssets.mbPixelLighting);
            case MODEL_VERT_EARLY -> // MODEL PER-VERTEX-LIGHTING EARLY
                    system = new ModelRenderer(this, MODEL_VERT_EARLY, alphas, renderAssets.mbVertexLighting);
            case MODEL_DIFFUSE -> system = new ModelRenderer(this, MODEL_DIFFUSE, alphas, renderAssets.mbVertexDiffuse);
            case MODEL_PIX -> // MODEL PER-PIXEL-LIGHTING
                    system = new ModelRenderer(this, MODEL_PIX, alphas, renderAssets.mbPixelLighting);
            case MODEL_PIX_TESS -> {
                // MODEL PER-PIXEL-LIGHTING-TESSELLATION
                system = new TessellationRenderer(this, MODEL_PIX_TESS, alphas, renderAssets.mbPixelLightingTessellation);
                system.addPreRunnables(regularBlendR, depthTestR);
            }
            case BILLBOARD_GROUP -> system = new BillboardSetRenderer(this, BILLBOARD_GROUP, alphas, renderAssets.billboardGroupShaders);
            case PARTICLE_GROUP -> {
                final PointCloudMode pointCloudModeParticles = Settings.settings.scene.renderer.pointCloud;
                system = switch (pointCloudModeParticles) {
                    case TRIANGLES -> new ParticleSetInstancedRenderer(this, PARTICLE_GROUP, alphas, renderAssets.particleGroupShaders);
                    case POINTS -> new ParticleSetPointRenderer(this, PARTICLE_GROUP, alphas, renderAssets.particleGroupShaders);
                };
                system.addPreRunnables(additiveBlendR, depthTestR, noDepthWritesR);
                system.addPostRunnables(regularBlendR, depthWritesR);
            }
            case PARTICLE_GROUP_EXT_BILLBOARD -> {
                system = new ParticleSetInstancedRenderer(this, PARTICLE_GROUP_EXT_BILLBOARD, alphas, renderAssets.particleGroupExtBillboardShaders);
                system.addPreRunnables(additiveBlendR, depthTestR, noDepthWritesR);
                system.addPostRunnables(regularBlendR, depthWritesR);
            }
            case PARTICLE_GROUP_EXT_MODEL -> {
                system = new ParticleSetInstancedRenderer(this, PARTICLE_GROUP_EXT_MODEL, alphas, renderAssets.particleGroupExtModelShaders);
                system.addPreRunnables(additiveBlendR, depthTestR, noDepthWritesR);
                system.addPostRunnables(regularBlendR, depthWritesR);
            }
            case STAR_GROUP -> {
                final PointCloudMode pointCloudMode = Settings.settings.scene.renderer.pointCloud;
                system = switch (pointCloudMode) {
                    case TRIANGLES -> new StarSetInstancedRenderer(this, STAR_GROUP, alphas, renderAssets.starGroupShaders);
                    case POINTS -> new StarSetPointRenderer(this, STAR_GROUP, alphas, renderAssets.starGroupShaders);
                };
                system.addPreRunnables(additiveBlendR, depthTestR, noDepthWritesR);
                system.addPostRunnables(regularBlendR, depthWritesR);
            }
            case VARIABLE_GROUP -> {
                final PointCloudMode pointCloudMode = Settings.settings.scene.renderer.pointCloud;
                system = switch (pointCloudMode) {
                    case TRIANGLES -> new VariableSetInstancedRenderer(this, VARIABLE_GROUP, alphas, renderAssets.variableGroupShaders);
                    case POINTS -> new VariableSetPointRenderer(this, VARIABLE_GROUP, alphas, renderAssets.variableGroupShaders);
                };
                system.addPreRunnables(additiveBlendR, depthTestR, noDepthWritesR);
                system.addPostRunnables(regularBlendR, depthWritesR);
            }
            case ORBITAL_ELEMENTS_PARTICLE -> {
                system = new ElementsRenderer(this, ORBITAL_ELEMENTS_PARTICLE, alphas, renderAssets.orbitElemShaders);
                system.addPreRunnables(additiveBlendR, depthTestR, noDepthWritesR);
                system.addPostRunnables(regularBlendR, depthWritesR);
            }
            case ORBITAL_ELEMENTS_GROUP -> {
                system = new ElementsSetRenderer(this, ORBITAL_ELEMENTS_GROUP, alphas, renderAssets.orbitElemShaders);
                system.addPreRunnables(additiveBlendR, depthTestR, noDepthWritesR);
                system.addPostRunnables(regularBlendR, depthWritesR);

            }
            case MODEL_VERT_STAR -> // MODEL STARS
                    system = new ModelRenderer(this, MODEL_VERT_STAR, alphas, renderAssets.mbVertexLightingStarSurface);
            case FONT_LABEL -> // LABELS
                    system = new TextRenderer(this, FONT_LABEL, alphas, renderAssets.fontBatch, renderAssets.distanceFieldFontShader, renderAssets.font3d, renderAssets.font2d, renderAssets.fontTitles);
            case BILLBOARD_SSO -> {
                system = new BillboardRenderer(this, BILLBOARD_SSO, alphas, renderAssets.billboardShaders, Constants.DATA_LOCATION_TOKEN + "tex/base/sso.png", false);
                system.addPreRunnables(additiveBlendR, depthTestNoWritesR);
            }
            case BILLBOARD_STAR -> {
                system = new BillboardRenderer(this, BILLBOARD_STAR, alphas, renderAssets.billboardShaders, Settings.settings.scene.star.getStarTexture(), true);
                system.addPreRunnables(additiveBlendR, depthTestNoWritesR);
                system.addPostRunnables(lightGlowPass.getLpu());
            }
            case BILLBOARD_GAL -> {
                system = new BillboardRenderer(this, BILLBOARD_GAL, alphas, renderAssets.galShaders, Constants.DATA_LOCATION_TOKEN + "tex/base/static.jpg", false);
                system.addPreRunnables(additiveBlendR, depthTestNoWritesR);
            }
            case BILLBOARD_SPRITE -> {
                system = new BillboardRenderer(this, BILLBOARD_SPRITE, alphas, renderAssets.spriteShaders, null, false);
                system.addPreRunnables(additiveBlendR, depthTestNoWritesR);
            }
            case MODEL_ATM -> // MODEL ATMOSPHERE
                    system = new ModelRenderer(this, MODEL_ATM, alphas, renderAssets.mbAtmosphere) {
                        @Override
                        public float getAlpha(IRenderable s) {
                            return alphas[ComponentType.Atmospheres.ordinal()] * (float) FastMath.pow(alphas[s.getComponentType().getFirstOrdinal()], 2);
                        }

                        @Override
                        protected boolean mustRender() {
                            return alphas[ComponentType.Atmospheres.ordinal()] * alphas[ComponentType.Planets.ordinal()] > 0;
                        }
                    };
            case MODEL_CLOUD -> // MODEL CLOUDS
                    system = new ModelRenderer(this, MODEL_CLOUD, alphas, renderAssets.mbCloud);
            case MODEL_PIX_TRANSPARENT -> // MODEL PER-PIXEL-LIGHTING WITH TRANSPARENCIES
                    system = new ModelRenderer(this, MODEL_PIX_TRANSPARENT, alphas, renderAssets.mbPixelLighting);
            case LINE_LATE -> {
                // LINE LATE (TRANSPARENCIES)
                system = new LinePrimitiveRenderer(this, LINE_LATE, alphas, renderAssets.lineCpuShaders);
                system.addPreRunnables(regularBlendR, depthTestR, noDepthWritesR);
            }
            case VOLUME -> // VOLUME MODEL
                    system = new VolumeRenderer(this, VOLUME, alphas);
            case PARTICLE_EFFECTS -> {
                system = new ParticleEffectsRenderer(this, PARTICLE_EFFECTS, alphas, renderAssets.particleEffectShaders);
                system.addPreRunnables(additiveBlendR, noDepthTestR);
                system.addPostRunnables(regularBlendR);
            }
            case SHAPE -> {
                system = new ShapeRenderer(this, SHAPE, alphas, globalResources.getShapeShader());
                system.addPreRunnables(regularBlendR, depthTestR);
            }
            case SPRITE -> {
                system = new SpriteRenderer(this, SPRITE, alphas, globalResources.getSpriteShader());
                system.addPreRunnables(regularBlendR, noDepthTestR);
            }
        }

        // Add system.
        if (system != null) {
            addRenderSystem(system);
        }
        return system;
    }

    private void addRenderSystem(IRenderSystem renderSystem) {
        renderSystems.put(renderSystem.getRenderGroup(), renderSystem);
    }

    public synchronized void setRendering(boolean rendering) {
        this.rendering.set(rendering);
    }

    public List<List<IRenderable>> getRenderLists() {
        return renderLists;
    }

    private void initRenderMode(ICamera camera) {
        if (Settings.settings.runtime.openXr) {
            // Using Steam OpenVR renderer
            renderMode = sgrList[SGR_OPENXR_IDX];
        } else if (Settings.settings.program.modeStereo.active) {
            // Stereoscopic mode
            renderMode = sgrList[SGR_STEREO_IDX];
        } else if (Settings.settings.program.modeCubemap.active) {
            // 360 mode: cube map -> equi-rectangular map
            renderMode = sgrList[SGR_CUBEMAP_IDX];
        } else {
            // Default mode
            renderMode = sgrList[SGR_DEFAULT_IDX];
        }
    }

    public void renderModel(IRenderable r, IntModelBatch batch) {
        if (r instanceof Render render) {
            if (Mapper.model.has(render.entity)) {
                modelEntityRenderSystem.renderOpaque(render.entity, batch, (float) 1, false);
            }
        }
    }

    public void clearScreen() {
        Gdx.gl.glClearColor(0, 0, 0, 0);
        GL32.glClearDepth(1000000.0);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
    }

    /**
     * Main render function.
     *
     * @param camera The camera.
     * @param t      The session time, in seconds.
     * @param rw     The actual width of the frame buffer to render.
     * @param rh     The actual height of the frame buffer to render.
     * @param tw     The width of the target to render.
     * @param th     The height of the target to render.
     * @param fb     The frame buffer. Null to render to screen.
     * @param ppb    The post process bean.
     */
    public void render(final ICamera camera,
                       final double t,
                       final int rw,
                       final int rh,
                       final int tw,
                       final int th,
                       final FrameBuffer fb,
                       final PostProcessBean ppb) {

        if (rendering.get()) {
            // Init render mode (stereo, 360, etc.) if necessary.
            if (renderMode == null) {
                initRenderMode(camera);
            }

            // Do all render passes (SVT, shadow mapping, etc.).
            for (var renderPass : renderPasses) {
                renderPass.render(camera);
            }

            // Main render operation.
            renderMode.render(this, camera, t, rw, rh, tw, th, fb, ppb);
        }
    }

    @Override
    public IRenderMode getRenderProcess() {
        return renderMode;
    }

    @Override
    public FrameBuffer getGlowFrameBuffer() {
        return lightGlowPass.getOcclusionFrameBuffer();
    }

    public IRenderSystem getOrInitializeRenderSystem(RenderGroup rg) {
        var renderSystem = renderSystems.get(rg);
        if (renderSystem == null) {
            // Initialize it.
            renderSystem = initializeRenderSystem(rg);
        }
        return renderSystem;
    }

    /**
     * Renders the scene given a camera, a session time in seconds and a render context.
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

            // Iterate over render groups and get systems.
            for (var renderGroup : renderGroups) {
                List<IRenderable> l = renderLists.get(renderGroup.ordinal());
                if (l != null && !l.isEmpty()) {
                    var renderSystem = getOrInitializeRenderSystem(renderGroup);
                    if (renderSystem != null) {
                        renderSystem.render(l, camera, t, renderContext);
                    }
                }
            }
        } catch (GdxRuntimeException | GaiaSkyShaderCompileException gre) {
            // Escalate.
            throw gre;
        } catch (Exception e) {
            // Maybe we can live with this, only log.
            logger.error(e);
        }

    }

    /**
     * Empty renderable object to act as a stub so that autonomous systems are processed.
     */
    private final IRenderable stubRenderable = new IRenderable() {
        @Override
        public ComponentTypes getComponentType() {
            return null;
        }

        @Override
        public double getDistToCamera() {
            return 0;
        }

        @Override
        public float getOpacity() {
            return 0;
        }
    };

    /**
     * This must be called when all the rendering for the current frame has
     * finished.
     */
    public void swapRenderLists() {
        // Clear lists to get them ready for update pass.
        for (var rg : values()) {
            renderLists.get(rg.ordinal()).clear();
        }

        // Add stub for particle effects (the only system that does not need renderables).
        for (var rg : autonomousGroups) {
            renderLists.get(rg.ordinal()).add(stubRenderable);
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
        boolean allOn = comp.isEmpty() || comp.allSetLike(visible);

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

    public boolean isVR() {
        return xrDriver != null;
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
                            if (newVisibility) visible.set(ct.ordinal());
                            else visible.clear(ct.ordinal());
                            times[idx] = (long) (GaiaSky.instance.getT() * 1000f);
                        }
                    } else {
                        // Only toggle
                        visible.flip(ct.ordinal());
                        times[idx] = (long) (GaiaSky.instance.getT() * 1000f);
                    }
                }
            }
            case LINE_RENDERER_UPDATE -> GaiaSky.postRunnable(this::updateLineRenderSystems);
            case STEREOSCOPIC_CMD -> {
                if (!isVR()) {
                    boolean stereo = (Boolean) data[0];
                    if (stereo) renderMode = sgrList[SGR_STEREO_IDX];
                    else {
                        if (Settings.settings.runtime.openXr) renderMode = sgrList[SGR_OPENXR_IDX];
                        else renderMode = sgrList[SGR_DEFAULT_IDX];
                    }
                }
            }
            case CUBEMAP_CMD -> {
                boolean cubemap = (Boolean) data[0] && !Settings.settings.runtime.openXr;
                if (cubemap) {
                    renderMode = sgrList[SGR_CUBEMAP_IDX];
                } else {
                    if (Settings.settings.runtime.openXr) renderMode = sgrList[SGR_OPENXR_IDX];
                    else renderMode = sgrList[SGR_DEFAULT_IDX];
                }
            }
            case CAMERA_MODE_CMD -> {
                CameraMode cm = (CameraMode) data[0];
                if (Settings.settings.runtime.openXr) renderMode = sgrList[SGR_OPENXR_IDX];
                else if (Settings.settings.program.modeStereo.active) renderMode = sgrList[SGR_STEREO_IDX];
                else if (Settings.settings.program.modeCubemap.active) renderMode = sgrList[SGR_CUBEMAP_IDX];
                else renderMode = sgrList[SGR_DEFAULT_IDX];

            }
            case REBUILD_SHADOW_MAP_DATA_CMD -> shadowMapPass.buildShadowMapData();
            case LIGHT_GLOW_CMD -> {
                boolean glow = (Boolean) data[0];
                if (glow) {
                    lightGlowPass.buildLightGlowData();
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
            return visible.get(ordinal) ? MathUtilsDouble.lint(diff, 0, Settings.settings.scene.fadeMs, 0, 1) : MathUtilsDouble.lint(diff, 0, Settings.settings.scene.fadeMs, 1, 0);
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
            resizeRenderSystems(tw, th);
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
     */
    public void resizeRenderSystems(final int tw, final int th) {
        var systems = renderSystems.values();
        for (IRenderSystem rendSys : systems) {
            rendSys.resize(tw, th);
        }
    }

    public void dispose() {
        // Dispose render systems
        if (renderSystems != null) {
            var systems = renderSystems.values();
            for (IRenderSystem rendSys : systems) {
                rendSys.dispose();
            }
            renderSystems.clear();
        }

        // Dispose SGRs
        if (sgrList != null) {
            Arrays.stream(sgrList).forEach(IRenderMode::dispose);
            sgrList = null;
        }

        // Dispose render passes.
        if (renderPasses != null) {
            renderPasses.forEach(Disposable::dispose);
        }
    }

    public void updateLineRenderSystems() {
        // CPU lines.
        var currentCPU = renderSystems.get(RenderGroup.LINE);
        if (currentCPU != null) {
            renderSystems.remove(currentCPU.getRenderGroup());
            AbstractRenderSystem lineSys = getLineCPURenderSystem();
            renderSystems.put(lineSys.getRenderGroup(), lineSys);
            currentCPU.dispose();
        }

        // GPU lines.
        var currentGPU = renderSystems.get(RenderGroup.LINE_GPU);
        if (currentGPU != null) {
            renderSystems.remove(currentGPU.getRenderGroup());
            AbstractRenderSystem lineSys = getLineGPURenderSystem();
            renderSystems.put(lineSys.getRenderGroup(), lineSys);
            currentGPU.dispose();
        }
    }

    private AbstractRenderSystem getLineGPURenderSystem() {
        AbstractRenderSystem sys;
        // We need OpenGL 4.x for the geometry shader (uses double-precision) in the polyline quad-strip renderer.
        ExtShaderProgram[] lineGpuShaders;
        if (Settings.settings.scene.renderer.line.isNormalLineRenderer() || Gdx.graphics.getGLVersion().getMajorVersion() < 4 || Settings.settings.program.safeMode) {
            lineGpuShaders = renderAssets.primitiveGpuShaders;
        } else {
            lineGpuShaders = renderAssets.lineQuadGpuShaders;
        }
        sys = new PrimitiveVertexRenderSystem<>(this, LINE_GPU, alphas, lineGpuShaders, true);
        sys.addPreRunnables(additiveBlendR, depthTestR, noDepthWritesR);
        return sys;
    }

    private AbstractRenderSystem getLineCPURenderSystem() {
        AbstractRenderSystem sys;
        // We need OpenGL 4.x for the geometry shader (uses double-precision) in the polyline quad-strip renderer.
        if (Settings.settings.scene.renderer.line.isNormalLineRenderer() || Gdx.graphics.getGLVersion().getMajorVersion() < 4 || Settings.settings.program.safeMode) {
            // Normal line renderer.
            sys = new LinePrimitiveRenderer(this, LINE, alphas, renderAssets.lineCpuShaders);
            sys.addPreRunnables(regularBlendR, depthTestR, noDepthWritesR);
        } else {
            // Polyline quad-strip renderer.
            sys = new LineQuadstripRenderer(this, LINE, alphas, renderAssets.lineQuadCpuShaders);
            sys.addPreRunnables(additiveBlendR, depthTestR, noDepthWritesR);
        }
        return sys;
    }

    public RenderAssets getRenderAssets() {
        return this.renderAssets;
    }

    public LightGlowRenderPass getLightGlowPass() {
        return this.lightGlowPass;
    }

    /**
     * Resets the render flags for all systems.
     */
    public void resetRenderSystemFlags() {
        var systems = renderSystems.values();
        for (IRenderSystem system : systems) {
            if (system instanceof AbstractRenderSystem) {
                ((AbstractRenderSystem) system).resetFlags();
            }
        }
    }

    public RenderModeOpenXR getRenderModeOpenXR() {
        return (RenderModeOpenXR) sgrList[SGR_OPENXR_IDX];
    }

    public XrDriver getVrContext() {
        return xrDriver;
    }

    public FrameBuffer getFrameBuffer(final int w, final int h) {
        final int key = getKey(w, h);
        if (!frameBufferMap.containsKey(key)) {
            final FrameBuffer fb = PingPongBuffer.createMainFrameBuffer(w, h, true, true, true, Format.RGB888, true);
            frameBufferMap.put(key, fb);
        }
        return frameBufferMap.get(key);
    }

    private int getKey(final int w, final int h) {
        return 31 * h + w;
    }

    public ModelEntityRenderSystem getModelRenderSystem() {
        return modelEntityRenderSystem;
    }

    public Map<XrControllerDevice, Entity> getXRControllerToModel() {
        return getRenderModeOpenXR().getXRControllerToModel();
    }

    public boolean isCubemapRenderMode() {
        return renderMode != null && sgrList != null && renderMode == sgrList[SGR_CUBEMAP_IDX];
    }
}
