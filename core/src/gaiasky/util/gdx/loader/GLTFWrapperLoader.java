package gaiasky.util.gdx.loader;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.utils.Array;
import gaiasky.util.gdx.loader.GLTFWrapperLoader.GLTFLoaderParameters;
import gaiasky.util.gdx.model.IntModel;
import net.mgsx.gltf.loaders.gltf.GLTFAssetLoader;
import net.mgsx.gltf.loaders.shared.SceneAssetLoaderParameters;
import net.mgsx.gltf.scene3d.scene.SceneAsset;

/**
 * Wraps around {@link GLTFAssetLoader} and converts the loaded GLTF scene into an IntModel.
 */
public class GLTFWrapperLoader extends AsynchronousAssetLoader<IntModel, GLTFLoaderParameters> {

    private final GLTFAssetLoader gltfAssetLoader;

    public GLTFWrapperLoader(FileHandleResolver resolver) {
        super(resolver);
        gltfAssetLoader = new GLTFAssetLoader();
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file, GLTFLoaderParameters parameter) {
        gltfAssetLoader.loadAsync(manager, fileName, file, convertParameters(parameter));
    }

    @Override
    public IntModel loadSync(AssetManager manager, String fileName, FileHandle file, GLTFLoaderParameters parameter) {
        SceneAsset scene = gltfAssetLoader.loadSync(manager, fileName, file, convertParameters(parameter));

        // Convert to IntModel.
        Model model = scene.scene.model;

        return new IntModel(model);
    }

    @Override
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, GLTFLoaderParameters parameter) {
        return gltfAssetLoader.getDependencies(fileName, file, convertParameters(parameter));
    }

    public SceneAssetLoaderParameters convertParameters(GLTFLoaderParameters parameter) {
        var result = new SceneAssetLoaderParameters();
        result.withData = parameter.withData;
        return result;
    }

    public static class GLTFLoaderParameters extends AssetLoaderParameters<IntModel> {
        public boolean withData = false;
    }
}
