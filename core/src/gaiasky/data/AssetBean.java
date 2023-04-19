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

@SuppressWarnings({ "rawtypes", "unchecked" })
public class AssetBean {
    private static final Set<AssetBean> assetDescriptors;
    private static AssetManager assetManager;

    static {
        assetDescriptors = new HashSet<>();
    }

    private final String assetName;
    private final Class assetClass;
    private AssetLoaderParameters assetParams = null;

    private AssetBean(String assetName, Class assetClass) {
        super();
        this.assetName = assetName;
        this.assetClass = assetClass;
    }

    private AssetBean(String assetName, Class assetClass, AssetLoaderParameters params) {
        this(assetName, assetClass);
        this.assetParams = params;
    }

    public static void addAsset(String assetName, Class assetClass) {
        if (assetManager == null) {
            assetDescriptors.add(new AssetBean(assetName, assetClass));
        } else {
            assetManager.load(assetName, assetClass);
        }
    }

    public static void addAsset(String assetName, Class assetClass, AssetLoaderParameters params) {
        if (assetManager == null) {
            assetDescriptors.add(new AssetBean(assetName, assetClass, params));
        } else {
            assetManager.load(assetName, assetClass, params);
        }
    }

    public static Set<AssetBean> getAssets() {
        return assetDescriptors;
    }

    public static void setAssetManager(AssetManager manager) {
        AssetBean.assetManager = manager;
    }

    public static AssetManager manager() {
        return assetManager;
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
