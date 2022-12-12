package gaiasky.render;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.utils.Array;
import gaiasky.util.GlobalResources;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.TextUtils;
import gaiasky.util.gdx.IntModelBatch;
import gaiasky.util.gdx.contrib.utils.ShaderLoader;
import gaiasky.util.gdx.g2d.BitmapFont;
import gaiasky.util.gdx.g2d.ExtSpriteBatch;
import gaiasky.util.gdx.loader.BitmapFontLoader.BitmapFontParameter;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.gdx.shader.loader.AtmosphereShaderProviderLoader.AtmosphereShaderProviderParameter;
import gaiasky.util.gdx.shader.loader.GroundShaderProviderLoader.GroundShaderProviderParameter;
import gaiasky.util.gdx.shader.loader.RelativisticShaderProviderLoader.RelativisticShaderProviderParameter;
import gaiasky.util.gdx.shader.loader.TessellationShaderProviderLoader;
import gaiasky.util.gdx.shader.provider.*;
import gaiasky.util.gdx.shader.provider.ShaderProgramProvider.ShaderProgramParameter;

/**
 * Centralizes the creation and loading of render assets: shaders, fonts, batch objects, etc.
 */
public class RenderAssets {
    /**
     * Shader name parts.
     */
    public static final String SUFFIX_SSR = "SSR";
    public static final String SUFFIX_VELBUFF = "Velbuff";
    public static final String SUFFIX_REL = "Rel";
    public static final String SUFFIX_GRAV = "Grav";
    public static final String SUFFIX_COLMAP = "Colmap";
    private static final Log logger = Logger.getLogger(RenderAssets.class);
    private final GlobalResources globalResources;
    public ExtShaderProgram distanceFieldFontShader;
    public ExtShaderProgram[]
            starBillboardShaders,
            galShaders,
            spriteShaders,
            pointShaders,
            lineShaders,
            lineQuadShaders,
            lineGpuShaders,
            billboardGroupShaders,
            particleEffectShaders,
            particleGroupShaders,
            starGroupShaders,
            variableGroupShaders,
            starPointShaders,
            orbitElemShaders;
    public IntModelBatch
            mbVertexLighting,
            mbVertexLightingAdditive,
            mbVertexDiffuse,
            mbVertexLightingStarSurface,
            mbVertexLightingBeam,
            mbVertexLightingThruster,
            mbVertexLightingGrid,
            mbVertexLightingRecGrid,
            mbPixelLighting,
            mbPixelLightingDust,
            mbPixelLightingDepth,
            mbPixelLightingOpaque,
            mbPixelLightingTessellation,
            mbPixelLightingOpaqueTessellation,
            mbPixelLightingDepthTessellation,
            mbSkybox,
            mbAtmosphere,
            mbCloud;
    public BitmapFont
            font2d,
            font3d,
            fontTitles;
    public ExtSpriteBatch spriteBatch, fontBatch;
    private AssetDescriptor<ExtShaderProgram>[]
            starGroupDesc,
            particleGroupDesc,
            variableGroupDesc,
            particleEffectDesc,
            orbitElemDesc,
            pointDesc,
            lineDesc,
            lineQuadDesc,
            lineGpuDesc,
            billboardGroupDesc,
            starPointDesc,
            galDesc,
            spriteDesc,
            starBillboardDesc;

    public RenderAssets(final GlobalResources globalResources) {
        this.globalResources = globalResources;
    }

