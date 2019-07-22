/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.scenegraph.component;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.TextureLoader.TextureParameter;
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
import gaia.cu9.ari.gaiaorbit.data.AssetBean;
import gaia.cu9.ari.gaiaorbit.util.*;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.gdx.model.IntModel;
import gaia.cu9.ari.gaiaorbit.util.gdx.model.IntModelInstance;
import gaia.cu9.ari.gaiaorbit.util.gdx.shader.AtmosphereAttribute;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;

import java.util.Map;

public class CloudComponent {
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
    public ModelComponent mc;
    public Matrix4 localTransform;

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
        mc.initialize();
        aux = new Vector3();
        aux3 = new Vector3d();
    }

    public void initialize(boolean force) {
        if (!GlobalConf.scene.LAZY_TEXTURE_INIT || force) {
            // Add textures to load
            cloudUnpacked = addToLoad(cloud);
            cloudtransUnpacked = addToLoad(cloudtrans);
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
     *
     * @param tex
     */
    private String addToLoad(String tex) {
        if (tex == null)
            return null;
        tex = GlobalResources.unpackTexName(tex);
        AssetBean.addAsset(tex, Texture.class, textureParams);
        return tex;
    }

    public void doneLoading(AssetManager manager) {
        this.manager = manager;
        Pair<IntModel, Map<String, Material>> pair = ModelCache.cache.getModel("sphere", params, Usage.Position | Usage.Normal | Usage.Tangent | Usage.BiNormal | Usage.TextureCoordinates);
        IntModel cloudModel = pair.getFirst();
        Material material = pair.getSecond().get("base");
        material.clear();

        // CREATE CLOUD MODEL
        mc.instance = new IntModelInstance(cloudModel, this.localTransform);

        if (!GlobalConf.scene.LAZY_TEXTURE_INIT)
            initMaterial();

        // Initialised
        texInitialised = !GlobalConf.scene.LAZY_TEXTURE_INIT;
        // Loading
        texLoading = false;
    }

    public void touch() {
        if (GlobalConf.scene.LAZY_TEXTURE_INIT && !texInitialised) {

            if (!texLoading) {
                if (cloud != null)
                    logger.info(I18n.bundle.format("notif.loading", cloud));
                if (cloudtrans != null)
                    logger.info(I18n.bundle.format("notif.loading", cloudtrans));
                initialize(true);
                // Set to loading
                texLoading = true;
            } else if (isFinishedLoading(manager)) {
                Gdx.app.postRunnable(() -> initMaterial());

                // Set to initialised
                texInitialised = true;
                texLoading = false;
            }
        }

    }

    public void update(Vector3d transform) {
        transform.getMatrix(localTransform).scl(size);
    }

    public void initMaterial() {
        material = mc.instance.materials.first();
        if (cloud != null && manager.isLoaded(cloudUnpacked)) {
            Texture tex = manager.get(cloudUnpacked, Texture.class);
            material.set(new TextureAttribute(TextureAttribute.Diffuse, tex));
        }
        if (cloudtrans != null && manager.isLoaded(cloudtransUnpacked)) {
            Texture tex = manager.get(cloudtransUnpacked, Texture.class);
            material.set(new TextureAttribute(TextureAttribute.Normal, tex));
        }
        material.set(new BlendingAttribute(1.0f));
        // Do not cull
        material.set(new IntAttribute(IntAttribute.CullFace, 0));
    }

    /**
     * Disposes and unloads all currently loaded textures immediately
     *
     * @param manager The asset manager
     **/
    public void disposeTextures(AssetManager manager) {
        if (cloud != null && manager.isLoaded(cloudUnpacked)) {
            manager.unload(cloudUnpacked);
            cloudUnpacked = null;
            unload(material, TextureAttribute.Diffuse);
        }
        if (cloudtrans != null && manager.isLoaded(cloudtransUnpacked)) {
            manager.unload(cloudtransUnpacked);
            cloudtransUnpacked = null;
            unload(material, TextureAttribute.Normal);
        }
        texLoading = false;
        texInitialised = false;
    }

    private void unload(Material mat, long attrMask) {
        if (mat != null) {
            Attribute attr = mat.get(attrMask);
            mat.remove(attrMask);
            if (attr != null && attr instanceof TextureAttribute) {
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
        this.cloud = GlobalConf.data.dataFile(cloud);
    }

    public void setCloudtrans(String cloudtrans) {
        this.cloudtrans = cloudtrans;
    }

}
