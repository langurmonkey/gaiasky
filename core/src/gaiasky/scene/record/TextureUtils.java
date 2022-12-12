package gaiasky.scene.record;

import com.badlogic.gdx.assets.AssetManager;

public class TextureUtils {
    /**
     * Checks whether the texture with the given name is loaded.
     *
     * @param tex     The name of the texture.
     * @param manager The asset manager.
     *
     * @return Whether the texture is loaded.
     */
    public static boolean isLoaded(String tex, AssetManager manager) {
        if (tex == null)
            return true;
        return manager.isLoaded(tex);
    }

    /**
     * Checks whether the given cubemap is loaded.
     *
     * @param cubemap The cubemap component.
     * @param manager The asset manager.
     *
     * @return Whether the cubemap component is loaded.
     */
    public static boolean isLoaded(CubemapComponent cubemap, AssetManager manager) {
        return cubemap == null || cubemap.isLoaded(manager);
    }
}
