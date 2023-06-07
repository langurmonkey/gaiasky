/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

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
import gaiasky.util.gdx.shader.loader.TessellationShaderProviderLoader.TessellationShaderProviderParameter;
import gaiasky.util.gdx.shader.provider.*;
import gaiasky.util.gdx.shader.provider.ShaderProgramProvider.ShaderProgramParameter;

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
    public ExtShaderProgram[] starBillboardShaders, galShaders, spriteShaders, pointShaders, lineCpuShaders, lineQuadCpuShaders, lineQuadGpuShaders, primitiveGpuShaders, billboardGroupShaders, particleEffectShaders, particleGroupShaders, particleGroupExtBillboardShaders, particleGroupExtModelShaders, starGroupShaders, variableGroupShaders, starPointShaders, orbitElemShaders;
    public IntModelBatch mbVertexLighting, mbVertexLightingAdditive, mbVertexDiffuse, mbVertexLightingStarSurface, mbVertexLightingBeam, mbVertexLightingThruster, mbVertexLightingGrid, mbVertexLightingRecGrid, mbPixelLighting, mbPixelLightingDust, mbPixelLightingDepth, mbPixelLightingOpaque, mbPixelLightingSvtDetection, mbPixelLightingTessellation, mbPixelLightingOpaqueTessellation, mbPixelLightingSvtDetectionTessellation, mbPixelLightingDepthTessellation, mbSkybox, mbAtmosphere, mbCloud;
    public BitmapFont font2d, font3d, fontTitles;
    public ExtSpriteBatch spriteBatch, fontBatch;
    private AssetDescriptor<ExtShaderProgram>[] starGroupDesc, particleGroupDesc, particleGroupExtBillboardDesc, particleGroupExtModelDesc, variableGroupDesc, particleEffectDesc, orbitElemDesc, pointDesc, lineCpuDesc, lineQuadCpuDesc, lineQuadGpuDesc, primitiveGpuDesc, billboardGroupDesc, starPointDesc, galDesc, spriteDesc, starBillboardDesc;

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
        var namesSource = new Array<String>(String.class);
        var definesSource = new Array<String>(String.class);
        namesSource.add(SUFFIX_SSR, SUFFIX_VELBUFF);
        definesSource.add("#define ssrFlag\n", "#define velocityBufferFlag\n");
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
        starBillboardDesc = loadShader(manager, "shader/star.billboard.vertex.glsl", "shader/star.billboard.fragment.glsl", TextUtils.concatAll("star.billboard", names),
                                       defines);
        spriteDesc = loadShader(manager, "shader/sprite.vertex.glsl", "shader/sprite.fragment.glsl", TextUtils.concatAll("sprite", names), defines);
        billboardGroupDesc = loadShader(manager, "shader/billboard.group.vertex.glsl", "shader/billboard.group.fragment.glsl",
                                        TextUtils.concatAll("billboard.group", names), defines);
        pointDesc = loadShader(manager, "shader/point.cpu.vertex.glsl", "shader/point.cpu.fragment.glsl", TextUtils.concatAll("point.cpu", names), defines);
        lineCpuDesc = loadShader(manager, "shader/line.cpu.vertex.glsl", "shader/line.cpu.fragment.glsl", TextUtils.concatAll("line.cpu", names), defines);
        lineQuadCpuDesc = loadShader(manager, "shader/line.quad.cpu.vertex.glsl", "shader/line.quad.cpu.geometry.glsl", "shader/line.quad.cpu.fragment.glsl", TextUtils.concatAll("line.quad.cpu", names), defines);
        lineQuadGpuDesc = loadShader(manager, "shader/line.quad.gpu.vertex.glsl", "shader/line.quad.gpu.geometry.glsl", "shader/line.quad.gpu.fragment.glsl", TextUtils.concatAll("line.quad.gpu", names), defines);
        primitiveGpuDesc = loadShader(manager, "shader/line.gpu.vertex.glsl", "shader/line.gpu.fragment.glsl", TextUtils.concatAll("primitive.gpu", names), defines);
        galDesc = loadShader(manager, "shader/gal.vertex.glsl", "shader/gal.fragment.glsl", TextUtils.concatAll("gal", names), defines);
        particleEffectDesc = loadShader(manager, "shader/particle.effect.vertex.glsl", "shader/particle.effect.fragment.glsl",
                                        TextUtils.concatAll("particle.effect", names), defines);
        orbitElemDesc = loadShader(manager, "shader/orbitelem.vertex.glsl", "shader/particle.group.quad.fragment.glsl", TextUtils.concatAll("orbitelem", names), defines);
        // Initialize point cloud shaders - depends on point cloud mode
        final String pointTriSuffix = Settings.settings.scene.renderer.pointCloud.isTriangles() ? ".quad" : ".point";
        final String pointTriSuffixParticles = !Settings.settings.runtime.openXr && Settings.settings.scene.renderer.pointCloud.isTriangles() ? ".quad" : ".point";
        particleGroupDesc = loadShader(manager, "shader/particle.group" + pointTriSuffixParticles + ".vertex.glsl",
                                       "shader/particle.group" + pointTriSuffixParticles + ".fragment.glsl", TextUtils.concatAll("particle.group", namesColMap),
                                       definesColMap);
        particleGroupExtBillboardDesc = loadShader(manager, "shader/particle.group.quad.vertex.glsl", "shader/particle.group.quad.fragment.glsl",
                                                   TextUtils.concatAll("particle.group.ext", namesColMap), definesColMap, "#define extendedParticlesFlag");
        particleGroupExtModelDesc = loadShader(manager, "shader/particle.group.model.vertex.glsl", "shader/particle.group.model.fragment.glsl",
                                               TextUtils.concatAll("particle.group.ext.model", namesColMap), definesColMap, "#define extendedParticlesFlag");
        starGroupDesc = loadShader(manager, "shader/star.group" + pointTriSuffix + ".vertex.glsl", "shader/star.group" + pointTriSuffix + ".fragment.glsl",
                                   TextUtils.concatAll("star.group", namesColMap), definesColMap);
        variableGroupDesc = loadShader(manager, "shader/variable.group" + pointTriSuffix + ".vertex.glsl", "shader/star.group" + pointTriSuffix + ".fragment.glsl",
                                       TextUtils.concatAll("variable.group", namesColMap), definesColMap);
        // Regular stars
        starPointDesc = loadShader(manager, "shader/star.group.point.vertex.glsl", "shader/star.group.point.fragment.glsl", TextUtils.concatAll("star.point", names),
                                   defines);

        // Add shaders to load (with providers)
        manager.load("per-vertex-lighting", GroundShaderProvider.class, new GroundShaderProviderParameter("shader/default.vertex.glsl", "shader/default.fragment.glsl"));
        manager.load("per-vertex-lighting-additive", RelativisticShaderProvider.class,
                     new RelativisticShaderProviderParameter("shader/default.vertex.glsl", "shader/default.additive.fragment.glsl"));
        manager.load("per-vertex-diffuse", RelativisticShaderProvider.class,
                     new RelativisticShaderProviderParameter("shader/default.vertex.glsl", "shader/default.diffuse.fragment.glsl"));
        manager.load("per-vertex-lighting-grid", RelativisticShaderProvider.class,
                     new RelativisticShaderProviderParameter("shader/default.vertex.glsl", "shader/default.grid.fragment.glsl"));
        manager.load("per-vertex-lighting-recgrid", RelativisticShaderProvider.class,
                     new RelativisticShaderProviderParameter("shader/default.vertex.glsl", "shader/default.gridrec.fragment.glsl"));
        manager.load("per-vertex-lighting-starsurface", RelativisticShaderProvider.class,
                     new RelativisticShaderProviderParameter("shader/starsurface.vertex.glsl", "shader/starsurface.fragment.glsl"));
        manager.load("per-vertex-lighting-beam", RelativisticShaderProvider.class,
                     new RelativisticShaderProviderParameter("shader/default.vertex.glsl", "shader/beam.fragment.glsl"));
        manager.load("per-vertex-lighting-thruster", GroundShaderProvider.class,
                     new GroundShaderProviderParameter("shader/default.vertex.glsl", "shader/thruster.fragment.glsl"));

        manager.load("per-pixel-lighting", GroundShaderProvider.class, new GroundShaderProviderParameter("shader/normal.vertex.glsl", "shader/normal.fragment.glsl"));
        manager.load("per-pixel-lighting-tessellation", TessellationShaderProvider.class,
                     new TessellationShaderProviderParameter("shader/tessellation/tess.normal.vertex.glsl", "shader/tessellation/tess.normal.control.glsl",
                                                             "shader/tessellation/tess.normal.eval.glsl", "shader/tessellation/tess.normal.fragment.glsl"));
        manager.load("per-pixel-lighting-dust", GroundShaderProvider.class, new GroundShaderProviderParameter("shader/normal.vertex.glsl", "shader/dust.fragment.glsl"));
        manager.load("per-pixel-lighting-depth", RelativisticShaderProvider.class,
                     new RelativisticShaderProviderParameter("shader/normal.vertex.glsl", "shader/depth.fragment.glsl"));
        manager.load("per-pixel-lighting-depth-tessellation", TessellationShaderProvider.class,
                     new TessellationShaderProviderParameter("shader/tessellation/tess.simple.vertex.glsl", "shader/tessellation/tess.depth.control.glsl",
                                                             "shader/tessellation/tess.simple.eval.glsl", "shader/tessellation/tess.depth.fragment.glsl"));
        manager.load("per-pixel-lighting-opaque", RelativisticShaderProvider.class,
                     new RelativisticShaderProviderParameter("shader/normal.vertex.glsl", "shader/opaque.fragment.glsl"));
        manager.load("per-pixel-lighting-opaque-tessellation", TessellationShaderProvider.class,
                     new TessellationShaderProviderParameter("shader/tessellation/tess.simple.vertex.glsl", "shader/tessellation/tess.simple.control.glsl",
                                                             "shader/tessellation/tess.simple.eval.glsl", "shader/tessellation/tess.opaque.fragment.glsl"));
        manager.load("per-pixel-lighting-svtdetection", RelativisticShaderProvider.class,
                     new RelativisticShaderProviderParameter("shader/normal.vertex.glsl", "shader/svt.detection.fragment.glsl"));
        manager.load("per-pixel-lighting-svtdetection-tessellation", TessellationShaderProvider.class,
                     new TessellationShaderProviderParameter("shader/tessellation/tess.simple.vertex.glsl", "shader/tessellation/tess.simple.control.glsl",
                                                             "shader/tessellation/tess.simple.eval.glsl", "shader/tessellation/tess.svt.detection.fragment.glsl"));

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
        lineCpuShaders = fetchShaderProgram(manager, lineCpuDesc, TextUtils.concatAll("line.cpu", names));

        /*
         * LINE QUAD CPU
         */
        lineQuadCpuShaders = fetchShaderProgram(manager, lineQuadCpuDesc, TextUtils.concatAll("line.quad.cpu", names));

        /*
         * LINE QUAD GPU
         */
        lineQuadGpuShaders = fetchShaderProgram(manager, lineQuadGpuDesc, TextUtils.concatAll("line.quad.gpu", names));

        /*
         * PRIMITIVE GPU
         */
        primitiveGpuShaders = fetchShaderProgram(manager, primitiveGpuDesc, TextUtils.concatAll("primitive.gpu", names));

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
         * PARTICLE GROUP EXT BILLBOARDS - default and relativistic
         */
        particleGroupExtBillboardShaders = fetchShaderProgram(manager, particleGroupExtBillboardDesc, TextUtils.concatAll("particle.group.ext", names));

        /*
         * PARTICLE GROUP EXT MODELS - default and relativistic
         */
        particleGroupExtModelShaders = fetchShaderProgram(manager, particleGroupExtModelDesc, TextUtils.concatAll("particle.group.ext.model", names));

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
        mbVertexLightingBeam = new IntModelBatch(perVertexLightingBeam);
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
    private AssetDescriptor<ExtShaderProgram>[] loadShader(AssetManager manager,
                                                           String vertexShader,
                                                           String fragmentShader,
                                                           String[] names,
                                                           String[] prepend) {
        return loadShader(manager, vertexShader, null, fragmentShader, names, prepend);
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
    private AssetDescriptor<ExtShaderProgram>[] loadShader(AssetManager manager,
                                                           String vertexShader,
                                                           String geometryShader,
                                                           String fragmentShader,
                                                           String[] names,
                                                           String[] prepend) {
        return loadShader(manager, vertexShader, geometryShader, fragmentShader, names, prepend, null);
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
    private AssetDescriptor<ExtShaderProgram>[] loadShader(AssetManager manager,
                                                           String vertexShader,
                                                           String fragmentShader,
                                                           String[] names,
                                                           String[] prepend,
                                                           String fixedPrepend) {
        return loadShader(manager, vertexShader, null, fragmentShader, names, prepend, fixedPrepend);
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
    private AssetDescriptor<ExtShaderProgram>[] loadShader(AssetManager manager,
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

    private ExtShaderProgram[] fetchShaderProgram(final AssetManager manager,
                                                  final AssetDescriptor<ExtShaderProgram>[] descriptors,
                                                  final String... names) {
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

    private ExtShaderProgram fetchShaderProgram(final AssetManager manager,
                                                final String descriptor,
                                                final String name) {
        ExtShaderProgram shader = manager.get(descriptor);
        if (!shader.isLazy() && !shader.isCompiled()) {
            logger.error(name + " shader compilation failed:\n" + shader.getLog());
        }
        return shader;
    }
}
