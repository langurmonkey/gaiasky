/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.ShaderProgramLoader;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Array;
import gaiasky.render.util.ShaderLoader;
import gaiasky.util.GlobalResources;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.TextUtils;
import gaiasky.util.gdx.IntModelBatch;
import gaiasky.util.gdx.g2d.BitmapFont;
import gaiasky.util.gdx.g2d.ExtSpriteBatch;
import gaiasky.util.gdx.loader.BitmapFontLoader.BitmapFontParameter;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.gdx.shader.loader.AtmosphereShaderProviderLoader.AtmosphereShaderProviderParameter;
import gaiasky.util.gdx.shader.loader.GroundShaderProviderLoader.GroundShaderProviderParameter;
import gaiasky.util.gdx.shader.loader.RelativisticShaderProviderLoader.RelativisticShaderProviderParameter;
import gaiasky.util.gdx.shader.loader.TessellationShaderProviderLoader.TessellationShaderProviderParameter;
import gaiasky.util.gdx.shader.provider.*;
import gaiasky.util.gdx.shader.provider.ShaderProgramProvider.ShaderProgramParameter;
import gaiasky.util.i18n.I18n;

/**
 * Loads and initializes shaders, fonts, batches and other resources used for rendering, especially
 * by {@link gaiasky.scene.system.render.SceneRenderer}.
 */
public class RenderAssets {
    /**
     * Shader name parts.
     */
    public static final String SUFFIX_SSR = "SSR";
    public static final String SUFFIX_REL = "Rel";
    public static final String SUFFIX_GRAV = "Grav";
    public static final String SUFFIX_COLMAP = "Colmap";
    private static final Log logger = Logger.getLogger(RenderAssets.class);
    private final GlobalResources globalResources;
    public ExtShaderProgram distanceFieldFontShader;
    public ExtShaderProgram[] billboardShaders, galShaders, spriteShaders, pointShaders, lineCpuShaders, lineQuadCpuShaders, lineQuadGpuShaders,
            primitiveGpuShaders, billboardGroupShaders, particleEffectShaders, particleGroupShaders, particleGroupExtBillboardShaders,
            particleGroupExtModelShaders, starGroupShaders, variableGroupShaders, starPointShaders, orbitElemShaders;
    public IntModelBatch mbVertexLighting, mbVertexLightingAdditive, mbVertexDiffuse, mbVertexLightingStarSurface, mbVertexLightingThruster,
            mbVertexLightingGrid, mbVertexLightingRecGrid, mbPixelLighting, mbPixelLightingDust, mbPixelLightingDepth, mbPixelLightingOpaque,
            mbPixelLightingSvtDetection, mbPixelLightingTessellation, mbPixelLightingOpaqueTessellation, mbPixelLightingSvtDetectionTessellation,
            mbPixelLightingDepthTessellation, mbSkybox, mbAtmosphere, mbCloud;
    public BitmapFont fontDistanceFiled;
    public ExtSpriteBatch spriteBatch, fontBatch;
    private AssetDescriptor<ExtShaderProgram>[] starGroupDesc, particleGroupDesc, particleGroupExtBillboardDesc, particleGroupExtModelDesc,
            variableGroupDesc, particleEffectDesc, orbitElemDesc, pointDesc, lineCpuDesc, lineQuadCpuDesc, lineQuadGpuDesc, primitiveGpuDesc,
            billboardGroupDesc, starPointDesc, galDesc, spriteDesc, billboardDesc;

    public RenderAssets(final GlobalResources globalResources) {
        this.globalResources = globalResources;
    }

