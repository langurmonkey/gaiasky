/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer.FrameBufferBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;
import gaia.cu9.ari.gaiaorbit.GaiaSky;
import gaia.cu9.ari.gaiaorbit.assets.AtmosphereShaderProviderLoader.AtmosphereShaderProviderParameter;
import gaia.cu9.ari.gaiaorbit.assets.GroundShaderProviderLoader.GroundShaderProviderParameter;
import gaia.cu9.ari.gaiaorbit.assets.RelativisticShaderProviderLoader.RelativisticShaderProviderParameter;
import gaia.cu9.ari.gaiaorbit.assets.TessellationShaderProviderLoader;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.render.ComponentTypes.ComponentType;
import gaia.cu9.ari.gaiaorbit.render.IPostProcessor.PostProcessBean;
import gaia.cu9.ari.gaiaorbit.render.system.*;
import gaia.cu9.ari.gaiaorbit.render.system.AbstractRenderSystem.RenderSystemRunnable;
import gaia.cu9.ari.gaiaorbit.render.system.ModelBatchRenderSystem.ModelRenderType;
import gaia.cu9.ari.gaiaorbit.scenegraph.AbstractPositionEntity;
import gaia.cu9.ari.gaiaorbit.scenegraph.ModelBody;
import gaia.cu9.ari.gaiaorbit.scenegraph.Particle;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode.RenderGroup;
import gaia.cu9.ari.gaiaorbit.scenegraph.Star;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.CameraManager.CameraMode;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.gdx.IntModelBatch;
import gaia.cu9.ari.gaiaorbit.util.gdx.IntRenderableSorter;
import gaia.cu9.ari.gaiaorbit.util.gdx.contrib.postprocess.filters.Glow;
import gaia.cu9.ari.gaiaorbit.util.gdx.contrib.utils.GaiaSkyFrameBuffer;
import gaia.cu9.ari.gaiaorbit.util.gdx.contrib.utils.ShaderLoader;
import gaia.cu9.ari.gaiaorbit.util.gdx.g2d.BitmapFont;
import gaia.cu9.ari.gaiaorbit.util.gdx.g2d.ExtSpriteBatch;
import gaia.cu9.ari.gaiaorbit.util.gdx.loader.BitmapFontLoader.BitmapFontParameter;
import gaia.cu9.ari.gaiaorbit.util.gdx.shader.*;
import gaia.cu9.ari.gaiaorbit.util.gdx.shader.ShaderProgramProvider.ShaderProgramParameter;
import gaia.cu9.ari.gaiaorbit.util.gdx.shader.provider.IntShaderProvider;
import gaia.cu9.ari.gaiaorbit.util.gravwaves.RelativisticEffectsManager;
import gaia.cu9.ari.gaiaorbit.util.math.Intersectord;
import gaia.cu9.ari.gaiaorbit.util.math.MathUtilsd;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import org.lwjgl.opengl.GL30;

import java.nio.IntBuffer;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Renders a scenegraph.
 *
 * @author Toni Sagrista
 */
public class SceneGraphRenderer extends AbstractRenderer implements IProcessRenderer, IObserver {
    private static final Log logger = Logger.getLogger(SceneGraphRenderer.class);
    public static SceneGraphRenderer instance;

    public static void initialise(AssetManager manager) {
        instance = new SceneGraphRenderer();
        instance.initialize(manager);
    }

    /** Contains the flags representing each type's visibility **/
    public static ComponentTypes visible;
    /** Contains the last update time of each of the flags **/
    public static long[] times;
    /** Alpha values for each type **/
    public static float[] alphas;

    private ExtShaderProgram[] starGroupShaders, particleGroupShaders, particleEffectShaders, orbitElemShaders, pointShaders, lineShaders, lineQuadShaders, lineGpuShaders, galaxyPointShaders, starPointShaders, galShaders, spriteShaders, starBillboardShaders;
    private AssetDescriptor<ExtShaderProgram>[] starGroupDesc, particleGroupDesc, particleEffectDesc, orbitElemDesc, pointDesc, lineDesc, lineQuadDesc, lineGpuDesc, galaxyPointDesc, starPointDesc, galDesc, spriteDesc, starBillboardDesc;

    /** Render lists for all render groups **/
    public static Array<Array<IRenderable>> render_lists;

    // Two model batches, for front (models), back and atmospheres
    private ExtSpriteBatch fontBatch, spriteBatch;

    private Array<IRenderSystem> renderProcesses;

    private RenderSystemRunnable depthTestR, additiveBlendR, noBlendR, noDepthTestR, depthTestAlwaysR, regularBlendR, noDepthWritesR;

    /** The particular current scene graph renderer **/
    private ISGR sgr;
    /**
     * Renderers vector, with 0 = normal, 1 = stereoscopic, 2 = FOV, 3 = cubemap
     **/
    private ISGR[] sgrs;
    // Indexes
    private final int SGR_DEFAULT_IDX = 0, SGR_STEREO_IDX = 1, SGR_FOV_IDX = 2, SGR_CUBEMAP_IDX = 3;

    // Camera at light position, with same direction. For shadow mapping
    private Camera cameraLight;
    private Array<ModelBody> shadowCandidates, shadowCandidatesTess;
    public FrameBuffer[] shadowMapFb;
    private Matrix4[] shadowMapCombined;
    public Map<ModelBody, Texture> smTexMap;
    public Map<ModelBody, Matrix4> smCombinedMap;

    // Light glow pre-render
    private FrameBuffer glowFb;
    private Texture glowTex;
    private IntModelBatch mbPixelLightingDepth, mbPixelLightingOpaque, mbPixelLightingOpaqueTessellation, mbPixelLightingDepthTessellation;

    private Vector3 aux1;
    private Vector3d aux1d, aux2d, aux3d, aux4d;

    private Array<IRenderable> stars;

    private AbstractRenderSystem billboardStarsProc;
    private MWModelRenderSystem mwrs;

    public SceneGraphRenderer() {
        super();
    }

    private AssetDescriptor<ExtShaderProgram>[] loadShader(AssetManager manager, String vfile, String ffile, String[] names, String[] prependVertex) {
        @SuppressWarnings("unchecked") AssetDescriptor<ExtShaderProgram>[] result = new AssetDescriptor[prependVertex.length];

        int i = 0;
        for (String prep : prependVertex) {
            ShaderProgramParameter spp = new ShaderProgramParameter();
            spp.prependVertexCode = prep;
            spp.vertexFile = vfile;
            spp.fragmentFile = ffile;
            manager.load(names[i], ExtShaderProgram.class, spp);
            AssetDescriptor<ExtShaderProgram> desc = new AssetDescriptor<>(names[i], ExtShaderProgram.class, spp);
            result[i] = desc;

            i++;
        }

        return result;
    }

