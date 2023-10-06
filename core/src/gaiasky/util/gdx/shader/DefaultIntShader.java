/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.environment.AmbientCubemap;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.environment.PointLight;
import com.badlogic.gdx.graphics.g3d.environment.SpotLight;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.util.Bits;
import gaiasky.util.Constants;
import gaiasky.util.Settings;
import gaiasky.util.gdx.IntRenderable;
import gaiasky.util.gdx.model.gltf.scene3d.attributes.PBRFloatAttribute;
import gaiasky.util.gdx.model.gltf.scene3d.attributes.PBRIridescenceAttribute;
import gaiasky.util.gdx.model.gltf.scene3d.attributes.PBRTextureAttribute;
import gaiasky.util.gdx.model.gltf.scene3d.attributes.PBRVolumeAttribute;
import gaiasky.util.gdx.shader.attribute.*;
import gaiasky.util.gdx.shader.loader.ShaderTemplatingLoader;
import gaiasky.util.gdx.shader.provider.ShaderProgramProvider;

import java.util.Objects;

public class DefaultIntShader extends BaseIntShader {
    /**
     * Attributes which are not required but always supported.
     */
    private final static Bits optionalAttributes = Bits.indexes(IntAttribute.CullFace, DepthTestAttribute.Type);
    private final static Attributes tmpAttributes = new Attributes();
    private static String defaultVertexShader = null;
    private static String defaultFragmentShader = null;
    // Global uniforms
    public final int u_projTrans;
    public final int u_projViewTrans;
    public final int u_cameraPosition;
    public final int u_cameraDirection;
    public final int u_cameraUp;
    public final int u_cameraNearFar;
    public final int u_cameraK;
    public final int u_time;
    // Vel buffer
    public final int u_prevProjView;
    public final int u_dCamPos;
    // VR
    public final int u_vrScale;
    public final int u_vrOffset;
    // Eclipses
    public final int u_eclipseOutlines;
    public final int u_eclipsingBodyPos;
    public final int u_eclipsingBodyRadius;
    // Object uniforms
    public final int u_worldTrans;
    public final int u_normalMatrix;
    public final int u_bones;
    // Material uniforms,
    public final int u_aoTexture;
    public final int u_opacity;
    public final int u_diffuseColor;
    public final int u_diffuseTexture;
    public final int u_specularColor;
    public final int u_specularTexture;
    public final int u_emissiveColor;
    public final int u_emissiveTexture;
    public final int u_metallicColor;
    public final int u_metallicTexture;
    public final int u_shininess;
    public final int u_roughnessColor;
    public final int u_roughnessTexture;
    public final int u_diffuseScatteringColor;
    public final int u_occlusionMetallicRoughnessTexture;
    public final int u_normalTexture;
    public final int u_heightTexture;
    public final int u_heightScale;
    public final int u_elevationMultiplier;
    public final int u_heightNoiseSize;
    public final int u_heightSize;
    public final int u_tessQuality;
    public final int u_bodySize;
    public final int u_alphaTest;
    public int u_ior;
    // Iridescence.
    public int u_iridescenceFactor;
    public int u_iridescenceIOR;
    public int u_iridescenceThicknessMin;
    public int u_iridescenceThicknessMax;
    public int u_iridescenceTexture;
    public int u_iridescenceThicknessTexture;
    // Volume
    public int u_thicknessTexture;
    public int u_thicknessFactor;
    public int u_volumeDistance;
    public int u_volumeColor;
    // Cubemaps.
    protected final int u_reflectionCubemap;
    protected final int u_diffuseCubemap;
    protected final int u_normalCubemap;
    protected final int u_specularCubemap;
    protected final int u_emissiveCubemap;
    protected final int u_heightCubemap;
    protected final int u_roughnessCubemap;
    protected final int u_metallicCubemap;

    // SVT.
    protected final int u_svtTileSize;
    protected final int u_svtResolution;
    protected final int u_svtDepth;
    protected final int u_svtId;
    protected final int u_svtDetectionFactor;
    protected final int u_svtBufferTexture;
    protected final int u_svtIndirectionDiffuseTexture;
    protected final int u_svtIndirectionSpecularTexture;
    protected final int u_svtIndirectionNormalTexture;
    protected final int u_svtIndirectionHeightTexture;
    protected final int u_svtIndirectionEmissiveTexture;
    protected final int u_svtIndirectionMetallicTexture;
    protected final int u_svtIndirectionRoughnessTexture;
    // Lighting uniforms
    protected final int u_ambientCubemap;
    protected final int u_dirLights0color;
    protected final int u_dirLights0direction;
    protected final int u_dirLights1color;
    protected final int u_pointLights0color;
    protected final int u_pointLights0position;
    protected final int u_pointLights0intensity;
    protected final int u_pointLights1color;
    protected final int u_spotLights0color;
    protected final int u_spotLights0position;
    protected final int u_spotLights0intensity;
    protected final int u_spotLights0direction;
    protected final int u_spotLights0cutoffAngle;
    protected final int u_spotLights0exponent;
    protected final int u_spotLights1color;
    protected final int u_fogColor;
    protected final int u_shadowMapProjViewTrans;
    protected final int u_shadowTexture;
    protected final int u_shadowPCFOffset;
    protected final boolean lighting;
    protected final boolean shadowMap;
    protected final DirectionalLight[] directionalLights;
    protected final PointLight[] pointLights;
    protected final SpotLight[] spotLights;
    /**
     * The attributes that this shader supports
     */
    protected final Bits attributesMask;
    protected final Config config;
    private final long vertexMask;
    protected int dirLightsLoc;
    protected int dirLightsColorOffset;
    protected int dirLightsDirectionOffset;
    protected int dirLightsSize;
    protected int pointLightsLoc;
    protected int pointLightsColorOffset;
    protected int pointLightsPositionOffset;
    protected int pointLightsIntensityOffset;
    protected int pointLightsSize;
    protected int spotLightsLoc;
    protected int spotLightsColorOffset;
    protected int spotLightsPositionOffset;
    protected int spotLightsDirectionOffset;
    protected int spotLightsIntensityOffset;
    protected int spotLightsCutoffAngleOffset;
    protected int spotLightsExponentOffset;
    protected int spotLightsSize;
    /**
     * The renderable used to create this shader, invalid after the call to init
     */
    private IntRenderable renderable;
    private boolean lightsSet;

    public DefaultIntShader(final IntRenderable renderable) {
        this(renderable, new Config());
    }

    public DefaultIntShader(final IntRenderable renderable,
                            final Config config) {
        this(renderable, config, createPrefix(renderable, config));
    }

    public DefaultIntShader(final IntRenderable renderable,
                            final Config config,
                            final String prefix) {
        this(renderable, config, prefix, config.vertexShaderCode != null ? config.vertexShaderCode : getDefaultVertexShader(),
                config.fragmentShaderCode != null ? config.fragmentShaderCode : getDefaultFragmentShader());
    }

    public DefaultIntShader(final IntRenderable renderable,
                            final Config config,
                            final String prefix,
                            final String vertexShader,
                            final String fragmentShader) {
        this(renderable, config,
                new ExtShaderProgram(ShaderProgramProvider.getShaderCode(prefix, vertexShader), ShaderProgramProvider.getShaderCode(prefix, fragmentShader)));
    }

