package gaia.cu9.ari.gaiaorbit.data;

import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;

import java.util.HashSet;
import java.util.Set;

/**
 * Utility class to hold the assets that must be loaded when the OpenGL context is present.
 * If the AssetManager has been set, it delegates the loading to it.
 * @author Toni Sagrista
 *
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class AssetBean {
    private static AssetManager assetManager;
    private static Set<AssetBean> assetDescriptors;

    private String assetName;

    private Class assetClass;
    private AssetLoaderParameters assetParams = null;

    static {
        assetDescriptors = new HashSet<AssetBean>();
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

    private AssetBean(String assetName, Class assetClass) {
        super();
        this.assetName = assetName;
        this.assetClass = assetClass;
    }

    private AssetBean(String assetName, Class assetClass, AssetLoaderParameters params) {
        this(assetName, assetClass);
        this.assetParams = params;
    }

    /**
     * Invokes the load operation on the given AssetManager for this given AssetBean.
     * @param manager
     */
    public void load(AssetManager manager) {
        if (assetParams != null) {
            manager.load(assetName, assetClass, assetParams);
        } else {
            manager.load(assetName, assetClass);
        }
    }
}
