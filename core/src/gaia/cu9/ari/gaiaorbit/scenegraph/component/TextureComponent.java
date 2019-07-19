/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.scenegraph.component;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.TextureLoader.TextureParameter;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.math.Vector2;
import gaia.cu9.ari.gaiaorbit.data.AssetBean;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf.SceneConf.ElevationType;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;
import gaia.cu9.ari.gaiaorbit.util.gdx.model.IntModelInstance;
import gaia.cu9.ari.gaiaorbit.util.gdx.shader.FloatExtAttribute;
import gaia.cu9.ari.gaiaorbit.util.gdx.shader.TextureExtAttribute;
import gaia.cu9.ari.gaiaorbit.util.gdx.shader.Vector2Attribute;

import java.util.Map;

/**
 * A basic component that contains the info on the textures.
 *
 * @author Toni Sagrista
 */
public class TextureComponent implements IObserver {
    /** Generated height keyword **/
    private static final String GEN_HEIGHT_KEYWORD = "generate";
    /** Default texture parameters **/
    protected static final TextureParameter textureParamsMipMap, textureParams;


    static {
        textureParamsMipMap = new TextureParameter();
        textureParamsMipMap.genMipMaps = true;
        textureParamsMipMap.magFilter = TextureFilter.Linear;
        textureParamsMipMap.minFilter = TextureFilter.MipMapLinearLinear;

        textureParams = new TextureParameter();
        textureParams.genMipMaps = false;
        textureParams.magFilter = TextureFilter.Linear;
        textureParams.minFilter = TextureFilter.Linear;
    }

    public String base, specular, normal, night, ring, height, ringnormal;
    public String baseUnpacked, specularUnpacked, normalUnpacked, nightUnpacked, ringUnpacked, heightUnpacked, ringnormalUnpacked;
    public Texture baseTex, heightTex;
    // Height scale in internal units
    public Float heightScale = 0.005f;
    public Float heightNoiseSize = 10f;
    public Vector2 heightSize = new Vector2();
    public float[][] heightMap;

    private Material material;

    /** Add also color even if texture is present **/
    public boolean coloriftex = false;

    public TextureComponent() {
        super();
        EventManager.instance.subscribe(this, Events.ELEVATION_TYPE_CMD, Events.ELEVATION_MUTLIPLIER_CMD, Events.TESSELLATION_QUALITY_CMD);
    }

    public void initialize(AssetManager manager) {
        // Add textures to load
        baseUnpacked = addToLoad(base, textureParamsMipMap, manager);
        normalUnpacked = addToLoad(normal, textureParams, manager);
        specularUnpacked = addToLoad(specular, textureParamsMipMap, manager);
        nightUnpacked = addToLoad(night, textureParamsMipMap, manager);
        ringUnpacked = addToLoad(ring, textureParamsMipMap, manager);
        ringnormalUnpacked = addToLoad(ringnormal, textureParamsMipMap, manager);
        if (height != null && !height.endsWith(GEN_HEIGHT_KEYWORD))
            heightUnpacked = addToLoad(height, textureParamsMipMap, manager);
    }

    public void initialize() {
        // Add textures to load
        baseUnpacked = addToLoad(base, textureParamsMipMap);
        normalUnpacked = addToLoad(normal, textureParams);
        specularUnpacked = addToLoad(specular, textureParamsMipMap);
        nightUnpacked = addToLoad(night, textureParamsMipMap);
        ringUnpacked = addToLoad(ring, textureParamsMipMap);
        ringnormalUnpacked = addToLoad(ringnormal, textureParamsMipMap);
        if (height != null && !height.endsWith(GEN_HEIGHT_KEYWORD))
            heightUnpacked = addToLoad(height, textureParamsMipMap);
    }

