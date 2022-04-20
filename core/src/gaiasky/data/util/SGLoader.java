/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AssetLoader;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.data.SceneGraphJsonLoader;
import gaiasky.util.CrashReporter;
import gaiasky.scenegraph.ISceneGraph;
import gaiasky.scenegraph.SceneGraphNode;
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
 * {@link AssetLoader} for all the {@link SceneGraphNode} instances. Loads all
 * the entities in the scene graph.
 */
public class SGLoader extends AsynchronousAssetLoader<ISceneGraph, SGLoader.SGLoaderParameter> {
    private static final Log logger = Logger.getLogger(SGLoader.class);

    ISceneGraph sg;

    public SGLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, SGLoaderParameter parameter) {
        return null;
    }

    @Override
    public void loadAsync(AssetManager manager, String files, FileHandle file, SGLoaderParameter parameter) {
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
            sg = SceneGraphJsonLoader.loadSceneGraph(fileHandles, parameter.time);
        } catch (Exception e) {
            GaiaSky.postRunnable(() -> {
                CrashReporter.reportCrash(e, logger);
                Gdx.app.exit();
            });
        }

        logger.info(I18n.msg("notif.render.init"));
    }

    /**
     *
     */
    public ISceneGraph loadSync(AssetManager manager, String fileName, FileHandle file, SGLoaderParameter parameter) {
        return sg;
    }

    static public class SGLoaderParameter extends AssetLoaderParameters<ISceneGraph> {
        public String[] files;
        public ITimeFrameProvider time;

        public SGLoaderParameter(String[] files, ITimeFrameProvider time) {
            this.files = files;
            this.time = time;
        }
    }
}
