package gaiasky.data.util;

import com.artemis.World;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.data.SceneGraphJsonLoader;
import gaiasky.data.util.WorldLoader.EntityLoaderParameter;
import gaiasky.desktop.util.CrashReporter;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.i18n.I18n;
import gaiasky.util.time.ITimeFrameProvider;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Loads the world and all its entities from a list of JSON descriptor files.
 */
public class WorldLoader extends AsynchronousAssetLoader<World, EntityLoaderParameter> {
    private static final Log logger = Logger.getLogger(WorldLoader.class);

    World world;

    public WorldLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file, EntityLoaderParameter parameter) {
        // Add autoload files to the mix
        Array<String> filePaths = new Array<>(parameter.files);
        Path dataFolder = Paths.get(Settings.settings.data.location);
        File[] autoloadFiles = dataFolder.toFile().listFiles((dir, name) -> name != null && name.startsWith("autoload-") && name.endsWith(".json"));
        Objects.requireNonNull(autoloadFiles, "Your data folder does not point to a valid directory: " + dataFolder) ;
        for (File autoloadFile : autoloadFiles) {
            filePaths.add(autoloadFile.getAbsolutePath().replace("\\\\", "/"));
        }

        FileHandle[] fileHandles = new FileHandle[filePaths.size];
        for (int i = 0; i < filePaths.size; i++) {
            fileHandles[i] = this.resolve(filePaths.get(i));
        }

        try {
            //sg = SceneGraphJsonLoader.loadSceneGraph(fileHandles, parameter.time);

        } catch (Exception e) {
            GaiaSky.postRunnable(() -> {
                CrashReporter.reportCrash(e, logger);
                Gdx.app.exit();
            });
        }

        logger.info(I18n.msg("notif.render.init"));
    }

    @Override
    public World loadSync(AssetManager manager, String fileName, FileHandle file, EntityLoaderParameter parameter) {
        return world;
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
