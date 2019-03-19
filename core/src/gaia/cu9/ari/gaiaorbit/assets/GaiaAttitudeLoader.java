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
        server = new GaiaAttitudeServer(fileName, parameter.files);
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
        String[] files;

        public GaiaAttitudeLoaderParameter(String[] files) {
            super();
            this.files = files;
        }

    }
}
