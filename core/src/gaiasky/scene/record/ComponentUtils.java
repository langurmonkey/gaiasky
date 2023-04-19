/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

import com.badlogic.gdx.assets.AssetManager;

public class ComponentUtils {
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