    public DefaultIntShader(final IntRenderable renderable,
                            final Config config,
                            final ExtShaderProgram shaderProgram) {
        final Attributes attributes = combineAttributes(renderable);
        this.config = config;
        this.program = shaderProgram;
        this.lighting = renderable.environment != null;
        this.shadowMap = lighting && renderable.environment.shadowMap != null;
        this.renderable = renderable;
        attributesMask = attributes.getMask().copy().or(optionalAttributes);
        vertexMask = renderable.meshPart.mesh.getVertexAttributes().getMaskWithSizePacked();

        this.directionalLights = new DirectionalLight[lighting && config.numDirectionalLights > 0 ? config.numDirectionalLights : 0];
        for (int i = 0; i < directionalLights.length; i++)
            directionalLights[i] = new DirectionalLight();
        this.pointLights = new PointLight[lighting && config.numPointLights > 0 ? config.numPointLights : 0];
        for (int i = 0; i < pointLights.length; i++)
            pointLights[i] = new PointLight();
        this.spotLights = new SpotLight[lighting && config.numSpotLights > 0 ? config.numSpotLights : 0];
        for (int i = 0; i < spotLights.length; i++)
            spotLights[i] = new SpotLight();

        // Global uniforms
        u_dirLights0color = register(new Uniform("u_dirLights[0].color"));
        u_dirLights0direction = register(new Uniform("u_dirLights[0].direction"));
        u_dirLights1color = register(new Uniform("u_dirLights[1].color"));
        u_pointLights0color = register(new Uniform("u_pointLights[0].color"));
        u_pointLights0position = register(new Uniform("u_pointLights[0].position"));
        u_pointLights0intensity = register(new Uniform("u_pointLights[0].intensity"));
        u_pointLights1color = register(new Uniform("u_pointLights[1].color"));
        u_spotLights0color = register(new Uniform("u_spotLights[0].color"));
        u_spotLights0position = register(new Uniform("u_spotLights[0].position"));
        u_spotLights0intensity = register(new Uniform("u_spotLights[0].intensity"));
        u_spotLights0direction = register(new Uniform("u_spotLights[0].direction"));
        u_spotLights0cutoffAngle = register(new Uniform("u_spotLights[0].cutoffAngle"));
        u_spotLights0exponent = register(new Uniform("u_spotLights[0].exponent"));
        u_spotLights1color = register(new Uniform("u_spotLights[1].color"));
        u_fogColor = register(new Uniform("u_fogColor"));
        u_shadowMapProjViewTrans = register(new Uniform("u_shadowMapProjViewTrans"));
        u_shadowTexture = register(new Uniform("u_shadowTexture"));
        u_shadowPCFOffset = register(new Uniform("u_shadowPCFOffset"));
        u_projTrans = register(Inputs.projTrans, Setters.projTrans);
        u_projViewTrans = register(Inputs.projViewTrans, Setters.projViewTrans);
        u_cameraPosition = register(Inputs.cameraPosition, Setters.cameraPosition);
        u_cameraDirection = register(Inputs.cameraDirection, Setters.cameraDirection);
        u_cameraUp = register(Inputs.cameraUp, Setters.cameraUp);
        u_cameraNearFar = register(Inputs.cameraNearFar, Setters.cameraNearFar);
        u_cameraK = register(Inputs.cameraK, Setters.cameraK);
        u_time = register(Inputs.time, Setters.time);
        u_prevProjView = register(Inputs.prevProjView, Setters.prevProjView);
        u_dCamPos = register(Inputs.dCamPos, Setters.dCamPos);
        u_vrScale = register(Inputs.vrScale, Setters.vrScale);
        u_vrOffset = register(Inputs.vrOffset, Setters.vrOffset);
        // Eclipses
        u_eclipseOutlines = register(Inputs.eclipseOutlines, Setters.eclipseOutlines);
        u_eclipsingBodyPos = register(Inputs.eclipsingBodyPos, Setters.eclipsingBodyPos);
        u_eclipsingBodyRadius = register(Inputs.eclipsingBodyRadius, Setters.eclipsingBodyRadius);
        // Object uniforms
        u_worldTrans = register(Inputs.worldTrans, Setters.worldTrans);
        u_normalMatrix = register(Inputs.normalMatrix, Setters.normalMatrix);
        u_bones = (renderable.bones != null && config.numBones > 0) ? register(Inputs.bones, new Setters.Bones(config.numBones)) : -1;
        u_aoTexture = register(Inputs.aoTexture, Setters.aoTexture);
        u_opacity = register(Inputs.opacity);
        u_diffuseColor = register(Inputs.diffuseColor, Setters.diffuseColor);
        u_diffuseTexture = register(Inputs.diffuseTexture, Setters.diffuseTexture);
        u_specularColor = register(Inputs.specularColor, Setters.specularColor);
        u_specularTexture = register(Inputs.specularTexture, Setters.specularTexture);
        u_emissiveColor = register(Inputs.emissiveColor, Setters.emissiveColor);
        u_emissiveTexture = register(Inputs.emissiveTexture, Setters.emissiveTexture);
        u_metallicColor = register(Inputs.metallicColor, Setters.metallicColor);
        u_metallicTexture = register(Inputs.metallicTexture, Setters.metallicTexture);
        u_shininess = register(Inputs.shininess, Setters.shininess);
        u_roughnessColor = register(Inputs.roughnessColor, Setters.roughnessColor);
        u_roughnessTexture = register(Inputs.roughnessTexture, Setters.roughnessTexture);
        u_diffuseScatteringColor = register(Inputs.diffuseScatteringColor, Setters.diffuseScatteringColor);
        u_occlusionMetallicRoughnessTexture = register(Inputs.occlusionMetallicRoughnessTexture, Setters.occlusionMetallicRoughnessTexture);
        u_normalTexture = register(Inputs.normalTexture, Setters.normalTexture);
        u_heightTexture = register(Inputs.heightTexture, Setters.heightTexture);
        u_heightScale = register(Inputs.heightScale, Setters.heightScale);
        u_elevationMultiplier = register(Inputs.elevationMultiplier, Setters.elevationMultiplier);
        u_heightNoiseSize = register(Inputs.heightNoiseSize, Setters.heightNoiseSize);
        u_heightSize = register(Inputs.heightSize, Setters.heightSize);
        u_tessQuality = register(Inputs.tessQuality, Setters.tessQuality);
        u_bodySize = register(Inputs.bodySize, Setters.bodySize);
        u_alphaTest = register(Inputs.alphaTest);
        u_ambientCubemap = lighting ? register(Inputs.ambientCube, new Setters.ACubemap(config.numDirectionalLights, config.numPointLights)) : -1;
        u_reflectionCubemap = register(Inputs.reflectionCubemap, Setters.reflectionCubemap);
        u_diffuseCubemap = register(Inputs.diffuseCubemap, Setters.diffuseCubemap);
        u_normalCubemap = register(Inputs.normalCubemap, Setters.normalCubemap);
        u_specularCubemap = register(Inputs.specularCubemap, Setters.specularCubemap);
        u_emissiveCubemap = register(Inputs.emissionCubemap, Setters.emissiveCubemap);
        u_heightCubemap = register(Inputs.heightCubemap, Setters.heightCubemap);
        u_roughnessCubemap = register(Inputs.roughnessCubemap, Setters.roughnessCubemap);
        u_metallicCubemap = register(Inputs.metallicCubemap, Setters.metallicCubemap);
        u_svtTileSize = register(Inputs.svtTileSize, Setters.svtTileSize);
        u_svtResolution = register(Inputs.svtResolution, Setters.svtResolution);
        u_svtDepth = register(Inputs.svtDepth, Setters.svtDepth);


        // Iridescence
        u_iridescenceFactor = register(Inputs.iridescenceFactorUniform, Setters.iridescenceFactorSetter);
        u_iridescenceIOR = register(Inputs.iridescenceIORUniform, Setters.iridescenceIORSetter);
        u_iridescenceThicknessMin = register(Inputs.iridescenceThicknessMinUniform, Setters.iridescenceThicknessMinSetter);
        u_iridescenceThicknessMax = register(Inputs.iridescenceThicknessMaxUniform, Setters.iridescenceThicknessMaxSetter);
        u_iridescenceTexture = register(Inputs.iridescenceTextureUniform, Setters.iridescenceTextureSetter);
        u_iridescenceThicknessTexture = register(Inputs.iridescenceThicknessTextureUniform, Setters.iridescenceThicknessTextureSetter);
        // Volume, IOR
        u_ior = register(Inputs.iorUniform, Setters.iorSetter);
        u_thicknessFactor = register(Inputs.thicknessFactorUniform, Setters.thicknessFactorSetter);
        u_volumeDistance = register(Inputs.volumeDistanceUniform, Setters.volumeDistanceSetter);
        u_volumeColor = register(Inputs.volumeColorUniform, Setters.volumeColorSetter);
        u_thicknessTexture = register(Inputs.thicknessTextureUniform, Setters.thicknessTextureSetter);

        u_svtId = register(Inputs.svtId, Setters.svtId);
        u_svtDetectionFactor = register(Inputs.svtDetectionFactor, Setters.svtDetectionFactor);
        u_svtBufferTexture = register(Inputs.svtCacheTexture, Setters.svtBufferTexture);
        u_svtIndirectionDiffuseTexture = register(Inputs.svtIndirectionDiffuseTexture, Setters.svtIndirectionDiffuseTexture);
        u_svtIndirectionNormalTexture = register(Inputs.svtIndirectionNormalTexture, Setters.svtIndirectionNormalTexture);
        u_svtIndirectionSpecularTexture = register(Inputs.svtIndirectionSpecularTexture, Setters.svtIndirectionSpecularTexture);
        u_svtIndirectionHeightTexture = register(Inputs.svtIndirectionHeightTexture, Setters.svtIndirectionHeightTexture);
        u_svtIndirectionMetallicTexture = register(Inputs.svtIndirectionMetallicTexture, Setters.svtIndirectionMetallicTexture);
        u_svtIndirectionEmissiveTexture = register(Inputs.svtIndirectionEmissiveTexture, Setters.svtIndirectionEmissiveTexture);
        u_svtIndirectionRoughnessTexture = register(Inputs.svtIndirectionRoughnessTexture, Setters.svtIndirectionRoughnessTexture);
    }

