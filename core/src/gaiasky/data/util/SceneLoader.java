package gaiasky.data.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.data.SceneJsonLoader;
import gaiasky.data.util.SceneLoader.SceneLoaderParameters;
import gaiasky.scene.Scene;
import gaiasky.util.CrashReporter;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.i18n.I18n;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Loads the world and all its entities from a list of JSON descriptor files.
 */
public class SceneLoader extends AsynchronousAssetLoader<Scene, SceneLoaderParameters> {
    private static final Log logger = Logger.getLogger(SceneLoader.class);

    private Scene scene;

    public SceneLoader(FileHandleResolver resolver) {
        super(resolver);
        this.scene = scene;
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file, SceneLoaderParameters parameter) {
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
            scene = new Scene();
            scene.initialize();
            SceneJsonLoader.loadScene(fileHandles, scene);

        } catch (Exception e) {
            GaiaSky.postRunnable(() -> {
                CrashReporter.reportCrash(e, logger);
                Gdx.app.exit();
            });
        }

        logger.info(I18n.msg("notif.render.init"));
    }

    @Override
    public Scene loadSync(AssetManager manager, String fileName, FileHandle file, SceneLoaderParameters parameter) {
        return scene;
    }

    @Override
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, SceneLoaderParameters parameter) {
        return null;
    }

    static public class SceneLoaderParameters extends AssetLoaderParameters<Scene> {
        public String[] files;

        public SceneLoaderParameters(String[] files) {
            this.files = files;
        }
    }
}