    public boolean isFinishedLoading(AssetManager manager) {
        return isFL(baseUnpacked, manager) && isFL(normalUnpacked, manager) && isFL(specularUnpacked, manager) && isFL(nightUnpacked, manager) && isFL(ringUnpacked, manager) && isFL(ringnormalUnpacked, manager) && isFL(heightUnpacked, manager);
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
     * @return The actual loaded texture path
     */
    private String addToLoad(String tex, TextureParameter texParams, AssetManager manager) {
        if (tex == null)
            return null;

        tex = GlobalResources.unpackTexName(tex);
        manager.load(tex, Texture.class, texParams);

        return tex;
    }

    /**
     * Adds the texture to load and unpacks any star (*) with the current
     * quality setting.
     *
     * @param tex
     * @return The actual loaded texture path
     */
    private String addToLoad(String tex, TextureParameter texParams) {
        if (tex == null)
            return null;

        tex = GlobalResources.unpackTexName(tex);
        AssetBean.addAsset(tex, Texture.class, texParams);

        return tex;
    }

    public Material initMaterial(AssetManager manager, IntModelInstance instance, float[] cc, boolean culling) {
        this.material = instance.materials.get(0);
        if (base != null) {
            baseTex = manager.get(baseUnpacked, Texture.class);
            material.set(new TextureAttribute(TextureAttribute.Diffuse, baseTex));
        }
        if (cc != null && (coloriftex || base == null)) {
            // Add diffuse colour
            material.set(new ColorAttribute(ColorAttribute.Diffuse, cc[0], cc[1], cc[2], cc[3]));
        }

        if (normal != null) {
            Texture tex = manager.get(normalUnpacked, Texture.class);
            material.set(new TextureAttribute(TextureAttribute.Normal, tex));
        }
        if (specular != null) {
            Texture tex = manager.get(specularUnpacked, Texture.class);
            material.set(new TextureAttribute(TextureAttribute.Specular, tex));
            // Control amount of specularity
            material.set(new ColorAttribute(ColorAttribute.Specular, 0.5f, 0.5f, 0.5f, 1f));
        }
        if (night != null) {
            Texture tex = manager.get(nightUnpacked, Texture.class);
            material.set(new TextureExtAttribute(TextureExtAttribute.Night, tex));
        }
        if (height != null) {
            if (!height.endsWith(GEN_HEIGHT_KEYWORD)) {
                heightTex = manager.get(heightUnpacked, Texture.class);
                if (!GlobalConf.scene.ELEVATION_TYPE.isNone()) {
                    initializeElevationData();
                }
            } else {
                initializeGenElevationData();
            }
        }
        if (instance.materials.size > 1) {
            // Ring material
            Material ringMat = instance.materials.get(1);
            if (ring != null) {
                ringMat.set(new TextureAttribute(TextureAttribute.Diffuse, manager.get(ringUnpacked, Texture.class)));
            }
            if (ringnormal != null) {
                ringMat.set(new TextureAttribute(TextureAttribute.Normal, manager.get(ringnormalUnpacked, Texture.class)));
            }
            ringMat.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA));
            if (!culling)
                ringMat.set(new IntAttribute(IntAttribute.CullFace, GL20.GL_NONE));
        }
        if (!culling) {
            material.set(new IntAttribute(IntAttribute.CullFace, GL20.GL_NONE));
        }

