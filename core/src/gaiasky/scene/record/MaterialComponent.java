/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;
import gaiasky.GaiaSky;
import gaiasky.data.AssetBean;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.scene.api.IUpdatable;
import gaiasky.util.*;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings.ElevationType;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.gdx.loader.OwnTextureLoader.OwnTextureParameter;
import gaiasky.util.gdx.loader.PFMTextureLoader.PFMTextureParameter;
import gaiasky.util.gdx.model.IntModelInstance;
import gaiasky.util.gdx.shader.Material;
import gaiasky.util.gdx.shader.attribute.*;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.svt.SVTManager;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A basic component that fully describes the material of an object.
 * It contains basic textures (diffuse, specular, reflection, emissive, etc.),
 * cubemaps (same as textures), and even some sparse virtual texture trees (SVT).
 */
public class MaterialComponent extends NamedComponent implements IObserver, IMaterialProvider, IUpdatable<MaterialComponent> {
    /** Default texture parameters **/
    protected static final OwnTextureParameter textureParamsMipMap, textureParams;
    protected static final PFMTextureParameter pfmTextureParams;
    private static final Log logger = Logger.getLogger(MaterialComponent.class);

    // Default height scale is 4 km.
    private static final float DEFAULT_HEIGHT_SCALE = (float) (4.0 * Constants.KM_TO_U);

    // Default reflection cubemap for all materials.
    public static CubemapComponent reflectionCubemap;

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

    static {
        reflectionCubemap = new CubemapComponent();
    }

    // Texture location strings.
    public boolean texInitialised, texLoading;
    public String diffuse, specular, normal, emissive, ring, height, ringnormal, roughness, metallic, ao;
    public String diffuseUnpacked, specularUnpacked, normalUnpacked, emissiveUnpacked, ringUnpacked, heightUnpacked, ringnormalUnpacked, roughnessUnapcked, metallicUnpacked, aoUnapcked;
    // Material properties and colors.
    public float[] diffuseColor;
    public float[] specularColor;
    public float[] metallicColor;
    public float[] emissiveColor;
    public float roughnessColor = Float.NaN;
    /**
     * Height scale in internal units. The mapping value of white in the height map (maximum height value in this body). Black is mapped to 0.
     */
    public Float heightScale = DEFAULT_HEIGHT_SCALE;
    public Vector2 heightSize = new Vector2();
    public IHeightData heightData;
    public NoiseComponent nc;
    // Sparse virtual texture sets.
    public VirtualTextureComponent diffuseSvt, specularSvt, heightSvt, normalSvt, emissiveSvt, roughnessSvt, metallicSvt;
    public Array<VirtualTextureComponent> svts;
    // Cubemaps.
    public CubemapComponent diffuseCubemap, specularCubemap, normalCubemap, emissiveCubemap, heightCubemap, roughnessCubemap, metallicCubemap;
    // Biome lookup texture.
    public String biomeLUT = Constants.DATA_LOCATION_TOKEN + "tex/base/biome-lut.png";
    public float biomeHueShift = 0;
    /** Add also color even if texture is present **/
    public boolean colorIfTexture = false;
    /** The actual material **/
    private Material material, ringMaterial;
    private final AtomicBoolean heightGenerated = new AtomicBoolean(false);
    private final AtomicBoolean heightInitialized = new AtomicBoolean(false);
    private Texture heightTex, specularTex, diffuseTex, normalTex;

    public MaterialComponent() {
        super();
        EventManager.instance.subscribe(this, Event.ELEVATION_TYPE_CMD, Event.ELEVATION_MULTIPLIER_CMD, Event.TESSELLATION_QUALITY_CMD);
    }

    private static OwnTextureParameter getTP(String tex) {
        return getTP(tex, false);
    }

    private static OwnTextureParameter getTP(String tex, boolean mipmap) {
        if (tex != null && tex.endsWith(".pfm")) {
            return pfmTextureParams;
        } else {
            if (mipmap)
                return textureParamsMipMap;
            else
                return textureParams;
        }
    }

