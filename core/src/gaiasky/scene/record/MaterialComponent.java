/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.data.AssetBean;
import gaiasky.data.util.GlobalResources;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.render.ComponentTypes;
import gaiasky.render.gdx.graphics.VolumeTexture;
import gaiasky.render.gdx.loader.OwnTextureLoader.OwnTextureParameter;
import gaiasky.render.gdx.loader.PFMTextureLoader.PFMTextureParameter;
import gaiasky.render.gdx.loader.VolumeTextureLoader;
import gaiasky.render.gdx.model.IntModelInstance;
import gaiasky.render.gdx.shader.Material;
import gaiasky.render.gdx.shader.attribute.*;
import gaiasky.scene.api.IUpdatable;
import gaiasky.util.*;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings.ElevationType;
import gaiasky.render.util.BiomeLUTManager;
import gaiasky.util.i18n.I18n;
import gaiasky.util.svt.SVTManager;
import net.jafama.FastMath;

import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Contains the material of a model or part of a model.
 */
public final class MaterialComponent extends NamedComponent implements IObserver, IMaterialProvider, IUpdatable<MaterialComponent> {
    private static final Log logger = Logger.getLogger(MaterialComponent.class);
    /**
     * Default texture parameters
     **/
    private static final OwnTextureParameter textureParamsMipMap, textureParams;
    private static final PFMTextureParameter pfmTextureParams;

    private static final BiomeLUTManager lutManager = new BiomeLUTManager(GaiaSky.settings().data.dataPath("default-data/tex/lut"));

    /** Reflection cubemap for all materials. **/
    @SuppressWarnings("GDXJavaStaticResource")
    public static final CubemapComponent sharedReflectionCubemap = new CubemapComponent();

    public static BiomeLUTManager getLUTManager() {
        lutManager.loadAll();
        return lutManager;
    }

    private static void initializeBiomeLUTManager() {
        lutManager.loadAll();
    }

    // Default height scale is 4 km.
    private static final float DEFAULT_HEIGHT_SCALE = (float) (4.0 * Constants.KM_TO_U);


    static {
        textureParamsMipMap = new OwnTextureParameter();
        textureParamsMipMap.genMipMaps = true;
        textureParamsMipMap.magFilter = TextureFilter.Linear;
        textureParamsMipMap.minFilter = TextureFilter.MipMapLinearLinear;

        textureParams = new OwnTextureParameter();
        textureParams.genMipMaps = false;
        textureParams.magFilter = TextureFilter.Linear;
        textureParams.minFilter = TextureFilter.Linear;

        pfmTextureParams = new PFMTextureParameter(textureParams);
        pfmTextureParams.invert = false;
        pfmTextureParams.internalFormat = GL20.GL_RGB;
    }

    // Texture location strings.
    public boolean texInitialised, texLoading;
    public String diffuse, specular, normal, emissive, ring, height, ringnormal, roughness, metallic, ao, occlusionMetallicRoughness,
            texture0, texture1, volume0, volume1, volume2, volume3;
    public String diffuseUnpacked, specularUnpacked, normalUnpacked, emissiveUnpacked, ringUnpacked,
            heightUnpacked, ringnormalUnpacked, roughnessUnapcked, metallicUnpacked, aoUnpacked, occlusionMetallicRoughnessUnpacked, texture0Unpacked,
            texture1Unpacked, volume0Unpacked, volume1Unpacked, volume2Unpacked, volume3Unpacked;
    // Material properties and colors.
    public float[] diffuseColor;
    public float[] specularColor;
    public float[] metallicColor;
    public float[] emissiveColor;
    public float[] diffuseScatteringColor;
    public float[] ringDiffuseScatteringColor;
    public float roughnessColor = Float.NaN;
    /**
     * Height scale in internal units. The mapping value of white in the height map (maximum height value in this body). Black is mapped to 0.
     */
    public Float heightScale = DEFAULT_HEIGHT_SCALE;
    public Vector2 heightSize = new Vector2();
    public IHeightData heightData;
    public NoiseComponent nc;
    // Sparse Virtual Textures.
    public VirtualTextureComponent diffuseSvt, specularSvt, heightSvt, normalSvt, emissiveSvt, roughnessSvt, metallicSvt, aoSvt;
    public Array<VirtualTextureComponent> svts;
    // Cubemaps.
    public CubemapComponent diffuseCubemap, specularCubemap, normalCubemap, emissiveCubemap, heightCubemap,
            roughnessCubemap, metallicCubemap, aoCubemap;
    /** AO texture. **/
    public Texture aoTexture;
    /** Occlusion clouds: ambient occlusion uses the cloud texture. **/
    public boolean occlusionClouds = false;
    /**
     * Biome lookup texture name.
     * This is the name stripped of suffixes and prefixes. Examples:
     * <p>
     * biome_lut_earthlike_00.jpg -> earthlike
     * <p>
     * biome_lut_desert_03.jpg -> desert
      */
    public String lut = "earthlike";
    public float lutHueShift = 0f;
    public float lutSaturation = 0.5f;
    /**
     * Add also color even if texture is present
     **/
    public boolean colorIfTexture = false;
    /**
     * The actual material
     **/
    private Material material, ringMaterial;
    private final AtomicBoolean heightGenerated = new AtomicBoolean(false);
    private final AtomicBoolean heightInitialized = new AtomicBoolean(false);
    private Texture heightTex, specularTex, diffuseTex, normalTex, emissiveTex;

    public MaterialComponent() {
        super();
        svts = new Array<>();
        EventManager.instance.subscribe(this, Event.ELEVATION_TYPE_CMD, Event.ELEVATION_MULTIPLIER_CMD,
                                        Event.TESSELLATION_QUALITY_CMD, Event.TOGGLE_VISIBILITY_CMD);
    }

    private static OwnTextureParameter getTP(String tex) {
        return getTP(tex, false);
    }

    private static OwnTextureParameter getTP(String tex,
                                             boolean mipmap) {
        if (tex != null && tex.endsWith(".pfm")) {
            return pfmTextureParams;
        } else {
            if (mipmap)
                return textureParamsMipMap;
            else
                return textureParams;
        }
    }

    public void initialize(String name,
                           AssetManager manager) {
        super.initialize(name);

        // Regular textures
        if (diffuse != null && !diffuse.endsWith(Constants.GEN_KEYWORD))
            diffuseUnpacked = addToLoad(diffuse, getTP(diffuse, true), manager);
        if (normal != null && !normal.endsWith(Constants.GEN_KEYWORD))
            normalUnpacked = addToLoad(normal, getTP(normal), manager);
        if (specular != null && !specular.endsWith(Constants.GEN_KEYWORD))
            specularUnpacked = addToLoad(specular, getTP(specular, true), manager);
        if (emissive != null && !emissive.endsWith(Constants.GEN_KEYWORD))
            emissiveUnpacked = addToLoad(emissive, getTP(emissive, true), manager);
        if (roughness != null && !roughness.endsWith(Constants.GEN_KEYWORD))
            roughnessUnapcked = addToLoad(roughness, getTP(roughness, true), manager);
        if (metallic != null && !metallic.endsWith(Constants.GEN_KEYWORD))
            metallicUnpacked = addToLoad(metallic, getTP(metallic, true), manager);
        if (ao != null && !ao.endsWith(Constants.GEN_KEYWORD))
            aoUnpacked = addToLoad(ao, getTP(ao, true), manager);
        if (occlusionMetallicRoughness != null && !occlusionMetallicRoughness.endsWith(Constants.GEN_KEYWORD))
            occlusionMetallicRoughnessUnpacked = addToLoad(occlusionMetallicRoughness, getTP(occlusionMetallicRoughness, true), manager);
        if (height != null && !height.endsWith(Constants.GEN_KEYWORD))
            heightUnpacked = addToLoad(height, getTP(height, true), manager);
        if (ring != null)
            ringUnpacked = addToLoad(ring, getTP(ring, true), manager);
        if (ringnormal != null)
            ringnormalUnpacked = addToLoad(ringnormal, getTP(ringnormal, true), manager);
        if (texture0 != null)
            texture0Unpacked = addToLoad(texture0, getTP(texture0, true), manager);
        if (texture1 != null)
            texture1Unpacked = addToLoad(texture1, getTP(texture1, true), manager);

        // Volumes (3D textures)
        if (volume0 != null)
            volume0Unpacked = addToLoad(volume0, new VolumeTextureLoader.VolumeTextureParameter(), manager);
        if (volume1 != null)
            volume1Unpacked = addToLoad(volume1, new VolumeTextureLoader.VolumeTextureParameter(), manager);
        if (volume2 != null)
            volume2Unpacked = addToLoad(volume2, new VolumeTextureLoader.VolumeTextureParameter(), manager);
        if (volume3 != null)
            volume3Unpacked = addToLoad(volume3, new VolumeTextureLoader.VolumeTextureParameter(), manager);

        // Cube maps
        if (diffuseCubemap != null)
            diffuseCubemap.initialize(manager);
        if (normalCubemap != null)
            normalCubemap.initialize(manager);
        if (specularCubemap != null)
            specularCubemap.initialize(manager);
        if (emissiveCubemap != null)
            emissiveCubemap.initialize(manager);
        if (heightCubemap != null)
            heightCubemap.initialize(manager);
        if (roughnessCubemap != null)
            roughnessCubemap.initialize(manager);
        if (metallicCubemap != null)
            metallicCubemap.initialize(manager);
        if (aoCubemap != null)
            aoCubemap.initialize(manager);

        // SVTs
        if (diffuseSvt != null)
            diffuseSvt.initialize("diffuseSvt", this, TextureAttribute.SvtIndirectionDiffuse);
        if (heightSvt != null)
            heightSvt.initialize("heightSvt", this, TextureAttribute.SvtIndirectionHeight);
        if (specularSvt != null)
            specularSvt.initialize("specularSvt", this, TextureAttribute.SvtIndirectionSpecular);
        if (normalSvt != null)
            normalSvt.initialize("normalSvt", this, TextureAttribute.SvtIndirectionNormal);
        if (emissiveSvt != null)
            emissiveSvt.initialize("emissiveSvt", this, TextureAttribute.SvtIndirectionEmissive);
        if (roughnessSvt != null)
            roughnessSvt.initialize("roughnessSvt", this, TextureAttribute.SvtIndirectionRoughness);
        if (metallicSvt != null)
            metallicSvt.initialize("metallicSvt", this, TextureAttribute.SvtIndirectionMetallic);
        if (aoSvt != null)
            aoSvt.initialize("aoSvt", this, TextureAttribute.SvtIndirectionAmbientOcclusion);

        this.heightGenerated.set(false);
    }

