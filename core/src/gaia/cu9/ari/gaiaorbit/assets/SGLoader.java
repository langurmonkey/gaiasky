package gaia.cu9.ari.gaiaorbit.assets;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AssetLoader;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;

import gaia.cu9.ari.gaiaorbit.data.SceneGraphJsonLoader;
import gaia.cu9.ari.gaiaorbit.scenegraph.ISceneGraph;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;

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
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, SGLoaderParameter parameter) {
        return null;
    }

    @Override
    public void loadAsync(AssetManager manager, String files, FileHandle file, SGLoaderParameter parameter) {
        String[] tokens = files.split("\\s*,\\s*");

        FileHandle[] filehandles = new FileHandle[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            filehandles[i] = this.resolve(tokens[i]);
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