    public static String getDefaultVertexShader() {
        if (defaultVertexShader == null)
            defaultVertexShader = ShaderTemplatingLoader.load(Gdx.files.internal("shader/normal.vertex.glsl"));
        return defaultVertexShader;
    }

    public static String getDefaultFragmentShader() {
        if (defaultFragmentShader == null)
            defaultFragmentShader = ShaderTemplatingLoader.load(Gdx.files.internal("shader/normal.fragment.glsl"));
        return defaultFragmentShader;
    }

    private static boolean and(final long mask,
                               final long flag) {
        return (mask & flag) == flag;
    }

    private static boolean or(final long mask,
                              final long flag) {
        return (mask & flag) != 0;
    }

    // TODO: Perhaps move responsibility for combining attributes to IntRenderableProvider?
    private static Attributes combineAttributes(final IntRenderable renderable) {
        tmpAttributes.clear();
        if (renderable.environment != null)
            tmpAttributes.set(renderable.environment);
        if (renderable.material != null)
            tmpAttributes.set(renderable.material);
        return tmpAttributes;
    }

    private static Bits combineAttributeMasks(final IntRenderable renderable) {
        Bits mask = Bits.empty();
        if (renderable.environment != null)
            mask.or(renderable.environment.getMask());
        if (renderable.material != null)
            mask.or(renderable.material.getMask());
        return mask;
    }

    public static String createPrefix(final IntRenderable renderable,
                                      final Config config) {
        final Attributes attributes = combineAttributes(renderable);
        StringBuilder prefix = new StringBuilder();
        final long vertexMask = renderable.meshPart.mesh.getVertexAttributes().getMask();
        if (and(vertexMask, Usage.Position))
            prefix.append("#define positionFlag\n");
        if (or(vertexMask, Usage.ColorUnpacked | Usage.ColorPacked))
            prefix.append("#define colorFlag\n");
        if (and(vertexMask, Usage.BiNormal))
            prefix.append("#define binormalFlag\n");
        if (and(vertexMask, Usage.Tangent))
            prefix.append("#define tangentFlag\n");
        if (and(vertexMask, Usage.Normal))
            prefix.append("#define normalFlag\n");
        if (and(vertexMask, Usage.Normal) || and(vertexMask, Usage.Tangent | Usage.BiNormal)) {
            if (renderable.environment != null) {
                prefix.append("#define lightingFlag\n");
                prefix.append("#define ambientCubemapFlag\n");
                prefix.append("#define numDirectionalLights ").append(config.numDirectionalLights).append("\n");
                prefix.append("#define numPointLights ").append(config.numPointLights).append("\n");
                prefix.append("#define numSpotLights ").append(config.numSpotLights).append("\n");
                if (attributes.has(ColorAttribute.Fog)) {
                    prefix.append("#define fogFlag\n");
                }
                if (renderable.environment.shadowMap != null)
                    prefix.append("#define shadowMapFlag\n");
            }
        }

        if (attributes.has(Vector3Attribute.EclipsingBodyPos)) {
            prefix.append("#define eclipsingBodyFlag\n");

            if (attributes.has(IntAttribute.EclipseOutlines)) {
                prefix.append("#define eclipseOutlines\n");
            }
        }

        final int n = renderable.meshPart.mesh.getVertexAttributes().size();
        for (int i = 0; i < n; i++) {
            final VertexAttribute attr = renderable.meshPart.mesh.getVertexAttributes().get(i);
            if (attr.usage == Usage.BoneWeight)
                prefix.append("#define boneWeight").append(attr.unit).append("Flag\n");
            else if (attr.usage == Usage.TextureCoordinates)
                prefix.append("#define texCoord").append(attr.unit).append("Flag\n");
        }
        if (attributes.has(BlendingAttribute.Type)) {
            prefix.append("#define " + BlendingAttribute.Alias + "Flag\n");
        }
        if (attributes.has(TextureAttribute.Diffuse)) {
            prefix.append("#define " + TextureAttribute.DiffuseAlias + "Flag\n");
        }
        if (attributes.has(TextureAttribute.Specular)) {
            prefix.append("#define " + TextureAttribute.SpecularAlias + "Flag\n");
        }
        if (attributes.has(TextureAttribute.Normal)) {
            prefix.append("#define " + TextureAttribute.NormalAlias + "Flag\n");
        }
        if (attributes.has(TextureAttribute.Emissive)) {
            prefix.append("#define " + TextureAttribute.EmissiveAlias + "Flag\n");
        }
        if (attributes.has(TextureAttribute.Metallic)) {
            prefix.append("#define " + TextureAttribute.MetallicAlias + "Flag\n");
        }
        if (attributes.has(TextureAttribute.Height)) {
            prefix.append("#define " + TextureAttribute.HeightAlias + "Flag\n");
        }
        if (attributes.has(TextureAttribute.AO)) {
            prefix.append("#define " + TextureAttribute.AOAlias + "Flag\n");
        }
        if (attributes.has(TextureAttribute.Roughness)) {
            prefix.append("#define " + TextureAttribute.RoughnessAlias + "Flag\n");
        }
        if (attributes.has(TextureAttribute.OcclusionMetallicRoughness)) {
            prefix.append("#define " + TextureAttribute.OcclusionMetallicRoughnessAlias + "Flag\n");
        }
        if (attributes.has(FloatAttribute.Time)) {
            prefix.append("#define " + FloatAttribute.TimeAlias + "Flag\n");
        }
        if (attributes.has(FloatAttribute.HeightNoiseSize)) {
            prefix.append("#define heightFlag\n");
        }

        if (attributes.has(TextureAttribute.Ambient)) {
            prefix.append("#define " + TextureAttribute.AmbientAlias + "Flag\n");
        }
        if (attributes.has(ColorAttribute.Diffuse))
            prefix.append("#define " + ColorAttribute.DiffuseAlias + "Flag\n");
        if (attributes.has(ColorAttribute.Specular))
            prefix.append("#define " + ColorAttribute.SpecularAlias + "Flag\n");
        if (attributes.has(ColorAttribute.Emissive))
            prefix.append("#define " + ColorAttribute.EmissiveAlias + "Flag\n");
        if (attributes.has(ColorAttribute.Metallic))
            prefix.append("#define " + ColorAttribute.MetallicAlias + "Flag\n");
        if (attributes.has(ColorAttribute.Roughness))
            prefix.append("#define " + ColorAttribute.RoughnessAlias + "Flag\n");
        if (attributes.has(ColorAttribute.DiffuseScattering))
            prefix.append("#define " + ColorAttribute.DiffuseScatteringAlias + "Flag\n");
        if (attributes.has(FloatAttribute.AlphaTest))
            prefix.append("#define " + FloatAttribute.AlphaTestAlias + "Flag\n");
        if (attributes.has(FloatAttribute.Shininess))
            prefix.append("#define " + FloatAttribute.ShininessAlias + "Flag\n");


        if (attributes.has(Matrix4Attribute.PrevProjView)) {
            prefix.append("#define velocityBufferFlag\n");
        }
        if (attributes.has(ColorAttribute.Metallic) || attributes.has(TextureAttribute.Metallic)) {
            prefix.append("#define metallicFlag\n");
            if (attributes.has(CubemapAttribute.ReflectionCubemap)) {
                prefix.append("#define " + CubemapAttribute.ReflectionCubemapAlias + "Flag\n");
            }
        }
        // Material Iridescence
        if (attributes.has(PBRIridescenceAttribute.Type)) {
            prefix.append("#define iridescenceFlag\n");
        }
        if (attributes.has(PBRTextureAttribute.IridescenceTexture)) {
            prefix.append("#define iridescenceTextureFlag\n");
        }
        if (attributes.has(PBRTextureAttribute.IridescenceThicknessTexture)) {
            prefix.append("#define iridescenceThicknessTextureFlag\n");
        }
        // Volume, IOR
        if (attributes.has(PBRVolumeAttribute.Type)) {
            prefix.append("#define volumeFlag\n");
        }
        if (attributes.has(PBRTextureAttribute.ThicknessTexture)) {
            prefix.append("#define thicknessTextureFlag\n");
        }
        if (attributes.has(PBRFloatAttribute.IOR)) {
            prefix.append("#define iorFlag\n");
        }

        if (Settings.settings.postprocess.ssr.active) {
            prefix.append("#define ssrFlag\n");
        }

        boolean cubemap = false;
        if (attributes.has(CubemapAttribute.DiffuseCubemap)) {
            prefix.append("#define " + CubemapAttribute.DiffuseCubemapAlias + "Flag\n");
            cubemap = true;
        }
        if (attributes.has(CubemapAttribute.NormalCubemap)) {
            prefix.append("#define " + CubemapAttribute.NormalCubemapAlias + "Flag\n");
            cubemap = true;
        }
        if (attributes.has(CubemapAttribute.SpecularCubemap)) {
            prefix.append("#define " + CubemapAttribute.SpecularCubemapAlias + "Flag\n");
            cubemap = true;
        }
        if (attributes.has(CubemapAttribute.EmissiveCubemap)) {
            prefix.append("#define " + CubemapAttribute.EmissiveCubemapAlias + "Flag\n");
            cubemap = true;
        }
        if (attributes.has(CubemapAttribute.MetallicCubemap)) {
            prefix.append("#define " + CubemapAttribute.MetallicCubemapAlias + "Flag\n");
            cubemap = true;
        }
        if (attributes.has(CubemapAttribute.RoughnessCubemap)) {
            prefix.append("#define " + CubemapAttribute.RoughnessCubemapAlias + "Flag\n");
            cubemap = true;
        }
        if (attributes.has(CubemapAttribute.HeightCubemap)) {
            prefix.append("#define " + CubemapAttribute.HeightCubemapAlias + "Flag\n");
            cubemap = true;
        }
        if (cubemap) {
            prefix.append("#define cubemapFlag\n");
        }

        boolean svtCache = false;
        boolean svtIndirection = false;
        if (attributes.has(TextureAttribute.SvtCache)) {
            prefix.append("#define " + TextureAttribute.SvtCacheAlias + "Flag\n");
            svtCache = true;
        }
        if (attributes.has(TextureAttribute.SvtIndirectionDiffuse)) {
            prefix.append("#define " + TextureAttribute.SvtIndirectionDiffuseAlias + "Flag\n");
            svtIndirection = true;
        }
        if (attributes.has(TextureAttribute.SvtIndirectionNormal)) {
            prefix.append("#define " + TextureAttribute.SvtIndirectionNormalAlias + "Flag\n");
            svtIndirection = true;
        }
        if (attributes.has(TextureAttribute.SvtIndirectionSpecular)) {
            prefix.append("#define " + TextureAttribute.SvtIndirectionSpecularAlias + "Flag\n");
            svtIndirection = true;
        }
        if (attributes.has(TextureAttribute.SvtIndirectionHeight)) {
            prefix.append("#define " + TextureAttribute.SvtIndirectionHeightAlias + "Flag\n");
            svtIndirection = true;
        }
        if (attributes.has(TextureAttribute.SvtIndirectionEmissive)) {
            prefix.append("#define " + TextureAttribute.SvtIndirectionEmissiveAlias + "Flag\n");
            svtIndirection = true;
        }
        if (attributes.has(TextureAttribute.SvtIndirectionMetallic)) {
            prefix.append("#define " + TextureAttribute.SvtIndirectionMetallicAlias + "Flag\n");
            svtIndirection = true;
        }
        if (attributes.has(TextureAttribute.SvtIndirectionRoughness)) {
            prefix.append("#define " + TextureAttribute.SvtIndirectionRoughnessAlias + "Flag\n");
            svtIndirection = true;
        }
        if (svtCache && svtIndirection && attributes.has(FloatAttribute.SvtId)
                && attributes.has(FloatAttribute.SvtDepth)
                && attributes.has(FloatAttribute.SvtTileSize)
                && attributes.has(Vector2Attribute.SvtResolution)) {
            prefix.append("#define svtFlag\n");
        }

        if (renderable.bones != null && config.numBones > 0)
            prefix.append("#define numBones ").append(config.numBones).append("\n");
        return prefix.toString();
    }