    /**
     * Sends shaders and fonts to load via the asset manager.
     *
     * @param manager The asset manager.
     */
    public void initialize(AssetManager manager) {

        var safeMode = Settings.settings.program.safeMode;
        ShaderLoader.Pedantic = false;
        ExtShaderProgram.pedantic = false;

        /* DATA LOAD */

        // Build arrays of names and defines.
        var namesSource = new Array<>(String[]::new);
        var definesSource = new Array<>(String[]::new);
        namesSource.add(SUFFIX_SSR);
        definesSource.add("#define ssrFlag\n");
        if (Settings.settings.runtime.relativisticAberration && Settings.settings.runtime.gravitationalWaves) {
            namesSource.add(SUFFIX_REL, SUFFIX_GRAV);
            definesSource.add("#define relativisticEffects\n", "#define gravitationalWaves\n");
        }
        String[] defines = GlobalResources.combinations(definesSource.toArray());
        String[] names = GlobalResources.combinations(namesSource.toArray());

        // Color mapping in particle and star shaders.
        var namesSourceColMap = new Array<>(namesSource);
        var definesSourceColMap = new Array<>(definesSource);
        namesSourceColMap.add(SUFFIX_COLMAP);
        definesSourceColMap.add("#define colorMap\n");
        String[] definesColMap = GlobalResources.combinations(definesSourceColMap.toArray());
        String[] namesColMap = GlobalResources.combinations(namesSourceColMap.toArray());

        // Direct shaders
        billboardDesc = loadShaderExt(manager,
                                      "shader/billboard.vertex.glsl",
                                      "shader/billboard.fragment.glsl",
                                      TextUtils.concatAll("regular.billboard", names),
                                      defines);
        spriteDesc = loadShaderExt(manager, "shader/sprite.vertex.glsl", "shader/sprite.fragment.glsl", TextUtils.concatAll("sprite", names), defines);
        billboardGroupDesc = loadShaderExt(manager, "shader/billboard.group.vertex.glsl", "shader/billboard.group.fragment.glsl",
                                           TextUtils.concatAll("billboard.group", names), defines);
        pointDesc = loadShaderExt(manager,
                                  "shader/point.cpu.vertex.glsl",
                                  "shader/point.cpu.fragment.glsl",
                                  TextUtils.concatAll("point.cpu", names),
                                  defines);
        lineCpuDesc = loadShaderExt(manager,
                                    "shader/line.cpu.vertex.glsl",
                                    "shader/line.cpu.fragment.glsl",
                                    TextUtils.concatAll("line.cpu", names),
                                    defines);
        if (!safeMode) {
            // In safe mode we use OpenGL 3.3. Our geometry shaders use '#version 400 core', since they make use of double-precision.
            lineQuadCpuDesc = loadShaderExt(manager,
                                            "shader/line.quad.cpu.vertex.glsl",
                                            "shader/line.quad.cpu.geometry.glsl",
                                            "shader/line.quad.cpu.fragment.glsl",
                                            TextUtils.concatAll("line.quad.cpu", names),
                                            defines);
            lineQuadGpuDesc = loadShaderExt(manager,
                                            "shader/line.quad.gpu.vertex.glsl",
                                            "shader/line.quad.gpu.geometry.glsl",
                                            "shader/line.quad.gpu.fragment.glsl",
                                            TextUtils.concatAll("line.quad.gpu", names),
                                            defines);
        }
        primitiveGpuDesc = loadShaderExt(manager,
                                         "shader/line.gpu.vertex.glsl",
                                         "shader/line.gpu.fragment.glsl",
                                         TextUtils.concatAll("primitive.gpu", names),
                                         defines);
        galDesc = loadShaderExt(manager, "shader/gal.vertex.glsl", "shader/gal.fragment.glsl", TextUtils.concatAll("gal", names), defines);
        particleEffectDesc = loadShaderExt(manager, "shader/particle.effect.vertex.glsl", "shader/particle.effect.fragment.glsl",
                                           TextUtils.concatAll("particle.effect", names), defines);
        orbitElemDesc = loadShaderExt(manager,
                                      "shader/orbitelem.vertex.glsl",
                                      "shader/particle.group.quad.fragment.glsl",
                                      TextUtils.concatAll("orbitelem", names),
                                      defines);
        // Initialize point cloud shaders - depends on point cloud mode
        final String pointTriSuffix = Settings.settings.scene.renderer.pointCloud.isTriangles() ? ".quad" : ".point";
        final String pointTriSuffixParticles = Settings.settings.scene.renderer.pointCloud.isTriangles() ? ".quad" : ".point";
        particleGroupDesc = loadShaderExt(manager,
                                          "shader/particle.group" + pointTriSuffixParticles + ".vertex.glsl",
                                          "shader/particle.group" + pointTriSuffixParticles + ".fragment.glsl",
                                          TextUtils.concatAll("particle.group", namesColMap),
                                          definesColMap);
        particleGroupExtBillboardDesc = loadShaderExt(manager,
                                                      "shader/particle.group.quad.vertex.glsl",
                                                      "shader/particle.group.quad.fragment.glsl",
                                                      TextUtils.concatAll("particle.group.ext", namesColMap),
                                                      definesColMap,
                                                      "#define extendedParticlesFlag");
        particleGroupExtModelDesc = loadShaderExt(manager,
                                                  "shader/particle.group.model.vertex.glsl",
                                                  "shader/particle.group.model.fragment.glsl",
                                                  TextUtils.concatAll("particle.group.ext.model", namesColMap),
                                                  definesColMap,
                                                  "#define extendedParticlesFlag");
        starGroupDesc = loadShaderExt(manager,
                                      "shader/star.group" + pointTriSuffix + ".vertex.glsl",
                                      "shader/star.group" + pointTriSuffix + ".fragment.glsl",
                                      TextUtils.concatAll("star.group", namesColMap),
                                      definesColMap);
        variableGroupDesc = loadShaderExt(manager,
                                          "shader/variable.group" + pointTriSuffix + ".vertex.glsl",
                                          "shader/star.group" + pointTriSuffix + ".fragment.glsl",
                                          TextUtils.concatAll("variable.group", namesColMap),
                                          definesColMap);
        // Regular stars
        starPointDesc = loadShaderExt(manager,
                                      "shader/star.group.point.vertex.glsl",
                                      "shader/star.group.point.fragment.glsl",
                                      TextUtils.concatAll("star.point", names),
                                      defines);

        // Add shaders to load (with providers)
        manager.load("per-vertex-lighting",
                     GroundShaderProvider.class,
                     new GroundShaderProviderParameter("shader/default.vertex.glsl", "shader/default.fragment.glsl"));
        manager.load("per-vertex-lighting-additive", RelativisticShaderProvider.class,
                     new RelativisticShaderProviderParameter("shader/default.vertex.glsl", "shader/default.additive.fragment.glsl"));
        manager.load("per-vertex-diffuse", RelativisticShaderProvider.class,
                     new RelativisticShaderProviderParameter("shader/default.vertex.glsl", "shader/default.diffuse.fragment.glsl"));
        manager.load("per-vertex-lighting-grid", RelativisticShaderProvider.class,
                     new RelativisticShaderProviderParameter("shader/default.vertex.glsl", "shader/grid.fragment.glsl"));
        manager.load("per-vertex-lighting-recgrid", RelativisticShaderProvider.class,
                     new RelativisticShaderProviderParameter("shader/default.vertex.glsl", "shader/gridrec.fragment.glsl"));
        manager.load("per-vertex-lighting-starsurface", RelativisticShaderProvider.class,
                     new RelativisticShaderProviderParameter("shader/starsurface.vertex.glsl", "shader/starsurface.fragment.glsl"));
        manager.load("per-vertex-lighting-thruster", GroundShaderProvider.class,
                     new GroundShaderProviderParameter("shader/default.vertex.glsl", "shader/thruster.fragment.glsl"));

        manager.load("per-pixel-lighting",
                     GroundShaderProvider.class,
                     new GroundShaderProviderParameter("shader/pbr.vertex.glsl", "shader/pbr.fragment.glsl"));
        manager.load("per-pixel-lighting-tessellation", TessellationShaderProvider.class,
                     new TessellationShaderProviderParameter("shader/tessellation/tess.pbr.vertex.glsl", "shader/tessellation/tess.pbr.control.glsl",
                                                             "shader/tessellation/tess.pbr.eval.glsl", "shader/tessellation/tess.pbr.fragment.glsl"));
        manager.load("per-pixel-lighting-dust",
                     GroundShaderProvider.class,
                     new GroundShaderProviderParameter("shader/pbr.vertex.glsl", "shader/dust.fragment.glsl"));
        manager.load("per-pixel-lighting-depth", RelativisticShaderProvider.class,
                     new RelativisticShaderProviderParameter("shader/pbr.vertex.glsl", "shader/depth.fragment.glsl"));
        manager.load("per-pixel-lighting-depth-tessellation", TessellationShaderProvider.class,
                     new TessellationShaderProviderParameter("shader/tessellation/tess.simple.vertex.glsl",
                                                             "shader/tessellation/tess.depth.control.glsl",
                                                             "shader/tessellation/tess.simple.eval.glsl",
                                                             "shader/tessellation/tess.depth.fragment.glsl"));
        manager.load("per-pixel-lighting-opaque", RelativisticShaderProvider.class,
                     new RelativisticShaderProviderParameter("shader/pbr.vertex.glsl", "shader/opaque.fragment.glsl"));
        manager.load("per-pixel-lighting-opaque-tessellation", TessellationShaderProvider.class,
                     new TessellationShaderProviderParameter("shader/tessellation/tess.simple.vertex.glsl",
                                                             "shader/tessellation/tess.simple.control.glsl",
                                                             "shader/tessellation/tess.simple.eval.glsl",
                                                             "shader/tessellation/tess.opaque.fragment.glsl"));
        manager.load("per-pixel-lighting-svtdetection", RelativisticShaderProvider.class,
                     new RelativisticShaderProviderParameter("shader/pbr.vertex.glsl", "shader/svt.detection.fragment.glsl"));
        manager.load("per-pixel-lighting-svtdetection-tessellation", TessellationShaderProvider.class,
                     new TessellationShaderProviderParameter("shader/tessellation/tess.simple.vertex.glsl",
                                                             "shader/tessellation/tess.simple.control.glsl",
                                                             "shader/tessellation/tess.simple.eval.glsl",
                                                             "shader/tessellation/tess.svt.detection.fragment.glsl"));

        manager.load("skybox",
                     RelativisticShaderProvider.class,
                     new RelativisticShaderProviderParameter("shader/skybox.vertex.glsl", "shader/skybox.fragment.glsl"));
        manager.load("atmosphere",
                     AtmosphereShaderProvider.class,
                     new AtmosphereShaderProviderParameter("shader/atm.vertex.glsl", "shader/atm.fragment.glsl"));
        manager.load("cloud",
                     GroundShaderProvider.class,
                     new GroundShaderProviderParameter("shader/cloud.vertex.glsl", "shader/cloud.fragment.glsl"));
        manager.load("shader/font.vertex.glsl", ExtShaderProgram.class);

        // Add fonts to load
        BitmapFontParameter bfp = new BitmapFontParameter();
        bfp.magFilter = TextureFilter.Linear;
        bfp.minFilter = TextureFilter.Linear;
        manager.load("skins/fonts/font-distance-field.fnt", BitmapFont.class, bfp);

    }

