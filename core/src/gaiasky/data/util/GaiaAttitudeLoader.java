/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.util;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import gaiasky.util.gaia.GaiaAttitudeServer;

/*
 * Loader for Gaia attitude data.
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