    @Override
    public void initialize(AssetManager manager) {
        ShaderLoader.Pedantic = false;
        ExtShaderProgram.pedantic = false;

        /* DATA LOAD */
        String[] defines = new String[] { "", "#define relativisticEffects\n", "#define gravitationalWaves\n", "#define relativisticEffects\n#define gravitationalWaves\n" };

        // Add shaders to load (no providers)
        starBillboardDesc = loadShader(manager, "shader/star.billboard.vertex.glsl", "shader/star.billboard.fragment.glsl", genShaderNames("star.billboard"), defines);
        spriteDesc = loadShader(manager, "shader/sprite.vertex.glsl", "shader/sprite.fragment.glsl", genShaderNames("sprite"), defines);
        starPointDesc = loadShader(manager, "shader/star.point.vertex.glsl", "shader/star.point.fragment.glsl", genShaderNames("star.point"), defines);
        galaxyPointDesc = loadShader(manager, "shader/galaxy.vertex.glsl", "shader/galaxy.fragment.glsl", genShaderNames("galaxy"), defines);
        pointDesc = loadShader(manager, "shader/point.cpu.vertex.glsl", "shader/point.cpu.fragment.glsl", genShaderNames("point.cpu"), defines);
        lineDesc = loadShader(manager, "shader/line.cpu.vertex.glsl", "shader/line.cpu.fragment.glsl", genShaderNames("line.cpu"), defines);
        lineQuadDesc = loadShader(manager, "shader/line.quad.vertex.glsl", "shader/line.quad.fragment.glsl", genShaderNames("line.quad"), defines);
        lineGpuDesc = loadShader(manager, "shader/line.gpu.vertex.glsl", "shader/line.gpu.fragment.glsl", genShaderNames("line.gpu"), defines);
        galDesc = loadShader(manager, "shader/gal.vertex.glsl", "shader/gal.fragment.glsl", genShaderNames("gal"), defines);
        particleEffectDesc = loadShader(manager, "shader/particle.effect.vertex.glsl", "shader/particle.effect.fragment.glsl", genShaderNames("particle.effect"), defines);
        particleGroupDesc = loadShader(manager, "shader/particle.group.vertex.glsl", "shader/particle.group.fragment.glsl", genShaderNames("particle.group"), defines);
        starGroupDesc = loadShader(manager, "shader/star.group.vertex.glsl", "shader/star.group.fragment.glsl", genShaderNames("star.group"), defines);
        orbitElemDesc = loadShader(manager, "shader/orbitelem.vertex.glsl", "shader/particle.group.fragment.glsl", genShaderNames("orbitelem"), defines);

        // Add shaders to load (with providers)
        manager.load("per-vertex-lighting", GroundShaderProvider.class, new GroundShaderProviderParameter("shader/default.vertex.glsl", "shader/default.fragment.glsl"));
        manager.load("per-vertex-lighting-additive", RelativisticShaderProvider.class, new RelativisticShaderProviderParameter("shader/default.vertex.glsl", "shader/default.additive.fragment.glsl"));
        manager.load("per-vertex-lighting-grid", RelativisticShaderProvider.class, new RelativisticShaderProviderParameter("shader/default.vertex.glsl", "shader/default.grid.fragment.glsl"));
        manager.load("per-vertex-lighting-starsurface", RelativisticShaderProvider.class, new RelativisticShaderProviderParameter("shader/starsurface.vertex.glsl", "shader/starsurface.fragment.glsl"));
        manager.load("per-vertex-lighting-beam", RelativisticShaderProvider.class, new RelativisticShaderProviderParameter("shader/default.vertex.glsl", "shader/beam.fragment.glsl"));

        manager.load("per-pixel-lighting", GroundShaderProvider.class, new GroundShaderProviderParameter("shader/normal.vertex.glsl", "shader/normal.fragment.glsl"));
        manager.load("per-pixel-lighting-tessellation", TessellationShaderProvider.class, new TessellationShaderProviderLoader.TessellationShaderProviderParameter("shader/tessellation/tess.normal.vertex.glsl", "shader/tessellation/tess.normal.control.glsl", "shader/tessellation/tess.normal.eval.glsl", "shader/tessellation/tess.normal.fragment.glsl"));
        manager.load("per-pixel-lighting-dust", GroundShaderProvider.class, new GroundShaderProviderParameter("shader/normal.vertex.glsl", "shader/dust.fragment.glsl"));
        manager.load("per-pixel-lighting-depth", RelativisticShaderProvider.class, new RelativisticShaderProviderParameter("shader/normal.vertex.glsl", "shader/depth.fragment.glsl"));
        manager.load("per-pixel-lighting-depth-tessellation", TessellationShaderProvider.class, new TessellationShaderProviderLoader.TessellationShaderProviderParameter("shader/tessellation/tess.simple.vertex.glsl", "shader/tessellation/tess.depth.control.glsl", "shader/tessellation/tess.simple.eval.glsl", "shader/tessellation/tess.depth.fragment.glsl"));
        manager.load("per-pixel-lighting-opaque", RelativisticShaderProvider.class, new RelativisticShaderProviderParameter("shader/normal.vertex.glsl", "shader/opaque.fragment.glsl"));
        manager.load("per-pixel-lighting-opaque-tessellation", TessellationShaderProvider.class, new TessellationShaderProviderLoader.TessellationShaderProviderParameter("shader/tessellation/tess.simple.vertex.glsl", "shader/tessellation/tess.simple.control.glsl", "shader/tessellation/tess.simple.eval.glsl", "shader/tessellation/tess.opaque.fragment.glsl"));

        manager.load("atmosphere", AtmosphereShaderProvider.class, new AtmosphereShaderProviderParameter("shader/atm.vertex.glsl", "shader/atm.fragment.glsl"));
        manager.load("cloud", GroundShaderProvider.class, new GroundShaderProviderParameter("shader/cloud.vertex.glsl", "shader/cloud.fragment.glsl"));
        manager.load("shader/font.vertex.glsl", ExtShaderProgram.class);

        // Add fonts to load
        BitmapFontParameter bfp = new BitmapFontParameter();
        bfp.magFilter = TextureFilter.Linear;
        bfp.minFilter = TextureFilter.Linear;
        manager.load("font/main-font.fnt", BitmapFont.class, bfp);
        manager.load("font/font2d.fnt", BitmapFont.class, bfp);
        manager.load("font/font-titles.fnt", BitmapFont.class, bfp);

        stars = new Array<>();

        renderProcesses = new Array<>();

        noDepthTestR = (renderSystem, renderables, camera) -> {
            Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
            Gdx.gl.glDepthMask(false);
        };
        depthTestR = (renderSystem, renderables, camera) -> {
            Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
            Gdx.gl.glDepthMask(true);
        };
        depthTestAlwaysR = (renderSystem, renderables, camera) -> {
            Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
            Gdx.gl.glDepthMask(true);
            Gdx.gl.glDepthFunc(GL20.GL_ALWAYS);
        };
        noDepthWritesR = (renderSystem, renderables, camera) -> {
            Gdx.gl.glDepthMask(false);
        };
        additiveBlendR = (renderSystem, renderables, camera) -> {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE);
        };
        regularBlendR = (renderSystem, renderables, camera) -> {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        };
        noBlendR = (renderSystem, renderables, camera) -> {
            Gdx.gl.glDisable(GL20.GL_BLEND);
        };

