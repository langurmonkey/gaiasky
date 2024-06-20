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
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.TimeUtils;
import gaiasky.GaiaSky;
import gaiasky.data.AssetBean;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.render.BlendMode;
import gaiasky.render.ComponentTypes;
import gaiasky.scene.api.IUpdatable;
import gaiasky.scene.component.Model;
import gaiasky.util.*;
import gaiasky.util.Logger.Log;
import gaiasky.util.gdx.loader.OwnTextureLoader.OwnTextureParameter;
import gaiasky.util.gdx.model.IntModel;
import gaiasky.util.gdx.model.IntModelInstance;
import gaiasky.util.gdx.shader.Material;
import gaiasky.util.gdx.shader.attribute.*;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;
import gaiasky.util.svt.SVTManager;
import net.jafama.FastMath;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import static gaiasky.scene.record.MaterialComponent.convertToComponent;

public class CloudComponent extends NamedComponent implements IMaterialProvider, IUpdatable<CloudComponent> {
    /**
     * Default texture parameters
     **/
    protected static final OwnTextureParameter textureParams;
    private static final Log logger = Logger.getLogger(CloudComponent.class);

    static {
        textureParams = new OwnTextureParameter();
        textureParams.genMipMaps = true;
        textureParams.magFilter = TextureFilter.Linear;
        textureParams.minFilter = TextureFilter.MipMapLinearNearest;
    }

    public int quality;
    public float size;
    public NoiseComponent nc;
    public ModelComponent mc;
    public Matrix4 localTransform;
    /**
     * RGB color of generated clouds
     **/
    public float[] color = new float[]{1f, 1f, 1f, 0.7f};
    public String diffuse, diffuseUnpacked;
    /**
     * The material component associated to the same model.
     **/
    public MaterialComponent materialComponent;
    // Cubemap.
    public CubemapComponent diffuseCubemap;
    // Virtual texture.
    public VirtualTextureComponent diffuseSvt;
    public Map<Object, Object> svtParams;
    // Model parameters
    public Map<String, Object> params;
    Vector3 aux;
    Vector3d aux3;
    private AssetManager manager;
    private final AtomicBoolean generated = new AtomicBoolean(false);
    private Texture cloudTex;
    private Material material;
    private boolean texInitialised, texLoading;

    public CloudComponent() {
        localTransform = new Matrix4();
        mc = new ModelComponent(false);
        mc.setBlendMode(BlendMode.COLOR);
        mc.initialize(null);
        aux = new Vector3();
        aux3 = new Vector3d();
    }

    public void initialize(String name, boolean force) {
        super.initialize(name);
        this.initialize(force);
    }

    private void initialize(boolean force) {
        if (!Settings.settings.scene.initialization.lazyTexture || force) {
            if (diffuse != null && !diffuse.endsWith(Constants.GEN_KEYWORD)) {
                // Add textures to load
                diffuseUnpacked = addToLoad(diffuse);
                if (diffuseUnpacked != null)
                    logger.info(I18n.msg("notif.loading", diffuseUnpacked));
            }
            if (diffuseCubemap != null)
                diffuseCubemap.initialize(manager);
            if (diffuseSvt != null)
                diffuseSvt.initialize("diffuseSvt", this, TextureAttribute.SvtIndirectionDiffuse);
        }
        this.generated.set(false);
    }

    public boolean isFinishedLoading(AssetManager manager) {
        return ComponentUtils.isLoaded(diffuseUnpacked, manager) && ComponentUtils.isLoaded(diffuseCubemap, manager);
    }

    /**
     * Adds the texture to load and unpacks any star (*) with the current
     * quality setting.
     */
    private String addToLoad(String tex) {
        if (tex == null)
            return null;
        tex = GlobalResources.unpackAssetPath(tex);
        AssetBean.addAsset(tex, Texture.class, textureParams);
        return tex;
    }

