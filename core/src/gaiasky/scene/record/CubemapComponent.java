/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.TextureLoader.TextureParameter;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.utils.Disposable;
import gaiasky.data.AssetBean;
import gaiasky.util.GlobalResources;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.gdx.OwnCubemap;
import gaiasky.util.i18n.I18n;

public class CubemapComponent implements Disposable {
    private static final Log logger = Logger.getLogger(CubemapComponent.class);

    public OwnCubemap cubemap;
    protected boolean loaded = false;
    public String location;
    protected String cmBack, cmFront, cmUp, cmDown, cmRight, cmLeft;

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
                String cubemapLocation = location == null ? Settings.settings.data.reflectionSkyboxLocation : location;
                String cubemapLocationUnapacked = Settings.settings.data.dataFile(cubemapLocation);
                cubemapLocationUnapacked = GlobalResources.unpackAssetPath(cubemapLocationUnapacked);
                logger.info(I18n.msg("notif.loading", "cubemap: " + cubemapLocationUnapacked));
                cmBack = GlobalResources.resolveCubemapSide(cubemapLocationUnapacked, "bk", "back", "b");
                cmFront = GlobalResources.resolveCubemapSide(cubemapLocationUnapacked, "ft", "front", "f");
                cmUp = GlobalResources.resolveCubemapSide(cubemapLocationUnapacked, "up", "top", "u", "t");
                cmDown = GlobalResources.resolveCubemapSide(cubemapLocationUnapacked, "dn", "bottom", "d");
                cmRight = GlobalResources.resolveCubemapSide(cubemapLocationUnapacked, "rt", "right", "r");
                cmLeft = GlobalResources.resolveCubemapSide(cubemapLocationUnapacked, "lf", "left", "l");

                addToLoad(cmBack, textureParams, manager);
                addToLoad(cmFront, textureParams, manager);
                addToLoad(cmUp, textureParams, manager);
                addToLoad(cmDown, textureParams, manager);
                addToLoad(cmRight, textureParams, manager);
                addToLoad(cmLeft, textureParams, manager);
            } catch (RuntimeException e) {
                logger.error(e, "Error loading skybox: " + Settings.settings.data.reflectionSkyboxLocation);
            }
        }
    }

    public synchronized void prepareCubemap(AssetManager manager) {
        if (cubemap == null) {
            TextureData bk = manager.get(cmBack, Texture.class).getTextureData();
            TextureData ft = manager.get(cmFront, Texture.class).getTextureData();
            TextureData up = manager.get(cmUp, Texture.class).getTextureData();
            TextureData dn = manager.get(cmDown, Texture.class).getTextureData();
            TextureData rt = manager.get(cmRight, Texture.class).getTextureData();
            TextureData lf = manager.get(cmLeft, Texture.class).getTextureData();
            cubemap = new OwnCubemap(rt, lf, up, dn, ft, bk, TextureFilter.MipMapLinearLinear, TextureFilter.Linear);
        }
    }

    public boolean isLoaded(AssetManager manager) {
        return manager.isLoaded(cmBack)
                && manager.isLoaded(cmFront)
                && manager.isLoaded(cmUp)
                && manager.isLoaded(cmDown)
                && manager.isLoaded(cmLeft)
                && manager.isLoaded(cmRight);
    }

    /**
     * Adds the texture to load and unpacks any star (*) with the current
     * quality setting.
     *
     * @param tex The texture file to load.
     *
     * @return The actual loaded texture path
     */
    private String addToLoad(String tex, TextureParameter texParams, AssetManager manager) {
        if (manager == null)
            return addToLoad(tex, texParams);

        if (tex == null)
            return null;

        tex = GlobalResources.unpackAssetPath(tex);
        logger.debug(I18n.msg("notif.loading", tex));
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
    private String addToLoad(String tex, TextureParameter texParams) {
        if (tex == null)
            return null;

        tex = GlobalResources.unpackAssetPath(tex);
        logger.debug(I18n.msg("notif.loading", tex));
        AssetBean.addAsset(tex, Texture.class, texParams);

        return tex;
    }

    public void setLocation(String location) {
        this.location = location;
    }


    @Override
    public void dispose() {
        if (cubemap != null) {
            cubemap.dispose();
            cubemap = null;
        }
        loaded = false;
    }
}