        if (GlobalConf.scene.SHADOW_MAPPING) {
            // Shadow map camera
            cameraLight = new PerspectiveCamera(0.5f, GlobalConf.scene.SHADOW_MAPPING_RESOLUTION, GlobalConf.scene.SHADOW_MAPPING_RESOLUTION);

            // Aux vectors
            aux1 = new Vector3();
            aux1d = new Vector3d();
            aux2d = new Vector3d();
            aux3d = new Vector3d();
            aux4d = new Vector3d();

            // Build frame buffers and arrays
            buildShadowMapData();
        }

        if (GlobalConf.postprocess.POSTPROCESS_LIGHT_SCATTERING) {
            buildGlowData();
        }
    }

    private ExtShaderProgram[] fetchShaderProgram(AssetManager manager, AssetDescriptor<ExtShaderProgram>[] descriptors, String... names) {
        int n = descriptors.length;
        ExtShaderProgram[] shaders = new ExtShaderProgram[n];

        for (int i = 0; i < n; i++) {
            shaders[i] = manager.get(descriptors[i]);
            if (!shaders[i].isCompiled()) {
                logger.error(names[i] + " shader compilation failed:\n" + shaders[i].getLog());
            }
        }
        return shaders;
    }

    public void doneLoading(AssetManager manager) {
        IntBuffer intBuffer = BufferUtils.newIntBuffer(16);
        Gdx.gl20.glGetIntegerv(GL20.GL_MAX_TEXTURE_SIZE, intBuffer);
        int maxTexSize = intBuffer.get();
        logger.info("Max texture size: " + maxTexSize + "^2 pixels");

        /*
          STAR BILLBOARD SHADER
         */
        starBillboardShaders = fetchShaderProgram(manager, starBillboardDesc, genShaderFullNames("star.billboard"));

        /*
         * GALAXY SHADER
         */
        galShaders = fetchShaderProgram(manager, galDesc, genShaderFullNames("gal"));

        /*
         * FONT SHADER
         */
        ExtShaderProgram distanceFieldFontShader = manager.get("shader/font.vertex.glsl");
        if (!distanceFieldFontShader.isCompiled()) {
            logger.error("Distance field font shader compilation failed:\n" + distanceFieldFontShader.getLog());
        }

        /*
         * SPRITE SHADER
         */
        spriteShaders = fetchShaderProgram(manager, spriteDesc, genShaderFullNames("sprite"));

        /*
         * POINT CPU
         */
        pointShaders = fetchShaderProgram(manager, pointDesc, genShaderFullNames("point.cpu"));

        /*
         * LINE CPU
         */
        lineShaders = fetchShaderProgram(manager, lineDesc, genShaderFullNames("line.cpu"));

        /*
         * LINE QUAD
         */
        lineQuadShaders = fetchShaderProgram(manager, lineQuadDesc, genShaderFullNames("line.quad"));

        /*
         * LINE GPU
         */
        lineGpuShaders = fetchShaderProgram(manager, lineGpuDesc, genShaderFullNames("line.gpu"));

        /*
         * GALAXY POINTS
         */
        galaxyPointShaders = fetchShaderProgram(manager, galaxyPointDesc, genShaderFullNames("galaxy"));

        /*
         * PARTICLE EFFECT - default and relativistic
         */
        particleEffectShaders = fetchShaderProgram(manager, particleEffectDesc, genShaderFullNames("particle.effect"));

        /*
         * PARTICLE GROUP - default and relativistic
         */
        particleGroupShaders = fetchShaderProgram(manager, particleGroupDesc, genShaderFullNames("particle.group"));

        /*
         * STAR GROUP - default and relativistic
         */
        starGroupShaders = fetchShaderProgram(manager, starGroupDesc, genShaderFullNames("star.group"));

        /*
         * STAR POINT
         */
        starPointShaders = fetchShaderProgram(manager, starPointDesc, genShaderFullNames("star.point"));

        /*
         * ORBITAL ELEMENTS PARTICLES - default and relativistic
         */
        orbitElemShaders = fetchShaderProgram(manager, orbitElemDesc, genShaderFullNames("orbitelem"));

        RenderGroup[] renderGroups = RenderGroup.values();
        render_lists = new Array<>(renderGroups.length);
        for (int i = 0; i < renderGroups.length; i++) {
            render_lists.add(new Array<>(100));
        }

        // Per-vertex lighting shaders
        IntShaderProvider perVertexLighting = manager.get("per-vertex-lighting");
        IntShaderProvider perVertexLightingAdditive = manager.get("per-vertex-lighting-additive");
        IntShaderProvider perVertexLightingGrid = manager.get("per-vertex-lighting-grid");
        IntShaderProvider perVertexLightingStarsurface = manager.get("per-vertex-lighting-starsurface");
        IntShaderProvider perVertexLightingBeam = manager.get("per-vertex-lighting-beam");

        // Per-pixel lighting shaders
        IntShaderProvider perPixelLighting = manager.get("per-pixel-lighting");
        TessellationShaderProvider perPixelLightingTessellation = manager.get("per-pixel-lighting-tessellation");
        IntShaderProvider perPixelLightingDust = manager.get("per-pixel-lighting-dust");
        IntShaderProvider perPixelLightingDepth = manager.get("per-pixel-lighting-depth");
        IntShaderProvider perPixelLightingDepthTessellation = manager.get("per-pixel-lighting-depth-tessellation");
        IntShaderProvider perPixelLightingOpaque = manager.get("per-pixel-lighting-opaque");
        TessellationShaderProvider perPixelLightingOpaqueTessellation = manager.get("per-pixel-lighting-opaque-tessellation");

        // Others
        IntShaderProvider atmosphere = manager.get("atmosphere");
        IntShaderProvider cloud = manager.get("cloud");

        // Create empty sorter
        IntRenderableSorter noSorter = (camera, renderables) -> {
            // Does nothing
        };

        // Create model batches
        IntModelBatch mbVertexLighting = new IntModelBatch(perVertexLighting, noSorter);
        IntModelBatch mbVertexLightingAdditive = new IntModelBatch(perVertexLightingAdditive, noSorter);
        IntModelBatch mbVertexLightingStarsurface = new IntModelBatch(perVertexLightingStarsurface, noSorter);
        IntModelBatch mbVertexLightingBeam = new IntModelBatch(perVertexLightingBeam, noSorter);
        IntModelBatch mbVertexLightingGrid = new IntModelBatch(perVertexLightingGrid, noSorter);

        IntModelBatch mbPixelLighting = new IntModelBatch(perPixelLighting, noSorter);
        IntModelBatch mbPixelLightingDust = new IntModelBatch(perPixelLightingDust, noSorter);
        mbPixelLightingDepth = new IntModelBatch(perPixelLightingDepth, noSorter);
        mbPixelLightingOpaque = new IntModelBatch(perPixelLightingOpaque, noSorter);
        IntModelBatch mbPixelLightingTessellation = new IntModelBatch(perPixelLightingTessellation, noSorter);
        mbPixelLightingOpaqueTessellation = new IntModelBatch(perPixelLightingOpaqueTessellation, noSorter);
        mbPixelLightingDepthTessellation = new IntModelBatch(perPixelLightingDepthTessellation, noSorter);

        IntModelBatch mbAtmosphere = new IntModelBatch(atmosphere, noSorter);
        IntModelBatch mbCloud = new IntModelBatch(cloud, noSorter);

        // Fonts - all of these are distance field fonts
        BitmapFont font3d = manager.get("font/main-font.fnt");
        BitmapFont font2d = manager.get("font/font2d.fnt");
        BitmapFont fontTitles = manager.get("font/font-titles.fnt");

        // Sprites
        spriteBatch = GlobalResources.extSpriteBatch;
        spriteBatch.enableBlending();

        // Font batch
        fontBatch = new ExtSpriteBatch(2000, distanceFieldFontShader);
        fontBatch.enableBlending();

        ComponentType[] comps = ComponentType.values();

        // Set reference
        visible = new ComponentTypes();
        for (int i = 0; i < GlobalConf.scene.VISIBILITY.length; i++) {
            if (GlobalConf.scene.VISIBILITY[i]) {
                visible.set(ComponentType.values()[i].ordinal());
            }
        }
        // Invisible are always visible :_D
        visible.set(ComponentType.Invisible.ordinal());

        times = new long[comps.length];
        alphas = new float[comps.length];
        for (int i = 0; i < comps.length; i++) {
            times[i] = -20000L;
            alphas[i] = 0f;
        }

        /*
         * INITIALIZE SGRs
         */
        sgrs = new ISGR[4];
        sgrs[SGR_DEFAULT_IDX] = new SGR();
        sgrs[SGR_STEREO_IDX] = new SGRStereoscopic();
        sgrs[SGR_FOV_IDX] = new SGRFov();
        sgrs[SGR_CUBEMAP_IDX] = new SGRCubemap();
        sgr = null;

        /*
         *
         * ======= INITIALIZE RENDER COMPONENTS =======
         *
         */

        // POINTS
        AbstractRenderSystem pixelStarProc = new StarPointRenderSystem(RenderGroup.POINT_STAR, alphas, starPointShaders, ComponentType.Stars);
        pixelStarProc.addPreRunnables(additiveBlendR, noDepthTestR);

        // MODEL BACKGROUND - (MW panorama, CMWB)
        AbstractRenderSystem modelBackgroundProc = new ModelBatchRenderSystem(RenderGroup.MODEL_VERT, alphas, mbVertexLighting, ModelRenderType.NORMAL);
        modelBackgroundProc.addPreRunnables(regularBlendR, depthTestR);
        modelBackgroundProc.addPostRunnables((renderSystem, renderables, camera) -> {
            // This always goes at the back, clear depth buffer
            Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
        });

        // MODEL GRID - (Ecl, Eq, Gal grids)
        AbstractRenderSystem modelGridsProc = new ModelBatchRenderSystem(RenderGroup.MODEL_VERT_GRID, alphas, mbVertexLightingGrid, ModelRenderType.NORMAL);
        modelGridsProc.addPreRunnables(regularBlendR, depthTestR);
        modelGridsProc.addPostRunnables((renderSystem, renderables, camera) -> {
            // This always goes at the back, clear depth buffer
            Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
        });

        // ANNOTATIONS - (grids)
        AbstractRenderSystem annotationsProc = new FontRenderSystem(RenderGroup.FONT_ANNOTATION, alphas, spriteBatch, null, null, font2d, null);
        annotationsProc.addPreRunnables(regularBlendR, noDepthTestR);
        annotationsProc.addPostRunnables((renderSystem, renderables, camera) -> {
            // This always goes at the back, clear depth buffer
            Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
        });

        // BILLBOARD STARS
        billboardStarsProc = new BillboardStarRenderSystem(RenderGroup.BILLBOARD_STAR, alphas, starBillboardShaders, "data/tex/base/star_glow_s.png", ComponentType.Stars.ordinal());
        billboardStarsProc.addPreRunnables(additiveBlendR, noDepthTestR);
        billboardStarsProc.addPostRunnables(new RenderSystemRunnable() {

            private float[] positions = new float[Glow.N * 2];
            private float[] viewAngles = new float[Glow.N];
            private float[] colors = new float[Glow.N * 3];
            private Vector3 auxV = new Vector3();
            private Vector3d auxD = new Vector3d();

            @Override
            public void run(AbstractRenderSystem renderSystem, Array<IRenderable> renderables, ICamera camera) {
                int size = renderables.size;
                if (PostProcessorFactory.instance.getPostProcessor().isLightScatterEnabled() && Particle.renderOn) {
                    // Compute light positions for light scattering or light
                    // glow
                    int lightIndex = 0;
                    float angleEdgeDeg = camera.getAngleEdge() * MathUtils.radDeg;
                    for (int i = size - 1; i >= 0; i--) {
                        IRenderable s = renderables.get(i);
                        if (s instanceof Particle) {
                            Particle p = (Particle) s;
                            if (lightIndex < Glow.N && (GlobalConf.program.CUBEMAP360_MODE || GaiaSky.instance.cam.getDirection().angle(p.translation) < angleEdgeDeg)) {
                                Vector3d pos3d = p.translation.put(auxD);

                                // Aberration
                                GlobalResources.applyRelativisticAberration(pos3d, camera);
                                // GravWaves
                                RelativisticEffectsManager.getInstance().gravitationalWavePos(pos3d);
                                Vector3 pos3 = pos3d.put(auxV);

                                camera.getCamera().project(pos3);
                                // Here we **need** to use
                                // Gdx.graphics.getWidth/Height() because we use
                                // camera.project() which uses screen
                                // coordinates only
                                positions[lightIndex * 2] = auxV.x / Gdx.graphics.getWidth();
                                positions[lightIndex * 2 + 1] = auxV.y / Gdx.graphics.getHeight();
                                viewAngles[lightIndex] = (float) p.viewAngleApparent;
                                colors[lightIndex * 3] = p.cc[0];
                                colors[lightIndex * 3 + 1] = p.cc[1];
                                colors[lightIndex * 3 + 2] = p.cc[2];
                                lightIndex++;
                            }
                        }
                    }
                    EventManager.instance.post(Events.LIGHT_POS_2D_UPDATED, lightIndex, positions, viewAngles, colors, glowTex);
                } else {
                    EventManager.instance.post(Events.LIGHT_POS_2D_UPDATED, 0, positions, viewAngles, colors, glowTex);
                }
            }

        });

        // BILLBOARD GALAXIES
        AbstractRenderSystem billboardGalaxiesProc = new BillboardStarRenderSystem(RenderGroup.BILLBOARD_GAL, alphas, galShaders, "data/tex/base/static.jpg", ComponentType.Galaxies.ordinal());
        billboardGalaxiesProc.addPreRunnables(regularBlendR, noDepthTestR);

        // BILLBOARD SPRITES
        AbstractRenderSystem billboardSpritesProc = new BillboardSpriteRenderSystem(RenderGroup.BILLBOARD_SPRITE, alphas, spriteShaders, ComponentType.Clusters.ordinal());
        billboardSpritesProc.addPreRunnables(additiveBlendR, depthTestR, noDepthWritesR);

        // LINES CPU
        AbstractRenderSystem lineProc = getLineRenderSystem();

        // LINES GPU
        AbstractRenderSystem lineGpuProc = new VertGPURenderSystem(RenderGroup.LINE_GPU, alphas, lineGpuShaders, GL20.GL_LINE_STRIP);
        lineGpuProc.addPreRunnables(regularBlendR, depthTestR);

        // POINTS CPU
        AbstractRenderSystem pointProc = new PointRenderSystem(RenderGroup.POINT, alphas, pointShaders);

        // POINTS GPU
        AbstractRenderSystem pointGpuProc = new VertGPURenderSystem(RenderGroup.POINT_GPU, alphas, lineGpuShaders, GL20.GL_POINTS);
        pointGpuProc.addPreRunnables(regularBlendR, depthTestR);

        // MODELS DUST AND MESH
        AbstractRenderSystem modelMeshOpaqueProc = new ModelBatchRenderSystem(RenderGroup.MODEL_PIX_DUST, alphas, mbPixelLightingDust, ModelRenderType.NORMAL);
        AbstractRenderSystem modelMeshAdditiveProc = new ModelBatchRenderSystem(RenderGroup.MODEL_VERT_ADDITIVE, alphas, mbVertexLightingAdditive, ModelRenderType.NORMAL);

        // MODEL PER-PIXEL-LIGHTING
        AbstractRenderSystem modelPerPixelLighting = new ModelBatchRenderSystem(RenderGroup.MODEL_PIX, alphas, mbPixelLighting, ModelRenderType.NORMAL);
        modelPerPixelLighting.addPreRunnables(regularBlendR, depthTestR);

        // MODEL PER-PIXEL-LIGHTING-TESSELLATION
        AbstractRenderSystem modelPerPixelLightingTess = new ModelBatchTessellationRenderSystem(RenderGroup.MODEL_PIX_TESS, alphas, mbPixelLightingTessellation);
        modelPerPixelLightingTess.addPreRunnables(regularBlendR, depthTestR);

        // MODEL BEAM
        AbstractRenderSystem modelBeamProc = new ModelBatchRenderSystem(RenderGroup.MODEL_VERT_BEAM, alphas, mbVertexLightingBeam, ModelRenderType.NORMAL);
        modelBeamProc.addPreRunnables(regularBlendR, depthTestR);

        // GALAXY
        mwrs = new MWModelRenderSystem(RenderGroup.GALAXY, alphas, galaxyPointShaders);
        AbstractRenderSystem milkyWayProc = mwrs;

        // PARTICLE EFFECTS
        AbstractRenderSystem particleEffectsProc = new ParticleEffectsRenderSystem(null, alphas, particleEffectShaders);
        particleEffectsProc.addPreRunnables(additiveBlendR, noDepthTestR);
        particleEffectsProc.addPostRunnables(regularBlendR);

        // PARTICLE GROUP
        AbstractRenderSystem particleGroupProc = new ParticleGroupRenderSystem(RenderGroup.PARTICLE_GROUP, alphas, particleGroupShaders);
        particleGroupProc.addPreRunnables(additiveBlendR, depthTestR, noDepthWritesR);

        // STAR GROUP
        AbstractRenderSystem starGroupProc = new StarGroupRenderSystem(RenderGroup.STAR_GROUP, alphas, starGroupShaders);
        starGroupProc.addPreRunnables(additiveBlendR, depthTestR, noDepthWritesR);

        // ORBITAL ELEMENTS PARTICLES
        AbstractRenderSystem orbitElemProc = new OrbitalElementsParticlesRenderSystem(RenderGroup.PARTICLE_ORBIT_ELEMENTS, alphas, orbitElemShaders);
        orbitElemProc.addPreRunnables(additiveBlendR, depthTestR, noDepthWritesR);

        // MODEL STARS
        AbstractRenderSystem modelStarsProc = new ModelBatchRenderSystem(RenderGroup.MODEL_VERT_STAR, alphas, mbVertexLightingStarsurface, ModelRenderType.NORMAL);
        modelStarsProc.addPreRunnables(regularBlendR, depthTestR);

        // LABELS
        AbstractRenderSystem labelsProc = new FontRenderSystem(RenderGroup.FONT_LABEL, alphas, fontBatch, distanceFieldFontShader, font3d, font2d, fontTitles);
        labelsProc.addPreRunnables(regularBlendR, depthTestR, noDepthWritesR);

        // BILLBOARD SSO
        AbstractRenderSystem billboardSSOProc = new BillboardStarRenderSystem(RenderGroup.BILLBOARD_SSO, alphas, starBillboardShaders, "data/tex/base/sso.png", -1);
        billboardSSOProc.addPreRunnables(additiveBlendR, depthTestR);

        // MODEL ATMOSPHERE
        AbstractRenderSystem modelAtmProc = new ModelBatchRenderSystem(RenderGroup.MODEL_ATM, alphas, mbAtmosphere, ModelRenderType.ATMOSPHERE) {
            @Override
            public float getAlpha(IRenderable s) {
                return alphas[ComponentType.Atmospheres.ordinal()] * (float) Math.pow(alphas[s.getComponentType().getFirstOrdinal()], 2);
            }

            @Override
            protected boolean mustRender() {
                return alphas[ComponentType.Atmospheres.ordinal()] * alphas[ComponentType.Planets.ordinal()] > 0;
            }
        };
        modelAtmProc.addPreRunnables(regularBlendR, depthTestR);

        // MODEL CLOUDS
        AbstractRenderSystem modelCloudProc = new ModelBatchRenderSystem(RenderGroup.MODEL_CLOUD, alphas, mbCloud, ModelRenderType.CLOUD);

        // SHAPES
        AbstractRenderSystem shapeProc = new ShapeRenderSystem(RenderGroup.SHAPE, alphas);
        shapeProc.addPreRunnables(regularBlendR, depthTestR);

        // Add components to set
        renderProcesses.add(modelBackgroundProc);
        renderProcesses.add(modelGridsProc);
        renderProcesses.add(pixelStarProc);
        renderProcesses.add(annotationsProc);

        // Opaque meshes
        renderProcesses.add(modelMeshOpaqueProc);

        // Milky way
        renderProcesses.add(milkyWayProc);

        // Billboards
        renderProcesses.add(billboardStarsProc);

        // Stars, particles
        renderProcesses.add(particleGroupProc);
        renderProcesses.add(starGroupProc);
        renderProcesses.add(orbitElemProc);

        // Models
        renderProcesses.add(modelPerPixelLighting);
        renderProcesses.add(modelPerPixelLightingTess);
        renderProcesses.add(modelBeamProc);

        // Labels
        renderProcesses.add(labelsProc);

        // Additive meshes
        renderProcesses.add(modelMeshAdditiveProc);

        // Galaxy and nebulae billboards
        renderProcesses.add(billboardSpritesProc);
        renderProcesses.add(billboardGalaxiesProc);

        // Primitives
        renderProcesses.add(pointProc);
        renderProcesses.add(pointGpuProc);

        // Lines
        renderProcesses.add(lineProc);
        renderProcesses.add(lineGpuProc);

        // Billboards SSO
        renderProcesses.add(billboardSSOProc);

        // Models
        renderProcesses.add(modelStarsProc);
        renderProcesses.add(modelAtmProc);
        renderProcesses.add(modelCloudProc);
        renderProcesses.add(shapeProc);
        renderProcesses.add(particleEffectsProc);

        // INIT GL STATE
        GL30.glClampColor(GL30.GL_CLAMP_READ_COLOR, GL30.GL_FALSE);
        GL30.glClampColor(GL30.GL_CLAMP_VERTEX_COLOR, GL30.GL_FALSE);
        GL30.glClampColor(GL30.GL_CLAMP_FRAGMENT_COLOR, GL30.GL_FALSE);

        EventManager.instance.subscribe(this, Events.TOGGLE_VISIBILITY_CMD, Events.PIXEL_RENDERER_UPDATE, Events.LINE_RENDERER_UPDATE, Events.STEREOSCOPIC_CMD, Events.CAMERA_MODE_CMD, Events.CUBEMAP360_CMD, Events.REBUILD_SHADOW_MAP_DATA_CMD, Events.LIGHT_SCATTERING_CMD);

    }

    private String[] genShaderNames(String baseName) {
        return new String[] { baseName, baseName + "Rel", baseName + "Grav", baseName + "RelGrav" };
    }

    private String[] genShaderFullNames(String baseName) {
        return new String[] { baseName, baseName + " (rel)", baseName + " (grav)", baseName + " (rel+grav)" };
    }

    private void initSGR(ICamera camera) {
        if (camera.getNCameras() > 1) {
            // FOV mode
            sgr = sgrs[SGR_FOV_IDX];
        } else if (GlobalConf.program.STEREOSCOPIC_MODE) {
            // Stereoscopic mode
            sgr = sgrs[SGR_STEREO_IDX];
        } else if (GlobalConf.program.CUBEMAP360_MODE) {
            // 360 mode: cube map -> equirectangular map
            sgr = sgrs[SGR_CUBEMAP_IDX];
        } else {
            // Default mode
            sgr = sgrs[SGR_DEFAULT_IDX];
        }
    }

    public void renderGlowPass(ICamera camera, FrameBuffer fb) {
        if (fb == null)
            fb = glowFb;
        if (GlobalConf.postprocess.POSTPROCESS_LIGHT_SCATTERING && fb != null) {
            // Get all billboard stars
            Array<IRenderable> bbStars = render_lists.get(RenderGroup.BILLBOARD_STAR.ordinal());

            stars.clear();
            for (IRenderable st : bbStars) {
                if (st instanceof Star) {
                    stars.add(st);
                    break;
                }
            }

            // Get all models
            Array<IRenderable> models = render_lists.get(RenderGroup.MODEL_PIX.ordinal());
            Array<IRenderable> modelsTess = render_lists.get(RenderGroup.MODEL_PIX_TESS.ordinal());

            fb.begin();
            Gdx.gl.glEnable(GL30.GL_DEPTH);
            Gdx.gl.glClearColor(0, 0, 0, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
            Gdx.gl.glClearDepthf(1);

            if (!GlobalConf.program.CUBEMAP360_MODE) {
                // Render billboard stars
                if (stars.size > 0)
                    billboardStarsProc.render(stars, camera, 0, null);

                // Render models
                if (models.size > 0) {
                    mbPixelLightingOpaque.begin(camera.getCamera());
                    for (IRenderable model : models) {
                        if (model instanceof ModelBody) {
                            ModelBody mb = (ModelBody) model;
                            mb.render(mbPixelLightingOpaque, 1, 0, false);
                        }
                    }
                    mbPixelLightingOpaque.end();
                }

                // Render tessellated models
                if (modelsTess.size > 0) {
                    mbPixelLightingOpaqueTessellation.begin(camera.getCamera());
                    for (IRenderable model : modelsTess) {
                        if (model instanceof ModelBody) {
                            ModelBody mb = (ModelBody) model;
                            mb.render(mbPixelLightingOpaqueTessellation, 1, 0, false);
                        }
                    }
                    mbPixelLightingOpaqueTessellation.end();
                }
            }

            // Save to texture for later use
            glowTex = fb.getColorBufferTexture();

            fb.end();

        }

    }

    private void addCandidates(Array<IRenderable> models, Array<ModelBody> candidates, boolean clear) {
        if (candidates != null) {
            if (clear)
                candidates.clear();
            int num = 0;
            for (int i = 0; i < models.size; i++) {
                if (models.get(i) instanceof ModelBody) {
                    ModelBody mr = (ModelBody) models.get(i);
                    if (mr.isShadow()) {
                        candidates.insert(num, mr);
                        mr.shadow = 0;
                        num++;
                        if (num == GlobalConf.scene.SHADOW_MAPPING_N_SHADOWS)
                            break;
                    }
                }
            }
        }
    }

    private void renderShadowMapCandidates(Array<ModelBody> candidates, int shadowNRender, ICamera camera) {
        int i = 0;
        // Normal bodies
        for (ModelBody candidate : candidates) {
            // Yes!
            candidate.shadow = shadowNRender;

            Vector3 camDir = aux1.set(candidate.mc.dlight.direction);
            // Direction is that of the light
            cameraLight.direction.set(camDir);

            double radius = candidate.getRadius();
            // Distance from camera to object, radius * sv[0]
            double distance = radius * candidate.shadowMapValues[0];
            // Position, factor of radius
            candidate.getAbsolutePosition(aux1d);
            aux1d.sub(camera.getPos()).sub(camDir.nor().scl((float) distance));
            aux1d.put(cameraLight.position);
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
            shadowMapFb[i].begin();
            Gdx.gl.glClearColor(0, 0, 0, 0);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

            // No tessellation
            mbPixelLightingDepth.begin(cameraLight);
            candidate.render(mbPixelLightingDepth, 1, 0);
            mbPixelLightingDepth.end();

            // Save frame buffer and combined matrix
            candidate.shadow = shadowNRender;
            shadowMapCombined[i].set(cameraLight.combined);
            smCombinedMap.put(candidate, shadowMapCombined[i]);
            smTexMap.put(candidate, shadowMapFb[i].getColorBufferTexture());

            shadowMapFb[i].end();
            i++;
        }
    }

    private void renderShadowMapCandidatesTess(Array<ModelBody> candidates, int shadowNRender, ICamera camera) {
        int i = 0;
        // Normal bodies
        for (ModelBody candidate : candidates) {
            double radius = candidate.getRadius();
            // Only render when camera very close to surface
            if (candidate.distToCamera < radius * 1.1) {
                candidate.shadow = shadowNRender;

                Vector3 shadowCameraDir = aux1.set(candidate.mc.dlight.direction);

                // Shadow camera direction is that of the light
                cameraLight.direction.set(shadowCameraDir);

                Vector3 shadowCamDir = aux1.set(candidate.mc.dlight.direction);
                // Direction is that of the light
                cameraLight.direction.set(shadowCamDir);

                // Distance from camera to object, radius * sv[0]
                float distance = (float) (radius * candidate.shadowMapValues[0] * 0.01);
                // Position, factor of radius
                Vector3d objPos = candidate.getAbsolutePosition(aux1d);
                Vector3d camPos = camera.getPos();
                Vector3d camDir = aux3d.set(camera.getDirection()).nor().scl(100 * Constants.KM_TO_U);
                boolean intersect = Intersectord.checkIntersectSegmentSphere(camPos, aux3d.set(camPos).add(camDir), objPos, radius);
                if(intersect){
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
                shadowMapFb[i].begin();
                Gdx.gl.glClearColor(0, 0, 0, 0);
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

                // Tessellation
                mbPixelLightingDepthTessellation.begin(cameraLight);
                candidate.render(mbPixelLightingDepthTessellation, 1, 0);
                mbPixelLightingDepthTessellation.end();

                // Save frame buffer and combined matrix
                candidate.shadow = shadowNRender;
                shadowMapCombined[i].set(cameraLight.combined);
                smCombinedMap.put(candidate, shadowMapCombined[i]);
                smTexMap.put(candidate, shadowMapFb[i].getColorBufferTexture());

                shadowMapFb[i].end();
                i++;
            } else {
                candidate.shadow = -1;
            }
        }

    }

    private void renderShadowMap(ICamera camera) {
        if (GlobalConf.scene.SHADOW_MAPPING) {
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
            Array<IRenderable> models = render_lists.get(RenderGroup.MODEL_PIX.ordinal());
            Array<IRenderable> modelsTess = render_lists.get(RenderGroup.MODEL_PIX_TESS.ordinal());
            models.sort(Comparator.comparingDouble(a -> ((AbstractPositionEntity) a).getDistToCamera()));

            int shadowNRender = GlobalConf.program.STEREOSCOPIC_MODE ? 2 : GlobalConf.program.CUBEMAP360_MODE ? 6 : 1;

            if (shadowMapFb != null && smCombinedMap != null) {
                addCandidates(models, shadowCandidates, true);
                addCandidates(modelsTess, shadowCandidatesTess, true);

                // Clear maps
                smTexMap.clear();
                smCombinedMap.clear();

                renderShadowMapCandidates(shadowCandidates, shadowNRender, camera);
                //renderShadowMapCandidatesTess(shadowCandidatesTess, shadowNRender, camera);
            }
        }
    }

    @Override
    public void render(ICamera camera, double t, int rw, int rh, FrameBuffer fb, PostProcessBean ppb) {
        if (sgr == null)
            initSGR(camera);

        // Shadow maps are the same for all
        renderShadowMap(camera);

        // In stereo and cubemap modes, the glow pass is rendered in the SGR itself
        if (!GlobalConf.program.STEREOSCOPIC_MODE && !GlobalConf.program.CUBEMAP360_MODE) {
            renderGlowPass(camera, glowFb);
        }
        sgr.render(this, camera, t, rw, rh, fb, ppb);
    }

    public FrameBuffer getGlowFb() {
        return glowFb;
    }

    /**
     * Renders the scene
     *
     * @param camera The camera to use
     * @param t      The time in seconds since the start
     * @param rc     The render context
     */
    public void renderScene(ICamera camera, double t, RenderingContext rc) {
        // Update time difference since last update
        for (ComponentType ct : ComponentType.values()) {
            alphas[ct.ordinal()] = calculateAlpha(ct, t);
        }

        int size = renderProcesses.size;
        for (int i = 0; i < size; i++) {
            IRenderSystem process = renderProcesses.get(i);
            // If we have no render group, this means all the info is already in
            // the render system. No lists needed
            if (process.getRenderGroup() != null) {
                Array<IRenderable> l = render_lists.get(process.getRenderGroup().ordinal());
                process.render(l, camera, t, rc);
            } else {
                process.render(null, camera, t, rc);
            }
        }
        rc.ppb.pp.getCombinedBuffer().getResultBuffer().getDepthBufferHandle();

    }

    /**
     * Renders all the systems which are the same type of the given class
     *
     * @param camera The camera to use
     * @param t      The time in seconds since the start
     * @param rc     The render contex
     * @param clazz  The class
     */
    public void renderSystem(ICamera camera, double t, RenderingContext rc, Class<? extends IRenderSystem> clazz) {
        // Update time difference since last update
        for (ComponentType ct : ComponentType.values()) {
            alphas[ct.ordinal()] = calculateAlpha(ct, t);
        }

        int size = renderProcesses.size;
        for (int i = 0; i < size; i++) {
            IRenderSystem process = renderProcesses.get(i);
            if (clazz.isInstance(process)) {
                // If we have no render group, this means all the info is already in
                // the render system. No lists needed
                if (process.getRenderGroup() != null) {
                    Array<IRenderable> l = render_lists.get(process.getRenderGroup().ordinal());
                    process.render(l, camera, t, rc);
                } else {
                    process.render(null, camera, t, rc);
                }
            }
        }
    }

    /**
     * This must be called when all the rendering for the current frame has
     * finished.
     */
    public void clearLists() {
        for (RenderGroup rg : RenderGroup.values()) {
            render_lists.get(rg.ordinal()).clear();
        }
    }

    /**
     * Checks if a given component type is on
     *
     * @param comp The component
     * @return Whether the component is on
     */
    public boolean isOn(ComponentType comp) {
        return visible.get(comp.ordinal()) || alphas[comp.ordinal()] > 0;
    }

    /**
     * Checks if the component types are all on
     *
     * @param comp The components
     * @return Whether the components are all on
     */
    public boolean isOn(ComponentTypes comp) {
        boolean allon = comp.allSetLike(visible);

        if (!allon) {
            allon = true;
            for (int i = comp.nextSetBit(0); i >= 0; i = comp.nextSetBit(i + 1)) {
                // operate on index i here
                allon = allon && alphas[i] > 0;
                if (i == Integer.MAX_VALUE) {
                    break; // or (i+1) would overflow
                }
            }
        }
        return allon;
    }

    public boolean isOn(int ordinal) {
        return visible.get(ordinal) || alphas[ordinal] > 0;
    }

    @Override
    public void notify(Events event, final Object... data) {
        switch (event) {
        case TOGGLE_VISIBILITY_CMD:
            ComponentType ct = ComponentType.getFromKey((String) data[0]);
            if (ct != null) {
                int idx = ct.ordinal();
                if (data.length == 3) {
                    // We have the boolean
                    boolean currvis = visible.get(ct.ordinal());
                    boolean newvis = (boolean) data[2];
                    if (currvis != newvis) {
                        // Only update if visibility different
                        if (newvis)
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

        case PIXEL_RENDERER_UPDATE:
            Gdx.app.postRunnable(() -> {
                AbstractRenderSystem.POINT_UPDATE_FLAG = true;
                // updatePixelRenderSystem();
            });
            break;
        case LINE_RENDERER_UPDATE:
            Gdx.app.postRunnable(() -> updateLineRenderSystem());
            break;
        case STEREOSCOPIC_CMD:
            boolean stereo = (Boolean) data[0];
            if (stereo)
                sgr = sgrs[SGR_STEREO_IDX];
            else {
                sgr = sgrs[SGR_DEFAULT_IDX];
            }
            break;
        case CUBEMAP360_CMD:
            boolean cubemap = (Boolean) data[0];
            if (cubemap)
                sgr = sgrs[SGR_CUBEMAP_IDX];
            else
                sgr = sgrs[SGR_DEFAULT_IDX];
            break;
        case CAMERA_MODE_CMD:
            CameraMode cm = (CameraMode) data[0];
            if (cm.isGaiaFov())
                sgr = sgrs[SGR_FOV_IDX];
            else {
                if (GlobalConf.program.STEREOSCOPIC_MODE)
                    sgr = sgrs[SGR_STEREO_IDX];
                else if (GlobalConf.program.CUBEMAP360_MODE)
                    sgr = sgrs[SGR_CUBEMAP_IDX];
                else
                    sgr = sgrs[SGR_DEFAULT_IDX];

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
     * @return The alpha value.
     */
    private float calculateAlpha(ComponentType type, double t) {
        int ordinal = type.ordinal();
        long diff = (long) (t * 1000f) - times[ordinal];
        if (diff > GlobalConf.scene.OBJECT_FADE_MS) {
            if (visible.get(ordinal)) {
                alphas[ordinal] = 1;
            } else {
                alphas[ordinal] = 0;
            }
            return alphas[ordinal];
        } else {
            return visible.get(ordinal) ? MathUtilsd.lint(diff, 0, GlobalConf.scene.OBJECT_FADE_MS, 0, 1) : MathUtilsd.lint(diff, 0, GlobalConf.scene.OBJECT_FADE_MS, 1, 0);
        }
    }

    public void resize(final int w, final int h) {
        resize(w, h, false);
    }

    public void resize(final int w, final int h, boolean resizeRenderSys) {
        if (resizeRenderSys)
            resizeRenderSystems(w, h);

        for (ISGR sgr : sgrs) {
            sgr.resize(w, h);
        }
    }

    public void resizeRenderSystems(final int w, final int h) {
        for (IRenderSystem rendSys : renderProcesses) {
            rendSys.resize(w, h);
        }
    }

    public void dispose() {
        if (sgrs != null)
            for (ISGR sgr : sgrs) {
                if (sgr != null)
                    sgr.dispose();
            }
    }

    /**
     * Builds the shadow map data; frame buffers, arrays, etc.
     */
    private void buildShadowMapData() {
        if (shadowMapFb != null) {
            for (FrameBuffer fb : shadowMapFb)
                fb.dispose();
            shadowMapFb = null;
        }
        shadowMapCombined = null;

        // Shadow map frame buffer
        shadowMapFb = new FrameBuffer[GlobalConf.scene.SHADOW_MAPPING_N_SHADOWS];
        // Shadow map combined matrices
        shadowMapCombined = new Matrix4[GlobalConf.scene.SHADOW_MAPPING_N_SHADOWS];
        // Init
        for (int i = 0; i < GlobalConf.scene.SHADOW_MAPPING_N_SHADOWS; i++) {
            shadowMapFb[i] = new FrameBuffer(Format.RGBA8888, GlobalConf.scene.SHADOW_MAPPING_RESOLUTION, GlobalConf.scene.SHADOW_MAPPING_RESOLUTION, true);
            shadowMapCombined[i] = new Matrix4();
        }
        if (smTexMap == null)
            smTexMap = new HashMap<>();
        smTexMap.clear();

        if (smCombinedMap == null)
            smCombinedMap = new HashMap<>();
        smCombinedMap.clear();

        if (shadowCandidates == null) {
            shadowCandidates = new Array<>(GlobalConf.scene.SHADOW_MAPPING_N_SHADOWS);
            shadowCandidatesTess = new Array<>(GlobalConf.scene.SHADOW_MAPPING_N_SHADOWS);
        }
        shadowCandidates.clear();
        shadowCandidatesTess.clear();
    }

    private void buildGlowData() {
        if (glowFb == null) {
            FrameBufferBuilder fbb = new FrameBufferBuilder(1920, 1080);
            fbb.addBasicColorTextureAttachment(Format.RGBA8888);
            fbb.addBasicDepthRenderBuffer();
            glowFb = new GaiaSkyFrameBuffer(fbb);
        }
    }

    public void updateLineRenderSystem() {
        LineRenderSystem current = null;
        for (IRenderSystem proc : renderProcesses) {
            if (proc instanceof LineRenderSystem) {
                current = (LineRenderSystem) proc;
            }
        }
        final int idx = renderProcesses.indexOf(current, true);
        if ((current instanceof LineQuadRenderSystem && GlobalConf.scene.isNormalLineRenderer()) || (!(current instanceof LineQuadRenderSystem) && !GlobalConf.scene.isNormalLineRenderer())) {
            renderProcesses.removeIndex(idx);
            AbstractRenderSystem lineSys = getLineRenderSystem();
            renderProcesses.insert(idx, lineSys);
            current.dispose();
        }
    }

    private AbstractRenderSystem getLineRenderSystem() {
        AbstractRenderSystem sys;
        if (GlobalConf.scene.isNormalLineRenderer()) {
            // Normal
            sys = new LineRenderSystem(RenderGroup.LINE, alphas, lineShaders);
            sys.addPreRunnables(regularBlendR, depthTestR);
        } else {
            // Quad
            sys = new LineQuadRenderSystem(RenderGroup.LINE, alphas, lineQuadShaders);
            sys.addPreRunnables(additiveBlendR, depthTestR);
        }
        return sys;
    }

}
