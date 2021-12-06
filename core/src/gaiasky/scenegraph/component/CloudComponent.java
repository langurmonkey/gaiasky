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
import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import gaiasky.GaiaSky;
import gaiasky.data.AssetBean;
import gaiasky.desktop.util.SysUtils;
import gaiasky.util.*;
import gaiasky.util.Logger.Log;
import gaiasky.util.gdx.model.IntModel;
import gaiasky.util.gdx.model.IntModelInstance;
import gaiasky.util.gdx.shader.AtmosphereAttribute;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;

import java.util.Map;
import java.util.Random;

public class CloudComponent extends NamedComponent {
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

    private Texture cloudTex;

    public String cloud, cloudtrans, cloudUnpacked, cloudtransUnpacked;
    private Material material;

    private boolean texInitialised, texLoading;
    // Model parameters
    public Map<String, Object> params;

    Vector3 aux;
    Vector3d aux3;

    public CloudComponent() {
        localTransform = new Matrix4();
        mc = new ModelComponent(false);
        mc.initialize(null, 0L);
        aux = new Vector3();
        aux3 = new Vector3d();
    }

    public void initialize(String name, Long id, boolean force) {
        super.initialize(name, id);
        this.initialize(force);
    }

    private void initialize(boolean force) {
        if (!Settings.settings.scene.initialization.lazyTexture || force) {
            if (cloud != null && !cloud.endsWith(Constants.GEN_KEYWORD)) {
                // Add textures to load
                cloudUnpacked = addToLoad(cloud);
                cloudtransUnpacked = addToLoad(cloudtrans);
            }
        }
    }

    public boolean isFinishedLoading(AssetManager manager) {
        return isFL(cloudUnpacked, manager) && isFL(cloudtransUnpacked, manager);
    }

    public boolean isFL(String tex, AssetManager manager) {
        if (tex == null)
            return true;
        return manager.isLoaded(tex);
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
        Pair<IntModel, Map<String, Material>> pair = ModelCache.cache.getModel("sphere", params, Usage.Position | Usage.Normal | Usage.Tangent | Usage.BiNormal | Usage.TextureCoordinates, GL20.GL_TRIANGLES);
        IntModel cloudModel = pair.getFirst();
        Material material = pair.getSecond().get("base");
        material.clear();

        // CREATE CLOUD MODEL
        mc.instance = new IntModelInstance(cloudModel, this.localTransform);

        if (!Settings.settings.scene.initialization.lazyTexture)
            initMaterial();

        // Initialised
        texInitialised = !Settings.settings.scene.initialization.lazyTexture;
        // Loading
        texLoading = false;
    }

    public void touch() {
        if (Settings.settings.scene.initialization.lazyTexture && !texInitialised) {

            if (!texLoading) {
                initialize(true);
                if (cloud != null)
                    logger.info(I18n.txt("notif.loading", cloudUnpacked));
                if (cloudtrans != null)
                    logger.info(I18n.txt("notif.loading", cloudtransUnpacked));
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
        material.set(new BlendingAttribute(1.0f));
        // Do not cull
        material.set(new IntAttribute(IntAttribute.CullFace, 0));
    }

    private void initializeGenCloudData() {

        Thread t = new Thread(() -> {
            final int N = Settings.settings.graphics.quality.texWidthTarget;
            final int M = Settings.settings.graphics.quality.texHeightTarget;

            Pixmap cloudPixmap = nc.generateData(N, M);
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
            });
        });
        t.start();
    }

    public void disposeTexture(AssetManager manager, Material material, String name, String nameUnpacked, long texAttribute, Texture tex) {
        if (name != null && manager != null && manager.isLoaded(nameUnpacked)) {
            unload(material, texAttribute);
            manager.unload(nameUnpacked);
        }
        if (tex != null) {
            unload(material, texAttribute);
            tex.dispose();
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
        texLoading = false;
        texInitialised = false;
    }

    private void unload(Material mat, long attrMask) {
        if (mat != null) {
            Attribute attr = mat.get(attrMask);
            mat.remove(attrMask);
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
        setSize(sizeKm + gaussian(rand, 20.0, 5.0, 10.0));
        // Cloud
        setCloud("generate");
        // Params
        setParams(createModelParameters(200L, 1.0, false));
        // Noise
        NoiseComponent nc = new NoiseComponent();
        nc.randomizeAll(rand);
        setNoise(nc);
    }

    public void copyFrom(CloudComponent other) {
        this.size = other.size;
        this.cloud = other.cloud;
        this.params = other.params;
        this.nc = new NoiseComponent();
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
}
