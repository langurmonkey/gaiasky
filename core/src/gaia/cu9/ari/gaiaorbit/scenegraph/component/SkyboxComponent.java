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
import gaia.cu9.ari.gaiaorbit.data.AssetBean;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;

public class SkyboxComponent {

    public static Cubemap skybox;
    protected static boolean skyboxLoad = false;
    protected static String skyboxBack = "data/tex/skybox/stars/stars_bk.jpg";
    protected static String skyboxFront = "data/tex/skybox/stars/stars_ft.jpg";
    protected static String skyboxUp = "data/tex/skybox/stars/stars_up.jpg";
    protected static String skyboxDown = "data/tex/skybox/stars/stars_dn.jpg";
    protected static String skyboxRight = "data/tex/skybox/stars/stars_rt.jpg";
    protected static String skyboxLeft = "data/tex/skybox/stars/stars_lf.jpg";

    public synchronized static void initSkybox() {
        if (!skyboxLoad) {
            TextureParameter textureParams = new TextureParameter();
            textureParams.genMipMaps = false;
            textureParams.magFilter = TextureFilter.Linear;
            textureParams.minFilter = TextureFilter.Linear;
            skyboxLoad = true;
            addToLoad(skyboxBack = GlobalConf.data.dataFile(skyboxBack), textureParams);
            addToLoad(skyboxFront = GlobalConf.data.dataFile(skyboxFront), textureParams);
            addToLoad(skyboxUp = GlobalConf.data.dataFile(skyboxUp), textureParams);
            addToLoad(skyboxDown = GlobalConf.data.dataFile(skyboxDown), textureParams);
            addToLoad(skyboxRight = GlobalConf.data.dataFile(skyboxRight), textureParams);
            addToLoad(skyboxLeft = GlobalConf.data.dataFile(skyboxLeft), textureParams);
        }
    }

    public synchronized static void prepareSkybox() {
        if (skybox == null) {
            AssetManager m = AssetBean.manager();
            Texture bk = m.get(skyboxBack, Texture.class);
            Texture ft = m.get(skyboxFront, Texture.class);
            Texture up = m.get(skyboxUp, Texture.class);
            Texture dn = m.get(skyboxDown, Texture.class);
            Texture rt = m.get(skyboxRight, Texture.class);
            Texture lf = m.get(skyboxLeft, Texture.class);
            skybox = new Cubemap(bk.getTextureData(), ft.getTextureData(), up.getTextureData(), dn.getTextureData(), rt.getTextureData(), lf.getTextureData());
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
