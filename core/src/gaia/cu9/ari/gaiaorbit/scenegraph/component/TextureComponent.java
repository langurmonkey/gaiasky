/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.scenegraph.component;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.TextureLoader.TextureParameter;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.math.Vector2;
import gaia.cu9.ari.gaiaorbit.data.AssetBean;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
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
 *
 */
public class TextureComponent {
    /** Default texture parameters **/
    protected static final TextureParameter textureParams;
    static {
        textureParams = new TextureParameter();
        textureParams.genMipMaps = true;
        textureParams.magFilter = TextureFilter.Linear;
        textureParams.minFilter = TextureFilter.MipMapLinearLinear;
    }

    public String base, specular, normal, night, ring, height;
    public String baseUnpacked, specularUnpacked, normalUnpacked, nightUnpacked, ringUnpacked, heightUnpacked;
    public Texture baseTex;
    // Height scale in internal units
    public Float heightScale = 0.005f;
    public Vector2 heightSize = new Vector2();
    public float[][] heightMap;

    /** Add also color even if texture is present **/
    public boolean coloriftex = false;

    public TextureComponent() {

    }

    public void initialize(AssetManager manager) {
        // Add textures to load
        baseUnpacked = addToLoad(base, manager);
        normalUnpacked = addToLoad(normal, manager);
        specularUnpacked = addToLoad(specular, manager);
        nightUnpacked = addToLoad(night, manager);
        ringUnpacked = addToLoad(ring, manager);
        heightUnpacked = addToLoad(height, manager);
    }

    public void initialize() {
        // Add textures to load
        baseUnpacked = addToLoad(base);
        normalUnpacked = addToLoad(normal);
        specularUnpacked = addToLoad(specular);
        nightUnpacked = addToLoad(night);
        ringUnpacked = addToLoad(ring);
        heightUnpacked = addToLoad(height);
    }

    public boolean isFinishedLoading(AssetManager manager) {
        return isFL(baseUnpacked, manager) && isFL(normalUnpacked, manager) && isFL(specularUnpacked, manager) && isFL(nightUnpacked, manager) && isFL(ringUnpacked, manager) && isFL(heightUnpacked, manager);
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
    private String addToLoad(String tex, AssetManager manager) {
        if (tex == null)
            return null;

        tex = GlobalResources.unpackTexName(tex);
        manager.load(tex, Texture.class, textureParams);

        return tex;
    }

    /**
     * Adds the texture to load and unpacks any star (*) with the current
     * quality setting.
     * 
     * @param tex
     * @return The actual loaded texture path
     */
    private String addToLoad(String tex) {
        if (tex == null)
            return null;

        tex = GlobalResources.unpackTexName(tex);
        AssetBean.addAsset(tex, Texture.class, textureParams);

        return tex;
    }


    public Material initMaterial(AssetManager manager, IntModelInstance instance, float[] cc, boolean culling) {
        Material material = instance.materials.get(0);
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
        if(height != null) {
            Texture tex = manager.get(heightUnpacked, Texture.class);
            // Get height data for CPU (camera) use
            Pixmap heightPixmap = new Pixmap(new FileHandle(GlobalResources.unpackTexName(height)));
            heightMap = new float[heightPixmap.getWidth()][heightPixmap.getHeight()];
            for(int i = 0; i < heightPixmap.getWidth(); i ++){
                for(int j = 0; j < heightPixmap.getHeight(); j++){
                    Color col = new Color(heightPixmap.getPixel(i, j));
                    heightMap[i][j] = (1f - col.r) * heightScale;
                }
            }
            heightSize.set(tex.getWidth(), tex.getHeight());
            material.set(new TextureExtAttribute(TextureExtAttribute.Height, tex));
            material.set(new FloatExtAttribute(FloatExtAttribute.HeightScale, heightScale));
            material.set(new Vector2Attribute(Vector2Attribute.HeightSize, heightSize));
        }
        if (instance.materials.size > 1) {
            // Ring material
            Material ringMat = instance.materials.get(1);
            Texture tex = manager.get(ringUnpacked, Texture.class);
            ringMat.set(new TextureAttribute(TextureAttribute.Diffuse, tex));
            ringMat.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA));
            if (!culling)
                ringMat.set(new IntAttribute(IntAttribute.CullFace, GL20.GL_NONE));
        }
        if (!culling) {
            material.set(new IntAttribute(IntAttribute.CullFace, GL20.GL_NONE));
        }

        return material;
    }

    /**
     * Initialises the materials by binding the necessary textures to them.
     * 
     * @param manager
     *            The asset manager.
     * @param materials
     *            A map with at least one material under the key "base".
     * @param cc
     *            Plain color used if there is no texture.
     * @param culling
     *            Whether to cull the back-facing faces or not.
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
        if(height != null) {
            Texture tex = manager.get(heightUnpacked, Texture.class);
            material.set(new TextureAttribute(TextureExtAttribute.Height, tex));
            material.set(new FloatExtAttribute(FloatExtAttribute.HeightScale, heightScale));
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
    public void setHeight(String height){
        this.height = GlobalConf.data.dataFile(height);
    }

    public void setHeightScale(Double heightScale){
        this.heightScale = (float)(heightScale * Constants.KM_TO_U);
    }



    public void setColoriftex(Boolean coloriftex) {
        this.coloriftex = coloriftex;
    }

    public boolean hasHeight(){
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
        if (height != null && manager.containsAsset(heightUnpacked)) {
            manager.unload(heightUnpacked);
            heightUnpacked = null;
        }
    }
}
