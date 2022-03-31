/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph.component;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.TextureLoader.TextureParameter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.TimeUtils;
import gaiasky.GaiaSky;
import gaiasky.data.AssetBean;
import gaiasky.desktop.util.SysUtils;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.util.*;
import gaiasky.util.Logger.Log;
import gaiasky.util.gdx.model.IntModel;
import gaiasky.util.gdx.model.IntModelInstance;
import gaiasky.util.gdx.shader.Material;
import gaiasky.util.gdx.shader.attribute.*;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class CloudComponent extends NamedComponent implements IObserver {
    private static final Log logger = Logger.getLogger(CloudComponent.class);

    /** Default texture parameters **/
    protected static final TextureParameter textureParams;

    static {
        textureParams = new TextureParameter();
        textureParams.genMipMaps = true;
        textureParams.magFilter = TextureFilter.Linear;
        textureParams.minFilter = TextureFilter.MipMapLinearNearest;
    }

    private AssetManager manager;
    public int quality;
    public float size;
    public NoiseComponent nc;
    public ModelComponent mc;
    public Matrix4 localTransform;

    /** RGB color of generated clouds **/
    public float[] color = new float[] { 1f, 1f, 1f, 0.7f };

    private AtomicBoolean generated = new AtomicBoolean(false);
    private Texture cloudTex;

    public String cloud, cloudtrans, cloudUnpacked, cloudtransUnpacked;

    private Material material;

    // CUBEMAPS
    public CubemapComponent diffuseCubemap;

    private boolean texInitialised, texLoading;
    // Model parameters
    public Map<String, Object> params;

    Vector3 aux;
    Vector3d aux3;

    public CloudComponent() {
        localTransform = new Matrix4();
        mc = new ModelComponent(false);
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
            if (cloud != null && !cloud.endsWith(Constants.GEN_KEYWORD)) {
                // Add textures to load
                cloudUnpacked = addToLoad(cloud);
                cloudtransUnpacked = addToLoad(cloudtrans);
                if (cloudUnpacked != null)
                    logger.info(I18n.txt("notif.loading", cloudUnpacked));
                if (cloudtransUnpacked != null)
                    logger.info(I18n.txt("notif.loading", cloudtransUnpacked));
            }
            if (diffuseCubemap != null)
                diffuseCubemap.initialize(manager);
        }
        this.generated.set(false);
    }

    public boolean isFinishedLoading(AssetManager manager) {
        return isFL(cloudUnpacked, manager) && isFL(cloudtransUnpacked, manager) && isFL(diffuseCubemap, manager);
    }

    public boolean isFL(String tex, AssetManager manager) {
        if (tex == null)
            return true;
        return manager.isLoaded(tex);
    }

    public boolean isFL(CubemapComponent cubemap, AssetManager manager) {
        return cubemap == null || cubemap.isLoaded(manager);
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
        Pair<IntModel, Map<String, Material>> pair = ModelCache.cache.getModel("sphere", params, Bits.indexes(Usage.Position, Usage.Normal, Usage.Tangent, Usage.BiNormal, Usage.TextureCoordinates), GL20.GL_TRIANGLES);
        IntModel cloudModel = pair.getFirst();
        Material material = pair.getSecond().get("base");
        material.clear();

        // CREATE CLOUD MODEL
        mc.instance = new IntModelInstance(cloudModel, this.localTransform);

        if (!Settings.settings.scene.initialization.lazyTexture)
            initMaterial();

        // Subscribe to new graphics quality setting event
        EventManager.instance.subscribe(this, Event.GRAPHICS_QUALITY_UPDATED);

        // Initialised
        texInitialised = !Settings.settings.scene.initialization.lazyTexture;
        // Loading
        texLoading = false;
    }

    public void touch() {
        if (Settings.settings.scene.initialization.lazyTexture && !texInitialised) {

            if (!texLoading) {
                initialize(true);
                // Set to loading
                texLoading = true;
            } else if (isFinishedLoading(manager)) {
                GaiaSky.postRunnable(this::initMaterial);

                // Set to initialised
                texInitialised = true;
                texLoading = false;
            }
        }

    }

    public void update(Vector3b transform) {
        transform.getMatrix(localTransform).scl(size);
    }

    public void initMaterial() {
        material = mc.instance.materials.first();

        if (cloud != null && material.get(TextureAttribute.Diffuse) == null) {
            if (!cloud.endsWith(Constants.GEN_KEYWORD)) {
                Texture tex = manager.get(cloudUnpacked, Texture.class);
                material.set(new TextureAttribute(TextureAttribute.Diffuse, tex));
            } else {
                initializeGenCloudData();
            }
        }
        if (cloudtrans != null && manager.isLoaded(cloudtransUnpacked)) {
            Texture tex = manager.get(cloudtransUnpacked, Texture.class);
            material.set(new TextureAttribute(TextureAttribute.Normal, tex));
        }
        if (diffuseCubemap != null) {
            diffuseCubemap.prepareCubemap(manager);
            material.set(new CubemapAttribute(CubemapAttribute.DiffuseCubemap, diffuseCubemap.cubemap));
        }
        material.set(new BlendingAttribute(1.0f));
        // Do not cull
        material.set(new IntAttribute(IntAttribute.CullFace, 0));
    }

    public void setGenerated(boolean generated) {
        this.generated.set(generated);
    }

    private synchronized void initializeGenCloudData() {
        if (!generated.get()) {
            generated.set(true);
            GaiaSky.instance.getExecutorService().execute(() -> {
                // Begin
                EventManager.publish(Event.PROCEDURAL_GENERATION_CLOUD_INFO, this, true);

                final int N = Settings.settings.graphics.quality.texWidthTarget;
                final int M = Settings.settings.graphics.quality.texHeightTarget;
                long start = TimeUtils.millis();
                GaiaSky.postRunnable(() -> logger.info(I18n.txt("gui.procedural.info.generate", I18n.txt("gui.procedural.cloud"), N, M)));

                if (nc == null) {
                    nc = new NoiseComponent();
                    Random noiseRandom = new Random();
                    nc.randomizeAll(noiseRandom, noiseRandom.nextBoolean(), true);
                }
                Pixmap cloudPixmap = nc.generateData(N, M, color, I18n.txt("gui.procedural.progress", I18n.txt("gui.procedural.cloud"), name));
                // Write to disk if necessary
                if (Settings.settings.program.saveProceduralTextures) {
                    SysUtils.saveProceduralPixmap(cloudPixmap, this.name + "-cloud");
                }
                GaiaSky.postRunnable(() -> {
                    if (cloudPixmap != null) {
                        cloudTex = new Texture(cloudPixmap, true);
                        cloudTex.setFilter(TextureFilter.MipMapLinearLinear, TextureFilter.Linear);
                        material.set(new TextureAttribute(TextureAttribute.Diffuse, cloudTex));
                    }
                    long elapsed = TimeUtils.millis() - start;
                    logger.info(I18n.txt("gui.procedural.info.done", I18n.txt("gui.procedural.cloud"), elapsed / 1000d));
                });

                // End
                EventManager.publish(Event.PROCEDURAL_GENERATION_CLOUD_INFO, this, false);
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
        disposeTexture(manager, material, cloud, cloudUnpacked, TextureAttribute.Diffuse, cloudTex);
        disposeTexture(manager, material, cloudtrans, cloudtransUnpacked, TextureAttribute.Normal, null);
        disposeCubemap(manager, material, CubemapAttribute.DiffuseCubemap, diffuseCubemap);
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

    public void setCloud(String cloud) {
        this.cloud = Settings.settings.data.dataFile(cloud);
    }

    public void setCloudtrans(String cloudtrans) {
        this.cloudtrans = cloudtrans;
    }

    public void setNoise(NoiseComponent noise) {
        this.nc = noise;
    }

    public void setDiffuseCubemap(String diffuseCubemap) {
        this.diffuseCubemap = new CubemapComponent();
        this.diffuseCubemap.setLocation(diffuseCubemap);
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        if (event == Event.GRAPHICS_QUALITY_UPDATED) {
            GaiaSky.postRunnable(() -> {
                if (texInitialised) {
                    // Remove current textures
                    this.disposeTextures(this.manager);
                    // Set generated status to false
                    this.generated.set(false);
                }
            });
        }
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
        setCloud("generate");
        // Color
        if (rand.nextBoolean()) {
            // White
            color[0] = 1f;
            color[1] = 1f;
            color[2] = 1f;
            color[3] = 0.7f;
        } else {
            // Random
            color[0] = rand.nextFloat();
            color[1] = rand.nextFloat();
            color[2] = rand.nextFloat();
            color[3] = rand.nextFloat();
        }
        // Params
        setParams(createModelParameters(200L, 1.0, false));
        // Noise
        NoiseComponent nc = new NoiseComponent();
        nc.randomizeAll(rand, rand.nextBoolean(), true);
        setNoise(nc);
    }

    public void copyFrom(CloudComponent other) {
        this.size = other.size;
        this.cloud = other.cloud;
        this.params = other.params;
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
        EventManager.instance.removeAllSubscriptions(this);
    }
}
