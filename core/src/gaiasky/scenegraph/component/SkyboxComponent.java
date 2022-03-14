/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph.component;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.TextureLoader.TextureParameter;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.TextureData;
import gaiasky.data.AssetBean;
import gaiasky.util.GlobalResources;
import gaiasky.util.I18n;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.Settings.GraphicsQuality;
import gaiasky.util.gdx.OwnCubemap;

public class SkyboxComponent {
    private static final Log logger = Logger.getLogger(SkyboxComponent.class);

    public OwnCubemap skybox;
    protected boolean loaded = false;
    public String location;
    protected String skyboxBack, skyboxFront, skyboxUp, skyboxDown, skyboxRight, skyboxLeft;

    public synchronized void initialize() {
        initialize(null);
    }

    public synchronized void initialize(AssetManager manager) {
        if (!loaded) {
            TextureParameter textureParams = new TextureParameter();
            textureParams.genMipMaps = true;
            textureParams.magFilter = TextureFilter.Linear;
            textureParams.minFilter = TextureFilter.MipMapLinearLinear;
            loaded = true;
            try {
                String skbLoc = location == null ? Settings.settings.data.skyboxLocation : location;
                logger.info(I18n.txt("notif.loading", "skybox: " + skbLoc));
                skyboxBack = GlobalResources.unpackSkyboxSide(skbLoc, "bk");
                skyboxFront = GlobalResources.unpackSkyboxSide(skbLoc, "ft");
                skyboxUp = GlobalResources.unpackSkyboxSide(skbLoc, "up");
                skyboxDown = GlobalResources.unpackSkyboxSide(skbLoc, "dn");
                skyboxRight = GlobalResources.unpackSkyboxSide(skbLoc, "rt");
                skyboxLeft = GlobalResources.unpackSkyboxSide(skbLoc, "lf");

                addToLoad(skyboxBack, textureParams, manager);
                addToLoad(skyboxFront, textureParams, manager);
                addToLoad(skyboxUp, textureParams, manager);
                addToLoad(skyboxDown, textureParams, manager);
                addToLoad(skyboxRight, textureParams, manager);
                addToLoad(skyboxLeft, textureParams, manager);
            } catch (RuntimeException e) {
                logger.error(e, "Error loading skybox: " + Settings.settings.data.skyboxLocation);
            }
        }
    }

    public synchronized void prepareSkybox() {
        if (skybox == null) {
            AssetManager m = AssetBean.manager();
            TextureData bk = m.get(skyboxBack, Texture.class).getTextureData();
            TextureData ft = m.get(skyboxFront, Texture.class).getTextureData();
            TextureData up = m.get(skyboxUp, Texture.class).getTextureData();
            TextureData dn = m.get(skyboxDown, Texture.class).getTextureData();
            TextureData rt = m.get(skyboxRight, Texture.class).getTextureData();
            TextureData lf = m.get(skyboxLeft, Texture.class).getTextureData();
            skybox = new OwnCubemap(rt, lf, up, dn, ft, bk, TextureFilter.MipMapLinearLinear, TextureFilter.Linear);
        }
    }

    public boolean isLoaded(AssetManager manager){
        return manager.isLoaded(skyboxBack)
                && manager.isLoaded(skyboxFront)
                && manager.isLoaded(skyboxUp)
                && manager.isLoaded(skyboxDown)
                && manager.isLoaded(skyboxLeft)
                && manager.isLoaded(skyboxRight);
    }

    /**
     * Adds the texture to load and unpacks any star (*) with the current
     * quality setting.
     *
     * @param tex The texture file to load.
     * @return The actual loaded texture path
     */
    private String addToLoad(String tex, TextureParameter texParams, AssetManager manager) {
        if (manager == null)
            return addToLoad(tex, texParams);

        if (tex == null)
            return null;

        tex = GlobalResources.unpackAssetPath(tex);
        logger.info(I18n.txt("notif.loading", tex));
        manager.load(tex, Texture.class, texParams);

        return tex;
    }

    /**
     * Adds the texture to load and unpacks any star (*) with the current
     * quality setting.
     *
     * @param tex The texture file to load.
     * @return The actual loaded texture path
     */
    private String addToLoad(String tex, TextureParameter texParams) {
        if (tex == null)
            return null;

        tex = GlobalResources.unpackAssetPath(tex);
        logger.info(I18n.txt("notif.loading", tex));
        AssetBean.addAsset(tex, Texture.class, texParams);

        return tex;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
