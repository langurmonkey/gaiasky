/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data;

import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings({ "rawtypes" })
public class AssetBean<T> {

    /**
     * Asset descriptors set.
     */
    private static final Set<AssetBean> assetDescriptors = new HashSet<>();

    /**
     * Reference to the main asset manager.
     */
    private static final AtomicReference<AssetManager> assetManager = new AtomicReference<>();

    private final String assetName;
    private final Class<T> assetClass;
    private AssetLoaderParameters<T> assetParams = null;

    private AssetBean(String assetName, Class<T> assetClass) {
        super();
        this.assetName = assetName;
        this.assetClass = assetClass;
    }

    private AssetBean(String assetName, Class<T> assetClass, AssetLoaderParameters<T> params) {
        this(assetName, assetClass);
        this.assetParams = params;
    }

    public static <T> void addAsset(String assetName, Class<T> assetClass) {
        if (assetManager == null) {
            assetDescriptors.add(new AssetBean<T>(assetName, assetClass));
        } else {
            assetManager.get().load(assetName, assetClass);
        }
    }

    public static  <T> void addAsset(String assetName, Class<T> assetClass, AssetLoaderParameters<T> params) {
        if (assetManager == null) {
            assetDescriptors.add(new AssetBean<T>(assetName, assetClass, params));
        } else {
            assetManager.get().load(assetName, assetClass, params);
        }
    }

    public static Set<AssetBean> getAssets() {
        return assetDescriptors;
    }

    public static void setAssetManager(AssetManager manager) {
        AssetBean.assetManager.set(manager);
    }

    public static AssetManager manager() {
        return assetManager.get();
    }

    /**
     * Invokes the load operation on the given AssetManager for this given AssetBean.
     *
     * @param manager The asset manager.
     */
    public void load(AssetManager manager) {
        if (assetParams != null) {
            manager.load(assetName, assetClass, assetParams);
        } else {
            manager.load(assetName, assetClass);
        }
    }
}