    public void initialize(String name, AssetManager manager) {
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
            aoUnapcked = addToLoad(ao, getTP(ao, true), manager);
        if (height != null && !height.endsWith(Constants.GEN_KEYWORD))
            heightUnpacked = addToLoad(height, getTP(height, true), manager);
        if (ring != null)
            ringUnpacked = addToLoad(ring, getTP(ring, true), manager);
        if (ringnormal != null)
            ringnormalUnpacked = addToLoad(ringnormal, getTP(ringnormal, true), manager);

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

        this.heightGenerated.set(false);
    }

    public void initialize(String name) {
        initialize(name, null);
    }

    public boolean isFinishedLoading(AssetManager manager) {
        return ComponentUtils.isLoaded(diffuseUnpacked, manager) && ComponentUtils.isLoaded(normalUnpacked, manager) && ComponentUtils.isLoaded(specularUnpacked, manager) && ComponentUtils.isLoaded(emissiveUnpacked, manager) && ComponentUtils.isLoaded(ringUnpacked, manager) && ComponentUtils.isLoaded(ringnormalUnpacked, manager) && ComponentUtils.isLoaded(heightUnpacked, manager) && ComponentUtils.isLoaded(roughnessUnapcked, manager) && ComponentUtils.isLoaded(metallicUnpacked, manager)
                && ComponentUtils.isLoaded(aoUnapcked, manager) && ComponentUtils.isLoaded(diffuseCubemap, manager) && ComponentUtils.isLoaded(normalCubemap, manager) && ComponentUtils.isLoaded(emissiveCubemap, manager) && ComponentUtils.isLoaded(specularCubemap, manager) && ComponentUtils.isLoaded(roughnessCubemap, manager) && ComponentUtils.isLoaded(metallicCubemap, manager) && ComponentUtils.isLoaded(heightCubemap, manager);
    }

    public boolean hasSVT() {
        return diffuseSvt != null || normalSvt != null || emissiveSvt != null || specularSvt != null || heightSvt != null || metallicSvt != null || roughnessSvt != null;
    }