    /**
     * Sends shaders and fonts to load via the asset manager.
     *
     * @param manager The asset manager.
     */
    public void initialize(AssetManager manager) {

        ShaderLoader.Pedantic = false;
        ExtShaderProgram.pedantic = false;

        /* DATA LOAD */

        // Build arrays of names and defines.
        Array<String> namesSource = new Array<>(String.class);
        Array<String> definesSource = new Array<>(String.class);
        namesSource.add(SUFFIX_SSR, SUFFIX_VELBUFF);
        definesSource.add("#define ssrFlag\n", "#define velocityBufferFlag\n");
        if (Settings.settings.runtime.relativisticAberration && Settings.settings.runtime.gravitationalWaves) {
            namesSource.add(SUFFIX_REL, SUFFIX_GRAV);
            definesSource.add("#define relativisticEffects\n", "#define gravitationalWaves\n");
        }
        String[] defines = GlobalResources.combinations(definesSource.toArray());
        String[] names = GlobalResources.combinations(namesSource.toArray());

        // Color mapping in shaders
        namesSource.add(SUFFIX_COLMAP);
        definesSource.add("#define colorMap\n");
        String[] definesCmap = GlobalResources.combinations(definesSource.toArray());
        String[] namesCmap = GlobalResources.combinations(namesSource.toArray());

        // Direct shaders
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

        manager.load("skybox", RelativisticShaderProvider.class, new RelativisticShaderProviderParameter("shader/skybox.vertex.glsl", "shader/skybox.fragment.glsl"));
        manager.load("atmosphere", AtmosphereShaderProvider.class, new AtmosphereShaderProviderParameter("shader/atm.vertex.glsl", "shader/atm.fragment.glsl"));
        manager.load("cloud", GroundShaderProvider.class, new GroundShaderProviderParameter("shader/cloud.vertex.glsl", "shader/cloud.fragment.glsl"));
        manager.load("shader/font.vertex.glsl", ExtShaderProgram.class);

        // Add fonts to load
        BitmapFontParameter bfp = new BitmapFontParameter();
        bfp.magFilter = TextureFilter.Linear;
        bfp.minFilter = TextureFilter.Linear;
        manager.load("skins/fonts/main-font.fnt", BitmapFont.class, bfp);
        manager.load("skins/fonts/font2d.fnt", BitmapFont.class, bfp);
        manager.load("skins/fonts/font-titles.fnt", BitmapFont.class, bfp);
    }