    public void initialize(String name) {
        initialize(name, null);
    }

    public boolean isFinishedLoading(AssetManager manager) {
        return ComponentUtils.isLoaded(diffuseUnpacked, manager)
                && ComponentUtils.isLoaded(normalUnpacked, manager)
                && ComponentUtils.isLoaded(specularUnpacked, manager)
                && ComponentUtils.isLoaded(emissiveUnpacked, manager)
                && ComponentUtils.isLoaded(ringUnpacked, manager)
                && ComponentUtils.isLoaded(ringnormalUnpacked, manager)
                && ComponentUtils.isLoaded(heightUnpacked, manager)
                && ComponentUtils.isLoaded(roughnessUnapcked, manager)
                && ComponentUtils.isLoaded(metallicUnpacked, manager)
                && ComponentUtils.isLoaded(aoUnpacked, manager)
                && ComponentUtils.isLoaded(diffuseCubemap, manager)
                && ComponentUtils.isLoaded(normalCubemap, manager)
                && ComponentUtils.isLoaded(emissiveCubemap, manager)
                && ComponentUtils.isLoaded(specularCubemap, manager)
                && ComponentUtils.isLoaded(roughnessCubemap, manager)
                && ComponentUtils.isLoaded(metallicCubemap, manager)
                && ComponentUtils.isLoaded(heightCubemap, manager)
                && ComponentUtils.isLoaded(aoCubemap, manager)
                && ComponentUtils.isLoaded(volume0, manager)
                && ComponentUtils.isLoaded(volume1, manager)
                && ComponentUtils.isLoaded(volume2, manager)
                && ComponentUtils.isLoaded(volume3, manager)
                && ComponentUtils.isLoaded(texture0, manager)
                && ComponentUtils.isLoaded(texture1, manager);
    }

    public boolean hasSVT() {
        return diffuseSvt != null || normalSvt != null || emissiveSvt != null || specularSvt != null
                || heightSvt != null || metallicSvt != null || roughnessSvt != null || aoSvt != null;
    }

    /**
     * Adds the texture to load and unpacks any star (*) with the current
     * quality setting.
     *
     * @param tex The texture file to load.
     *
     * @return The actual loaded texture path
     */
    private String addToLoad(String tex,
                             OwnTextureParameter texParams,
                             AssetManager manager) {
        if (manager == null)
            return addToLoad(tex, texParams);

        if (tex == null)
            return null;

        tex = GlobalResources.unpackAssetPath(tex);
        logger.info(I18n.msg("notif.loading", tex));
        manager.load(tex, Texture.class, texParams);

        return tex;
    }

    /**
     * Adds the texture to load and unpacks any star (*) with the current
     * quality setting.
     *
     * @param tex The texture file to load.
     *
     * @return The actual loaded texture path
     */
    private String addToLoad(String tex,
                             OwnTextureParameter texParams) {
        if (tex == null)
            return null;

        tex = GlobalResources.unpackAssetPath(tex);
        logger.info(I18n.msg("notif.loading", tex));
        AssetBean.addAsset(tex, Texture.class, texParams);

        return tex;
    }

    /**
     * Adds the 3D texture to load and unpacks any star (*) with the current
     * quality setting.
     *
     * @param tex The 3D texture file to load.
     *
     * @return The actual loaded 3D texture path
     */
    private String addToLoad(String tex,
                             VolumeTextureLoader.VolumeTextureParameter texParams,
                             AssetManager manager) {
        if (manager == null)
            return addToLoad(tex, texParams);

        if (tex == null)
            return null;

        tex = GlobalResources.unpackAssetPath(tex);
        logger.info(I18n.msg("notif.loading", tex));
        manager.load(tex, VolumeTexture.class, texParams);

        return tex;
    }

    /**
     * Adds the 3D texture to load and unpacks any star (*) with the current
     * quality setting.
     *
     * @param tex The 3D texture file to load.
     *
     * @return The actual loaded 3D texture path
     */
    private String addToLoad(String tex,
                             VolumeTextureLoader.VolumeTextureParameter texParams) {
        if (tex == null)
            return null;

        tex = GlobalResources.unpackAssetPath(tex);
        logger.info(I18n.msg("notif.loading", tex));
        AssetBean.addAsset(tex, VolumeTexture.class, texParams);

        return tex;
    }

    public void initMaterial(AssetManager manager,
                             IntModelInstance instance,
                             float[] diffuseCol,
                             boolean culling) {
        if (instance != null) {
            initMaterial(manager, instance.materials.get(0), instance.materials.size > 1 ? instance.materials.get(1) : null, diffuseCol, culling);
        }
    }