    /**
     * Adds the texture to load and unpacks any star (*) with the current
     * quality setting.
     *
     * @param tex The texture file to load.
     *
     * @return The actual loaded texture path
     */
    private String addToLoad(String tex, OwnTextureParameter texParams, AssetManager manager) {
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
    private String addToLoad(String tex, OwnTextureParameter texParams) {
        if (tex == null)
            return null;

        tex = GlobalResources.unpackAssetPath(tex);
        logger.info(I18n.msg("notif.loading", tex));
        AssetBean.addAsset(tex, Texture.class, texParams);

        return tex;
    }

    public void initMaterial(AssetManager manager, IntModelInstance instance, float[] diffuseCol, boolean culling) {
        initMaterial(manager, instance.materials.get(0), instance.materials.size > 1 ? instance.materials.get(1) : null, diffuseCol, culling);
    }

    public void initMaterial(AssetManager manager, Material mat, Material ring, float[] diffuseCol, boolean culling) {
        reflectionCubemap.initialize();
        this.material = mat;
        if (diffuse != null && material.get(TextureAttribute.Diffuse) == null) {
            if (!diffuse.endsWith(Constants.GEN_KEYWORD)) {
                Texture tex = manager.get(diffuseUnpacked, Texture.class);
                addDiffuseTex(tex);
            }
        }
        // Copy diffuse color
        if (diffuseCol != null) {
            diffuseColor = new float[4];
            diffuseColor[0] = diffuseCol[0];
            diffuseColor[1] = diffuseCol[1];
            diffuseColor[2] = diffuseCol[2];
            diffuseColor[3] = diffuseCol[3];
            if (colorIfTexture || diffuse == null) {
                // Add diffuse colour
                material.set(new ColorAttribute(ColorAttribute.Diffuse, diffuseColor[0], diffuseColor[1], diffuseColor[2], diffuseColor[3]));
            }
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
        if (height != null && material.get(TextureAttribute.Height) == null) {
            if (!height.endsWith(Constants.GEN_KEYWORD)) {
                Texture tex = manager.get(heightUnpacked, Texture.class);
                if (!Settings.settings.scene.renderer.elevation.type.isNone()) {
                    initializeElevationData(tex);
                }
            } else {
                initializeGenElevationData();
            }
        }
        if (ring != null) {
            // Ring material
            ringMaterial = ring;
            if (ringMaterial.get(TextureAttribute.Diffuse) == null) {
                ringMaterial.set(new TextureAttribute(TextureAttribute.Diffuse, manager.get(ringUnpacked, Texture.class)));
            }
            if (ringnormal != null && ringMaterial.get(TextureAttribute.Normal) == null) {
                ringMaterial.set(new TextureAttribute(TextureAttribute.Normal, manager.get(ringnormalUnpacked, Texture.class)));
            }
            ringMaterial.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA));
            if (!culling)
                ringMaterial.set(new IntAttribute(IntAttribute.CullFace, GL20.GL_NONE));
        }
        if (!culling) {
            material.set(new IntAttribute(IntAttribute.CullFace, GL20.GL_NONE));
        }
        // Add reflection cubemap if SSR is off and this material has metallic attributes
        if (metallic != null || metallicColor != null) {
            if (!Settings.settings.postprocess.ssr.active) {
                reflectionCubemap.prepareCubemap(manager);
                material.set(new CubemapAttribute(CubemapAttribute.ReflectionCubemap, reflectionCubemap.cubemap));
            }
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
                Texture tex = manager.get(aoUnapcked, Texture.class);
                material.set(new TextureAttribute(TextureAttribute.AO, tex));
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

        // Sparse Virtual Textures.
        svts = new Array<>();
        int svtId = 0;
        if (diffuseSvt != null || normalSvt != null || emissiveSvt != null || specularSvt != null || heightSvt != null || metallicSvt != null || roughnessSvt != null) {
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
            if (!Settings.settings.scene.renderer.elevation.type.isNone()) {
                addSVTAttributes(material, heightSvt, svtId);
                initializeElevationData(heightSvt, manager);
            }
        }
        if (metallicSvt != null) {
            addSVTAttributes(material, metallicSvt, svtId);
        }
        if (roughnessSvt != null) {
            addSVTAttributes(material, roughnessSvt, svtId);
        }
        if (svtId > 0) {
            // Broadcast this material for SVT manager.
            EventManager.publish(Event.SVT_MATERIAL_INFO, this, svtId, this);
        }

    }

    private void addSVTAttributes(Material material, VirtualTextureComponent svt, int id) {
        // Set ID.
        svt.id = id;
        // Set attributes.
        material.set(new FloatAttribute(FloatAttribute.SvtTileSize, svt.tileSize));
        material.set(new FloatAttribute(FloatAttribute.SvtId, svt.id));
        // Only update depth and resolution if it does not exist, or if it exists and its value is less than ours.
        if (!material.has(FloatAttribute.SvtDepth) || ((FloatAttribute) Objects.requireNonNull(material.get(FloatAttribute.SvtDepth))).value < svt.tree.depth) {
            // Depth.
            material.set(new FloatAttribute(FloatAttribute.SvtDepth, svt.tree.depth));
            // Resolution.
            double svtResolution = svt.tileSize * Math.pow(2.0, svt.tree.depth);
            material.set(new Vector2Attribute(Vector2Attribute.SvtResolution, new Vector2((float) (svtResolution * svt.tree.root.length), (float) svtResolution)));
        }

        // Add to list.
        svts.add(svt);
    }

    private void addHeightTex(Texture heightTex) {
        if (heightTex != null && material != null) {
            heightSize.set(heightTex.getWidth(), heightTex.getHeight());
            material.set(new TextureAttribute(TextureAttribute.Height, heightTex));
            material.set(new FloatAttribute(FloatAttribute.HeightScale, heightScale * (float) Settings.settings.scene.renderer.elevation.multiplier));
            material.set(new Vector2Attribute(Vector2Attribute.HeightSize, heightSize));
            material.set(new FloatAttribute(FloatAttribute.TessQuality, (float) Settings.settings.scene.renderer.elevation.quality));
        }
    }

    private void addDiffuseTex(Texture diffuseTex) {
        if (diffuseTex != null && material != null) {
            material.set(new TextureAttribute(TextureAttribute.Diffuse, diffuseTex));
        }
    }

    private void addSpecularTex(Texture specularTex) {
        if (specularTex != null && material != null) {
            material.set(new TextureAttribute(TextureAttribute.Specular, specularTex));
        }
    }

    private void addNormalTex(Texture normalTex) {
        if (normalTex != null && material != null) {
            material.set(new TextureAttribute(TextureAttribute.Normal, normalTex));
        }
    }

    private void addRoughnessTex(Texture roughnessTex) {
        if (roughnessTex != null && material != null) {
            material.set(new TextureAttribute(TextureAttribute.Roughness, roughnessTex));
        }
    }

    public void setGenerated(boolean generated) {
        this.heightGenerated.set(generated);
    }

    private synchronized void initializeGenElevationData() {
        if (heightGenerated.get()) {
            addHeightTex(heightTex);
        } else {
            heightGenerated.set(true);
            GaiaSky.instance.getExecutorService().execute(() -> {
                // Begin
                EventManager.publish(Event.PROCEDURAL_GENERATION_SURFACE_INFO, this, true);

                final int N = Settings.settings.graphics.quality.texWidthTarget;
                final int M = Settings.settings.graphics.quality.texHeightTarget;
                long start = TimeUtils.millis();
                GaiaSky.postRunnable(() -> logger.info(I18n.msg("gui.procedural.info.generate", I18n.msg("gui.procedural.surface"), Integer.toString(N), Integer.toString(M))));

                if (nc == null) {
                    nc = new NoiseComponent();
                    Random noiseRandom = new Random();
                    nc.randomizeAll(noiseRandom, noiseRandom.nextBoolean(), true);
                }
                Trio<float[][], float[][], Pixmap> trio = nc.generateElevation(N, M, heightScale, I18n.msg("gui.procedural.progress", I18n.msg("gui.procedural.surface"), name));
                float[][] elevationData = trio.getFirst();
                float[][] moistureData = trio.getSecond();
                Pixmap heightPixmap = trio.getThird();

                boolean cDiffuse = diffuse != null && diffuse.endsWith(Constants.GEN_KEYWORD);
                boolean cSpecular = specular != null && specular.endsWith(Constants.GEN_KEYWORD);
                boolean cNormal = normal != null && normal.endsWith(Constants.GEN_KEYWORD);
                // TODO implement emissive texture generation
                //boolean cEmissive = emissive != null && emissive.endsWith(Constants.GEN_KEYWORD);
                // TODO implement metallic texture generation
                //boolean cMetallic = metallic != null && metallic.endsWith(Constants.GEN_KEYWORD);

                // Create diffuse and specular textures
                if (cDiffuse || cSpecular) {
                    try {
                        BufferedImage lut = ImageIO.read(Settings.settings.data.dataFileHandle(biomeLUT).file());
                        int iw = lut.getWidth() - 1;
                        int ih = lut.getHeight() - 1;

                        final Pixmap diffusePixmap;
                        final Pixmap specularPixmap;
                        if (cDiffuse) {
                            diffusePixmap = new Pixmap(N, M, Pixmap.Format.RGBA8888);
                        } else {
                            diffusePixmap = null;
                        }
                        if (cSpecular) {
                            specularPixmap = new Pixmap(N, M, Pixmap.Format.RGBA8888);
                        } else {
                            specularPixmap = null;
                        }
                        IntStream.range(0, N).parallel().forEach(i -> {
                            final int ii = i;
                            IntStream.range(0, M).parallel().forEach(j -> {
                                // Normalize height
                                float height = elevationData[ii][j] / heightScale;
                                float moisture = moistureData[ii][j];

                                int x = (int) (iw * MathUtilsDouble.clamp(moisture, 0, 1));
                                int y = (int) (ih - ih * MathUtilsDouble.clamp(height, 0, 1));

                                java.awt.Color argb = new java.awt.Color(lut.getRGB(x, y));
                                float[] rgb = new float[] { argb.getRed() / 255f, argb.getGreen() / 255f, argb.getBlue() / 255f };
                                if (biomeHueShift != 0) {
                                    // Shift hue of lookup table by an amount in degrees
                                    float[] hsb = ColorUtils.rgbToHsb(rgb);
                                    hsb[0] = ((hsb[0] * 360f + biomeHueShift) % 360f) / 360f;
                                    rgb = ColorUtils.hsbToRgb(hsb);
                                }
                                Color col = new Color(rgb[0], rgb[1], rgb[2], 1f);

                                if (diffusePixmap != null)
                                    diffusePixmap.drawPixel(ii, j, Color.rgba8888(col));

                                boolean water = height <= 0.02f;
                                boolean snow = height > 0.85f;
                                if (water) {
                                    if (specularPixmap != null) {
                                        // White
                                        specularPixmap.drawPixel(ii, j, Color.rgba8888(1f, 1f, 1f, 1f));
                                    }
                                } else if (snow) {
                                    if (specularPixmap != null) {
                                        // Whitish
                                        specularPixmap.drawPixel(ii, j, Color.rgba8888(0.5f, 0.5f, 0.5f, 1f));
                                    }
                                } else {
                                    if (specularPixmap != null) {
                                        // Black
                                        specularPixmap.drawPixel(ii, j, Color.rgba8888(0f, 0f, 0f, 1f));
                                    }
                                }
                            });
                        });
                        // Write to disk if necessary
                        if (Settings.settings.program.saveProceduralTextures) {
                            SysUtils.saveProceduralPixmap(heightPixmap, this.name + "-height");
                            SysUtils.saveProceduralPixmap(diffusePixmap, this.name + "-diffuse");
                            SysUtils.saveProceduralPixmap(specularPixmap, this.name + "-specular");
                        }

                        GaiaSky.postRunnable(() -> {
                            if (heightPixmap != null) {
                                // Create texture, populate material
                                if (!Settings.settings.scene.renderer.elevation.type.isNone()) {
                                    heightData = new HeightDataPixmap(heightPixmap, null);
                                    heightTex = new Texture(heightPixmap, true);
                                    heightTex.setFilter(TextureFilter.MipMapLinearLinear, TextureFilter.Linear);

                                    addHeightTex(heightTex);
                                }
                            }
                            if (diffusePixmap != null) {
                                diffuseTex = new Texture(diffusePixmap, true);
                                diffuseTex.setFilter(TextureFilter.MipMapLinearLinear, TextureFilter.Linear);

                                addDiffuseTex(diffuseTex);
                            }
                            if (specularPixmap != null) {
                                specularTex = new Texture(specularPixmap, true);
                                specularTex.setFilter(TextureFilter.MipMapLinearLinear, TextureFilter.Linear);

                                addSpecularTex(specularTex);
                            }
                        });
                    } catch (IOException e) {
                        logger.error(e);
                    }
                }
                // Generate normal texture from height data (only if tessellation is off)
                if (cNormal && !Settings.settings.scene.renderer.elevation.type.isTessellation()) {
                    final Pixmap normalPixmap = new Pixmap(N, M, Pixmap.Format.RGBA8888);
                    float scale = 0.5f;
                    IntStream.range(0, M).forEach(j -> {
                        IntStream.range(0, N).forEach(i -> {
                            int im = i > 0 ? i - 1 : i;
                            int ip = i < N - 1 ? i + 1 : i;
                            int jm = j > 0 ? j - 1 : j;
                            int jp = j < M - 1 ? j + 1 : j;
                            float vtl = elevationData[im][jm] / heightScale;
                            float vl = elevationData[im][j] / heightScale;
                            float vbl = elevationData[im][jp] / heightScale;
                            float vt = elevationData[i][jm] / heightScale;
                            float vb = elevationData[i][jp] / heightScale;
                            float vtr = elevationData[ip][jm] / heightScale;
                            float vr = elevationData[ip][j] / heightScale;
                            float vbr = elevationData[ip][jp] / heightScale;
                            float dx = (vtl + vl * 2f + vbl - vtr - vr * 2f - vbr) * scale;
                            float dy = (vtl + vt * 2f + vtr - vbl - vb * 2f - vbr) * scale;
                            Vector3 normal = new Vector3(dx * 255f, dy * 255f, 255f).nor();
                            normalPixmap.drawPixel(i, j, Color.rgba8888(normal.x * 0.5f + 0.5f, normal.y * 0.5f + 0.5f, normal.z, 1f));
                        });
                    });
                    // Write to disk if necessary
                    if (Settings.settings.program.saveProceduralTextures) {
                        SysUtils.saveProceduralPixmap(normalPixmap, this.name + "-normal");
                    }
                    GaiaSky.postRunnable(() -> {
                        normalTex = new Texture(normalPixmap, true);
                        normalTex.setFilter(TextureFilter.MipMapLinearLinear, TextureFilter.Linear);

                        addNormalTex(normalTex);
                    });
                }
                long elapsed = TimeUtils.millis() - start;
                GaiaSky.postRunnable(() -> logger.info(I18n.msg("gui.procedural.info.done", I18n.msg("gui.procedural.surface"), Double.toString(elapsed / 1000d))));

                // End
                EventManager.publish(Event.PROCEDURAL_GENERATION_SURFACE_INFO, this, false);
            });
        }
    }

    private void initializeElevationData(VirtualTextureComponent svt, AssetManager manager) {
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
        this.diffuse = Settings.settings.data.dataFile(diffuse);
    }

    public void setSpecular(String specular) {
        this.specular = Settings.settings.data.dataFile(specular);
    }

    public void setSpecular(Double specular) {
        float r = specular.floatValue();
        this.specularColor = new float[] { r, r, r };
    }

    public void setSpecular(double[] specular) {
        if (specular.length > 1) {
            this.specularColor = new float[] { (float) specular[0], (float) specular[1], (float) specular[2] };
        } else {
            float r = (float) specular[0];
            this.specularColor = new float[] { r, r, r };
        }
    }

    public void setNormal(String normal) {
        this.normal = Settings.settings.data.dataFile(normal);
    }

    /**
     * @deprecated use {@link MaterialComponent#setEmissive(String)} instead
     */
    @Deprecated
    public void setNight(String emissive) {
        this.setEmissive(emissive);
    }

    public void setEmissive(String emissive) {
        this.emissive = Settings.settings.data.dataFile(emissive);
    }

    public void setEmissive(Double emissive) {
        float r = emissive.floatValue();
        this.emissiveColor = new float[] { r, r, r };
    }

    public void setEmissive(double[] emissive) {
        if (emissive.length > 1) {
            this.emissiveColor = new float[] { (float) emissive[0], (float) emissive[1], (float) emissive[2] };
        } else {
            float r = (float) emissive[0];
            this.emissiveColor = new float[] { r, r, r };
        }
    }

    public void setRing(String ring) {
        this.ring = Settings.settings.data.dataFile(ring);
    }

    public void setRingnormal(String ringnormal) {
        this.ringnormal = Settings.settings.data.dataFile(ringnormal);
    }

    public void setHeight(String height) {
        this.height = Settings.settings.data.dataFile(height);
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

    public void setBiomelut(String biomeLookupTex) {
        this.biomeLUT = biomeLookupTex;
    }

    public void setBiomehueshift(Double hueShift) {
        this.biomeHueShift = hueShift.floatValue();
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
        this.metallicColor = new float[] { r, r, r };
    }

    public void setMetallic(String metallic) {
        this.metallic = Settings.settings.data.dataFile(metallic);
    }

    public void setReflection(double[] metallic) {
        if (metallic.length > 1) {
            this.metallicColor = new float[] { (float) metallic[0], (float) metallic[1], (float) metallic[2] };
        } else {
            float r = (float) metallic[0];
            this.metallicColor = new float[] { r, r, r };
        }
    }

    public void setRoughness(String roughness) {
        this.roughness = Settings.settings.data.dataFile(roughness);
    }

    public void setRoughness(Double roughness) {
        this.roughnessColor = roughness.floatValue();
    }

    public void setAo(String ao) {
        this.ao = Settings.settings.data.dataFile(ao);
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

    public void setEmissiveColormap(String cubemap) {
        this.emissiveCubemap = new CubemapComponent();
        this.emissiveCubemap.setLocation(cubemap);
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
        MaterialComponent.reflectionCubemap = new CubemapComponent();
        MaterialComponent.reflectionCubemap.setLocation(reflectionCubemap);
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

    public void disposeTexture(AssetManager manager, Material material, String name, String nameUnpacked, int attributeIndex, Texture tex) {
        if (name != null && manager != null && manager.isLoaded(nameUnpacked)) {
            unload(material, attributeIndex);
            manager.unload(nameUnpacked);
        }
        if (tex != null) {
            unload(material, attributeIndex);
            tex.dispose();
        }
    }

    public void disposeCubemap(AssetManager manager, Material mat, int attributeIndex, CubemapComponent cubemap) {
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
        disposeTexture(manager, material, ao, aoUnapcked, TextureAttribute.AO, null);
        disposeCubemap(manager, material, CubemapAttribute.DiffuseCubemap, diffuseCubemap);
        disposeCubemap(manager, material, CubemapAttribute.NormalCubemap, normalCubemap);
        disposeCubemap(manager, material, CubemapAttribute.EmissiveCubemap, emissiveCubemap);
        disposeCubemap(manager, material, CubemapAttribute.SpecularCubemap, specularCubemap);
        disposeCubemap(manager, material, CubemapAttribute.RoughnessCubemap, roughnessCubemap);
        disposeCubemap(manager, material, CubemapAttribute.MetallicCubemap, metallicCubemap);
        disposeCubemap(manager, material, CubemapAttribute.HeightCubemap, heightCubemap);
        texLoading = false;
        texInitialised = false;
    }

    private void unload(Material mat, int attrIndex) {
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
    public void notify(final Event event, Object source, final Object... data) {
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
                                initializeElevationData(((TextureAttribute) Objects.requireNonNull(this.material.get(TextureAttribute.Height))).textureDescription.texture);
                            } else if (AssetBean.manager().isLoaded(heightUnpacked)) {
                                if (!height.endsWith(Constants.GEN_KEYWORD)) {
                                    Texture tex = AssetBean.manager().get(heightUnpacked, Texture.class);
                                    if (!Settings.settings.scene.renderer.elevation.type.isNone()) {
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
                GaiaSky.postRunnable(() -> this.material.set(new FloatAttribute(FloatAttribute.HeightScale, heightScale * newMultiplier)));
            }
        }
        case TESSELLATION_QUALITY_CMD -> {
            if (this.hasHeight() && this.material != null) {
                float newQuality = (Float) data[0];
                GaiaSky.postRunnable(() -> this.material.set(new FloatAttribute(FloatAttribute.TessQuality, newQuality)));
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
        this.biomeLUT = other.biomeLUT;
        this.biomeHueShift = other.biomeHueShift;
        this.heightScale = other.heightScale;
        this.nc = new NoiseComponent();
        if (other.nc != null) {
            this.nc.copyFrom(other.nc);
        } else {
            this.nc.randomizeAll(new Random());
        }
    }

    public void randomizeAll(long seed, double bodySize) {
        var rand = new Random(seed);
        setHeight("generate");
        setDiffuse("generate");
        setNormal("generate");
        setSpecular("generate");
        var dataPath = Settings.settings.data.dataPath("tex/base");
        Array<String> lookUpTables = new Array<>();
        try (var paths = Files.list(dataPath)) {
            List<Path> l = paths.filter(f -> f.toString().endsWith("-lut.png")).collect(Collectors.toList());
            for (Path p : l) {
                String name = p.toString();
                lookUpTables.add("data" + name.substring(name.indexOf("/tex/base/")));
            }
        } catch (Exception ignored) {
        }

        if (lookUpTables.isEmpty()) {
            lookUpTables.add(Constants.DATA_LOCATION_TOKEN + "tex/base/biome-lut.png");
            lookUpTables.add(Constants.DATA_LOCATION_TOKEN + "tex/base/biome-smooth-lut.png");
        }
        setBiomelut(lookUpTables.get(rand.nextInt(lookUpTables.size)));
        setBiomehueshift(rand.nextDouble() * 360.0);
        double sizeKm = bodySize * Constants.U_TO_KM;
        setHeightScale(gaussian(rand, sizeKm * 0.001, sizeKm * 0.0006, 1.0));
        // Noise
        NoiseComponent nc = new NoiseComponent();
        nc.randomizeAll(rand);
        setNoise(nc);
    }

    public void print(Log log) {
        log.debug("Height: " + height);
        log.debug("Diffuse: " + diffuse);
        log.debug("Specular: " + specular);
        log.debug("Normal: " + normal);
        log.debug("LUT: " + biomeLUT);
        log.debug("Hue shift: " + biomeHueShift);
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
    }
}
