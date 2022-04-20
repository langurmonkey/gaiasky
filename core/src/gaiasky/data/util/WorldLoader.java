package gaiasky.data.util;

import com.artemis.World;
import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import gaiasky.data.util.WorldLoader.EntityLoaderParameter;
import gaiasky.util.time.ITimeFrameProvider;

/**
 * Loads the world and all its entities from a list of JSON descriptor files.
 */
public class WorldLoader extends AsynchronousAssetLoader<World, EntityLoaderParameter> {

    World world;

    public WorldLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file, EntityLoaderParameter parameter) {

    }

    @Override
    public World loadSync(AssetManager manager, String fileName, FileHandle file, EntityLoaderParameter parameter) {
        return null;
    }

    @Override
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, EntityLoaderParameter parameter) {
        return null;
    }

    static public class EntityLoaderParameter extends AssetLoaderParameters<World> {
        public String[] files;
        public ITimeFrameProvider time;

        public EntityLoaderParameter(String[] files, ITimeFrameProvider time) {
            this.files = files;
            this.time = time;
        }
    }
}