    /**
     * Once the loading has finished, this method gets the results and sets up the
     * required objects.
     *
     * @param manager The asset manager.
     */
    public void doneLoading(AssetManager manager) {

        String[] names = GlobalResources.combinations(new String[]{" (ssr)", " (vel)", " (rel)", " (grav)"});

        /*
          BILLBOARD SHADER
         */
        billboardShaders = fetchShaderProgramExt(manager, billboardDesc, TextUtils.concatAll("regular.billboard", names));

        /*
         * GALAXY SHADER
         */
        galShaders = fetchShaderProgramExt(manager, galDesc, TextUtils.concatAll("gal", names));

        /*
         * FONT SHADER
         */
        distanceFieldFontShader = fetchShaderProgramExt(manager, "shader/font.vertex.glsl", "distance.field.font");

        /*
         * SPRITE SHADER
         */
        spriteShaders = fetchShaderProgramExt(manager, spriteDesc, TextUtils.concatAll("sprite", names));

        /*
         * POINT CPU
         */
        pointShaders = fetchShaderProgramExt(manager, pointDesc, TextUtils.concatAll("point.cpu", names));

        /*
         * LINE CPU
         */
        lineCpuShaders = fetchShaderProgramExt(manager, lineCpuDesc, TextUtils.concatAll("line.cpu", names));

        if (!Settings.settings.program.safeMode) {
            /*
             * LINE QUAD CPU
             */
            lineQuadCpuShaders = fetchShaderProgramExt(manager, lineQuadCpuDesc, TextUtils.concatAll("line.quad.cpu", names));

            /*
             * LINE QUAD GPU
             */
            lineQuadGpuShaders = fetchShaderProgramExt(manager, lineQuadGpuDesc, TextUtils.concatAll("line.quad.gpu", names));
        }

        /*
         * PRIMITIVE GPU
         */
        primitiveGpuShaders = fetchShaderProgramExt(manager, primitiveGpuDesc, TextUtils.concatAll("primitive.gpu", names));

        /*
         * BILLBOARD GROUP
         */
        billboardGroupShaders = fetchShaderProgramExt(manager, billboardGroupDesc, TextUtils.concatAll("billboard.group", names));

        /*
         * PARTICLE EFFECT - default and relativistic
         */
        particleEffectShaders = fetchShaderProgramExt(manager, particleEffectDesc, TextUtils.concatAll("particle.effect", names));

        /*
         * PARTICLE GROUP (TRI) - default and relativistic
         */
        particleGroupShaders = fetchShaderProgramExt(manager, particleGroupDesc, TextUtils.concatAll("particle.group", names));

        /*
         * PARTICLE GROUP EXT BILLBOARDS - default and relativistic
         */
        particleGroupExtBillboardShaders = fetchShaderProgramExt(manager,
                                                              particleGroupExtBillboardDesc,
                                                              TextUtils.concatAll("particle.group.ext", names));

        /*
         * PARTICLE GROUP EXT MODELS - default and relativistic
         */
        particleGroupExtModelShaders = fetchShaderProgramExt(manager, particleGroupExtModelDesc, TextUtils.concatAll("particle.group.ext.model", names));

        /*
         * STAR GROUP (TRI) - default and relativistic
         */
        starGroupShaders = fetchShaderProgramExt(manager, starGroupDesc, TextUtils.concatAll("star.group", names));

        /*
         * VARIABLE GROUP - default and relativistic
         */
        variableGroupShaders = fetchShaderProgramExt(manager, variableGroupDesc, TextUtils.concatAll("variable.group", names));

        /*
         * STAR POINT
         */
        starPointShaders = fetchShaderProgramExt(manager, starPointDesc, TextUtils.concatAll("star.point", names));

        /*
         * ORBITAL ELEMENTS PARTICLES - default and relativistic
         */
        orbitElemShaders = fetchShaderProgramExt(manager, orbitElemDesc, TextUtils.concatAll("orbitelem", names));

        // Per-vertex lighting shaders
        IntShaderProvider perVertexLighting = manager.get("per-vertex-lighting");
        IntShaderProvider perVertexLightingAdditive = manager.get("per-vertex-lighting-additive");
        IntShaderProvider perVertexDiffuse = manager.get("per-vertex-diffuse");
        IntShaderProvider perVertexLightingGrid = manager.get("per-vertex-lighting-grid");
        IntShaderProvider perVertexLightingRecGrid = manager.get("per-vertex-lighting-recgrid");
        IntShaderProvider perVertexLightingStarSurface = manager.get("per-vertex-lighting-starsurface");
        IntShaderProvider perVertexLightingThruster = manager.get("per-vertex-lighting-thruster");

        // Per-pixel lighting shaders
        IntShaderProvider perPixelLighting = manager.get("per-pixel-lighting");
        TessellationShaderProvider perPixelLightingTessellation = manager.get("per-pixel-lighting-tessellation");
        IntShaderProvider perPixelLightingDust = manager.get("per-pixel-lighting-dust");
        IntShaderProvider perPixelLightingDepth = manager.get("per-pixel-lighting-depth");
        IntShaderProvider perPixelLightingDepthTessellation = manager.get("per-pixel-lighting-depth-tessellation");
        IntShaderProvider perPixelLightingOpaque = manager.get("per-pixel-lighting-opaque");
        TessellationShaderProvider perPixelLightingOpaqueTessellation = manager.get("per-pixel-lighting-opaque-tessellation");
        IntShaderProvider perPixelLightingSvtDetection = manager.get("per-pixel-lighting-svtdetection");
        IntShaderProvider perPixelLightingSvtDetectionTessellation = manager.get("per-pixel-lighting-svtdetection-tessellation");

        // Others
        IntShaderProvider skybox = manager.get("skybox");
        IntShaderProvider atmosphere = manager.get("atmosphere");
        IntShaderProvider cloud = manager.get("cloud");

        // Create model batches
        mbVertexLighting = new IntModelBatch(perVertexLighting);
        mbVertexLightingAdditive = new IntModelBatch(perVertexLightingAdditive);
        mbVertexDiffuse = new IntModelBatch(perVertexDiffuse);
        mbVertexLightingStarSurface = new IntModelBatch(perVertexLightingStarSurface);
        mbVertexLightingThruster = new IntModelBatch(perVertexLightingThruster);
        mbVertexLightingGrid = new IntModelBatch(perVertexLightingGrid);
        mbVertexLightingRecGrid = new IntModelBatch(perVertexLightingRecGrid);

        mbPixelLighting = new IntModelBatch(perPixelLighting);
        mbPixelLightingDust = new IntModelBatch(perPixelLightingDust);
        mbPixelLightingDepth = new IntModelBatch(perPixelLightingDepth);
        mbPixelLightingOpaque = new IntModelBatch(perPixelLightingOpaque);
        mbPixelLightingSvtDetection = new IntModelBatch(perPixelLightingSvtDetection);
        mbPixelLightingTessellation = new IntModelBatch(perPixelLightingTessellation);
        mbPixelLightingOpaqueTessellation = new IntModelBatch(perPixelLightingOpaqueTessellation);
        mbPixelLightingSvtDetectionTessellation = new IntModelBatch(perPixelLightingSvtDetectionTessellation);
        mbPixelLightingDepthTessellation = new IntModelBatch(perPixelLightingDepthTessellation);

        mbSkybox = new IntModelBatch(skybox);
        mbAtmosphere = new IntModelBatch(atmosphere);
        mbCloud = new IntModelBatch(cloud);

        // Fonts - all of these are distance field fonts
        fontDistanceFiled = manager.get("skins/fonts/font-distance-field.fnt");

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
     * Loads a simple shader with vertex and fragment stages.
     *
     * @param manager        The manager.
     * @param vertexShader   The vertex shader location.
     * @param fragmentShader The fragment shader location.
     * @param name           The name.
     *
     * @return The asset descriptor.
     */
    private AssetDescriptor<ShaderProgram> loadShader(AssetManager manager,
                                                            String vertexShader,
                                                            String fragmentShader,
                                                            String name) {
        ShaderProgramLoader.ShaderProgramParameter spp = new ShaderProgramLoader.ShaderProgramParameter();
        spp.prependVertexCode = "";
        spp.prependFragmentCode = spp.prependVertexCode;
        spp.vertexFile = vertexShader;
        spp.fragmentFile = fragmentShader;
        manager.load(name, ShaderProgram.class, spp);
        return new AssetDescriptor<>(name, ShaderProgram.class, spp);
    }

    /**
     * Loads a simple shader with vertex and fragment stages.
     *
     * @param manager        The manager.
     * @param vertexShader   The vertex shader location.
     * @param fragmentShader The fragment shader location.
     * @param name           The name.
     *
     * @return The asset descriptor.
     */
    private AssetDescriptor<ExtShaderProgram> loadShaderExt(AssetManager manager,
                                                            String vertexShader,
                                                            String fragmentShader,
                                                            String name) {
        ShaderProgramParameter spp = new ShaderProgramParameter();
        spp.name = name;
        spp.prependVertexCode = "";
        spp.prependGeometryCode = spp.prependVertexCode;
        spp.prependFragmentCode = spp.prependVertexCode;
        spp.vertexFile = vertexShader;
        spp.geometryFile = null;
        spp.fragmentFile = fragmentShader;
        manager.load(name, ExtShaderProgram.class, spp);
        return new AssetDescriptor<>(name, ExtShaderProgram.class, spp);
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
    private AssetDescriptor<ExtShaderProgram>[] loadShaderExt(AssetManager manager,
                                                              String vertexShader,
                                                              String fragmentShader,
                                                              String[] names,
                                                              String[] prepend) {
        return loadShaderExt(manager, vertexShader, null, fragmentShader, names, prepend);
    }

    /**
     * Prepares a shader program for asynchronous loading.
     *
     * @param manager        The asset manager.
     * @param vertexShader   The vertex shader file.
     * @param geometryShader The geometry shader file.
     * @param fragmentShader The fragment shader file.
     * @param names          The shader names or identifiers.
     * @param prepend        The pre-processor defines for each shader name.
     *
     * @return The asset descriptor for the shader program.
     */
    private AssetDescriptor<ExtShaderProgram>[] loadShaderExt(AssetManager manager,
                                                              String vertexShader,
                                                              String geometryShader,
                                                              String fragmentShader,
                                                              String[] names,
                                                              String[] prepend) {
        return loadShaderExt(manager, vertexShader, geometryShader, fragmentShader, names, prepend, null);
    }

    /**
     * Prepares a shader program for asynchronous loading.
     *
     * @param manager        The asset manager.
     * @param vertexShader   The vertex shader file.
     * @param fragmentShader The fragment shader file.
     * @param names          The shader names or identifiers.
     * @param prepend        The pre-processor defines for each shader name.
     * @param fixedPrepend   The fixed defines that must appear in all shaders, if any.
     *
     * @return The asset descriptor for the shader program.
     */
    private AssetDescriptor<ExtShaderProgram>[] loadShaderExt(AssetManager manager,
                                                              String vertexShader,
                                                              String fragmentShader,
                                                              String[] names,
                                                              String[] prepend,
                                                              String fixedPrepend) {
        return loadShaderExt(manager, vertexShader, null, fragmentShader, names, prepend, fixedPrepend);
    }

    /**
     * Prepares a shader program for asynchronous loading.
     *
     * @param manager        The asset manager.
     * @param vertexShader   The vertex shader file.
     * @param fragmentShader The fragment shader file.
     * @param names          The shader names or identifiers.
     * @param prepend        The pre-processor defines for each shader name.
     * @param fixedPrepend   The fixed defines that must appear in all shaders, if any.
     *
     * @return The asset descriptor for the shader program.
     */
    private AssetDescriptor<ExtShaderProgram>[] loadShaderExt(AssetManager manager,
                                                              String vertexShader,
                                                              String geometryShader,
                                                              String fragmentShader,
                                                              String[] names,
                                                              String[] prepend,
                                                              String fixedPrepend) {
        AssetDescriptor<ExtShaderProgram>[] result = new AssetDescriptor[prepend.length];

        int i = 0;
        for (String prep : prepend) {
            ShaderProgramParameter spp = new ShaderProgramParameter();
            spp.name = names[i];
            spp.prependVertexCode = fixedPrepend != null ? fixedPrepend + "\n" + prep : prep;
            spp.prependGeometryCode = spp.prependVertexCode;
            spp.prependFragmentCode = spp.prependVertexCode;
            spp.vertexFile = vertexShader;
            spp.geometryFile = geometryShader;
            spp.fragmentFile = fragmentShader;
            manager.load(names[i], ExtShaderProgram.class, spp);
            AssetDescriptor<ExtShaderProgram> desc = new AssetDescriptor<>(names[i], ExtShaderProgram.class, spp);
            result[i] = desc;

            i++;
        }
        return result;
    }

    private ShaderProgram[] fetchShaderProgram(final AssetManager manager,
                                                     final AssetDescriptor<ShaderProgram>[] descriptors,
                                                     final String... names) {
        int n = descriptors.length;
        ShaderProgram[] shaders = new ShaderProgram[n];

        for (int i = 0; i < n; i++) {
            shaders[i] = manager.get(descriptors[i]);
            if (!shaders[i].isCompiled()) {
                logger.error(I18n.msg("notif.shader.compile.fail.log", names[i], shaders[i].getLog()));
            }
        }
        return shaders;
    }
    private ExtShaderProgram[] fetchShaderProgramExt(final AssetManager manager,
                                                     final AssetDescriptor<ExtShaderProgram>[] descriptors,
                                                     final String... names) {
        int n = descriptors.length;
        ExtShaderProgram[] shaders = new ExtShaderProgram[n];

        for (int i = 0; i < n; i++) {
            shaders[i] = manager.get(descriptors[i]);
            if (!shaders[i].isLazy() && !shaders[i].isCompiled()) {
                logger.error(I18n.msg("notif.shader.compile.fail.log", names[i], shaders[i].getLog()));
            }
        }
        return shaders;
    }

    private ExtShaderProgram fetchShaderProgramExt(final AssetManager manager,
                                                   final String descriptor,
                                                   final String name) {
        ExtShaderProgram shader = manager.get(descriptor);
        if (!shader.isLazy() && !shader.isCompiled()) {
            logger.error(I18n.msg("notif.shader.compile.fail.log", name, shader.getLog()));
        }
        return shader;
    }
}