    public void doneLoading(AssetManager manager) {
        this.manager = manager;
        Pair<IntModel, Map<String, Material>> pair = ModelCache.cache.getModel("sphere", params, Bits.indices(Usage.Position, Usage.Normal, Usage.Tangent, Usage.BiNormal, Usage.TextureCoordinates), GL20.GL_TRIANGLES);
        IntModel cloudModel = pair.getFirst();
        Material material = pair.getSecond().get("base");
        material.clear();

        // CREATE CLOUD MODEL
        mc.instance = new IntModelInstance(cloudModel, this.localTransform);

        if (!Settings.settings.scene.initialization.lazyTexture) {
            initMaterial(null);
        }

        // Initialised
        texInitialised = !Settings.settings.scene.initialization.lazyTexture;
        // Loading
        texLoading = false;
    }

    public void touch(Model model) {
        if (Settings.settings.scene.initialization.lazyTexture && !texInitialised) {

            if (!texLoading) {
                initialize(true);
                // Set to loading
                texLoading = true;
            } else if (isFinishedLoading(manager)) {
                GaiaSky.postRunnable(() -> this.initMaterial(model));

                // Set to initialised
                texInitialised = true;
                texLoading = false;
            }
        }

    }

    public void update(Vector3b transform) {
        transform.setToTranslation(localTransform).scl(size);
    }

    /**
     * Updates the cull face strategy depending on the distance to the camera.
     *
     * @param distToCamera The distance to the camera in internal units.
     */
    public void updateCullFace(double distToCamera) {
        if (material != null) {
            if (distToCamera > size) {
                // Outside. Cull back faces.
                ((IntAttribute) Objects.requireNonNull(material.get(IntAttribute.CullFace))).value = GL20.GL_BACK;
            } else {
                // Inside. Do not cull faces.
                ((IntAttribute) Objects.requireNonNull(material.get(IntAttribute.CullFace))).value = GL20.GL_NONE;
            }
        }
    }

    public void initMaterial(Model model) {
        material = mc.instance.materials.first();

        if (diffuse != null && material.get(TextureAttribute.Diffuse) == null) {
            if (!diffuse.endsWith(Constants.GEN_KEYWORD)) {
                Texture tex = manager.get(diffuseUnpacked, Texture.class);
                material.set(new TextureAttribute(TextureAttribute.Diffuse, tex));
            } else {
                initializeGenCloudData(model);
            }
        }
        if (diffuseCubemap != null) {
            diffuseCubemap.prepareCubemap(manager);
            material.set(new CubemapAttribute(CubemapAttribute.DiffuseCubemap, diffuseCubemap.cubemap));
        }
        if (diffuseSvt != null && materialComponent != null && materialComponent.svts != null) {
            if (materialComponent.diffuseSvt != null) {
                // Use the ID of the main material.
                addSVTAttributes(material, diffuseSvt, materialComponent.diffuseSvt.id);
                materialComponent.svts.add(diffuseSvt);
            } else {
                // We have no diffuse SVT in the main material!
                int svtId = SVTManager.nextSvtId();
                addSVTAttributes(material, diffuseSvt, svtId);
                materialComponent.svts.add(diffuseSvt);
            }
            if (diffuseSvt.id > 0) {
                // Broadcast this material for SVT manager.
                EventManager.publish(Event.SVT_MATERIAL_INFO, this, diffuseSvt.id, this);
            }
        }
        material.set(new BlendingAttribute(1.0f));
        // Do not cull, only when below the clouds!
        material.set(new IntAttribute(IntAttribute.CullFace, GL20.GL_BACK));
    }

    private void addSVTAttributes(Material material, VirtualTextureComponent svt, int id) {
        svt.doneLoading(null);
        // Set ID.
        svt.id = id;
        // Set attributes.
        double svtResolution = svt.tileSize * FastMath.pow(2.0, svt.tree.depth);
        material.set(new Vector2Attribute(Vector2Attribute.SvtResolution, new Vector2((float) (svtResolution * svt.tree.root.length), (float) svtResolution)));
        material.set(new FloatAttribute(FloatAttribute.SvtTileSize, svt.tileSize));
        material.set(new FloatAttribute(FloatAttribute.SvtDepth, svt.tree.depth));
        material.set(new FloatAttribute(FloatAttribute.SvtId, svt.id));
        material.set(new FloatAttribute(FloatAttribute.SvtDetectionFactor, (float) Settings.settings.scene.renderer.virtualTextures.detectionBufferFactor));
    }