    @Override
    public void init() {
        final ExtShaderProgram program = this.program;
        this.program = null;
        init(program, renderable);
        renderable = null;
        dirLightsLoc = loc(u_dirLights0color);
        dirLightsColorOffset = loc(u_dirLights0color) - dirLightsLoc;
        dirLightsDirectionOffset = loc(u_dirLights0direction) - dirLightsLoc;
        dirLightsSize = loc(u_dirLights1color) - dirLightsLoc;
        if (dirLightsSize < 0)
            dirLightsSize = 0;

        pointLightsLoc = loc(u_pointLights0color);
        pointLightsColorOffset = loc(u_pointLights0color) - pointLightsLoc;
        pointLightsPositionOffset = loc(u_pointLights0position) - pointLightsLoc;
        pointLightsIntensityOffset = has(u_pointLights0intensity) ? loc(u_pointLights0intensity) - pointLightsLoc : -1;
        pointLightsSize = loc(u_pointLights1color) - pointLightsLoc;
        if (pointLightsSize < 0)
            pointLightsSize = 0;

        spotLightsLoc = loc(u_spotLights0color);
        spotLightsColorOffset = loc(u_spotLights0color) - spotLightsLoc;
        spotLightsPositionOffset = loc(u_spotLights0position) - spotLightsLoc;
        spotLightsDirectionOffset = loc(u_spotLights0direction) - spotLightsLoc;
        spotLightsIntensityOffset = has(u_spotLights0intensity) ? loc(u_spotLights0intensity) - spotLightsLoc : -1;
        spotLightsCutoffAngleOffset = loc(u_spotLights0cutoffAngle) - spotLightsLoc;
        spotLightsExponentOffset = loc(u_spotLights0exponent) - spotLightsLoc;
        spotLightsSize = loc(u_spotLights1color) - spotLightsLoc;
        if (spotLightsSize < 0)
            spotLightsSize = 0;
    }

    @Override
    public boolean canRender(final IntRenderable renderable) {
        final Bits renderableMask = combineAttributeMasks(renderable);
        return attributesMask.equals(renderableMask.or(optionalAttributes)) && (vertexMask == renderable.meshPart.mesh.getVertexAttributes().getMaskWithSizePacked())
                && (renderable.environment != null) == lighting;
    }