    public void initMaterial(AssetManager manager,
                             Material mat,
                             Material ring,
                             float[] diffuseCol,
                             boolean culling) {
        sharedReflectionCubemap.initialize();
        this.material = mat;
        assert material != null;
        if (diffuse != null && material.get(TextureAttribute.Diffuse) == null) {
            if (!diffuse.endsWith(Constants.GEN_KEYWORD)) {
                Texture tex = manager.get(diffuseUnpacked, Texture.class);
                addDiffuseTex(tex);
            }
        }
        // Add diffuse color only when we have no diffuse texture!
        if (diffuseCol != null && (diffuseUnpacked == null || colorIfTexture)) {
            diffuseColor = new float[4];
            diffuseColor[0] = diffuseCol[0];
            diffuseColor[1] = diffuseCol[1];
            diffuseColor[2] = diffuseCol[2];
            diffuseColor[3] = diffuseCol[3];
            material.set(new ColorAttribute(ColorAttribute.Diffuse, diffuseColor[0], diffuseColor[1], diffuseColor[2], diffuseColor[3]));
        }

        if (normal != null && material.get(TextureAttribute.Normal) == null) {
            if (!normal.endsWith(Constants.GEN_KEYWORD)) {
                Texture tex = manager.get(normalUnpacked, Texture.class);
                addNormalTex(tex);
            }
        }
        if (specular != null && material.get(TextureAttribute.Specular) == null) {
            if (!specular.endsWith(Constants.GEN_KEYWORD)) {
                Texture tex = manager.get(specularUnpacked, Texture.class);
                addSpecularTex(tex);
            }
        }
        if (specularColor != null) {
            material.set(new ColorAttribute(ColorAttribute.Specular, specularColor[0], specularColor[1], specularColor[2], 1f));
        }
        if (emissive != null && material.get(TextureAttribute.Emissive) == null) {
            if (!emissive.endsWith(Constants.GEN_KEYWORD)) {
                Texture tex = manager.get(emissiveUnpacked, Texture.class);
                material.set(new TextureAttribute(TextureAttribute.Emissive, tex));
            }
        }
        if (emissiveColor != null) {
            material.set(new ColorAttribute(ColorAttribute.Emissive, emissiveColor[0], emissiveColor[1], emissiveColor[2], 1f));
        }
        if (diffuseScatteringColor != null) {
            material.set(new ColorAttribute(ColorAttribute.DiffuseScattering,
                                            diffuseScatteringColor[0],
                                            diffuseScatteringColor[1],
                                            diffuseScatteringColor[2],
                                            1f));
        }
        if (height != null && material.get(TextureAttribute.Height) == null) {
            if (!height.endsWith(Constants.GEN_KEYWORD)) {
                Texture tex = manager.get(heightUnpacked, Texture.class);
                initializeElevationData(tex);
            } else {
                initializeGenElevationData();
            }
        }
        if (ring != null) {
            // Ring material
            ringMaterial = ring;
            if (ringMaterial.get(TextureAttribute.Diffuse) == null && ringUnpacked != null) {
                ringMaterial.set(new TextureAttribute(TextureAttribute.Diffuse, manager.get(ringUnpacked, Texture.class)));
            }
            if (ringnormal != null && ringMaterial.get(TextureAttribute.Normal) == null && ringnormalUnpacked != null) {
                ringMaterial.set(new TextureAttribute(TextureAttribute.Normal, manager.get(ringnormalUnpacked, Texture.class)));
            }
            if (ringDiffuseScatteringColor != null) {
                ringMaterial.set(
                        new ColorAttribute(ColorAttribute.DiffuseScattering,
                                           ringDiffuseScatteringColor[0],
                                           ringDiffuseScatteringColor[1],
                                           ringDiffuseScatteringColor[2],
                                           1f));
            }
            // Thin surface
            ringMaterial.set(new FlagAttribute(FlagAttribute.ThinSurface, true));
            // Alpha blending for ring.
            ringMaterial.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA));
            if (!culling)
                ringMaterial.set(new IntAttribute(IntAttribute.CullFace, GL20.GL_NONE));
        }
        if (!culling) {
            material.set(new IntAttribute(IntAttribute.CullFace, GL20.GL_NONE));
        }
        // Add reflection cubemap if SSR is off and this material has metallic attributes
        if (metallic != null || metallicColor != null) {
            sharedReflectionCubemap.prepareCubemap(manager);
            material.set(new CubemapAttribute(CubemapAttribute.ReflectionCubemap, sharedReflectionCubemap.cubemap));
        }
        if (metallic != null && !metallic.endsWith(Constants.GEN_KEYWORD)) {
            if (material.get(TextureAttribute.Metallic) == null) {
                Texture tex = manager.get(metallicUnpacked, Texture.class);
                material.set(new TextureAttribute(TextureAttribute.Metallic, tex));
            }
        }
        if (metallicColor != null) {
            // Reflective color
            material.set(new ColorAttribute(ColorAttribute.Metallic, metallicColor[0], metallicColor[1], metallicColor[2], 1f));
        }
        if (roughness != null && material.get(TextureAttribute.Roughness) == null) {
            if (!roughness.endsWith(Constants.GEN_KEYWORD)) {
                Texture tex = manager.get(roughnessUnapcked, Texture.class);
                material.set(new TextureAttribute(TextureAttribute.Roughness, tex));
            }
        }
        if (Float.isFinite(roughnessColor)) {
            // Shininess is the opposite of roughness
            material.set(new FloatAttribute(FloatAttribute.Shininess, 1f - roughnessColor));
        }
        if (ao != null && material.get(TextureAttribute.AO) == null) {
            if (!ao.endsWith(Constants.GEN_KEYWORD)) {
                aoTexture = manager.get(aoUnpacked, Texture.class);
                material.set(new TextureAttribute(TextureAttribute.AO, aoTexture));
            }
        }
        if (occlusionMetallicRoughness != null && material.get(TextureAttribute.AO) == null) {
            if (!occlusionMetallicRoughness.endsWith(Constants.GEN_KEYWORD)) {
                Texture tex = manager.get(occlusionMetallicRoughnessUnpacked, Texture.class);
                material.set(new TextureAttribute(TextureAttribute.OcclusionMetallicRoughness, tex));
            }
        }
        if (texture0 != null && material.get(TextureAttribute.Texture0) == null) {
            Texture tex = manager.get(texture0Unpacked, Texture.class);
            addTexture0(tex);
        }
        if (texture1 != null && material.get(TextureAttribute.Texture1) == null) {
            Texture tex = manager.get(texture1Unpacked, Texture.class);
            addTexture1(tex);
        }

        // 3D textures; volumes.
        if (volume0 != null && material.get(Texture3DAttribute.Volume0) == null) {
            VolumeTexture tex = manager.get(volume0Unpacked, VolumeTexture.class);
            if (tex != null && material != null) {
                material.set(new Texture3DAttribute(Texture3DAttribute.Volume0, tex.texture()));
                material.set(new Vector3Attribute(Vector3Attribute.Volume0BoundsMin, tex.boundsMin()));
                material.set(new Vector3Attribute(Vector3Attribute.Volume0BoundsMax, tex.boundsMax()));
            }
        }
        if (volume1 != null) {
            assert material != null;
            if (material.get(Texture3DAttribute.Volume1) == null) {
                VolumeTexture tex = manager.get(volume1Unpacked, VolumeTexture.class);
                if (tex != null && material != null) {
                    material.set(new Texture3DAttribute(Texture3DAttribute.Volume1, tex.texture()));
                    material.set(new Vector3Attribute(Vector3Attribute.Volume1BoundsMin, tex.boundsMin()));
                    material.set(new Vector3Attribute(Vector3Attribute.Volume1BoundsMax, tex.boundsMax()));
                }
            }
        }
        if (volume2 != null) {
            assert material != null;
            if (material.get(Texture3DAttribute.Volume2) == null) {
                VolumeTexture tex = manager.get(volume2Unpacked, VolumeTexture.class);
                if (tex != null && material != null) {
                    material.set(new Texture3DAttribute(Texture3DAttribute.Volume2, tex.texture()));
                    material.set(new Vector3Attribute(Vector3Attribute.Volume2BoundsMin, tex.boundsMin()));
                    material.set(new Vector3Attribute(Vector3Attribute.Volume2BoundsMax, tex.boundsMax()));
                }
            }
        }
        if (volume3 != null) {
            assert material != null;
            if (material.get(Texture3DAttribute.Volume3) == null) {
                VolumeTexture tex = manager.get(volume3Unpacked, VolumeTexture.class);
                if (tex != null && material != null) {
                    material.set(new Texture3DAttribute(Texture3DAttribute.Volume3, tex.texture()));
                    material.set(new Vector3Attribute(Vector3Attribute.Volume3BoundsMin, tex.boundsMin()));
                    material.set(new Vector3Attribute(Vector3Attribute.Volume3BoundsMax, tex.boundsMax()));
                }
            }
        }

        // Cubemaps.
        if (diffuseCubemap != null) {
            diffuseCubemap.prepareCubemap(manager);
            material.set(new CubemapAttribute(CubemapAttribute.DiffuseCubemap, diffuseCubemap.cubemap));
        }
        if (normalCubemap != null) {
            normalCubemap.prepareCubemap(manager);
            material.set(new CubemapAttribute(CubemapAttribute.NormalCubemap, normalCubemap.cubemap));
        }
        if (emissiveCubemap != null) {
            emissiveCubemap.prepareCubemap(manager);
            material.set(new CubemapAttribute(CubemapAttribute.EmissiveCubemap, emissiveCubemap.cubemap));
        }
        if (specularCubemap != null) {
            specularCubemap.prepareCubemap(manager);
            material.set(new CubemapAttribute(CubemapAttribute.SpecularCubemap, specularCubemap.cubemap));
        }
        if (roughnessCubemap != null) {
            roughnessCubemap.prepareCubemap(manager);
            material.set(new CubemapAttribute(CubemapAttribute.RoughnessCubemap, roughnessCubemap.cubemap));
        }
        if (metallicCubemap != null) {
            metallicCubemap.prepareCubemap(manager);
            material.set(new CubemapAttribute(CubemapAttribute.MetallicCubemap, metallicCubemap.cubemap));
        }
        if (heightCubemap != null) {
            heightCubemap.prepareCubemap(manager);
            material.set(new CubemapAttribute(CubemapAttribute.HeightCubemap, heightCubemap.cubemap));
        }
        if (aoCubemap != null) {
            aoCubemap.prepareCubemap(manager);
            // Only add occlusion clouds if clouds are actually visible.
            if (!occlusionClouds || GaiaSky.instance.sceneRenderer.visible.get(ComponentTypes.ComponentType.Clouds.ordinal())) {
                material.set(new CubemapAttribute(CubemapAttribute.AmbientOcclusionCubemap, aoCubemap.cubemap));
            }
        }
        // Ambient occlusion represents clouds.
        if (occlusionClouds && GaiaSky.instance.sceneRenderer.visible.get(ComponentTypes.ComponentType.Clouds.ordinal())) {
            material.set(new OcclusionCloudsAttribute(true));
        }

        // Sparse Virtual Textures.
        int svtId = 0;
        if (diffuseSvt != null || normalSvt != null || emissiveSvt != null || specularSvt != null || heightSvt != null || metallicSvt != null || roughnessSvt != null || aoSvt != null) {
            svtId = SVTManager.nextSvtId();
        }
        // Add attributes.
        if (diffuseSvt != null) {
            addSVTAttributes(material, diffuseSvt, svtId);
        }
        if (normalSvt != null) {
            addSVTAttributes(material, normalSvt, svtId);
        }
        if (emissiveSvt != null) {
            addSVTAttributes(material, emissiveSvt, svtId);
        }
        if (specularSvt != null) {
            addSVTAttributes(material, specularSvt, svtId);
        }
        if (heightSvt != null) {
            addSVTAttributes(material, heightSvt, svtId);
            initializeElevationData(heightSvt, manager);
        }
        if (metallicSvt != null) {
            addSVTAttributes(material, metallicSvt, svtId);
        }
        if (roughnessSvt != null) {
            addSVTAttributes(material, roughnessSvt, svtId);
        }
        if (aoSvt != null) {
            addSVTAttributes(material, aoSvt, svtId);
        }
        if (svtId > 0) {
            // Broadcast this material for SVT manager.
            EventManager.publish(Event.SVT_MATERIAL_INFO, this, svtId, this);
        }

    }

    private void addSVTAttributes(Material material,
                                  VirtualTextureComponent svt) {
        addSVTAttributes(material, svt, svt.id);
    }

    private void addSVTAttributes(Material material,
                                  VirtualTextureComponent svt,
                                  int id) {
        svt.doneLoading(null);
        // Set ID.
        svt.id = id;
        // Set attributes.
        material.set(new FloatAttribute(FloatAttribute.SvtTileSize, svt.tileSize));
        material.set(new FloatAttribute(FloatAttribute.SvtId, svt.id));
        material.set(new FloatAttribute(FloatAttribute.SvtDetectionFactor,
                                        (float) GaiaSky.settings().scene.renderer.virtualTextures.detectionBufferFactor));
        // Only update depth and resolution if it does not exist, or if it exists and its value is less than ours.
        if (!material.has(FloatAttribute.SvtDepth) || ((FloatAttribute) Objects.requireNonNull(material.get(FloatAttribute.SvtDepth))).value < svt.tree.depth) {
            // Depth.
            material.set(new FloatAttribute(FloatAttribute.SvtDepth, svt.tree.depth));
            // Resolution.
            double svtResolution = svt.tileSize * FastMath.pow(2.0, svt.tree.depth);
            material.set(new Vector2Attribute(Vector2Attribute.SvtResolution,
                                              new Vector2((float) (svtResolution * svt.tree.root.length), (float) svtResolution)));
        }

        // Add to list.
        svts.add(svt);
    }

    private void addHeightTex(Texture heightTex) {
        if (heightTex != null && material != null) {
            heightSize.set(heightTex.getWidth(), heightTex.getHeight());
            material.set(new TextureAttribute(TextureAttribute.Height, heightTex));
            material.set(new FloatAttribute(FloatAttribute.HeightScale, heightScale));
            material.set(new FloatAttribute(FloatAttribute.ElevationMultiplier, (float) GaiaSky.settings().scene.renderer.elevation.multiplier));
            material.set(new Vector2Attribute(Vector2Attribute.HeightSize, heightSize));
            material.set(new FloatAttribute(FloatAttribute.TessQuality, (float) GaiaSky.settings().scene.renderer.elevation.quality));
        }
    }

    private void addDiffuseTex(Texture diffuseTex) {
        if (diffuseTex != null && material != null) {
            material.set(new TextureAttribute(TextureAttribute.Diffuse, diffuseTex));
            if (!colorIfTexture) {
                material.remove(ColorAttribute.Diffuse);
            }
        }
    }

    private void addSpecularTex(Texture specularTex) {
        if (specularTex != null && material != null) {
            material.set(new TextureAttribute(TextureAttribute.Specular, specularTex));
            if (!colorIfTexture) {
                material.remove(ColorAttribute.Specular);
            }
        }
    }

    private void addNormalTex(Texture normalTex) {
        if (normalTex != null && material != null) {
            material.set(new TextureAttribute(TextureAttribute.Normal, normalTex));
        }
    }

    private void addEmissiveTex(Texture emissiveTex) {
        if (emissiveTex != null && material != null) {
            material.set(new TextureAttribute(TextureAttribute.Emissive, emissiveTex));
            if (!colorIfTexture) {
                material.remove(ColorAttribute.Emissive);
            }
        }
    }

    private void addRoughnessTex(Texture roughnessTex) {
        if (roughnessTex != null && material != null) {
            material.set(new TextureAttribute(TextureAttribute.Roughness, roughnessTex));
        }
    }

    private void addTexture0(Texture tex) {
        if (tex != null && material != null) {
            material.set(new TextureAttribute(TextureAttribute.Texture0, tex));
        }
    }

    private void addTexture1(Texture tex) {
        if (tex != null && material != null) {
            material.set(new TextureAttribute(TextureAttribute.Texture1, tex));
        }
    }

    public void setGenerated(boolean generated) {
        this.heightGenerated.set(generated);
    }

    private synchronized void initializeGenElevationData() {
        if (GaiaSky.settings().scene.renderer.elevation.shaderMethod) {
            shaderBasedGeneration();
        } else {
            textureBasedGenerationMerged();
        }
    }

    private void shaderBasedGeneration() {
        if (!heightGenerated.get()) {
            heightGenerated.set(true);
            var t = GaiaSky.settings().data.dataFileHandle("$data/default-data/tex/noise/Craters/Craters 14 - 512x512.png");
            if (t.exists()) {
                heightTex = new Texture(t);
                addHeightTex(heightTex);
            }
        }
    }

    private void textureBasedGenerationMerged() {
        if (heightGenerated.get()) {
            addHeightTex(heightTex);
        } else {
            heightGenerated.set(true);
            GaiaSky.postRunnable(() -> {
                // 1ST FRAME - CREATE NOISE.
                int N = GaiaSky.settings().graphics.proceduralGenerationResolution[0];
                int M = GaiaSky.settings().graphics.proceduralGenerationResolution[1];
                logger.info(I18n.msg("gui.procedural.info.generate",
                                     I18n.msg("gui.procedural.surface"),
                                     Integer.toString(N),
                                     Integer.toString(M)));

                Random rand = new Random();
                if (nc == null) {
                    nc = new NoiseComponent();
                    switch (rand.nextInt(12)) {
                        case 0, 1, 2, 3, 4 -> nc.randomizeEarthLike(rand);
                        case 5 -> nc.randomizeRockyPlanet(rand);
                        case 6 -> nc.randomizeGasGiant(rand);
                        case 7, 8 -> nc.randomizeSnowPlanet(rand);
                        case 9, 10 -> nc.randomizeTropical(rand);
                        case 11 -> nc.randomizeForTerrain(rand);
                    }
                }

                GaiaSky.postRunnable(() -> {
                    // 2ND FRAME - GENERATE.
                    FrameBuffer fbPlanet = nc.generateProceduralSurface(N,
                                                                        M,
                                                                        lut,
                                                                        lutHueShift,
                                                                        lutSaturation,
                                                                        3,
                                                                        GaiaSky.settings().scene.renderer.elevation.type.isNone());

                    GaiaSky.postRunnable(() -> {
                        // 3TH FRAME - ADD TEXTURES TO MATERIAL.
                        int nAttachments = fbPlanet.getTextureAttachments().size;

                        Texture heightT = fbPlanet.getColorBufferTexture();
                        Texture diffuseT = fbPlanet.getTextureAttachments().get(1);
                        Texture specularT = fbPlanet.getTextureAttachments().get(2);
                        Texture emissiveT = fbPlanet.getTextureAttachments().get(3);
                        Texture normalT = nAttachments > 4 ? fbPlanet.getTextureAttachments().get(4) : null;

                        boolean cDiffuse = diffuse != null && diffuse.endsWith(Constants.GEN_KEYWORD);
                        boolean cSpecular = specular != null && specular.endsWith(Constants.GEN_KEYWORD);
                        boolean cNormal = normal != null && normal.endsWith(Constants.GEN_KEYWORD);
                        boolean cEmissive = emissive != null && emissive.endsWith(Constants.GEN_KEYWORD);
                        // TODO implement metallic texture generation
                        //boolean cMetallic = metallic != null && metallic.endsWith(Constants.GEN_KEYWORD);

                        // BIOME: HEIGHT and MOISTURE.
                        if (heightT != null) {
                            if (heightTex != null) {
                                heightTex.dispose();
                            }
                            // Create texture, populate material
                            if (!GaiaSky.settings().scene.renderer.elevation.type.isNone()) {
                                heightData = new HeightDataPixmap(heightT, null);
                                heightTex = heightT;
                                addHeightTex(heightTex);
                            }
                        }

                        // DIFFUSE.
                        if (cDiffuse) {
                            if (diffuseTex != null) {
                                diffuseTex.dispose();
                            }
                            if (diffuseT != null) {
                                diffuseTex = diffuseT;
                                addDiffuseTex(diffuseTex);
                            }
                        }

                        // SPECULAR.
                        if (cSpecular) {
                            if (specularTex != null) {
                                specularTex.dispose();
                            }
                            if (specularT != null) {
                                specularTex = specularT;
                                addSpecularTex(specularTex);
                            }
                        }

                        // NORMAL.
                        if (cNormal) {
                            if (normalTex != null) {
                                normalTex.dispose();
                            }
                            if (normalT != null) {
                                normalTex = normalT;
                                addNormalTex(normalTex);
                            }
                        }

                        // EMISSIVE.
                        if (cEmissive) {
                            if (emissiveTex != null) {
                                emissiveTex.dispose();
                            }
                            if (emissiveT != null) {
                                emissiveTex = emissiveT;
                                addEmissiveTex(emissiveTex);
                            }
                        }

                        // Save textures to disk as image files.
                        if (GaiaSky.settings().program.saveProceduralTextures) {
                            SysUtils.saveProceduralGLTextures(new Texture[]{
                                                                      heightT,
                                                                      diffuseT,
                                                                      specularT,
                                                                      normalT,
                                                                      emissiveT},
                                                              new String[]{
                                                                      name + "-biome",
                                                                      name + "-diffuse",
                                                                      name + "-specular",
                                                                      name + "-normal",
                                                                      name + "-emissive"},
                                                              Settings.ImageFormat.JPG);
                        }
                    });
                });
            });

        }
    }

    private void initializeElevationData(VirtualTextureComponent svt,
                                         AssetManager manager) {
        if (!heightInitialized.get()) {
            heightInitialized.set(true);
            GaiaSky.instance.getExecutorService().execute(() -> {
                // Construct RAM height map from texture
                heightData = new HeightDataSVT(svt.tree, manager);
            });
        }
    }

    private void initializeElevationData(Texture tex) {
        if (!heightInitialized.get()) {
            heightInitialized.set(true);
            GaiaSky.instance.getExecutorService().execute(() -> {
                // Construct RAM height map from texture
                heightData = new HeightDataPixmap(tex, () -> addHeightTex(tex));
            });
        }
    }

    @Override
    public Material getMaterial() {
        return material;
    }

    public Material getRingMaterial() {
        return ringMaterial;
    }

    private void removeElevationData() {
        heightData = null;
        material.remove(TextureAttribute.Height);
        material.remove(FloatAttribute.HeightScale);
        material.remove(FloatAttribute.ElevationMultiplier);
        material.remove(Vector2Attribute.HeightSize);
        material.remove(FloatAttribute.HeightNoiseSize);
        material.remove(FloatAttribute.TessQuality);

        if (normalTex != null && material != null) {
            if (material.get(TextureAttribute.Normal) == null) {
                addNormalTex(normalTex);
            }
        }
    }

    /**
     * @deprecated use {@link MaterialComponent#setDiffuse(String)} instead
     */
    @Deprecated
    public void setBase(String diffuse) {
        this.setDiffuse(diffuse);
    }

    public void setDiffuse(String diffuse) {
        this.diffuse = GaiaSky.settings().data.dataFile(diffuse);
    }

    public void setSpecular(String specular) {
        this.specular = GaiaSky.settings().data.dataFile(specular);
    }

    public void setSpecularValue(Double specular) {
        float r = specular.floatValue();
        this.specularColor = new float[]{r, r, r};
    }

    public void setSpecular(Double specular) {
        this.setSpecularValue(specular);
    }

    public void setSpecularValues(double[] specular) {
        if (specular.length > 1) {
            this.specularColor = new float[]{(float) specular[0], (float) specular[1], (float) specular[2]};
        } else {
            float r = (float) specular[0];
            this.specularColor = new float[]{r, r, r};
        }
    }

    public void setSpecular(double[] specular) {
        this.setSpecularValues(specular);
    }

    public void setNormal(String normal) {
        this.normal = GaiaSky.settings().data.dataFile(normal);
    }

    /**
     * @deprecated use {@link MaterialComponent#setEmissive(String)} instead
     */
    @Deprecated
    public void setNight(String emissive) {
        this.setEmissive(emissive);
    }

    public void setEmissive(String emissive) {
        this.emissive = GaiaSky.settings().data.dataFile(emissive);
    }

    public void setEmissive(Double emissive) {
        float r = emissive.floatValue();
        this.emissiveColor = new float[]{r, r, r};
    }

    public void setEmissive(double[] emissive) {
        if (emissive.length > 1) {
            this.emissiveColor = new float[]{(float) emissive[0], (float) emissive[1], (float) emissive[2]};
        } else {
            float r = (float) emissive[0];
            this.emissiveColor = new float[]{r, r, r};
        }
    }

    public void setDiffuseScattering(Double diffuseScattering) {
        float r = diffuseScattering.floatValue();
        this.diffuseScatteringColor = new float[]{r, r, r};
    }

    public void setDiffuseScattering(double[] diffuseScattering) {
        if (diffuseScattering.length > 1) {
            this.diffuseScatteringColor = new float[]{(float) diffuseScattering[0], (float) diffuseScattering[1], (float) diffuseScattering[2]};
        } else {
            float r = (float) diffuseScattering[0];
            this.diffuseScatteringColor = new float[]{r, r, r};
        }
    }

    public void setRing(String ring) {
        setRingDiffuse(ring);
    }

    public void setRingDiffuse(String ringDiffuse) {
        this.ring = GaiaSky.settings().data.dataFile(ringDiffuse);
    }

    public void setRingnormal(String ringNormal) {
        setRingNormal(ringNormal);
    }

    public void setRingNormal(String ringNormal) {
        this.ringnormal = GaiaSky.settings().data.dataFile(ringNormal);
    }

    public void setRingDiffuseScattering(Double ringDiffuseScattering) {
        float r = ringDiffuseScattering.floatValue();
        this.ringDiffuseScatteringColor = new float[]{r, r, r};
    }

    public void setRingDiffuseScattering(double[] ringDiffuseScattering) {
        if (ringDiffuseScattering.length > 1) {
            this.ringDiffuseScatteringColor = new float[]{(float) ringDiffuseScattering[0], (float) ringDiffuseScattering[1], (float) ringDiffuseScattering[2]};
        } else {
            float r = (float) ringDiffuseScattering[0];
            this.ringDiffuseScatteringColor = new float[]{r, r, r};
        }
    }

    public void setHeight(String height) {
        this.height = GaiaSky.settings().data.dataFile(height);
    }

    public void setHeightScaleKm(Double heightScale) {
        this.heightScale = (float) (heightScale * Constants.KM_TO_U);
    }

    public void setHeightScaleM(Double heightScale) {
        this.heightScale = (float) (heightScale * Constants.M_TO_U);
    }

    public void setHeightScale(Double heightScale) {
        setHeightScaleKm(heightScale);
    }

    public void setHeightMapTopKm(Double heightMapTopKm) {
        setHeightScaleKm(heightMapTopKm);
    }

    public void setHeightMapTopM(Double heightMapTopM) {
        setHeightScaleM(heightMapTopM);
    }

    public void setColorIfTexture(Boolean colorIfTexture) {
        this.colorIfTexture = colorIfTexture;
    }

    public void setNoise(NoiseComponent noise) {
        this.nc = noise;
    }

    public void setLUT(String biomeLookupTex) {
        this.lut = biomeLookupTex;
    }

    public void setBiomelut(String biomeLookupTex) {
        this.setLUT(biomeLookupTex);
    }

    public void setBiomeLUT(String biomeLookupTex) {
        this.setLUT(biomeLookupTex);
    }

    public void setLUTHueShift(Double hueShift) {
        this.lutHueShift = hueShift.floatValue();
    }

    public void setBiomehueshift(Double hueShift) {
        this.setLUTHueShift(hueShift);
    }

    public void setBiomeHueShift(Double hueShift) {
        this.setLUTHueShift(hueShift);
    }

    public void setLUTSaturation(Double saturation) {
        this.lutSaturation = saturation.floatValue();
    }

    public void setBiomeSaturation(Double saturation) {
        setLUTSaturation(saturation);
    }

    /**
     * @deprecated use {@link MaterialComponent#setMetallic(String)} instead
     */
    @Deprecated
    public void setReflection(Double metallicColor) {
        this.setMetallic(metallicColor);
    }

    public void setMetallic(Double metallicColor) {
        float r = metallicColor.floatValue();
        this.metallicColor = new float[]{r, r, r};
    }

    public void setMetallic(String metallic) {
        this.metallic = GaiaSky.settings().data.dataFile(metallic);
    }

    public void setReflection(double[] metallic) {
        if (metallic.length > 1) {
            this.metallicColor = new float[]{(float) metallic[0], (float) metallic[1], (float) metallic[2]};
        } else {
            float r = (float) metallic[0];
            this.metallicColor = new float[]{r, r, r};
        }
    }

    public void setRoughness(String roughness) {
        this.roughness = GaiaSky.settings().data.dataFile(roughness);
    }

    public void setRoughness(Double roughness) {
        this.roughnessColor = roughness.floatValue();
    }

    public void setAo(String ao) {
        this.ao = GaiaSky.settings().data.dataFile(ao);
    }

    public void setOcclusionMetallicRoughness(String texture) {
        this.occlusionMetallicRoughness = GaiaSky.settings().data.dataFile(texture);
    }

    public void setTexture0(String texture0) {
        this.texture0 = GaiaSky.settings().data.dataFile(texture0);
    }

    public void setTexture1(String texture1) {
        this.texture1 = GaiaSky.settings().data.dataFile(texture1);
    }

    public void setVolume0(String tex) {
        this.volume0 = GaiaSky.settings().data.dataFile(tex);
    }

    public void setVolume1(String tex) {
        this.volume1 = GaiaSky.settings().data.dataFile(tex);
    }

    public void setVolume2(String tex) {
        this.volume2 = GaiaSky.settings().data.dataFile(tex);
    }

    public void setVolume3(String tex) {
        this.volume3 = GaiaSky.settings().data.dataFile(tex);
    }

    public void setDiffuseCubemap(String cubemap) {
        this.diffuseCubemap = new CubemapComponent();
        this.diffuseCubemap.setLocation(cubemap);
    }

    public void setNormalCubemap(String cubemap) {
        this.normalCubemap = new CubemapComponent();
        this.normalCubemap.setLocation(cubemap);
    }

    public void setSpecularCubemap(String cubemap) {
        this.specularCubemap = new CubemapComponent();
        this.specularCubemap.setLocation(cubemap);
    }

    public void setEmissiveCubemap(String cubemap) {
        this.emissiveCubemap = new CubemapComponent();
        this.emissiveCubemap.setLocation(cubemap);
    }

    public void setNightCubemap(String cubemap) {
        this.setEmissiveCubemap(cubemap);
    }

    public void setHeightCubemap(String cubemap) {
        this.heightCubemap = new CubemapComponent();
        this.heightCubemap.setLocation(cubemap);
    }

    public void setMetallicCubemap(String cubemap) {
        this.metallicCubemap = new CubemapComponent();
        this.metallicCubemap.setLocation(cubemap);
    }

    public void setRoughnessCubemap(String cubemap) {
        this.roughnessCubemap = new CubemapComponent();
        this.roughnessCubemap.setLocation(cubemap);
    }

    public void setReflectionCubemap(String reflectionCubemap) {
        MaterialComponent.sharedReflectionCubemap.setLocation(reflectionCubemap);
        MaterialComponent.sharedReflectionCubemap.reset();
    }

    public void setAmbientOcclusionCubemap(String cubemap) {
        this.aoCubemap = new CubemapComponent();
        this.aoCubemap.setLocation(cubemap);
    }

    public void setOcclusionClouds(boolean state) {
        this.occlusionClouds = state;
    }


    public void setAmbientOcclusionCubemap(CubemapComponent cubemap) {
        this.aoCubemap = cubemap;
    }

    public void setSkybox(String diffuseCubemap) {
        setDiffuseCubemap(diffuseCubemap);
    }

    public void setDiffuseSVT(VirtualTextureComponent virtualTextureComponent) {
        if (this.diffuseSvt != null && !this.diffuseSvt.location.equals(virtualTextureComponent.location)) {
            logger.warn("Overwriting diffuse SVT: " + this.diffuseSvt.location + " -> " + virtualTextureComponent.location);
        }
        this.diffuseSvt = virtualTextureComponent;
    }

    public void setDiffuseSVT(Map<Object, Object> virtualTexture) {
        setDiffuseSVT(convertToComponent(virtualTexture));
    }

    public void setSpecularSVT(VirtualTextureComponent virtualTextureComponent) {
        if (this.specularSvt != null && !this.specularSvt.location.equals(virtualTextureComponent.location)) {
            logger.warn("Overwriting specular SVT: " + this.specularSvt.location + " -> " + virtualTextureComponent.location);
        }
        this.specularSvt = virtualTextureComponent;
    }

    public void setSpecularSVT(Map<Object, Object> virtualTexture) {
        setSpecularSVT(convertToComponent(virtualTexture));
    }

    public void setNormalSVT(VirtualTextureComponent virtualTextureComponent) {
        this.normalSvt = virtualTextureComponent;
    }

    public void setNormalSVT(Map<Object, Object> virtualTexture) {
        setNormalSVT(convertToComponent(virtualTexture));
    }

    public void setHeightSVT(VirtualTextureComponent virtualTextureComponent) {
        this.heightSvt = virtualTextureComponent;
    }

    public void setHeightSVT(Map<Object, Object> virtualTexture) {
        setHeightSVT(convertToComponent(virtualTexture));
    }

    public void setEmissiveSVT(VirtualTextureComponent virtualTextureComponent) {
        this.emissiveSvt = virtualTextureComponent;
    }

    public void setEmissiveSVT(Map<Object, Object> virtualTexture) {
        setEmissiveSVT(convertToComponent(virtualTexture));
    }

    public void setMetallicSVT(VirtualTextureComponent virtualTextureComponent) {
        this.metallicSvt = virtualTextureComponent;
    }

    public void setMetallicSVT(Map<Object, Object> virtualTexture) {
        setMetallicSVT(convertToComponent(virtualTexture));
    }

    public void setRoughnessSVT(VirtualTextureComponent virtualTextureComponent) {
        this.roughnessSvt = virtualTextureComponent;
    }

    public void setRoughnessSVT(Map<Object, Object> virtualTexture) {
        setRoughnessSVT(convertToComponent(virtualTexture));
    }

    public void setAoSVT(VirtualTextureComponent virtualTextureComponent) {
        this.aoSvt = virtualTextureComponent;
    }

    public void setAoSVT(Map<Object, Object> virtualTexture) {
        setAoSVT(convertToComponent(virtualTexture));
    }

    public static VirtualTextureComponent convertToComponent(Map<Object, Object> map) {
        var vt = new VirtualTextureComponent();
        if (map.containsKey("location"))
            vt.setLocation((String) map.get("location"));
        if (map.containsKey("tileSize"))
            vt.setTileSize((Long) map.get("tileSize"));
        return vt;
    }

    public boolean hasHeight() {
        return this.height != null && !this.height.isEmpty();
    }

    public void disposeTexture(AssetManager manager,
                               Material material,
                               String name,
                               String nameUnpacked,
                               int attributeIndex,
                               Texture tex) {
        if (name != null && manager != null && manager.isLoaded(nameUnpacked)) {
            unload(material, attributeIndex);
            manager.unload(nameUnpacked);
        }
        if (tex != null) {
            unload(material, attributeIndex);
            tex.dispose();
        }
    }

    public void disposeCubemap(AssetManager manager,
                               Material mat,
                               int attributeIndex,
                               CubemapComponent cubemap) {
        if (cubemap != null && cubemap.isLoaded(manager)) {
            unload(mat, attributeIndex);
            manager.unload(cubemap.cmBack);
            manager.unload(cubemap.cmFront);
            manager.unload(cubemap.cmUp);
            manager.unload(cubemap.cmDown);
            manager.unload(cubemap.cmRight);
            manager.unload(cubemap.cmLeft);
            cubemap.dispose();
        }
    }

    /**
     * Disposes and unloads all currently loaded textures immediately
     *
     * @param manager The asset manager
     **/
    public void disposeTextures(AssetManager manager) {
        disposeTexture(manager, material, diffuse, diffuseUnpacked, TextureAttribute.Diffuse, diffuseTex);
        disposeTexture(manager, material, normal, normalUnpacked, TextureAttribute.Normal, normalTex);
        disposeTexture(manager, material, specular, specularUnpacked, TextureAttribute.Specular, specularTex);
        disposeTexture(manager, material, emissive, emissiveUnpacked, TextureAttribute.Emissive, null);
        disposeTexture(manager, ringMaterial, ring, ringUnpacked, TextureAttribute.Diffuse, null);
        disposeTexture(manager, ringMaterial, ringnormal, ringnormalUnpacked, TextureAttribute.Normal, null);
        disposeTexture(manager, material, height, heightUnpacked, TextureAttribute.Height, heightTex);
        disposeTexture(manager, material, metallic, metallicUnpacked, TextureAttribute.Metallic, null);
        disposeTexture(manager, material, roughness, roughnessUnapcked, TextureAttribute.Roughness, null);
        disposeTexture(manager, material, ao, aoUnpacked, TextureAttribute.AO, null);
        disposeTexture(manager, material, texture0, texture0Unpacked, TextureAttribute.Texture0, null);
        disposeTexture(manager, material, texture1, texture1Unpacked, TextureAttribute.Texture1, null);
        disposeCubemap(manager, material, CubemapAttribute.DiffuseCubemap, diffuseCubemap);
        disposeCubemap(manager, material, CubemapAttribute.NormalCubemap, normalCubemap);
        disposeCubemap(manager, material, CubemapAttribute.EmissiveCubemap, emissiveCubemap);
        disposeCubemap(manager, material, CubemapAttribute.SpecularCubemap, specularCubemap);
        disposeCubemap(manager, material, CubemapAttribute.RoughnessCubemap, roughnessCubemap);
        disposeCubemap(manager, material, CubemapAttribute.MetallicCubemap, metallicCubemap);
        disposeCubemap(manager, material, CubemapAttribute.HeightCubemap, heightCubemap);
        disposeCubemap(manager, material, CubemapAttribute.AmbientOcclusionCubemap, aoCubemap);
        disposeCubemap(manager, material, CubemapAttribute.ReflectionCubemap, sharedReflectionCubemap);
        texLoading = false;
        texInitialised = false;
    }

    public void disposeNoiseBuffers() {
        if (nc != null) {
            nc.dispose();
        }
    }

    private void unload(Material mat,
                        int attrIndex) {
        if (mat != null) {
            Attribute attr = mat.get(attrIndex);
            mat.remove(attrIndex);
            if (attr instanceof TextureAttribute) {
                Texture tex = ((TextureAttribute) attr).textureDescription.texture;
                tex.dispose();
            }
        }
    }

    @Override
    public void notify(Event event,
                       Object source,
                       Object... data) {
        switch (event) {
            case ELEVATION_TYPE_CMD -> {
                if (this.hasHeight() && this.material != null) {
                    ElevationType newType = (ElevationType) data[0];
                    GaiaSky.postRunnable(() -> {
                        if (newType.isNone()) {
                            removeElevationData();
                        } else {
                            if (height.endsWith(Constants.GEN_KEYWORD))
                                initializeGenElevationData();
                            else if (heightData == null) {
                                if (this.material.has(TextureAttribute.Height)) {
                                    initializeElevationData(
                                            ((TextureAttribute) Objects.requireNonNull(this.material.get(TextureAttribute.Height))).textureDescription.texture);
                                } else if (AssetBean.manager().isLoaded(heightUnpacked)) {
                                    if (!height.endsWith(Constants.GEN_KEYWORD)) {
                                        Texture tex = AssetBean.manager().get(heightUnpacked, Texture.class);
                                        if (!GaiaSky.settings().scene.renderer.elevation.type.isNone()) {
                                            initializeElevationData(tex);
                                        }
                                    } else {
                                        initializeGenElevationData();
                                    }
                                }
                            }
                        }
                    });
                }
            }
            case ELEVATION_MULTIPLIER_CMD -> {
                if (this.hasHeight() && this.material != null) {
                    float newMultiplier = (Float) data[0];
                    GaiaSky.postRunnable(() -> {
                        this.material.set(new FloatAttribute(FloatAttribute.ElevationMultiplier, newMultiplier));
                    });
                }
            }
            case TESSELLATION_QUALITY_CMD -> {
                if (this.hasHeight() && this.material != null) {
                    float newQuality = (Float) data[0];
                    GaiaSky.postRunnable(() -> this.material.set(new FloatAttribute(FloatAttribute.TessQuality, newQuality)));
                }
            }
            case TOGGLE_VISIBILITY_CMD -> {
                // Check clouds.
                if (occlusionClouds && getMaterial() != null) {
                    GaiaSky.postRunnable(() -> {
                        if (GaiaSky.instance.sceneRenderer.visible.get(ComponentTypes.ComponentType.Clouds.ordinal())) {
                            // Add occlusion clouds attributes.
                            if (aoTexture != null) {
                                material.set(new TextureAttribute(TextureAttribute.AO, aoTexture));
                            } else if (aoCubemap != null) {
                                getMaterial().set(new CubemapAttribute(CubemapAttribute.AmbientOcclusionCubemap, aoCubemap.cubemap));
                            } else if (aoSvt != null) {
                                material.set(new TextureAttribute(TextureAttribute.SvtIndirectionAmbientOcclusion, aoSvt.indirectionBuffer));
                            }
                            getMaterial().set(new OcclusionCloudsAttribute(true));
                        } else {
                            // Remove occlusion clouds attributes.
                            getMaterial().remove(OcclusionCloudsAttribute.Type);
                            getMaterial().remove(CubemapAttribute.AmbientOcclusionCubemap);
                            getMaterial().remove(TextureAttribute.AO);
                            getMaterial().remove(TextureAttribute.SvtIndirectionAmbientOcclusion);
                        }
                    });
                }

            }
            default -> {
            }
        }
    }

    public String getTexturesString() {
        StringBuilder sb = new StringBuilder();
        if (diffuse != null)
            sb.append(diffuse);
        if (normal != null)
            sb.append(",").append(normal);
        if (specular != null)
            sb.append(",").append(specular);
        if (emissive != null)
            sb.append(",").append(emissive);
        if (ring != null)
            sb.append(",").append(ring);
        if (ringnormal != null)
            sb.append(",").append(ringnormal);
        if (height != null)
            sb.append(",").append(height);
        if (texture0 != null)
            sb.append(",").append(texture0);
        if (texture1 != null)
            sb.append(",").append(texture1);
        return sb.toString();
    }

    @Override
    public String toString() {
        return diffuse;
    }

    public void copyFrom(MaterialComponent other) {
        this.height = other.height;
        this.diffuse = other.diffuse;
        this.normal = other.normal;
        this.specular = other.specular;
        this.emissive = other.emissive;
        this.metallic = other.metallic;
        this.roughness = other.roughness;
        this.lut = other.lut;
        this.lutHueShift = other.lutHueShift;
        this.lutSaturation = other.lutSaturation;
        this.heightScale = other.heightScale;
        this.texture0 = other.texture0;
        this.texture1 = other.texture1;
        this.nc = new NoiseComponent();
        if (other.nc != null) {
            this.nc.copyFrom(other.nc);
        } else {
            this.nc.randomizeForTerrain(new Random());
        }
    }

    public void randomizeAll(long seed) {
        initializeBiomeLUTManager();

        var rand = new Random(seed);
        setHeight("generate");
        setDiffuse("generate");
        setSpecular("generate");
        setNormal("generate");
        setEmissive("generate");

        // Biome LUT.
        setBiomeLUT(lutManager.getPresetNames().get(rand.nextInt(lutManager.getSize())));
        if (rand.nextFloat() < 0.25f) {
            // Actually roll the dice for hue shift.
            setBiomeHueShift(rand.nextDouble() * 360.0);
        } else {
            // No hue shift.
            setBiomeHueShift(0.0);
        }
        // Saturation.
        if (rand.nextInt(6) < 5) {
            setBiomeSaturation(rand.nextDouble(0.3, 0.7));
        } else {
            setBiomeSaturation(rand.nextDouble(0.0, 0.5));
        }
        // Height scale.
        setHeightScale(gaussian(rand, 30.0, 10.0, 1.0, 80.0));

        // Noise.
        if (nc != null) {
            nc.dispose();
        }
        NoiseComponent nc = new NoiseComponent();
        nc.randomizeForTerrain(rand);
        setNoise(nc);
    }

    private String randomBiomeLut(Random rand,
                                  String... names) {
        Array<String> candidates = new Array<>(names.length);
        var lutNames = lutManager.getPresetNames();

        for (var name : names) {
            for (var lut : lutNames) {
                if (lut.contains(name)) {
                    candidates.add(lut);
                    break;
                }
            }
        }
        if (candidates.isEmpty()) {
            logger.error("Could not find any of the given names in the LUT manager: " + TextUtils.arrayToStr(names));
            return "earthlike";
        } else {
            return candidates.get(rand.nextInt(candidates.size));
        }
    }

    public void randomizeRockyPlanet(long seed) {
        initializeBiomeLUTManager();

        var rand = new Random(seed);
        setHeight("generate");
        setDiffuse("generate");
        setNormal("generate");
        setSpecular("generate");
        setEmissive("generate");

        setBiomeLUT(randomBiomeLut(rand, "rocky"));

        if (rand.nextBoolean()) {
            // In [340, 20] - close to home.
            setBiomeHueShift((rand.nextDouble(-20.0, 20.0) + 360.0) % 360.0);
        } else {
            // In [100, 140] - red waters.
            setBiomeHueShift(rand.nextDouble(100.0, 140.0));
        }
        // Saturation.
        setBiomeSaturation(rand.nextDouble(0.3, 0.7));
        // Height scale
        setHeightScale(gaussian(rand, 30.0, 40.0, 1.0, 80.0));
        // Noise
        if (nc != null) {
            nc.dispose();
        }
        NoiseComponent nc = new NoiseComponent();
        nc.randomizeRockyPlanet(rand);
        setNoise(nc);
    }

    public void randomizeEarthLike(long seed) {
        initializeBiomeLUTManager();

        var rand = new Random(seed);
        setHeight("generate");
        setDiffuse("generate");
        setNormal("generate");
        setSpecular("generate");
        setEmissive("generate");

        setBiomeLUT(randomBiomeLut(rand, "earthlike"));

        // Choose randomly in [0, 30] and [330, 360].
        setBiomeHueShift((rand.nextDouble(-20.0, 20.0) + 360.0) % 360.0);
        // Saturation.
        setBiomeSaturation(rand.nextDouble(0.3, 0.6));
        // Height scale
        setHeightScale(gaussian(rand, 40.0, 10.0, 5.0, 80.0));
        // Noise
        if (nc != null) {
            nc.dispose();
        }
        NoiseComponent nc = new NoiseComponent();
        nc.randomizeEarthLike(rand);
        setNoise(nc);
    }

    public void randomizeDesert(long seed) {
        initializeBiomeLUTManager();

        var rand = new Random(seed);
        setHeight("generate");
        setDiffuse("generate");
        setNormal("generate");
        setSpecular("generate");
        setEmissive("generate");

        setBiomeLUT(randomBiomeLut(rand, "desert"));

        setBiomeHueShift(0.0);
        // Saturation.
        setBiomeSaturation(rand.nextDouble(0.3, 0.6));
        // Height scale
        setHeightScale(gaussian(rand, 40.0, 10.0, 5.0, 80.0));
        // Noise
        if (nc != null) {
            nc.dispose();
        }
        NoiseComponent nc = new NoiseComponent();
        nc.randomizeDesert(rand);
        setNoise(nc);
    }

    public void randomizeTropical(long seed) {
        initializeBiomeLUTManager();

        var rand = new Random(seed);
        setHeight("generate");
        setDiffuse("generate");
        setNormal("generate");
        setSpecular("generate");
        setEmissive("generate");

        setBiomeLUT(randomBiomeLut(rand, "tropical", "earthlike"));

        setBiomeHueShift(0.0);
        // Saturation.
        setBiomeSaturation(rand.nextDouble(0.3, 0.6));
        // Height scale
        setHeightScale(gaussian(rand, 40.0, 10.0, 5.0, 80.0));
        // Noise
        if (nc != null) {
            nc.dispose();
        }
        NoiseComponent nc = new NoiseComponent();
        nc.randomizeTropical(rand);
        setNoise(nc);
    }

    public void randomizeLava(long seed) {
        initializeBiomeLUTManager();

        var rand = new Random(seed);
        setHeight("generate");
        setDiffuse("generate");
        setNormal("generate");
        setSpecular("generate");
        setEmissive("generate");

        setBiomeLUT(randomBiomeLut(rand, "lava"));

        setBiomeHueShift(0.0);
        // Saturation.
        setBiomeSaturation(0.5);
        // Height scale
        setHeightScale(gaussian(rand, 40.0, 10.0, 5.0, 80.0));
        // Noise
        if (nc != null) {
            nc.dispose();
        }
        NoiseComponent nc = new NoiseComponent();
        nc.randomizeEarthLike(rand);
        nc.setLatitudeInfluence(0.5);
        setNoise(nc);
    }

    public void randomizeAlien(long seed) {
        initializeBiomeLUTManager();

        var rand = new Random(seed);
        setHeight("generate");
        setDiffuse("generate");
        setNormal("generate");
        setSpecular("generate");
        setEmissive("generate");

        setBiomeLUT(randomBiomeLut(rand, "alien"));

        // Between 70 and 200.
        setBiomeHueShift((rand.nextDouble(70.0, 200.0)));
        // Saturation.
        setBiomeSaturation(rand.nextDouble(0.3, 0.6));
        // Height scale
        setHeightScale(gaussian(rand, 40.0, 10.0, 5.0, 80.0));
        // Noise
        if (nc != null) {
            nc.dispose();
        }
        NoiseComponent nc = new NoiseComponent();
        nc.randomizeAlien(rand);
        setNoise(nc);
    }

    public void randomizeFrozenPlanet(long seed) {
        initializeBiomeLUTManager();

        var rand = new Random(seed);
        setHeight("generate");
        setDiffuse("generate");
        setNormal("generate");
        setSpecular("generate");
        setEmissive("generate");

        setBiomeLUT(randomBiomeLut(rand, "ice"));

        // Choose randomly in [0, 30] and [330, 360].
        setBiomeHueShift((rand.nextDouble(-30.0, 30.0) + 360.0) % 360.0);
        // Saturation.
        setBiomeSaturation(rand.nextDouble(0.3, 0.6));
        // Height scale
        setHeightScale(gaussian(rand, 30.0, 40.0, 1.0, 80.0));
        // Noise
        if (nc != null) {
            nc.dispose();
        }
        NoiseComponent nc = new NoiseComponent();
        nc.randomizeEarthLike(rand);
        nc.setLatitudeInfluence(0.6);
        setNoise(nc);
    }

    public void randomizeGasGiant(long seed) {
        initializeBiomeLUTManager();

        var rand = new Random(seed);
        setHeight("generate");
        setDiffuse("generate");
        setNormal("generate");
        setSpecular("generate");
        setEmissive("generate");

        setBiomeLUT(lutManager.getPresetNames().get(rand.nextInt(lutManager.getSize())));
        // Actually roll the dice for hue shift.
        setBiomehueshift(rand.nextDouble() * 360.0);
        // Saturation.
        setBiomeSaturation(rand.nextDouble(0.35, 0.7));
        setHeightScale(1.0);
        // Noise
        if (nc != null) {
            nc.dispose();
        }
        NoiseComponent nc = new NoiseComponent();
        nc.randomizeGasGiant(rand);
        setNoise(nc);
    }

    public void print(Log log) {
        log.debug("Height: " + height);
        log.debug("Diffuse: " + diffuse);
        log.debug("Specular: " + specular);
        log.debug("Normal: " + normal);
        log.debug("LUT: " + lut);
        log.debug("Biome hue shift: " + lutHueShift);
        log.debug("Biome saturation: " + lutSaturation);
        log.debug("Height scale: " + heightScale);
        log.debug("---Noise---");
        if (nc != null) {
            nc.print(log);
        }
    }

    @Override
    public void dispose() {
        disposeTextures(GaiaSky.instance.assetManager);
        EventManager.instance.removeAllSubscriptions(this);
    }

    @Override
    public void updateWith(MaterialComponent object) {
        // Random attributes.
        if (object.specularColor != null) {
            this.specularColor = object.specularColor;
        }
        if (object.diffuseColor != null) {
            this.diffuseColor = object.diffuseColor;
        }
        if (object.emissiveColor != null) {
            this.emissiveColor = object.emissiveColor;
        }
        if (object.metallicColor != null) {
            this.metallicColor = object.metallicColor;
        }
        if (!Float.isNaN(object.roughnessColor)) {
            this.roughnessColor = object.roughnessColor;
        }
        if (object.heightScale != DEFAULT_HEIGHT_SCALE) {
            this.heightScale = object.heightScale;
        }
        // Regular textures.
        if (object.diffuse != null) {
            this.diffuse = object.diffuse;
        }
        if (object.specular != null) {
            this.specular = object.specular;
        }
        if (object.normal != null) {
            this.normal = object.normal;
        }
        if (object.height != null) {
            this.height = object.height;
        }
        if (object.emissive != null) {
            this.emissive = object.emissive;
        }
        if (object.metallic != null) {
            this.metallic = object.metallic;
        }
        if (object.roughness != null) {
            this.roughness = object.roughness;
        }
        if (object.texture0 != null) {
            this.texture0 = object.texture0;
        }
        if (object.texture1 != null) {
            this.texture1 = object.texture1;
        }
        // Cubemaps.
        if (object.diffuseCubemap != null) {
            this.diffuseCubemap = object.diffuseCubemap;
        }
        if (object.specularCubemap != null) {
            this.specularCubemap = object.specularCubemap;
        }
        if (object.normalCubemap != null) {
            this.normalCubemap = object.normalCubemap;
        }
        if (object.heightCubemap != null) {
            this.heightCubemap = object.heightCubemap;
        }
        if (object.emissiveCubemap != null) {
            this.emissiveCubemap = object.emissiveCubemap;
        }
        if (object.metallicCubemap != null) {
            this.metallicCubemap = object.metallicCubemap;
        }
        if (object.roughnessCubemap != null) {
            this.roughnessCubemap = object.roughnessCubemap;
        }
        if (object.aoCubemap != null) {
            this.aoCubemap = object.aoCubemap;
        }
        // SVTs.
        if (object.diffuseSvt != null) {
            this.diffuseSvt = object.diffuseSvt;
        }
        if (object.specularSvt != null) {
            this.specularSvt = object.specularSvt;
        }
        if (object.normalSvt != null) {
            this.normalSvt = object.normalSvt;
        }
        if (object.heightSvt != null) {
            this.heightSvt = object.heightSvt;
        }
        if (object.emissiveSvt != null) {
            this.emissiveSvt = object.emissiveSvt;
        }
        if (object.metallicSvt != null) {
            this.metallicSvt = object.metallicSvt;
        }
        if (object.roughnessSvt != null) {
            this.roughnessSvt = object.roughnessSvt;
        }
        if (object.aoSvt != null) {
            this.aoSvt = object.aoSvt;
        }
    }
}
