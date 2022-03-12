/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph.component;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.TextureLoader.TextureParameter;
import com.badlogic.gdx.graphics.Cubemap;
import com.badlogic.gdx.graphics.CubemapData;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.glutils.FacedCubemapData;
import gaiasky.data.AssetBean;
import gaiasky.util.GlobalResources;
import gaiasky.util.I18n;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.gdx.OwnCubemap;

public class SkyboxComponent {
    private static final Log logger = Logger.getLogger(SkyboxComponent.class);

    public  OwnCubemap skybox;
    protected  boolean skyboxLoad = false;
    protected  String skyboxBack, skyboxFront, skyboxUp, skyboxDown, skyboxRight, skyboxLeft;

    public synchronized  void initSkybox() {
        if (!skyboxLoad) {
            TextureParameter textureParams = new TextureParameter();
            textureParams.genMipMaps = true;
            textureParams.magFilter = TextureFilter.Linear;
            textureParams.minFilter = TextureFilter.MipMapLinearLinear;
            skyboxLoad = true;
            try {
                String skbLoc = Settings.settings.data.skyboxLocation;
                logger.info(I18n.txt("notif.loading", "skybox: " + skbLoc));
                skyboxBack = GlobalResources.unpackSkyboxSide(skbLoc, "bk");
                skyboxFront = GlobalResources.unpackSkyboxSide(skbLoc, "ft");
                skyboxUp = GlobalResources.unpackSkyboxSide(skbLoc, "up");
                skyboxDown = GlobalResources.unpackSkyboxSide(skbLoc, "dn");
                skyboxRight = GlobalResources.unpackSkyboxSide(skbLoc, "rt");
                skyboxLeft = GlobalResources.unpackSkyboxSide(skbLoc, "lf");

                addToLoad(skyboxBack, textureParams);
                addToLoad(skyboxFront, textureParams);
                addToLoad(skyboxUp, textureParams);
                addToLoad(skyboxDown, textureParams);
                addToLoad(skyboxRight, textureParams);
                addToLoad(skyboxLeft, textureParams);
            }catch(RuntimeException e){
                logger.error(e, "Error loading skybox: " + Settings.settings.data.skyboxLocation);
            }
        }
    }

    public synchronized  void prepareSkybox() {
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
    /**
     * Adds the texture to load and unpacks any star (*) with the current
     * quality setting.
     *
     * @param tex
     * @return The actual loaded texture path
     */
    private  String addToLoad(String tex, TextureParameter texParams) {
        if (tex == null)
            return null;

        tex = GlobalResources.unpackAssetPath(tex);
        AssetBean.addAsset(tex, Texture.class, texParams);

        return tex;
    }
}