    /**
     * Once the loading has finished, this method gets the results and sets up the
     * required objects.
     *
     * @param manager The asset manager.
     */
    public void doneLoading(AssetManager manager) {
        String[] names = GlobalResources.combinations(new String[] { " (ssr)", " (vel)", " (rel)", " (grav)" });

        /*
          STAR BILLBOARD SHADER
         */
        starBillboardShaders = fetchShaderProgram(manager, starBillboardDesc, TextUtils.concatAll("star.billboard", names));

        /*
         * GALAXY SHADER
         */
        galShaders = fetchShaderProgram(manager, galDesc, TextUtils.concatAll("gal", names));

        /*
         * FONT SHADER
         */
        distanceFieldFontShader = fetchShaderProgram(manager, "shader/font.vertex.glsl", "distance.field.font");

        /*
         * SPRITE SHADER
         */
        spriteShaders = fetchShaderProgram(manager, spriteDesc, TextUtils.concatAll("sprite", names));

        /*
         * POINT CPU
         */
        pointShaders = fetchShaderProgram(manager, pointDesc, TextUtils.concatAll("point.cpu", names));

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
        lineGpuShaders = fetchShaderProgram(manager, lineGpuDesc, TextUtils.concatAll("line.gpu", names));

        /*
         * BILLBOARD GROUP
         */
        billboardGroupShaders = fetchShaderProgram(manager, billboardGroupDesc, TextUtils.concatAll("billboard.group", names));

        /*
         * PARTICLE EFFECT - default and relativistic
         */
        particleEffectShaders = fetchShaderProgram(manager, particleEffectDesc, TextUtils.concatAll("particle.effect", names));

        /*
         * PARTICLE GROUP (TRI) - default and relativistic
         */
        particleGroupShaders = fetchShaderProgram(manager, particleGroupDesc, TextUtils.concatAll("particle.group", names));

        /*
         * STAR GROUP (TRI) - default and relativistic
         */
        starGroupShaders = fetchShaderProgram(manager, starGroupDesc, TextUtils.concatAll("star.group", names));

        /*
         * VARIABLE GROUP - default and relativistic
         */
        variableGroupShaders = fetchShaderProgram(manager, variableGroupDesc, TextUtils.concatAll("variable.group", names));

        /*
         * STAR POINT
         */
        starPointShaders = fetchShaderProgram(manager, starPointDesc, TextUtils.concatAll("star.point", names));

        /*
         * ORBITAL ELEMENTS PARTICLES - default and relativistic
         */
        orbitElemShaders = fetchShaderProgram(manager, orbitElemDesc, TextUtils.concatAll("orbitelem", names));

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
        IntShaderProvider skybox = manager.get("skybox");
        IntShaderProvider atmosphere = manager.get("atmosphere");
        IntShaderProvider cloud = manager.get("cloud");

        // Create model batches
        mbVertexLighting = new IntModelBatch(perVertexLighting);
        mbVertexLightingAdditive = new IntModelBatch(perVertexLightingAdditive);
        mbVertexDiffuse = new IntModelBatch(perVertexDiffuse);
        mbVertexLightingStarSurface = new IntModelBatch(perVertexLightingStarSurface);
        mbVertexLightingBeam = new IntModelBatch(perVertexLightingBeam);
        mbVertexLightingThruster = new IntModelBatch(perVertexLightingThruster);
        mbVertexLightingGrid = new IntModelBatch(perVertexLightingGrid);
        mbVertexLightingRecGrid = new IntModelBatch(perVertexLightingRecGrid);

        mbPixelLighting = new IntModelBatch(perPixelLighting);
        mbPixelLightingDust = new IntModelBatch(perPixelLightingDust);
        mbPixelLightingDepth = new IntModelBatch(perPixelLightingDepth);
        mbPixelLightingOpaque = new IntModelBatch(perPixelLightingOpaque);
        mbPixelLightingTessellation = new IntModelBatch(perPixelLightingTessellation);
        mbPixelLightingOpaqueTessellation = new IntModelBatch(perPixelLightingOpaqueTessellation);
        mbPixelLightingDepthTessellation = new IntModelBatch(perPixelLightingDepthTessellation);

        mbSkybox = new IntModelBatch(skybox);
        mbAtmosphere = new IntModelBatch(atmosphere);
        mbCloud = new IntModelBatch(cloud);

        // Fonts - all of these are distance field fonts
        font3d = manager.get("skins/fonts/main-font.fnt");
        font2d = manager.get("skins/fonts/font2d.fnt");
        font2d.getData().setScale(0.5f);
        fontTitles = manager.get("skins/fonts/font-titles.fnt");

        // Sprites
        spriteBatch = globalResources.getExtSpriteBatch();
        spriteBatch.enableBlending();

        // Font batch - additive, no depth writes
        // Two model batches, for front (models), back and atmospheres
        fontBatch = new ExtSpriteBatch(2000, distanceFieldFontShader);
        fontBatch.enableBlending();
        fontBatch.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE);
    }

    /**
     * Prepares a shader program for asynchronous loading.
     *
     * @param manager        The asset manager.
     * @param vertexShader   The vertex shader file.
     * @param fragmentShader The fragment shader file.
     * @param names          The shader names or identifiers.
     * @param prepend        The pre-processor defines for each shader name.
     *
     * @return The asset descriptor for the shader program.
     */
    private AssetDescriptor<ExtShaderProgram>[] loadShader(AssetManager manager, String vertexShader, String fragmentShader, String[] names, String[] prepend) {
        AssetDescriptor<ExtShaderProgram>[] result = new AssetDescriptor[prepend.length];

        int i = 0;
        for (String prep : prepend) {
            ShaderProgramParameter spp = new ShaderProgramParameter();
            spp.name = names[i];
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

    private ExtShaderProgram[] fetchShaderProgram(final AssetManager manager, final AssetDescriptor<ExtShaderProgram>[] descriptors, final String... names) {
        int n = descriptors.length;
        ExtShaderProgram[] shaders = new ExtShaderProgram[n];

        for (int i = 0; i < n; i++) {
            shaders[i] = manager.get(descriptors[i]);
            if (!shaders[i].isLazy() && !shaders[i].isCompiled()) {
                logger.error(names[i] + " shader compilation failed:\n" + shaders[i].getLog());
            }
        }
        return shaders;
    }

    private ExtShaderProgram fetchShaderProgram(final AssetManager manager, final String descriptor, final String name) {
        ExtShaderProgram shader = manager.get(descriptor);
        if (!shader.isLazy() && !shader.isCompiled()) {
            logger.error(name + " shader compilation failed:\n" + shader.getLog());
        }
        return shader;
    }
}
