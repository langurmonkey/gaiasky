/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render;

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
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Bits;
import com.badlogic.gdx.utils.BufferUtils;
import gaiasky.GaiaSky;
import gaiasky.assets.AtmosphereShaderProviderLoader.AtmosphereShaderProviderParameter;
import gaiasky.assets.GroundShaderProviderLoader.GroundShaderProviderParameter;
import gaiasky.assets.RelativisticShaderProviderLoader.RelativisticShaderProviderParameter;
import gaiasky.assets.TessellationShaderProviderLoader;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.IPostProcessor.PostProcessBean;
import gaiasky.render.system.*;
import gaiasky.render.system.AbstractRenderSystem.RenderSystemRunnable;
import gaiasky.scenegraph.ModelBody;
import gaiasky.scenegraph.Star;
import gaiasky.scenegraph.StubModel;
import gaiasky.scenegraph.camera.CameraManager.CameraMode;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.*;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings.PointCloudMode;
import gaiasky.util.gdx.IntModelBatch;
import gaiasky.util.gdx.IntRenderableSorter;
import gaiasky.util.gdx.contrib.utils.GaiaSkyFrameBuffer;
import gaiasky.util.gdx.contrib.utils.ShaderLoader;
import gaiasky.util.gdx.g2d.BitmapFont;
import gaiasky.util.gdx.g2d.ExtSpriteBatch;
import gaiasky.util.gdx.loader.BitmapFontLoader.BitmapFontParameter;
import gaiasky.util.gdx.shader.*;
import gaiasky.util.gdx.shader.ShaderProgramProvider.ShaderProgramParameter;
import gaiasky.util.gdx.shader.provider.IntShaderProvider;
import gaiasky.util.math.Intersectord;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;
import gaiasky.vr.openvr.VRContext;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL40;

import java.nio.IntBuffer;
import java.util.*;

import static gaiasky.render.SceneGraphRenderer.RenderGroup.*;

/**
 * Renders the scene graph
 */
public class SceneGraphRenderer extends AbstractRenderer implements IProcessRenderer, IObserver {

    /**
     * Describes to which render group this node belongs at a particular time
     * step
     */
    public enum RenderGroup {
        /**
         * Using normal shader for per-pixel lighting
         **/
        MODEL_PIX,
        /**
         * Using default shader, no normal map
         **/
        MODEL_BG,
        /**
         * IntShader - stars
         **/
        BILLBOARD_STAR,
        /**
         * IntShader - galaxies
         **/
        BILLBOARD_GAL,
        /**
         * IntShader - front (planets, satellites...)
         **/
        BILLBOARD_SSO,
        /**
         * Billboard with custom texture
         **/
        BILLBOARD_TEX,
        /**
         * Single pixel
         **/
        POINT_STAR,
        /**
         * Line
         **/
        LINE,
        /**
         * Annotations
         **/
        FONT_ANNOTATION,
        /**
         * Atmospheres of planets
         **/
        MODEL_ATM,
        /**
         * Label
         **/
        FONT_LABEL,
        /**
         * Model star
         **/
        MODEL_VERT_STAR,
        /**
         * Group of billboard datasets
         **/
        BILLBOARD_GROUP,
        /**
         * Model close up
         **/
        MODEL_CLOSEUP,
        /**
         * Beams
         **/
        MODEL_VERT_BEAM,
        /**
         * Particle group
         **/
        PARTICLE_GROUP,
        /**
         * Star group
         **/
        STAR_GROUP,
        /**
         * Shapes
         **/
        SHAPE,
        /**
         * Regular billboard sprite
         **/
        BILLBOARD_SPRITE,
        /**
         * Line GPU
         **/
        LINE_GPU,
        /**
         * A particle defined by orbital elements
         **/
        ORBITAL_ELEMENTS_PARTICLE,
        /**
         * A particle group defined by orbital elements
         **/
        ORBITAL_ELEMENTS_GROUP,
        /**
         * Transparent additive-blended meshes
         **/
        MODEL_VERT_ADDITIVE,
        /**
         * Grids shader
         **/
        MODEL_VERT_GRID,
        /**
         * Clouds
         **/
        MODEL_CLOUD,
        /**
         * Point
         **/
        POINT,
        /**
         * Point GPU
         **/
        POINT_GPU,
        /**
         * Opaque meshes (dust, etc.)
         **/
        MODEL_PIX_DUST,
        /**
         * Tessellated model
         **/
        MODEL_PIX_TESS,
        /**
         * Only diffuse
         **/
        MODEL_DIFFUSE,
        /**
         * Recursive grid
         */
        MODEL_VERT_RECGRID,
        /**
         * Thrusters
         */
        MODEL_VERT_THRUSTER,
        /**
         * Variable star group
         **/
        VARIABLE_GROUP,
        /**
         * Per-pixel lighting (early in the rendering pipeline)
         **/
        MODEL_PIX_EARLY,
        /**
         * Per-vertex lighting (early in the rendering pipeline)
         **/
        MODEL_VERT_EARLY,
        /**
         * None
         **/
        NONE;