    @Override
    public int compareTo(IntShader other) {
        if (other == null)
            return -1;
        if (other == this)
            return 0;
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof DefaultIntShader) && equals((DefaultIntShader) obj);
    }

    public boolean equals(DefaultIntShader obj) {
        return (obj == this);
    }

    @Override
    public void begin(final Camera camera,
                      final RenderContext context) {
        super.begin(camera, context);

        for (final DirectionalLight dirLight : directionalLights)
            dirLight.set(0, 0, 0, 0, -1, 0);
        for (final PointLight pointLight : pointLights)
            pointLight.set(0, 0, 0, 0, 0, 0, 0);
        for (final SpotLight spotLight : spotLights)
            spotLight.set(0, 0, 0, 0, 0, 0, 0, -1, 0, 0, 1, 0);
        lightsSet = false;

        if (has(u_time)) {
            float time = (float) GaiaSky.instance.getT();
            set(u_time, time);
        }
    }

    @Override
    public void render(IntRenderable renderable,
                       Attributes combinedAttributes) {
        if (!combinedAttributes.has(BlendingAttribute.Type))
            context.setBlending(false, GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        bindMaterial(combinedAttributes);
        if (lighting)
            bindLights(renderable, combinedAttributes);
        super.render(renderable, combinedAttributes);
    }

    @Override
    public void end() {
        super.end();
    }

    protected void bindMaterial(final Attributes attributes) {
        int cullFace = config.defaultCullFace;
        int depthFunc = config.defaultDepthFunc;
        float depthRangeNear = 0f;
        float depthRangeFar = 1f;
        boolean depthMask = true;

        for (final Attribute attr : attributes) {
            if (BlendingAttribute.is(attr.index)) {
                context.setBlending(true, ((BlendingAttribute) attr).sourceFunction, ((BlendingAttribute) attr).destFunction);
                set(u_opacity, ((BlendingAttribute) attr).opacity);
            } else if (attr.has(IntAttribute.CullFace)) {
                cullFace = ((IntAttribute) attr).value;
            } else if (attr.has(FloatAttribute.AlphaTest)) {
                set(u_alphaTest, ((FloatAttribute) attr).value);
            } else if (attr.has(DepthTestAttribute.Type)) {
                DepthTestAttribute dta = (DepthTestAttribute) attr;
                depthFunc = dta.depthFunc;
                depthRangeNear = dta.depthRangeNear;
                depthRangeFar = dta.depthRangeFar;
                depthMask = dta.depthMask;
            }
        }

        context.setCullFace(cullFace);
        //cull(0);
        context.setDepthTest(depthFunc, depthRangeNear, depthRangeFar);
        context.setDepthMask(depthMask);
    }

    private void cull(int face) {
        if ((face == GL20.GL_FRONT) || (face == GL20.GL_BACK) || (face == GL20.GL_FRONT_AND_BACK)) {
            Gdx.gl.glEnable(GL20.GL_CULL_FACE);
            Gdx.gl.glCullFace(face);
        } else
            Gdx.gl.glDisable(GL20.GL_CULL_FACE);
    }

    protected void bindLights(final IntRenderable renderable,
                              final Attributes attributes) {
        final Environment lights = renderable.environment;
        final DirectionalLightsAttribute dla = attributes.get(DirectionalLightsAttribute.class, DirectionalLightsAttribute.Type);
        final Array<DirectionalLight> dirs = dla == null ? null : dla.lights;
        final PointLightsAttribute pla = attributes.get(PointLightsAttribute.class, PointLightsAttribute.Type);
        final Array<PointLight> points = pla == null ? null : pla.lights;

        if (dirLightsLoc >= 0) {
            for (int i = 0; i < directionalLights.length; i++) {
                if (dirs == null || i >= dirs.size) {
                    if (lightsSet && directionalLights[i].color.r == 0f && directionalLights[i].color.g == 0f && directionalLights[i].color.b == 0f)
                        continue;
                    directionalLights[i].color.set(0, 0, 0, 1);
                } else if (lightsSet && directionalLights[i].equals(dirs.get(i)))
                    continue;
                else
                    directionalLights[i].set(dirs.get(i));

                int idx = dirLightsLoc + i * dirLightsSize;
                program.setUniformf(idx + dirLightsColorOffset, directionalLights[i].color.r, directionalLights[i].color.g, directionalLights[i].color.b);
                program.setUniformf(idx + dirLightsDirectionOffset, directionalLights[i].direction.x, directionalLights[i].direction.y,
                        directionalLights[i].direction.z);
                if (dirLightsSize <= 0)
                    break;
            }
        }

        if (pointLightsLoc >= 0) {
            for (int i = 0; i < pointLights.length; i++) {
                if (points == null || i >= points.size) {
                    if (lightsSet && pointLights[i].intensity == 0f)
                        continue;
                    pointLights[i].intensity = 0f;
                } else if (lightsSet && pointLights[i].equals(points.get(i)))
                    continue;
                else
                    pointLights[i].set(points.get(i));

                int idx = pointLightsLoc + i * pointLightsSize;
                program.setUniformf(idx + pointLightsColorOffset, pointLights[i].color.r * pointLights[i].intensity,
                        pointLights[i].color.g * pointLights[i].intensity,
                        pointLights[i].color.b * pointLights[i].intensity);
                program.setUniformf(idx + pointLightsPositionOffset, pointLights[i].position.x, pointLights[i].position.y, pointLights[i].position.z);
                if (pointLightsIntensityOffset >= 0)
                    program.setUniformf(idx + pointLightsIntensityOffset, pointLights[i].intensity);
                if (pointLightsSize <= 0)
                    break;
            }
        }

        if (attributes.has(ColorAttribute.Fog)) {
            set(u_fogColor, ((ColorAttribute) Objects.requireNonNull(attributes.get(ColorAttribute.Fog))).color);
        }

        if (lights != null && lights.shadowMap != null) {
            set(u_shadowMapProjViewTrans, lights.shadowMap.getProjViewTrans());
            set(u_shadowTexture, lights.shadowMap.getDepthMap());
            set(u_shadowPCFOffset, 1.f / (2f * lights.shadowMap.getDepthMap().texture.getWidth()));
        }

        lightsSet = true;
    }

    @Override
    public void dispose() {
        program.dispose();
        super.dispose();
    }

    public int getDefaultCullFace() {
        return config.defaultCullFace;
    }

    public void setDefaultCullFace(int cullFace) {
        config.defaultCullFace = cullFace;
    }

    public int getDefaultDepthFunc() {
        return config.defaultDepthFunc;
    }

    public void setDefaultDepthFunc(int depthFunc) {
        config.defaultDepthFunc = depthFunc;
    }

    public static class Config {
        /**
         * File with the vertex shader, if any
         **/
        public String vertexShaderFile = null;
        /**
         * File with the fragment shader, if any
         **/
        public String fragmentShaderFile = null;
        /**
         * The uber vertex shader to use, null to use the default vertex shader.
         */
        public String vertexShaderCode = null;
        /**
         * The uber fragment shader to use, null to use the default fragment shader.
         */
        public String fragmentShaderCode = null;
        /**
         * The number of directional lights to use
         */
        public int numDirectionalLights = Constants.N_DIR_LIGHTS;
        /**
         * The number of point lights to use
         */
        public int numPointLights = Constants.N_POINT_LIGHTS;
        /**
         * The number of spotlights to use
         */
        public int numSpotLights = Constants.N_SPOT_LIGHTS;
        /**
         * The number of bones to use
         */
        public int numBones = 0;
        /**
         * Set to 0 to disable culling.
         */
        public int defaultCullFace = GL20.GL_BACK;
        /**
         * Set to 0 to disable depth test.
         */
        public int defaultDepthFunc = GL20.GL_LEQUAL;

        public Config() {
        }

        public Config(final String vertexShaderFile,
                      final String fragmentShaderFile,
                      final String vertexShaderCode,
                      final String fragmentShaderCode) {
            this.vertexShaderFile = vertexShaderFile;
            this.fragmentShaderFile = fragmentShaderFile;
            this.vertexShaderCode = vertexShaderCode;
            this.fragmentShaderCode = fragmentShaderCode;
        }

        public Config(final String vertexShaderCode,
                      final String fragmentShaderCode) {
            this(null, null, vertexShaderCode, fragmentShaderCode);
        }
    }

    public static class Inputs {
        public final static Uniform projTrans = new Uniform("u_projTrans");
        public final static Uniform projViewTrans = new Uniform("u_projViewTrans");
        public final static Uniform cameraPosition = new Uniform("u_cameraPosition");
        public final static Uniform cameraDirection = new Uniform("u_cameraDirection");
        public final static Uniform cameraUp = new Uniform("u_cameraUp");
        public final static Uniform cameraNearFar = new Uniform("u_cameraNearFar");
        public final static Uniform cameraK = new Uniform("u_cameraK");

        public final static Uniform prevProjView = new Uniform("u_prevProjView");
        public final static Uniform dCamPos = new Uniform("u_dCamPos");
        public final static Uniform vrScale = new Uniform("u_vrScale");
        public final static Uniform vrOffset = new Uniform("u_vrOffset");

        public final static Uniform worldTrans = new Uniform("u_worldTrans");
        public final static Uniform normalMatrix = new Uniform("u_normalMatrix");
        public final static Uniform bones = new Uniform("u_bones");

        public final static Uniform eclipseOutlines = new Uniform("u_eclipseOutlines", IntAttribute.EclipseOutlines);
        public final static Uniform eclipsingBodyPos = new Uniform("u_eclipsingBodyPos", Vector3Attribute.EclipsingBodyPos);
        public final static Uniform eclipsingBodyRadius = new Uniform("u_eclipsingBodyRadius", FloatAttribute.EclipsingBodyRadius);

        public final static Uniform opacity = new Uniform("u_opacity", BlendingAttribute.Type);
        public final static Uniform aoTexture = new Uniform("u_aoTexture", TextureAttribute.AO);
        public final static Uniform diffuseColor = new Uniform("u_diffuseColor", ColorAttribute.Diffuse);
        public final static Uniform diffuseTexture = new Uniform("u_diffuseTexture", TextureAttribute.Diffuse);
        public final static Uniform specularColor = new Uniform("u_specularColor", ColorAttribute.Specular);
        public final static Uniform specularTexture = new Uniform("u_specularTexture", TextureAttribute.Specular);
        public final static Uniform emissiveColor = new Uniform("u_emissiveColor", ColorAttribute.Emissive);
        public final static Uniform emissiveTexture = new Uniform("u_emissiveTexture", TextureAttribute.Emissive);
        public final static Uniform metallicColor = new Uniform("u_metallicColor", ColorAttribute.Metallic);
        public final static Uniform metallicTexture = new Uniform("u_metallicTexture", TextureAttribute.Metallic);
        public final static Uniform shininess = new Uniform("u_shininess", FloatAttribute.Shininess);
        public final static Uniform roughnessColor = new Uniform("u_roughnessColor", ColorAttribute.Roughness);
        public final static Uniform roughnessTexture = new Uniform("u_roughnessTexture", TextureAttribute.Roughness);
        public final static Uniform diffuseScatteringColor = new Uniform("u_diffuseScatteringColor", ColorAttribute.DiffuseScattering);
        public final static Uniform occlusionMetallicRoughnessTexture = new Uniform("u_occlusionMetallicRoughnessTexture",
                TextureAttribute.OcclusionMetallicRoughness);

        public final static Uniform normalTexture = new Uniform("u_normalTexture", TextureAttribute.Normal);
        public final static Uniform heightTexture = new Uniform("u_heightTexture", TextureAttribute.Height);
        public final static Uniform heightScale = new Uniform("u_heightScale", FloatAttribute.HeightScale);
        public final static Uniform elevationMultiplier = new Uniform("u_elevationMultiplier", FloatAttribute.ElevationMultiplier);
        public final static Uniform heightNoiseSize = new Uniform("u_heightNoiseSize", FloatAttribute.HeightNoiseSize);
        public final static Uniform heightSize = new Uniform("u_heightSize", Vector2Attribute.HeightSize);
        public final static Uniform tessQuality = new Uniform("u_tessQuality", FloatAttribute.TessQuality);
        public final static Uniform bodySize = new Uniform("u_bodySize", FloatAttribute.BodySize);
        public final static Uniform alphaTest = new Uniform("u_alphaTest");

        public final static Uniform time = new Uniform("u_time", FloatAttribute.Time);
        public final static Uniform ambientCube = new Uniform("u_ambientCubemap");
        public final static Uniform dirLights = new Uniform("u_dirLights");
        public final static Uniform pointLights = new Uniform("u_pointLights");
        public final static Uniform spotLights = new Uniform("u_spotLights");

        public final static Uniform reflectionCubemap = new Uniform("u_reflectionCubemap");

        public final static Uniform diffuseCubemap = new Uniform("u_diffuseCubemap");
        public final static Uniform normalCubemap = new Uniform("u_normalCubemap");
        public final static Uniform specularCubemap = new Uniform("u_specularCubemap");
        public final static Uniform emissionCubemap = new Uniform("u_emissionCubemap");
        public final static Uniform heightCubemap = new Uniform("u_heightCubemap");
        public final static Uniform metallicCubemap = new Uniform("u_metallicCubemap");
        public final static Uniform roughnessCubemap = new Uniform("u_roughnessCubemap");


        public final static Uniform iridescenceFactorUniform = new Uniform("u_iridescenceFactor");
        public final static Uniform iridescenceIORUniform = new Uniform("u_iridescenceIOR");
        public final static Uniform iridescenceThicknessMinUniform = new Uniform("u_iridescenceThicknessMin");
        public final static Uniform iridescenceThicknessMaxUniform = new Uniform("u_iridescenceThicknessMax");
        public final static Uniform iridescenceTextureUniform = new Uniform("u_iridescenceSampler", PBRTextureAttribute.IridescenceTexture);
        public final static Uniform iridescenceThicknessTextureUniform = new Uniform("u_iridescenceThicknessSampler", PBRTextureAttribute.IridescenceThicknessTexture);

        public final static Uniform iorUniform = new Uniform("u_ior");
        public final static Uniform thicknessFactorUniform = new Uniform("u_thicknessFactor");
        public final static Uniform volumeDistanceUniform = new Uniform("u_attenuationDistance");
        public final static Uniform volumeColorUniform = new Uniform("u_attenuationColor");
        public final static Uniform thicknessTextureUniform = new Uniform("u_thicknessSampler", PBRTextureAttribute.ThicknessTexture);

        public final static Uniform svtTileSize = new Uniform("u_svtTileSize", FloatAttribute.SvtTileSize);
        public final static Uniform svtResolution = new Uniform("u_svtResolution", Vector2Attribute.SvtResolution);
        public final static Uniform svtDepth = new Uniform("u_svtDepth", FloatAttribute.SvtDepth);
        public final static Uniform svtId = new Uniform("u_svtId", FloatAttribute.SvtId);
        public final static Uniform svtDetectionFactor = new Uniform("u_svtDetectionFactor", FloatAttribute.SvtDetectionFactor);
        public final static Uniform svtCacheTexture = new Uniform("u_svtCacheTexture");
        public final static Uniform svtIndirectionDiffuseTexture = new Uniform("u_svtIndirectionDiffuseTexture");
        public final static Uniform svtIndirectionSpecularTexture = new Uniform("u_svtIndirectionSpecularTexture");
        public final static Uniform svtIndirectionNormalTexture = new Uniform("u_svtIndirectionNormalTexture");
        public final static Uniform svtIndirectionHeightTexture = new Uniform("u_svtIndirectionHeightTexture");
        public final static Uniform svtIndirectionEmissiveTexture = new Uniform("u_svtIndirectionEmissiveTexture");
        public final static Uniform svtIndirectionMetallicTexture = new Uniform("u_svtIndirectionMetallicTexture");
        public final static Uniform svtIndirectionRoughnessTexture = new Uniform("u_svtIndirectionRoughnessTexture");
    }

    public static class Setters {
        public final static Setter projTrans = new GlobalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                shader.set(inputID, shader.camera.projection);
            }
        };
        public final static Setter projViewTrans = new GlobalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                shader.set(inputID, shader.camera.combined);
            }
        };
        public final static Setter cameraPosition = new GlobalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                shader.set(inputID, shader.camera.position.x, shader.camera.position.y, shader.camera.position.z, 1.1881f / (shader.camera.far * shader.camera.far));
            }
        };
        public final static Setter cameraDirection = new GlobalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                shader.set(inputID, shader.camera.direction);
            }
        };
        public final static Setter cameraUp = new GlobalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                shader.set(inputID, shader.camera.up);
            }
        };
        public final static Setter cameraNearFar = new GlobalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                shader.set(inputID, shader.camera.near, shader.camera.far);
            }
        };
        public final static Setter cameraK = new GlobalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                shader.set(inputID, Constants.getCameraK());
            }
        };
        public final static Setter worldTrans = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                shader.set(inputID, renderable.worldTransform);
            }
        };
        public final static Setter normalMatrix = new LocalSetter() {
            private final Matrix3 tmpM = new Matrix3();

            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                shader.set(inputID, tmpM.set(renderable.worldTransform).inv().transpose());
            }
        };
        public final static Setter prevProjView = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                if (combinedAttributes.has(Matrix4Attribute.PrevProjView))
                    shader.set(inputID, ((Matrix4Attribute) (Objects.requireNonNull(combinedAttributes.get(Matrix4Attribute.PrevProjView)))).value);
            }
        };
        public final static Setter eclipseOutlines = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                if (combinedAttributes.has(IntAttribute.EclipseOutlines))
                    shader.set(inputID, ((IntAttribute) (Objects.requireNonNull(combinedAttributes.get(IntAttribute.EclipseOutlines)))).value);
            }
        };
        public final static Setter eclipsingBodyPos = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                if (combinedAttributes.has(Vector3Attribute.EclipsingBodyPos))
                    shader.set(inputID, ((Vector3Attribute) (Objects.requireNonNull(combinedAttributes.get(Vector3Attribute.EclipsingBodyPos)))).value);
            }
        };
        public final static Setter eclipsingBodyRadius = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                if (combinedAttributes.has(FloatAttribute.EclipsingBodyRadius))
                    shader.set(inputID, ((FloatAttribute) (Objects.requireNonNull(combinedAttributes.get(FloatAttribute.EclipsingBodyRadius)))).value);
            }
        };
        public final static Setter dCamPos = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                if (combinedAttributes.has(Vector3Attribute.DCamPos))
                    shader.set(inputID, ((Vector3Attribute) (Objects.requireNonNull(combinedAttributes.get(Vector3Attribute.DCamPos)))).value);
            }
        };
        public final static Setter vrScale = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                shader.set(inputID, (float) Constants.DISTANCE_SCALE_FACTOR);
            }
        };
        public final static Setter vrOffset = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                if (combinedAttributes.has(Vector3Attribute.VrOffset))
                    shader.set(inputID, ((Vector3Attribute) (Objects.requireNonNull(combinedAttributes.get(Vector3Attribute.VrOffset)))).value);
            }
        };
        public final static Setter aoTexture = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                final int unit = shader.context.textureBinder.bind(
                        ((TextureAttribute) (Objects.requireNonNull(combinedAttributes.get(TextureAttribute.AO)))).textureDescription);
                shader.set(inputID, unit);
            }
        };
        public final static Setter time = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                shader.set(inputID, ((FloatAttribute) (Objects.requireNonNull(combinedAttributes.get(FloatAttribute.Time)))).value);
            }
        };
        public final static Setter diffuseColor = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                shader.set(inputID, ((ColorAttribute) (Objects.requireNonNull(combinedAttributes.get(ColorAttribute.Diffuse)))).color);
            }
        };
        public final static Setter diffuseTexture = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                final int unit = shader.context.textureBinder.bind(
                        ((TextureAttribute) (Objects.requireNonNull(combinedAttributes.get(TextureAttribute.Diffuse)))).textureDescription);
                shader.set(inputID, unit);
            }
        };
        public final static Setter specularColor = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                shader.set(inputID, ((ColorAttribute) (Objects.requireNonNull(combinedAttributes.get(ColorAttribute.Specular)))).color);
            }
        };
        public final static Setter specularTexture = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                final int unit = shader.context.textureBinder.bind(
                        ((TextureAttribute) (Objects.requireNonNull(combinedAttributes.get(TextureAttribute.Specular)))).textureDescription);
                shader.set(inputID, unit);
            }
        };
        public final static Setter emissiveColor = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                shader.set(inputID, ((ColorAttribute) (Objects.requireNonNull(combinedAttributes.get(ColorAttribute.Emissive)))).color);
            }
        };
        public final static Setter emissiveTexture = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                final int unit = shader.context.textureBinder.bind(
                        ((TextureAttribute) (Objects.requireNonNull(combinedAttributes.get(TextureAttribute.Emissive)))).textureDescription);
                shader.set(inputID, unit);
            }
        };
        public final static Setter metallicColor = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                shader.set(inputID, ((ColorAttribute) (Objects.requireNonNull(combinedAttributes.get(ColorAttribute.Metallic)))).color);
            }
        };
        public final static Setter metallicTexture = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                final int unit = shader.context.textureBinder.bind(
                        ((TextureAttribute) (Objects.requireNonNull(combinedAttributes.get(TextureAttribute.Metallic)))).textureDescription);
                shader.set(inputID, unit);
            }
        };
        public final static Setter shininess = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                shader.set(inputID, ((FloatAttribute) (Objects.requireNonNull(combinedAttributes.get(FloatAttribute.Shininess)))).value);
            }
        };
        public final static Setter roughnessColor = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                shader.set(inputID, ((ColorAttribute) (Objects.requireNonNull(combinedAttributes.get(ColorAttribute.Roughness)))).color);
            }
        };
        public final static Setter roughnessTexture = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                final int unit = shader.context.textureBinder.bind(
                        ((TextureAttribute) (Objects.requireNonNull(combinedAttributes.get(TextureAttribute.Roughness)))).textureDescription);
                shader.set(inputID, unit);
            }
        };
        public final static Setter diffuseScatteringColor = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                shader.set(inputID, ((ColorAttribute) (Objects.requireNonNull(combinedAttributes.get(ColorAttribute.DiffuseScattering)))).color);
            }
        };
        public final static Setter occlusionMetallicRoughnessTexture = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                final int unit = shader.context.textureBinder.bind(
                        ((TextureAttribute) (Objects.requireNonNull(combinedAttributes.get(TextureAttribute.OcclusionMetallicRoughness)))).textureDescription);
                shader.set(inputID, unit);
            }
        };
        public final static Setter normalTexture = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                final int unit = shader.context.textureBinder.bind(
                        ((TextureAttribute) (Objects.requireNonNull(combinedAttributes.get(TextureAttribute.Normal)))).textureDescription);
                shader.set(inputID, unit);
            }
        };
        public final static Setter heightTexture = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                final int unit = shader.context.textureBinder.bind(
                        ((TextureAttribute) (Objects.requireNonNull(combinedAttributes.get(TextureAttribute.Height)))).textureDescription);
                shader.set(inputID, unit);
            }
        };
        public final static Setter heightScale = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                if (combinedAttributes.has(FloatAttribute.HeightScale))
                    shader.set(inputID, ((FloatAttribute) (Objects.requireNonNull(combinedAttributes.get(FloatAttribute.HeightScale)))).value);
            }
        };
        public final static Setter elevationMultiplier = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                if (combinedAttributes.has(FloatAttribute.ElevationMultiplier))
                    shader.set(inputID, ((FloatAttribute) (Objects.requireNonNull(combinedAttributes.get(FloatAttribute.ElevationMultiplier)))).value);
            }
        };
        public final static Setter heightNoiseSize = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                if (combinedAttributes.has(FloatAttribute.HeightNoiseSize))
                    shader.set(inputID, ((FloatAttribute) (Objects.requireNonNull(combinedAttributes.get(FloatAttribute.HeightNoiseSize)))).value);
            }
        };
        public final static Setter heightSize = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                if (combinedAttributes.has(Vector2Attribute.HeightSize))
                    shader.set(inputID, ((Vector2Attribute) (Objects.requireNonNull(combinedAttributes.get(Vector2Attribute.HeightSize)))).value);
            }
        };
        public final static Setter tessQuality = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                if (combinedAttributes.has(FloatAttribute.TessQuality))
                    shader.set(inputID, ((FloatAttribute) (Objects.requireNonNull(combinedAttributes.get(FloatAttribute.TessQuality)))).value);
            }
        };
        public final static Setter bodySize = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                if (combinedAttributes.has(FloatAttribute.BodySize))
                    shader.set(inputID, ((FloatAttribute) (Objects.requireNonNull(combinedAttributes.get(FloatAttribute.BodySize)))).value);
            }
        };
        public final static Setter reflectionCubemap = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                if (combinedAttributes.has(CubemapAttribute.ReflectionCubemap)) {
                    shader.set(inputID, shader.context.textureBinder.bind(
                            ((CubemapAttribute) Objects.requireNonNull(combinedAttributes.get(CubemapAttribute.ReflectionCubemap))).textureDescription));
                }
            }
        };
        public final static Setter diffuseCubemap = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                if (combinedAttributes.has(CubemapAttribute.DiffuseCubemap)) {
                    shader.set(inputID, shader.context.textureBinder.bind(
                            ((CubemapAttribute) Objects.requireNonNull(combinedAttributes.get(CubemapAttribute.DiffuseCubemap))).textureDescription));
                }
            }
        };
        public final static Setter normalCubemap = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                if (combinedAttributes.has(CubemapAttribute.NormalCubemap))
                    shader.set(inputID, shader.context.textureBinder.bind(
                            ((CubemapAttribute) Objects.requireNonNull(combinedAttributes.get(CubemapAttribute.NormalCubemap))).textureDescription));
            }
        };
        public final static Setter emissiveCubemap = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                if (combinedAttributes.has(CubemapAttribute.EmissiveCubemap)) {
                    shader.set(inputID, shader.context.textureBinder.bind(
                            ((CubemapAttribute) Objects.requireNonNull(combinedAttributes.get(CubemapAttribute.EmissiveCubemap))).textureDescription));
                }
            }
        };
        public final static Setter specularCubemap = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                if (combinedAttributes.has(CubemapAttribute.SpecularCubemap)) {
                    shader.set(inputID, shader.context.textureBinder.bind(
                            ((CubemapAttribute) Objects.requireNonNull(combinedAttributes.get(CubemapAttribute.SpecularCubemap))).textureDescription));
                }
            }
        };
        public final static Setter heightCubemap = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                if (combinedAttributes.has(CubemapAttribute.HeightCubemap)) {
                    shader.set(inputID, shader.context.textureBinder.bind(
                            ((CubemapAttribute) Objects.requireNonNull(combinedAttributes.get(CubemapAttribute.HeightCubemap))).textureDescription));
                }
            }
        };
        public final static Setter metallicCubemap = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                if (combinedAttributes.has(CubemapAttribute.MetallicCubemap)) {
                    shader.set(inputID, shader.context.textureBinder.bind(
                            ((CubemapAttribute) Objects.requireNonNull(combinedAttributes.get(CubemapAttribute.MetallicCubemap))).textureDescription));
                }
            }
        };
        public final static Setter roughnessCubemap = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                if (combinedAttributes.has(CubemapAttribute.RoughnessCubemap)) {
                    shader.set(inputID, shader.context.textureBinder.bind(
                            ((CubemapAttribute) Objects.requireNonNull(combinedAttributes.get(CubemapAttribute.RoughnessCubemap))).textureDescription));
                }
            }
        };
        public final static Setter iridescenceFactorSetter = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                PBRIridescenceAttribute a = combinedAttributes.get(PBRIridescenceAttribute.class, PBRIridescenceAttribute.Type);
                shader.set(inputID, a.factor);
            }
        };
        public final static Setter iridescenceIORSetter = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                PBRIridescenceAttribute a = combinedAttributes.get(PBRIridescenceAttribute.class, PBRIridescenceAttribute.Type);
                shader.set(inputID, a.ior);
            }
        };
        public final static Setter iridescenceThicknessMinSetter = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                PBRIridescenceAttribute a = combinedAttributes.get(PBRIridescenceAttribute.class, PBRIridescenceAttribute.Type);
                shader.set(inputID, a.thicknessMin);
            }
        };
        public final static Setter iridescenceThicknessMaxSetter = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                PBRIridescenceAttribute a = combinedAttributes.get(PBRIridescenceAttribute.class, PBRIridescenceAttribute.Type);
                shader.set(inputID, a.thicknessMax);
            }
        };
        public final static Setter iridescenceTextureSetter = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                final int unit = shader.context.textureBinder.bind(((TextureAttribute) (combinedAttributes
                        .get(PBRTextureAttribute.IridescenceTexture))).textureDescription);
                shader.set(inputID, unit);
            }
        };
        public final static Setter iridescenceThicknessTextureSetter = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                final int unit = shader.context.textureBinder.bind(((TextureAttribute) (combinedAttributes
                        .get(PBRTextureAttribute.IridescenceThicknessTexture))).textureDescription);
                shader.set(inputID, unit);
            }
        };
        public final static Setter iorSetter = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                PBRFloatAttribute a = combinedAttributes.get(PBRFloatAttribute.class, PBRFloatAttribute.IOR);
                shader.set(inputID, a.value);
            }
        };
        public final static Setter thicknessFactorSetter = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                PBRVolumeAttribute a = combinedAttributes.get(PBRVolumeAttribute.class, PBRVolumeAttribute.Type);
                shader.set(inputID, a.thicknessFactor);
            }
        };
        public final static Setter volumeDistanceSetter = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                PBRVolumeAttribute a = combinedAttributes.get(PBRVolumeAttribute.class, PBRVolumeAttribute.Type);
                shader.set(inputID, a.attenuationDistance);
            }
        };
        public final static Setter volumeColorSetter = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                PBRVolumeAttribute a = combinedAttributes.get(PBRVolumeAttribute.class, PBRVolumeAttribute.Type);
                shader.set(inputID, a.attenuationColor.r, a.attenuationColor.g, a.attenuationColor.b);
            }
        };
        public final static Setter thicknessTextureSetter = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                final int unit = shader.context.textureBinder.bind(((TextureAttribute) (Objects.requireNonNull(combinedAttributes
                        .get(PBRTextureAttribute.ThicknessTexture)))).textureDescription);
                shader.set(inputID, unit);
            }
        };

        public final static Setter svtTileSize = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                shader.set(inputID, ((FloatAttribute) (Objects.requireNonNull(combinedAttributes.get(FloatAttribute.SvtTileSize)))).value);
            }
        };
        public final static Setter svtResolution = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                shader.set(inputID, ((Vector2Attribute) (Objects.requireNonNull(combinedAttributes.get(Vector2Attribute.SvtResolution)))).value);
            }
        };
        public final static Setter svtDepth = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                shader.set(inputID, ((FloatAttribute) (Objects.requireNonNull(combinedAttributes.get(FloatAttribute.SvtDepth)))).value);
            }
        };
        public final static Setter svtId = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                shader.set(inputID, ((FloatAttribute) (Objects.requireNonNull(combinedAttributes.get(FloatAttribute.SvtId)))).value);
            }
        };
        public final static Setter svtDetectionFactor = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                shader.set(inputID, ((FloatAttribute) (Objects.requireNonNull(combinedAttributes.get(FloatAttribute.SvtDetectionFactor)))).value);
            }
        };
        public final static Setter svtBufferTexture = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                if (combinedAttributes.has(TextureAttribute.SvtCache)) {
                    shader.set(inputID, shader.context.textureBinder.bind(
                            ((TextureAttribute) Objects.requireNonNull(combinedAttributes.get(TextureAttribute.SvtCache))).textureDescription));
                }
            }
        };
        public final static Setter svtIndirectionDiffuseTexture = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                if (combinedAttributes.has(TextureAttribute.SvtIndirectionDiffuse)) {
                    shader.set(inputID, shader.context.textureBinder.bind(
                            ((TextureAttribute) (Objects.requireNonNull(combinedAttributes.get(TextureAttribute.SvtIndirectionDiffuse)))).textureDescription));
                }
            }
        };
        public final static Setter svtIndirectionSpecularTexture = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                if (combinedAttributes.has(TextureAttribute.SvtIndirectionSpecular)) {
                    shader.set(inputID, shader.context.textureBinder.bind(
                            ((TextureAttribute) (Objects.requireNonNull(combinedAttributes.get(TextureAttribute.SvtIndirectionSpecular)))).textureDescription));
                }
            }
        };
        public final static Setter svtIndirectionNormalTexture = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                if (combinedAttributes.has(TextureAttribute.SvtIndirectionNormal)) {
                    shader.set(inputID, shader.context.textureBinder.bind(
                            ((TextureAttribute) (Objects.requireNonNull(combinedAttributes.get(TextureAttribute.SvtIndirectionNormal)))).textureDescription));
                }
            }
        };
        public final static Setter svtIndirectionHeightTexture = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                if (combinedAttributes.has(TextureAttribute.SvtIndirectionHeight)) {
                    shader.set(inputID, shader.context.textureBinder.bind(
                            ((TextureAttribute) (Objects.requireNonNull(combinedAttributes.get(TextureAttribute.SvtIndirectionHeight)))).textureDescription));
                }
            }
        };
        public final static Setter svtIndirectionEmissiveTexture = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                if (combinedAttributes.has(TextureAttribute.SvtIndirectionEmissive)) {
                    shader.set(inputID, shader.context.textureBinder.bind(
                            ((TextureAttribute) (Objects.requireNonNull(combinedAttributes.get(TextureAttribute.SvtIndirectionEmissive)))).textureDescription));
                }
            }
        };
        public final static Setter svtIndirectionMetallicTexture = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                if (combinedAttributes.has(TextureAttribute.SvtIndirectionMetallic)) {
                    shader.set(inputID, shader.context.textureBinder.bind(
                            ((TextureAttribute) (Objects.requireNonNull(combinedAttributes.get(TextureAttribute.SvtIndirectionMetallic)))).textureDescription));
                }
            }
        };
        public final static Setter svtIndirectionRoughnessTexture = new LocalSetter() {
            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                if (combinedAttributes.has(TextureAttribute.SvtIndirectionRoughness)) {
                    shader.set(inputID, shader.context.textureBinder.bind(
                            ((TextureAttribute) (Objects.requireNonNull(combinedAttributes.get(TextureAttribute.SvtIndirectionRoughness)))).textureDescription));
                }
            }
        };

        public static class Bones extends LocalSetter {
            private final static Matrix4 idtMatrix = new Matrix4();
            public final float[] bones;

            public Bones(final int numBones) {
                this.bones = new float[numBones * 16];
            }

            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                for (int i = 0; i < bones.length; i++) {
                    final int idx = i / 16;
                    bones[i] = (renderable.bones == null || idx >= renderable.bones.length || renderable.bones[idx] == null) ?
                            idtMatrix.val[i % 16] :
                            renderable.bones[idx].val[i % 16];
                }
                shader.program.setUniformMatrix4fv(shader.loc(inputID), bones, 0, bones.length);
            }
        }

        public static class ACubemap extends LocalSetter {
            private final static float[] ones = {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
            private final static Vector3 tmpV1 = new Vector3();
            public final int dirLightsOffset;
            public final int pointLightsOffset;
            private final AmbientCubemap cacheAmbientCubemap = new AmbientCubemap();

            public ACubemap(final int dirLightsOffset,
                            final int pointLightsOffset) {
                this.dirLightsOffset = dirLightsOffset;
                this.pointLightsOffset = pointLightsOffset;
            }

            @Override
            public void set(BaseIntShader shader,
                            int inputID,
                            IntRenderable renderable,
                            Attributes combinedAttributes) {
                if (renderable.environment == null)
                    shader.program.setUniform3fv(shader.loc(inputID), ones, 0, ones.length);
                else {
                    renderable.worldTransform.getTranslation(tmpV1);
                    if (combinedAttributes.has(ColorAttribute.AmbientLight))
                        cacheAmbientCubemap.set(((ColorAttribute) Objects.requireNonNull(combinedAttributes.get(ColorAttribute.AmbientLight))).color);

                    if (combinedAttributes.has(DirectionalLightsAttribute.Type)) {
                        Array<DirectionalLight> lights = ((DirectionalLightsAttribute) Objects.requireNonNull(
                                combinedAttributes.get(DirectionalLightsAttribute.Type))).lights;
                        for (int i = dirLightsOffset; i < lights.size; i++)
                            cacheAmbientCubemap.add(lights.get(i).color, lights.get(i).direction);
                    }

                    if (combinedAttributes.has(PointLightsAttribute.Type)) {
                        Array<PointLight> lights = ((PointLightsAttribute) Objects.requireNonNull(
                                combinedAttributes.get(PointLightsAttribute.Type))).lights;
                        for (int i = pointLightsOffset; i < lights.size; i++)
                            cacheAmbientCubemap.add(lights.get(i).color, lights.get(i).position, tmpV1, lights.get(i).intensity);
                    }

                    cacheAmbientCubemap.clamp();
                    shader.program.setUniform3fv(shader.loc(inputID), cacheAmbientCubemap.data, 0, cacheAmbientCubemap.data.length);
                }
            }
        }
    }
}
