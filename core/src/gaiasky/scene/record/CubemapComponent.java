/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

import com.badlogic.gdx.assets.AssetManager;
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
import gaiasky.util.gdx.loader.OwnTextureLoader.OwnTextureParameter;
import gaiasky.util.i18n.I18n;

public class CubemapComponent implements Disposable {
    private static final Log logger = Logger.getLogger(CubemapComponent.class);
    public OwnCubemap cubemap;
    public String location;
    protected boolean loaded = false, prepared = false;
    protected String cmBack, cmFront, cmUp, cmDown, cmRight, cmLeft;


    public CubemapComponent() {
        super();
    }

    public synchronized void initialize() {
        initialize(null);
    }

    public synchronized void initialize(AssetManager manager) {
        if (!loaded) {
            OwnTextureParameter textureParams = new OwnTextureParameter();
            textureParams.genMipMaps = true;
            textureParams.magFilter = TextureFilter.Linear;
            textureParams.minFilter = TextureFilter.MipMapLinearLinear;
            loaded = true;
            try {
                String cubemapLocation = location == null ? Settings.settings.data.reflectionSkyboxLocation : location;
                String cubemapLocationUnpacked = Settings.settings.data.dataFile(cubemapLocation);
                cubemapLocationUnpacked = GlobalResources.unpackAssetPath(cubemapLocationUnpacked);
                logger.info(I18n.msg("notif.loading", "cubemap: " + cubemapLocationUnpacked));
                cmBack = GlobalResources.resolveCubemapSide(cubemapLocationUnpacked, "bk", "back", "b");
                cmFront = GlobalResources.resolveCubemapSide(cubemapLocationUnpacked, "ft", "front", "f");
                cmUp = GlobalResources.resolveCubemapSide(cubemapLocationUnpacked, "up", "top", "u", "t");
                cmDown = GlobalResources.resolveCubemapSide(cubemapLocationUnpacked, "dn", "bottom", "d");
                cmRight = GlobalResources.resolveCubemapSide(cubemapLocationUnpacked, "rt", "right", "r");
                cmLeft = GlobalResources.resolveCubemapSide(cubemapLocationUnpacked, "lf", "left", "l");

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
        if (cubemap == null && !prepared) {
            TextureData bk = manager.get(cmBack, Texture.class).getTextureData();
            TextureData ft = manager.get(cmFront, Texture.class).getTextureData();
            TextureData up = manager.get(cmUp, Texture.class).getTextureData();
            TextureData dn = manager.get(cmDown, Texture.class).getTextureData();
            TextureData rt = manager.get(cmRight, Texture.class).getTextureData();
            TextureData lf = manager.get(cmLeft, Texture.class).getTextureData();
            cubemap = new OwnCubemap(rt, lf, up, dn, ft, bk, TextureFilter.MipMapLinearLinear, TextureFilter.Linear);
            prepared = true;
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
     * @return The actual loaded texture path.
     */
    private String addToLoad(String tex, OwnTextureParameter texParams, AssetManager manager) {
        if (manager == null)
            return addToLoad(tex, texParams);

        if (tex == null)
            return null;

        tex = GlobalResources.unpackAssetPath(tex);

        // Only load asset if it has not been loaded yet.
        if (!manager.contains(tex, Texture.class)) {
            logger.debug(I18n.msg("notif.loading", tex));
            manager.load(tex, Texture.class, texParams);
        }

        return tex;
    }

    /**
     * Adds the texture to load and unpacks any star (*) with the current
     * quality setting.
     *
     * @param tex The texture file to load.
     * @return The actual loaded texture path
     */
    private String addToLoad(String tex, OwnTextureParameter texParams) {
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