        public boolean is(Bits renderGroupMask) {
            return (this.ordinal() < 0 && renderGroupMask.isEmpty()) || renderGroupMask.get(this.ordinal());
        }

        /**
         * Adds the given render groups to the given Bits mask
         *
         * @param renderGroupMask The bit mask
         * @param rgs             The render groups
         *
         * @return The bits instance
         */
        public static Bits add(Bits renderGroupMask, RenderGroup... rgs) {
            for (RenderGroup rg : rgs) {
                renderGroupMask.set(rg.ordinal());
            }
            return renderGroupMask;
        }

        /**
         * Sets the given Bits mask to the given render groups
         *
         * @param renderGroupMask The bit mask
         * @param rgs             The render groups
         *
         * @return The bits instance
         */
        public static Bits set(Bits renderGroupMask, RenderGroup... rgs) {
            renderGroupMask.clear();
            return add(renderGroupMask, rgs);
        }

    }

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
     * Alpha values for each type
     **/
    public float[] alphas;

    private ExtShaderProgram[] lineShaders;
    private ExtShaderProgram[] lineQuadShaders;
    private AssetDescriptor<ExtShaderProgram>[] starGroupDesc, particleGroupDesc, variableGroupDesc, particleEffectDesc, orbitElemDesc, pointDesc, lineDesc, lineQuadDesc, lineGpuDesc, billboardGroupDesc, starPointDesc, galDesc, spriteDesc, starBillboardDesc;

    /**
     * Render lists for all render groups
     **/
    public Array<Array<IRenderable>> renderLists;

    private Array<IRenderSystem> renderSystems;

    private RenderSystemRunnable depthTestR, additiveBlendR, noDepthTestR, regularBlendR, depthTestNoWritesR, noDepthWritesR, depthWritesR, clearDepthR;

    /**
     * The particular current scene graph renderer
     **/
    private ISGR sgr;
    /**
     * Renderers vector, with 0 = normal, 1 = stereoscopic, 2 = FOV, 3 = cubemap
     **/
    private ISGR[] sgrList;
    // Indexes
    private final int SGR_DEFAULT_IDX = 0, SGR_STEREO_IDX = 1, SGR_FOV_IDX = 2, SGR_CUBEMAP_IDX = 3, SGR_OPENVR_IDX = 4;

    // Camera at light position, with same direction. For shadow mapping
    private Camera cameraLight;
    private List<ModelBody> shadowCandidates, shadowCandidatesTess;
    // Dimension 1: number of shadows, dimension 2: number of lights
    public FrameBuffer[][] shadowMapFb;
    // Dimension 1: number of shadows, dimension 2: number of lights
    private Matrix4[][] shadowMapCombined;
    public Map<ModelBody, Texture> smTexMap;
    public Map<ModelBody, Matrix4> smCombinedMap;

    // Light glow pre-render
    private FrameBuffer glowFb;
    private IntModelBatch mbPixelLightingDepth, mbPixelLightingOpaque, mbPixelLightingOpaqueTessellation, mbPixelLightingDepthTessellation;
    private ExtSpriteBatch spriteBatch, fontBatch;
    private LightPositionUpdater lpu;

    private Vector3 aux1;
    private Vector3d aux1d, aux2d, aux3d;
    private Vector3b aux1b;

    // VRContext, may be null
    private final VRContext vrContext;
    private final GlobalResources globalResources;

    private Array<IRenderable> stars;

    private AbstractRenderSystem billboardStarsProc;

    public SceneGraphRenderer(final VRContext vrContext, final GlobalResources globalResources) {
        super();
        this.vrContext = vrContext;
        this.globalResources = globalResources;
    }