        return material;
    }

    private void initializeGenElevationData() {
        material.set(new FloatExtAttribute(FloatExtAttribute.HeightScale, heightScale * (float) GlobalConf.scene.ELEVATION_MULTIPLIER));
        material.set(new Vector2Attribute(Vector2Attribute.HeightSize, new Vector2(-1, -1)));
        material.set(new FloatExtAttribute(FloatExtAttribute.HeightNoiseSize, heightNoiseSize));
        material.set(new FloatExtAttribute(FloatExtAttribute.TessQuality, (float) GlobalConf.scene.TESSELLATION_QUALITY));
    }

    private void initializeElevationData() {
        // Get height data for CPU (camera) use
        Pixmap heightPixmap = new Pixmap(new FileHandle(GlobalResources.unpackTexName(height)));
        heightMap = new float[heightPixmap.getWidth()][heightPixmap.getHeight()];
        for (int i = 0; i < heightPixmap.getWidth(); i++) {
            for (int j = 0; j < heightPixmap.getHeight(); j++) {
                Color col = new Color(heightPixmap.getPixel(i, j));
                heightMap[i][j] = (1f - col.r) * heightScale;
            }
        }
        heightSize.set(heightTex.getWidth(), heightTex.getHeight());
        material.set(new TextureExtAttribute(TextureExtAttribute.Height, heightTex));
        material.set(new FloatExtAttribute(FloatExtAttribute.HeightScale, heightScale * (float) GlobalConf.scene.ELEVATION_MULTIPLIER));
        material.set(new Vector2Attribute(Vector2Attribute.HeightSize, heightSize));
        material.set(new FloatExtAttribute(FloatExtAttribute.TessQuality, (float) GlobalConf.scene.TESSELLATION_QUALITY));
    }

    private void removeElevationData() {
        heightMap = null;
        material.remove(TextureExtAttribute.Height);
        material.remove(FloatExtAttribute.HeightScale);
        material.remove(Vector2Attribute.HeightSize);
        material.remove(FloatExtAttribute.HeightNoiseSize);
        material.remove(FloatExtAttribute.TessQuality);
    }

    /**
     * Initialises the materials by binding the necessary textures to them.
     *
     * @param manager   The asset manager.
     * @param materials A map with at least one material under the key "base".
     * @param cc        Plain color used if there is no texture.
     * @param culling   Whether to cull the back-facing faces or not.
     */
    public void initMaterial(AssetManager manager, Map<String, Material> materials, float[] cc, boolean culling) {
        Material material = materials.get("base");
        if (base != null) {
            baseTex = manager.get(baseUnpacked, Texture.class);
            material.set(new TextureAttribute(TextureAttribute.Diffuse, baseTex));
        }
        if (cc != null && (coloriftex || base == null)) {
            // Add diffuse colour
            material.set(new ColorAttribute(ColorAttribute.Diffuse, cc[0], cc[1], cc[2], cc[3]));
        }

        if (normal != null) {
            Texture tex = manager.get(normalUnpacked, Texture.class);
            material.set(new TextureAttribute(TextureAttribute.Normal, tex));
        }
        if (specular != null) {
            Texture tex = manager.get(specularUnpacked, Texture.class);
            material.set(new TextureAttribute(TextureAttribute.Specular, tex));
            // Control amount of specularity
            material.set(new ColorAttribute(ColorAttribute.Specular, 0.5f, 0.5f, 0.5f, 1f));
        }
        if (night != null) {
            Texture tex = manager.get(nightUnpacked, Texture.class);
            material.set(new TextureExtAttribute(TextureExtAttribute.Night, tex));
        }
        if (height != null) {
            if (!height.endsWith(GEN_HEIGHT_KEYWORD)) {
                heightTex = manager.get(heightUnpacked, Texture.class);
                if (!GlobalConf.scene.ELEVATION_TYPE.isNone()) {
                    initializeElevationData();
                }
            } else {
                initializeGenElevationData();
            }
        }
        if (materials.containsKey("ring")) {
            // Ring material
            Material ringMat = materials.get("ring");
            Texture tex = manager.get(ringUnpacked, Texture.class);
            ringMat.set(new TextureAttribute(TextureAttribute.Diffuse, tex));
            ringMat.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA));
            if (!culling)
                ringMat.set(new IntAttribute(IntAttribute.CullFace, GL20.GL_NONE));
        }
        if (!culling) {
            material.set(new IntAttribute(IntAttribute.CullFace, GL20.GL_NONE));
        }
    }

    public void setBase(String base) {
        this.base = GlobalConf.data.dataFile(base);
    }

    public void setSpecular(String specular) {
        this.specular = GlobalConf.data.dataFile(specular);
    }

    public void setNormal(String normal) {
        this.normal = GlobalConf.data.dataFile(normal);
    }

    public void setNight(String night) {
        this.night = GlobalConf.data.dataFile(night);
    }

    public void setRing(String ring) {
        this.ring = GlobalConf.data.dataFile(ring);
    }

    public void setRingnormal(String ringnormal) {
        this.ringnormal = GlobalConf.data.dataFile(ringnormal);
    }

    public void setHeight(String height) {
        this.height = GlobalConf.data.dataFile(height);
    }

    public void setHeightScale(Double heightScale) {
        this.heightScale = (float) (heightScale * Constants.KM_TO_U);
    }

    /**
     * Only if height is {@link #GEN_HEIGHT_KEYWORD}
     * @param heightNoiseSize Size of the sampling area
     */
    public void setHeightNoiseSize(Double heightNoiseSize) {
        this.heightNoiseSize = heightNoiseSize.floatValue();
    }

    public void setColoriftex(Boolean coloriftex) {
        this.coloriftex = coloriftex;
    }

    public boolean hasHeight() {
        return this.height != null && !this.height.isEmpty();
    }

    /** Disposes all currently loaded textures **/
    public void disposeTextures(AssetManager manager) {
        if (base != null && manager.containsAsset(baseUnpacked)) {
            manager.unload(baseUnpacked);
            baseUnpacked = null;
        }
        if (normal != null && manager.containsAsset(normalUnpacked)) {
            manager.unload(normalUnpacked);
            normalUnpacked = null;
        }
        if (specular != null && manager.containsAsset(specularUnpacked)) {
            manager.unload(specularUnpacked);
            specularUnpacked = null;
        }
        if (night != null && manager.containsAsset(nightUnpacked)) {
            manager.unload(nightUnpacked);
            nightUnpacked = null;
        }
        if (ring != null && manager.containsAsset(ringUnpacked)) {
            manager.unload(ringUnpacked);
            ringUnpacked = null;
        }
        if (ringnormal != null && manager.containsAsset(ringnormalUnpacked)) {
            manager.unload(ringnormalUnpacked);
            ringnormalUnpacked = null;
        }
        if (height != null && heightUnpacked != null && manager.containsAsset(heightUnpacked)) {
            manager.unload(heightUnpacked);
            heightUnpacked = null;
            heightMap = null;
        }
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case ELEVATION_TYPE_CMD:
            if (this.hasHeight() && this.material != null) {
                ElevationType newType = (ElevationType) data[0];
                Gdx.app.postRunnable(() -> {
                    if (newType.isNone()) {
                        removeElevationData();
                    } else {
                        if (heightMap == null) {
                            if (height.endsWith(GEN_HEIGHT_KEYWORD))
                                initializeGenElevationData();
                            else
                                initializeElevationData();
                        }
                    }
                });
            }
            break;
        case ELEVATION_MUTLIPLIER_CMD:
            if (this.hasHeight() && this.material != null) {
                float newMultiplier = (Float) data[0];
                Gdx.app.postRunnable(() -> this.material.set(new FloatExtAttribute(FloatExtAttribute.HeightScale, heightScale * newMultiplier)));
            }
            break;
        case TESSELLATION_QUALITY_CMD:
            if (this.hasHeight() && this.material != null) {
                float newQuality = (Float) data[0];
                Gdx.app.postRunnable(() -> this.material.set(new FloatExtAttribute(FloatExtAttribute.TessQuality, newQuality)));
            }
            break;
        default:
            break;
        }
    }
}