    public void setGenerated(boolean generated) {
        this.generated.set(generated);
    }

    private synchronized void initializeGenCloudData(Model model) {
        if (!generated.get()) {
            generated.set(true);
            GaiaSky.postRunnable(() -> {

                final int N = Settings.settings.graphics.proceduralGenerationResolution[0];
                final int M = Settings.settings.graphics.proceduralGenerationResolution[1];
                long start = TimeUtils.millis();
                logger.info(I18n.msg("gui.procedural.info.generate", I18n.msg("gui.procedural.cloud"), N, M));

                if (nc == null) {
                    nc = new NoiseComponent();
                    Random noiseRandom = new Random();
                    nc.randomizeAll(noiseRandom, noiseRandom.nextBoolean(), true);
                }
                FrameBuffer cloudFb = nc.generateNoise(N, M, color);
                // Write to disk if necessary
                if (Settings.settings.program.saveProceduralTextures) {
                    SysUtils.saveProceduralGLTexture(cloudFb.getColorBufferTexture(), this.name + "-cloud");
                }
                if (cloudFb != null) {
                    cloudTex = cloudFb.getColorBufferTexture();
                    material.set(new TextureAttribute(TextureAttribute.Diffuse, cloudTex));
                    // Add to material of main body as ambient occlusion.
                    if (model != null && model.model != null && model.model.mtc != null &&
                            GaiaSky.instance.sceneRenderer.visible.get(ComponentTypes.ComponentType.Clouds.ordinal())) {
                        // Add occlusion clouds attributes.
                        model.model.mtc.getMaterial().remove(OcclusionCloudsAttribute.Type);
                        model.model.mtc.getMaterial().remove(TextureAttribute.AO);
                        model.model.mtc.getMaterial().set(new TextureAttribute(TextureAttribute.AO, cloudTex));
                        model.model.mtc.getMaterial().set(new OcclusionCloudsAttribute(true));
                    }
                }
                long elapsed = TimeUtils.millis() - start;
                logger.info(I18n.msg("gui.procedural.info.done", I18n.msg("gui.procedural.cloud"), elapsed / 1000d));

            });
        }
    }

    public void disposeTexture(AssetManager manager, Material material, String name, String nameUnpacked, int texAttributeIndex, Texture tex) {
        if (name != null && manager != null && manager.isLoaded(nameUnpacked)) {
            unload(material, texAttributeIndex);
            manager.unload(nameUnpacked);
        }
        if (tex != null) {
            unload(material, texAttributeIndex);
            tex.dispose();
        }
    }

