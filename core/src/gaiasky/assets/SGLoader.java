/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.assets;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AssetLoader;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import gaiasky.data.SceneGraphJsonLoader;
import gaiasky.scenegraph.ISceneGraph;
import gaiasky.scenegraph.SceneGraphNode;
import gaiasky.util.GlobalConf;
import gaiasky.util.I18n;
import gaiasky.util.Logger;
import gaiasky.util.time.ITimeFrameProvider;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * {@link AssetLoader} for all the {@link SceneGraphNode} instances. Loads all
 * the entities in the scene graph.
 * 
 * @author Toni Sagrista
 *
 */
public class SGLoader extends AsynchronousAssetLoader<ISceneGraph, SGLoader.SGLoaderParameter> {

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
        String[] tokens = files.split("\\s*,\\s*");

        // Add autoload files to the mix
        Array<String> filePaths = new Array<>(tokens);
        Path dataFolder = Paths.get(GlobalConf.data.DATA_LOCATION);
        File[] autoloadFiles = dataFolder.toFile().listFiles((dir, name) -> {
            return name != null && name.startsWith("autoload-") && name.endsWith(".json");
        });
        for(File autoloadFile : autoloadFiles) {
            filePaths.add(autoloadFile.getAbsolutePath());
        }

        FileHandle[] filehandles = new FileHandle[filePaths.size];
        for (int i = 0; i < filePaths.size; i++) {
            filehandles[i] = this.resolve(filePaths.get(i));
        }

        sg = SceneGraphJsonLoader.loadSceneGraph(filehandles, parameter.time, parameter.multithreading, parameter.maxThreads);
        Logger.getLogger(this.getClass()).info(I18n.bundle.get("notif.render.init"));
    }

    /**
     * 
     */
    public ISceneGraph loadSync(AssetManager manager, String fileName, FileHandle file, SGLoaderParameter parameter) {
        return sg;
    }

    static public class SGLoaderParameter extends AssetLoaderParameters<ISceneGraph> {
        ITimeFrameProvider time;
        boolean multithreading;
        int maxThreads;

        public SGLoaderParameter(ITimeFrameProvider time, boolean multithreading, int maxThreads) {
            this.time = time;
            this.multithreading = multithreading;
            this.maxThreads = maxThreads;
        }
    }
}