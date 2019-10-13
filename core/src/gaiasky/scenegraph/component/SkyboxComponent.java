/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.scenegraph.component;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.TextureLoader.TextureParameter;
import com.badlogic.gdx.graphics.Cubemap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.TextureData;
import gaia.cu9.ari.gaiaorbit.data.AssetBean;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;

public class SkyboxComponent {
    private static Log logger = Logger.getLogger(SkyboxComponent.class);

    public static Cubemap skybox;
    protected static boolean skyboxLoad = false;
    protected static String skyboxBack, skyboxFront, skyboxUp, skyboxDown, skyboxRight, skyboxLeft;

    public synchronized static void initSkybox() {
        if (!skyboxLoad) {
            TextureParameter textureParams = new TextureParameter();
            textureParams.genMipMaps = false;
            textureParams.magFilter = TextureFilter.Linear;
            textureParams.minFilter = TextureFilter.Linear;
            skyboxLoad = true;
            try {
                String skbLoc = GlobalConf.data.SKYBOX_LOCATION;
                logger.info(I18n.txt("notif.loading", " skybox: " + skbLoc));
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
                logger.error(e, "Error loading skybox: " + GlobalConf.data.SKYBOX_LOCATION);
            }
        }
    }

    public synchronized static void prepareSkybox() {
        if (skybox == null) {
            AssetManager m = AssetBean.manager();
            TextureData bk = m.get(skyboxBack, Texture.class).getTextureData();
            TextureData ft = m.get(skyboxFront, Texture.class).getTextureData();
            TextureData up = m.get(skyboxUp, Texture.class).getTextureData();
            TextureData dn = m.get(skyboxDown, Texture.class).getTextureData();
            TextureData rt = m.get(skyboxRight, Texture.class).getTextureData();
            TextureData lf = m.get(skyboxLeft, Texture.class).getTextureData();
            skybox = new Cubemap(rt, lf, up, dn, ft, bk);
        }
    }
    /**
     * Adds the texture to load and unpacks any star (*) with the current
     * quality setting.
     *
     * @param tex
     * @return The actual loaded texture path
     */
    private static String addToLoad(String tex, TextureParameter texParams) {
        if (tex == null)
            return null;

        tex = GlobalResources.unpackTexName(tex);
        AssetBean.addAsset(tex, Texture.class, texParams);

        return tex;
    }
}