    public void disposeCubemap(AssetManager manager, Material mat, int attributeIndex, CubemapComponent cubemap) {
        if (cubemap != null && cubemap.isLoaded(manager)) {
            unload(material, attributeIndex);
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
        disposeTexture(manager, material, diffuse, diffuseUnpacked, TextureAttribute.Diffuse, cloudTex);
        disposeCubemap(manager, material, CubemapAttribute.DiffuseCubemap, diffuseCubemap);
        texLoading = false;
        texInitialised = false;
    }

    public void disposeNoiseBuffers() {
        if (nc != null) {
            nc.dispose();
        }
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

    public void removeAtmosphericScattering(Material mat) {
        mat.remove(AtmosphereAttribute.CameraHeight);
    }

    public void setQuality(Long quality) {
        this.quality = quality.intValue();
    }

    public void setSize(Double size) {
        this.size = (float) (size * Constants.KM_TO_U);
    }

    public void setMc(ModelComponent mc) {
        this.mc = mc;
    }

    public void setLocalTransform(Matrix4 localTransform) {
        this.localTransform = localTransform;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public void setCloud(String diffuse) {
        setDiffuse(diffuse);
    }

    public void setDiffuse(String diffuse) {
        this.diffuse = Settings.settings.data.dataFile(diffuse);
    }

    public void setNoise(NoiseComponent noise) {
        this.nc = noise;
    }

    public void setDiffuseCubemap(String diffuseCubemap) {
        this.diffuseCubemap = new CubemapComponent();
        this.diffuseCubemap.setLocation(diffuseCubemap);
    }

    public void setDiffuseSVT(VirtualTextureComponent virtualTextureComponent) {
        this.diffuseSvt = virtualTextureComponent;
    }

    public void setDiffuseSVT(Map<Object, Object> virtualTexture) {
        this.svtParams = virtualTexture;
        setDiffuseSVT(convertToComponent(virtualTexture));
    }

    @Override
    public Material getMaterial() {
        return material;
    }

    public boolean hasSVT() {
        return diffuseSvt != null;
    }

    /**
     * Creates a random cloud component using the given seed and the base
     * body size. Generates a random cloud texture.
     *
     * @param seed The seed to use.
     * @param size The body size in internal units.
     */
    public void randomizeAll(long seed, double size) {
        Random rand = new Random(seed);

        // Size
        double sizeKm = size * Constants.U_TO_KM;
        setSize(sizeKm + gaussian(rand, 30.0, 8.0, 12.0));
        // Cloud
        setDiffuse("generate");
        // Color
        if (rand.nextBoolean()) {
            // White.
            color[0] = 1f;
            color[1] = 1f;
            color[2] = 1f;
            color[3] = 0.8f;
        } else {
            // Gaussian around white-ish.
            color[0] = (float) MathUtils.clamp(rand.nextGaussian(0.95, 0.2), 0.0, 1.0);
            color[1] = (float) MathUtils.clamp(rand.nextGaussian(0.95, 0.2), 0.0, 1.0);
            color[2] = (float) MathUtils.clamp(rand.nextGaussian(0.95, 0.2), 0.0, 1.0);
            color[3] = (float) MathUtils.clamp(rand.nextGaussian(0.7, 0.2), 0.0, 1.0);
        }
        // Params
        setParams(createModelParameters(200L, 1.0, false));
        // Noise
        if (nc != null) {
            nc.dispose();
        }
        NoiseComponent nc = new NoiseComponent();
        nc.randomizeAll(rand, rand.nextBoolean(), true);
        setNoise(nc);
    }

    public void copyFrom(CloudComponent other) {
        this.size = other.size;
        this.diffuse = other.diffuse;
        this.params = other.params;
        this.diffuseCubemap = other.diffuseCubemap;
        this.diffuseSvt = other.diffuseSvt;
        this.svtParams = other.svtParams;
        this.nc = new NoiseComponent();
        if (other.color != null) {
            this.color = Arrays.copyOf(other.color, other.color.length);
        }
        if (other.nc != null)
            this.nc.copyFrom(other.nc);
        else
            this.nc.randomizeAll(new Random());
    }

    public void print(Log log) {
        log.debug("Size: " + size);
        log.debug("---Noise---");
        if (nc != null) {
            nc.print(log);
        }
    }

    @Override
    public void dispose() {
        disposeTextures(manager);
    }

    @Override
    public void updateWith(CloudComponent object) {
        // Random attributes.
        if (object.size > 0) {
            this.size = object.size;
        }
        // Regular texture.
        if (object.diffuse != null) {
            this.diffuse = object.diffuse;
        }
        // Cubemap.
        if (object.diffuseCubemap != null) {
            this.diffuseCubemap = object.diffuseCubemap;
        }
        // SVT.
        if (object.diffuseSvt != null) {
            this.diffuseSvt = object.diffuseSvt;
        }
        if (object.svtParams != null) {
            this.svtParams = object.svtParams;
        }
    }
}