    private AssetDescriptor<ExtShaderProgram>[] loadShader(AssetManager manager, String vertexShader, String fragmentShader, String[] names, String[] prepend) {
        @SuppressWarnings("unchecked") AssetDescriptor<ExtShaderProgram>[] result = new AssetDescriptor[prepend.length];

        int i = 0;
        for (String prep : prepend) {
            ShaderProgramParameter spp = new ShaderProgramParameter();
            spp.prependVertexCode = prep;
            spp.prependFragmentCode = prep;
            spp.vertexFile = vertexShader;
            spp.fragmentFile = fragmentShader;
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
        String[] defines = GlobalResources.combinations(new String[] { "#define ssrFlag\n", "#define velocityBufferFlag\n", "#define relativisticEffects\n", "#define gravitationalWaves\n" });
        String[] names = GlobalResources.combinations(new String[] { "SSR", "Velbuff", "Rel", "Grav" });
        // Color mapping in shaders
        String[] definesCmap = GlobalResources.combinations(new String[] { "#define ssrFlag\n", "#define velocityBufferFlag\n", "#define relativisticEffects\n", "#define gravitationalWaves\n", "#define colorMap\n" });
        String[] namesCmap = GlobalResources.combinations(new String[] { "SSR", "Velbuff", "Rel", "Grav", "Colmap" });

        // Add shaders to load (no providers)
        starBillboardDesc = loadShader(manager, "shader/star.billboard.vertex.glsl", "shader/star.billboard.fragment.glsl", TextUtils.concatAll("star.billboard", names), defines);
        spriteDesc = loadShader(manager, "shader/sprite.vertex.glsl", "shader/sprite.fragment.glsl", TextUtils.concatAll("sprite", names), defines);
        billboardGroupDesc = loadShader(manager, "shader/billboard.group.vertex.glsl", "shader/billboard.group.fragment.glsl", TextUtils.concatAll("billboard.group", names), defines);
        pointDesc = loadShader(manager, "shader/point.cpu.vertex.glsl", "shader/point.cpu.fragment.glsl", TextUtils.concatAll("point.cpu", names), defines);
        lineDesc = loadShader(manager, "shader/line.cpu.vertex.glsl", "shader/line.cpu.fragment.glsl", TextUtils.concatAll("line.cpu", names), defines);
        lineQuadDesc = loadShader(manager, "shader/line.quad.vertex.glsl", "shader/line.quad.fragment.glsl", TextUtils.concatAll("line.quad", names), defines);
        lineGpuDesc = loadShader(manager, "shader/line.gpu.vertex.glsl", "shader/line.gpu.fragment.glsl", TextUtils.concatAll("line.gpu", names), defines);
        galDesc = loadShader(manager, "shader/gal.vertex.glsl", "shader/gal.fragment.glsl", TextUtils.concatAll("gal", names), defines);
        particleEffectDesc = loadShader(manager, "shader/particle.effect.vertex.glsl", "shader/particle.effect.fragment.glsl", TextUtils.concatAll("particle.effect", names), defines);
        orbitElemDesc = loadShader(manager, "shader/orbitelem.vertex.glsl", "shader/particle.group.quad.fragment.glsl", TextUtils.concatAll("orbitelem", names), defines);

        // Initialize point cloud shaders - depends on point cloud mode
        final String pointTriSuffix = Settings.settings.scene.renderer.pointCloud.isTriangles() ? ".quad" : "";
        particleGroupDesc = loadShader(manager, "shader/particle.group" + pointTriSuffix + ".vertex.glsl", "shader/particle.group" + pointTriSuffix + ".fragment.glsl", TextUtils.concatAll("particle.group", namesCmap), definesCmap);
        starGroupDesc = loadShader(manager, "shader/star.group" + pointTriSuffix + ".vertex.glsl", "shader/star.group" + pointTriSuffix + ".fragment.glsl", TextUtils.concatAll("star.group", namesCmap), definesCmap);
        variableGroupDesc = loadShader(manager, "shader/variable.group" + pointTriSuffix + ".vertex.glsl", "shader/star.group" + pointTriSuffix + ".fragment.glsl", TextUtils.concatAll("variable.group", namesCmap), definesCmap);
        // Regular stars
        starPointDesc = loadShader(manager, "shader/star.group.vertex.glsl", "shader/star.group.fragment.glsl", TextUtils.concatAll("star.point", names), defines);

        // Add shaders to load (with providers)
        manager.load("per-vertex-lighting", GroundShaderProvider.class, new GroundShaderProviderParameter("shader/default.vertex.glsl", "shader/default.fragment.glsl"));
        manager.load("per-vertex-lighting-additive", RelativisticShaderProvider.class, new RelativisticShaderProviderParameter("shader/default.vertex.glsl", "shader/default.additive.fragment.glsl"));
        manager.load("per-vertex-diffuse", RelativisticShaderProvider.class, new RelativisticShaderProviderParameter("shader/default.vertex.glsl", "shader/default.diffuse.fragment.glsl"));
        manager.load("per-vertex-lighting-grid", RelativisticShaderProvider.class, new RelativisticShaderProviderParameter("shader/default.vertex.glsl", "shader/default.grid.fragment.glsl"));
        manager.load("per-vertex-lighting-recgrid", RelativisticShaderProvider.class, new RelativisticShaderProviderParameter("shader/default.vertex.glsl", "shader/default.gridrec.fragment.glsl"));
        manager.load("per-vertex-lighting-starsurface", RelativisticShaderProvider.class, new RelativisticShaderProviderParameter("shader/starsurface.vertex.glsl", "shader/starsurface.fragment.glsl"));
        manager.load("per-vertex-lighting-beam", RelativisticShaderProvider.class, new RelativisticShaderProviderParameter("shader/default.vertex.glsl", "shader/beam.fragment.glsl"));
        manager.load("per-vertex-lighting-thruster", GroundShaderProvider.class, new GroundShaderProviderParameter("shader/default.vertex.glsl", "shader/thruster.fragment.glsl"));

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

        renderSystems = new Array<>();

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

    private ExtShaderProgram[] fetchShaderProgram(final AssetManager manager, final AssetDescriptor<ExtShaderProgram>[] descriptors, final String... names) {
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

    public void doneLoading(final AssetManager manager) {
        IntBuffer intBuffer = BufferUtils.newIntBuffer(16);
        Gdx.gl20.glGetIntegerv(GL20.GL_MAX_TEXTURE_SIZE, intBuffer);
        int maxTexSize = intBuffer.get();
        logger.info("Max texture size: " + maxTexSize + "^2 pixels");

        String[] names = GlobalResources.combinations(new String[] { " (ssr)", " (vel)", " (rel)", " (grav)" });

        /*
          STAR BILLBOARD SHADER
         */
        ExtShaderProgram[] starBillboardShaders = fetchShaderProgram(manager, starBillboardDesc, TextUtils.concatAll("star.billboard", names));

        /*
         * GALAXY SHADER
         */
        ExtShaderProgram[] galShaders = fetchShaderProgram(manager, galDesc, TextUtils.concatAll("gal", names));

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
        ExtShaderProgram[] spriteShaders = fetchShaderProgram(manager, spriteDesc, TextUtils.concatAll("sprite", names));

        /*
         * POINT CPU
         */
        ExtShaderProgram[] pointShaders = fetchShaderProgram(manager, pointDesc, TextUtils.concatAll("point.cpu", names));

        /*
         * LINE CPU
         */
        lineShaders = fetchShaderProgram(manager, lineDesc, TextUtils.concatAll("line.cpu", names));

        /*
         * LINE QUAD
         */
        lineQuadShaders = fetchShaderProgram(manager, lineQuadDesc, TextUtils.concatAll("line.quad", names));

        /*
         * LINE GPU
         */
        ExtShaderProgram[] lineGpuShaders = fetchShaderProgram(manager, lineGpuDesc, TextUtils.concatAll("line.gpu", names));

        /*
         * BILLBOARD GROUP
         */
        ExtShaderProgram[] billboardGroupShaders = fetchShaderProgram(manager, billboardGroupDesc, TextUtils.concatAll("billboard.group", names));

        /*
         * PARTICLE EFFECT - default and relativistic
         */
        ExtShaderProgram[] particleEffectShaders = fetchShaderProgram(manager, particleEffectDesc, TextUtils.concatAll("particle.effect", names));

        /*
         * PARTICLE GROUP (TRI) - default and relativistic
         */
        ExtShaderProgram[] particleGroupShaders = fetchShaderProgram(manager, particleGroupDesc, TextUtils.concatAll("particle.group", names));

        /*
         * STAR GROUP (TRI) - default and relativistic
         */
        ExtShaderProgram[] starGroupShaders = fetchShaderProgram(manager, starGroupDesc, TextUtils.concatAll("star.group", names));

        /*
         * VARIABLE GROUP - default and relativistic
         */
        ExtShaderProgram[] variableGroupShaders = fetchShaderProgram(manager, variableGroupDesc, TextUtils.concatAll("variable.group", names));

        /*
         * STAR POINT
         */
        ExtShaderProgram[] starPointShaders = fetchShaderProgram(manager, starPointDesc, TextUtils.concatAll("star.point", names));

        /*
         * ORBITAL ELEMENTS PARTICLES - default and relativistic
         */
        ExtShaderProgram[] orbitElemShaders = fetchShaderProgram(manager, orbitElemDesc, TextUtils.concatAll("orbitelem", names));

        RenderGroup[] renderGroups = values();
        renderLists = new Array<>(false, renderGroups.length);
        for (int i = 0; i < renderGroups.length; i++) {
            renderLists.add(new Array<>(false, 20));
        }

        // Per-vertex lighting shaders
        IntShaderProvider perVertexLighting = manager.get("per-vertex-lighting");
        IntShaderProvider perVertexLightingAdditive = manager.get("per-vertex-lighting-additive");
        IntShaderProvider perVertexDiffuse = manager.get("per-vertex-diffuse");
        IntShaderProvider perVertexLightingGrid = manager.get("per-vertex-lighting-grid");
        IntShaderProvider perVertexLightingRecGrid = manager.get("per-vertex-lighting-recgrid");
        IntShaderProvider perVertexLightingStarSurface = manager.get("per-vertex-lighting-starsurface");
        IntShaderProvider perVertexLightingBeam = manager.get("per-vertex-lighting-beam");
        IntShaderProvider perVertexLightingThruster = manager.get("per-vertex-lighting-thruster");

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
        IntModelBatch mbVertexDiffuse = new IntModelBatch(perVertexDiffuse, noSorter);
        IntModelBatch mbVertexLightingStarSurface = new IntModelBatch(perVertexLightingStarSurface, noSorter);
        IntModelBatch mbVertexLightingBeam = new IntModelBatch(perVertexLightingBeam, noSorter);
        IntModelBatch mbVertexLightingThruster = new IntModelBatch(perVertexLightingThruster, noSorter);
        IntModelBatch mbVertexLightingGrid = new IntModelBatch(perVertexLightingGrid, noSorter);
        IntModelBatch mbVertexLightingRecGrid = new IntModelBatch(perVertexLightingRecGrid, noSorter);

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
        font2d.getData().setScale(0.5f);
        BitmapFont fontTitles = manager.get("font/font-titles.fnt");

        // Sprites
        spriteBatch = globalResources.getExtSpriteBatch();
        spriteBatch.enableBlending();

        // Font batch - additive, no depth writes
        // Two model batches, for front (models), back and atmospheres
        fontBatch = new ExtSpriteBatch(2000, distanceFieldFontShader);
        fontBatch.enableBlending();
        fontBatch.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE);

        ComponentType[] comps = ComponentType.values();

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

        times = new long[comps.length];
        alphas = new float[comps.length];
        for (int i = 0; i < comps.length; i++) {
            times[i] = -20000L;
            alphas[i] = 0f;
        }

        /*
         * INITIALIZE SGRs
         */
        sgrList = new ISGR[5];
        sgrList[SGR_DEFAULT_IDX] = new SGR();
        sgrList[SGR_STEREO_IDX] = new SGRStereoscopic(globalResources.getSpriteBatch());
        sgrList[SGR_FOV_IDX] = new SGRFov();
        sgrList[SGR_CUBEMAP_IDX] = new SGRCubemapProjections();
        sgrList[SGR_OPENVR_IDX] = new SGROpenVR(vrContext, globalResources.getSpriteBatchVR());
        sgr = null;

        /*
         *
         * ======= INITIALIZE RENDER COMPONENTS =======
         *
         */

        final PointCloudMode pcm = Settings.settings.scene.renderer.pointCloud;

        // POINTS
        AbstractRenderSystem pixelStarProc = new StarPointRenderSystem(POINT_STAR, alphas, starPointShaders, ComponentType.Stars);
        pixelStarProc.addPreRunnables(additiveBlendR, noDepthTestR);

        // MODEL BACKGROUND - (MW panorama, CMWB)
        AbstractRenderSystem modelBackgroundProc = new ModelBatchRenderSystem(MODEL_BG, alphas, mbVertexDiffuse);
        modelBackgroundProc.addPostRunnables(clearDepthR);

        // MODEL GRID - (Ecl, Eq, Gal grids)
        AbstractRenderSystem modelGridsProc = new ModelBatchRenderSystem(MODEL_VERT_GRID, alphas, mbVertexLightingGrid);
        modelGridsProc.addPostRunnables(clearDepthR);
        // RECURSIVE GRID
        AbstractRenderSystem modelRecGridProc = new ModelBatchRenderSystem(MODEL_VERT_RECGRID, alphas, mbVertexLightingRecGrid);
        modelRecGridProc.addPreRunnables(regularBlendR, depthTestR);

        // ANNOTATIONS - (grids)
        AbstractRenderSystem annotationsProc = new FontRenderSystem(FONT_ANNOTATION, alphas, spriteBatch, null, null, font2d, null);
        annotationsProc.addPreRunnables(regularBlendR, noDepthTestR);
        annotationsProc.addPostRunnables(clearDepthR);

        // BILLBOARD STARS
        billboardStarsProc = new BillboardStarRenderSystem(BILLBOARD_STAR, alphas, starBillboardShaders, Settings.settings.scene.star.getStarTexture(), ComponentType.Stars.ordinal());
        billboardStarsProc.addPreRunnables(additiveBlendR, noDepthTestR);
        lpu = new LightPositionUpdater();
        billboardStarsProc.addPostRunnables(lpu);

        // BILLBOARD GALAXIES
        AbstractRenderSystem billboardGalaxiesProc = new BillboardStarRenderSystem(BILLBOARD_GAL, alphas, galShaders, "data/tex/base/static.jpg", ComponentType.Galaxies.ordinal());
        billboardGalaxiesProc.addPreRunnables(additiveBlendR, depthTestR, noDepthWritesR);

        // BILLBOARD SPRITES
        AbstractRenderSystem billboardSpritesProc = new BillboardSpriteRenderSystem(BILLBOARD_SPRITE, alphas, spriteShaders, ComponentType.Clusters.ordinal());
        billboardSpritesProc.addPreRunnables(additiveBlendR, depthTestNoWritesR);

        // LINES CPU
        AbstractRenderSystem lineProc = getLineRenderSystem();

        // LINES GPU
        AbstractRenderSystem lineGpuProc = new VertGPURenderSystem<>(LINE_GPU, alphas, lineGpuShaders, true);
        lineGpuProc.addPreRunnables(additiveBlendR, depthTestR, noDepthWritesR);

        // POINTS CPU
        AbstractRenderSystem pointProc = new PointRenderSystem(POINT, alphas, pointShaders);

        // POINTS GPU
        AbstractRenderSystem pointGpuProc = new VertGPURenderSystem<>(POINT_GPU, alphas, lineGpuShaders, false);
        pointGpuProc.addPreRunnables(regularBlendR, depthTestR);

        // MODELS DUST AND MESH
        AbstractRenderSystem modelMeshOpaqueProc = new ModelBatchRenderSystem(MODEL_PIX_DUST, alphas, mbPixelLightingDust);
        AbstractRenderSystem modelMeshAdditiveProc = new ModelBatchRenderSystem(MODEL_VERT_ADDITIVE, alphas, mbVertexLightingAdditive);
        // MODEL PER-PIXEL-LIGHTING EARLY
        AbstractRenderSystem modelPerPixelLightingEarly = new ModelBatchRenderSystem(MODEL_PIX_EARLY, alphas, mbPixelLighting);
        // MODEL PER-VERTEX-LIGHTING EARLY
        AbstractRenderSystem modelPerVertexLightingEarly = new ModelBatchRenderSystem(MODEL_VERT_EARLY, alphas, mbVertexLighting);

        // MODEL DIFFUSE
        AbstractRenderSystem modelMeshDiffuse = new ModelBatchRenderSystem(MODEL_DIFFUSE, alphas, mbVertexDiffuse);

        // MODEL PER-PIXEL-LIGHTING
        AbstractRenderSystem modelPerPixelLighting = new ModelBatchRenderSystem(MODEL_PIX, alphas, mbPixelLighting);

        // MODEL PER-PIXEL-LIGHTING-TESSELLATION
        AbstractRenderSystem modelPerPixelLightingTess = new ModelBatchTessellationRenderSystem(MODEL_PIX_TESS, alphas, mbPixelLightingTessellation);
        modelPerPixelLightingTess.addPreRunnables(regularBlendR, depthTestR);

        // MODEL BEAM
        AbstractRenderSystem modelBeamProc = new ModelBatchRenderSystem(MODEL_VERT_BEAM, alphas, mbVertexLightingBeam);

        // MODEL THRUSTER
        AbstractRenderSystem modelThrusterProc = new ModelBatchRenderSystem(MODEL_VERT_THRUSTER, alphas, mbVertexLightingThruster);

        // GALAXY
        BillboardGroupRenderSystem billboardGroupRenderSystem = new BillboardGroupRenderSystem(BILLBOARD_GROUP, alphas, billboardGroupShaders);

        // PARTICLE EFFECTS
        AbstractRenderSystem particleEffectsProc = new ParticleEffectsRenderSystem(null, alphas, particleEffectShaders);
        particleEffectsProc.addPreRunnables(additiveBlendR, noDepthTestR);
        particleEffectsProc.addPostRunnables(regularBlendR);

        // PARTICLE GROUP
        AbstractRenderSystem particleGroupProc = switch (pcm) {
            case TRIANGLES -> new ParticleGroupRenderSystem(PARTICLE_GROUP, alphas, particleGroupShaders);
            case TRIANGLES_INSTANCED -> new ParticleGroupInstRenderSystem(PARTICLE_GROUP, alphas, particleGroupShaders);
            case POINTS -> new ParticleGroupPointRenderSystem(PARTICLE_GROUP, alphas, particleGroupShaders);
        };
        particleGroupProc.addPreRunnables(additiveBlendR, depthTestR, noDepthWritesR);
        particleGroupProc.addPostRunnables(regularBlendR, depthWritesR);

        // STAR GROUP
        AbstractRenderSystem starGroupProc = switch (pcm) {
            case TRIANGLES -> new StarGroupRenderSystem(STAR_GROUP, alphas, starGroupShaders);
            case TRIANGLES_INSTANCED -> new StarGroupInstRenderSystem(STAR_GROUP, alphas, starGroupShaders);
            case POINTS -> new StarGroupPointRenderSystem(STAR_GROUP, alphas, starGroupShaders);
        };
        starGroupProc.addPreRunnables(additiveBlendR, depthTestR, noDepthWritesR);
        starGroupProc.addPostRunnables(regularBlendR, depthWritesR);

        // VARIABLE GROUP
        AbstractRenderSystem variableGroupProc = switch (pcm) {
            case TRIANGLES -> new VariableGroupRenderSystem(VARIABLE_GROUP, alphas, variableGroupShaders);
            case TRIANGLES_INSTANCED -> new VariableGroupInstRenderSystem(VARIABLE_GROUP, alphas, variableGroupShaders);
            case POINTS -> new VariableGroupPointRenderSystem(VARIABLE_GROUP, alphas, variableGroupShaders);
        };
        variableGroupProc.addPreRunnables(additiveBlendR, depthTestR, noDepthWritesR);
        variableGroupProc.addPostRunnables(regularBlendR, depthWritesR);

        // ORBITAL ELEMENTS PARTICLES
        AbstractRenderSystem orbitElemParticlesProc = new OrbitalElementsParticlesRenderSystem(ORBITAL_ELEMENTS_PARTICLE, alphas, orbitElemShaders);
        orbitElemParticlesProc.addPreRunnables(additiveBlendR, depthTestR, noDepthWritesR);
        orbitElemParticlesProc.addPostRunnables(regularBlendR, depthWritesR);
        // ORBITAL ELEMENTS GROUP
        AbstractRenderSystem orbitElemGroupProc = new OrbitalElementsGroupRenderSystem(ORBITAL_ELEMENTS_GROUP, alphas, orbitElemShaders);
        orbitElemGroupProc.addPreRunnables(additiveBlendR, depthTestR, noDepthWritesR);
        orbitElemGroupProc.addPostRunnables(regularBlendR, depthWritesR);

        // MODEL STARS
        AbstractRenderSystem modelStarsProc = new ModelBatchRenderSystem(MODEL_VERT_STAR, alphas, mbVertexLightingStarSurface);

        // LABELS
        AbstractRenderSystem labelsProc = new FontRenderSystem(FONT_LABEL, alphas, fontBatch, distanceFieldFontShader, font3d, font2d, fontTitles);

        // BILLBOARD SSO
        AbstractRenderSystem billboardSSOProc = new BillboardStarRenderSystem(BILLBOARD_SSO, alphas, starBillboardShaders, "data/tex/base/sso.png", -1);
        billboardSSOProc.addPreRunnables(additiveBlendR, depthTestNoWritesR);

        // MODEL ATMOSPHERE
        AbstractRenderSystem modelAtmProc = new ModelBatchRenderSystem(MODEL_ATM, alphas, mbAtmosphere) {
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
        AbstractRenderSystem modelCloudProc = new ModelBatchRenderSystem(MODEL_CLOUD, alphas, mbCloud);

        // SHAPES
        AbstractRenderSystem shapeProc = new ShapeRenderSystem(SHAPE, alphas, globalResources.getSpriteShader());
        shapeProc.addPreRunnables(regularBlendR, depthTestR);

        // Add components to set
        addRenderSystem(modelBackgroundProc);
        addRenderSystem(modelGridsProc);
        addRenderSystem(pixelStarProc);
        addRenderSystem(annotationsProc);

        // Opaque meshes
        addRenderSystem(modelMeshOpaqueProc);
        addRenderSystem(modelPerPixelLightingEarly);
        addRenderSystem(modelPerVertexLightingEarly);

        // Milky way
        addRenderSystem(billboardGroupRenderSystem);

        // Billboards
        addRenderSystem(billboardStarsProc);

        // Stars, particles
        addRenderSystem(particleGroupProc);
        addRenderSystem(starGroupProc);
        addRenderSystem(variableGroupProc);
        addRenderSystem(orbitElemParticlesProc);
        addRenderSystem(orbitElemGroupProc);

        // Diffuse meshes
        addRenderSystem(modelMeshDiffuse);

        // Models
        addRenderSystem(modelPerPixelLighting);
        addRenderSystem(modelPerPixelLightingTess);
        addRenderSystem(modelBeamProc);
        addRenderSystem(modelThrusterProc);

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

        // Models
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

        EventManager.instance.subscribe(this, Event.TOGGLE_VISIBILITY_CMD, Event.PIXEL_RENDERER_UPDATE, Event.LINE_RENDERER_UPDATE, Event.STEREOSCOPIC_CMD, Event.CAMERA_MODE_CMD, Event.CUBEMAP_CMD, Event.REBUILD_SHADOW_MAP_DATA_CMD, Event.LIGHT_SCATTERING_CMD);

    }

    public Array<Array<IRenderable>> renderLists() {
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
            sgr = sgrList[SGR_OPENVR_IDX];
        } else if (camera.getNCameras() > 1) {
            // FOV mode
            sgr = sgrList[SGR_FOV_IDX];
        } else if (Settings.settings.program.modeStereo.active) {
            // Stereoscopic mode
            sgr = sgrList[SGR_STEREO_IDX];
        } else if (Settings.settings.program.modeCubemap.active) {
            // 360 mode: cube map -> equirectangular map
            sgr = sgrList[SGR_CUBEMAP_IDX];
        } else {
            // Default mode
            sgr = sgrList[SGR_DEFAULT_IDX];
        }
    }

    Array<StubModel> controllers = new Array<>();

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
                SGROpenVR sgrVR = (SGROpenVR) sgrList[SGR_OPENVR_IDX];
                if (vrContext != null) {
                    for (StubModel m : sgrVR.controllerObjects) {
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
            mbPixelLightingOpaque.begin(camera.getCamera());
            for (IRenderable model : models) {
                if (model instanceof ModelBody) {
                    ModelBody mb = (ModelBody) model;
                    mb.render(mbPixelLightingOpaque, RenderGroup.MODEL_PIX, 1, 0, false);
                }
            }
            mbPixelLightingOpaque.end();

            // Render tessellated models
            if (modelsTess.size > 0) {
                mbPixelLightingOpaqueTessellation.begin(camera.getCamera());
                for (IRenderable model : modelsTess) {
                    if (model instanceof ModelBody) {
                        ModelBody mb = (ModelBody) model;
                        mb.render(mbPixelLightingOpaqueTessellation, RenderGroup.MODEL_PIX, 1, 0, false);
                    }
                }
                mbPixelLightingOpaqueTessellation.end();
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
            mbPixelLightingDepth.begin(cameraLight);
            candidate.render(mbPixelLightingDepth, 1, 0, null, RenderGroup.MODEL_PIX);
            mbPixelLightingDepth.end();

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
                mbPixelLightingDepthTessellation.begin(cameraLight);
                candidate.render(mbPixelLightingDepthTessellation, 1, 0, rc, RenderGroup.MODEL_PIX);
                mbPixelLightingDepthTessellation.end();

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

    @Override
    public void render(final ICamera camera, final double t, final int rw, final int rh, final int tw, final int th, final FrameBuffer fb, final PostProcessBean ppb) {
        if (sgr == null)
            initSGR(camera);

        // Shadow maps are the same for all
        renderShadowMap(camera);

        // In stereo and cubemap modes, the glow pass is rendered in the SGR itself
        if (!Settings.settings.program.modeStereo.active && !Settings.settings.program.modeCubemap.active && !Settings.settings.runtime.openVr) {
            renderGlowPass(camera, glowFb);
        }

        sgr.render(this, camera, t, rw, rh, tw, th, fb, ppb);
    }

    public ISGR getCurrentSGR() {
        return sgr;
    }

    public FrameBuffer getGlowFb() {
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

    private boolean isInstance(IRenderSystem process, Class<? extends IRenderSystem>... systemClasses) {
        for (Class<? extends IRenderSystem> systemClass : systemClasses) {
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
    protected void renderSystems(ICamera camera, double t, RenderingContext renderContext, Class<? extends IRenderSystem>... systemClasses) {
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
    public void clearLists() {
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
        boolean allon = comp.length() == 0 || comp.allSetLike(visible);

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
    public void notify(Event event, Object source, final Object... data) {
        switch (event) {
        case TOGGLE_VISIBILITY_CMD:
            ComponentType ct = ComponentType.getFromKey((String) data[0]);
            if (ct != null) {
                int idx = ct.ordinal();
                if (data.length == 2) {
                    // We have the boolean
                    boolean currvis = visible.get(ct.ordinal());
                    boolean newvis = (boolean) data[1];
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
            GaiaSky.postRunnable(() -> {
                EventManager.publish(Event.STAR_POINT_UPDATE_FLAG, this, true);
                // updatePixelRenderSystem();
            });
            break;
        case LINE_RENDERER_UPDATE:
            GaiaSky.postRunnable(this::updateLineRenderSystem);
            break;
        case STEREOSCOPIC_CMD:
            boolean stereo = (Boolean) data[0];
            if (stereo)
                sgr = sgrList[SGR_STEREO_IDX];
            else {
                if (Settings.settings.runtime.openVr)
                    sgr = sgrList[SGR_OPENVR_IDX];
                else
                    sgr = sgrList[SGR_DEFAULT_IDX];
            }
            break;
        case CUBEMAP_CMD:
            boolean cubemap = (Boolean) data[0] && !Settings.settings.runtime.openVr;
            if (cubemap) {
                sgr = sgrList[SGR_CUBEMAP_IDX];
            } else {
                if (Settings.settings.runtime.openVr)
                    sgr = sgrList[SGR_OPENVR_IDX];
                else
                    sgr = sgrList[SGR_DEFAULT_IDX];
            }
            break;
        case CAMERA_MODE_CMD:
            CameraMode cm = (CameraMode) data[0];
            if (cm.isGaiaFov())
                sgr = sgrList[SGR_FOV_IDX];
            else {
                if (Settings.settings.runtime.openVr)
                    sgr = sgrList[SGR_OPENVR_IDX];
                else if (Settings.settings.program.modeStereo.active)
                    sgr = sgrList[SGR_STEREO_IDX];
                else if (Settings.settings.program.modeCubemap.active)
                    sgr = sgrList[SGR_CUBEMAP_IDX];
                else
                    sgr = sgrList[SGR_DEFAULT_IDX];

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

        for (ISGR sgr : sgrList) {
            sgr.resize(w, h);
        }
    }

    public void resizeRenderSystems(final int w, final int h, final int rw, final int rh) {
        for (IRenderSystem rendSys : renderSystems) {
            rendSys.resize(w, h);
        }
    }

    public void dispose() {
        // Batches, etc.
        if (spriteBatch != null)
            spriteBatch.dispose();
        if (fontBatch != null)
            fontBatch.dispose();

        // Dispose render systems
        if(renderSystems != null) {
            for (IRenderSystem rendSys : renderSystems) {
                rendSys.dispose();
            }
            renderSystems.clear();
        }

        // Dispose SGRs
        if (sgrList != null) {
            for (ISGR sgr : sgrList) {
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
            sys = new LineRenderSystem(LINE, alphas, lineShaders);
            sys.addPreRunnables(regularBlendR, depthTestR, noDepthWritesR);
        } else {
            // Quad
            sys = new LineQuadRenderSystem(LINE, alphas, lineQuadShaders);
            sys.addPreRunnables(additiveBlendR, depthTestR, noDepthWritesR);
        }
        return sys;
    }

}
