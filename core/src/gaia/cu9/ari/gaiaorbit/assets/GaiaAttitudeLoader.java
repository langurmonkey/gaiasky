/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.assets;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.util.gaia.GaiaAttitudeServer;

/**
 * @author tsagrista
 */
public class GaiaAttitudeLoader extends AsynchronousAssetLoader<GaiaAttitudeServer, GaiaAttitudeLoader.GaiaAttitudeLoaderParameter> {

    GaiaAttitudeServer server;

    public GaiaAttitudeLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file, GaiaAttitudeLoaderParameter parameter) {
        server = new GaiaAttitudeServer(fileName);
    }

    @Override
    public GaiaAttitudeServer loadSync(AssetManager manager, String fileName, FileHandle file, GaiaAttitudeLoaderParameter parameter) {
        return server;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, GaiaAttitudeLoaderParameter parameter) {
        return null;
    }

    static public class GaiaAttitudeLoaderParameter extends AssetLoaderParameters<GaiaAttitudeServer> {
    }
}
