package gaiasky.util.gdx.loader;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AssetLoader;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.CubemapData;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.glutils.KTXTextureData;
import com.badlogic.gdx.utils.Array;
import gaiasky.util.gdx.OwnCubemap;
import gaiasky.util.gdx.loader.CubemapLoader.CubemapParameter;

/**
 * {@link AssetLoader} for {@link OwnCubemap} instances. The pixel data is loaded asynchronously. The texture is then created on the
 * rendering thread, synchronously. Passing a {@link CubemapParameter} to
 * {@link AssetManager#load(String, Class, AssetLoaderParameters)} allows one to specify parameters as can be passed to the
 * various Cubemap constructors, e.g. filtering and so on.
 *
 * @author mzechner, Vincent Bousquet
 */
public class CubemapLoader extends AsynchronousAssetLoader<OwnCubemap, CubemapParameter> {
    CubemapLoaderInfo info = new CubemapLoaderInfo();

    ;

    public CubemapLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file, CubemapParameter parameter) {
        info.filename = fileName;
        if (parameter == null || parameter.cubemapData == null) {
            Format format = null;
            boolean genMipMaps = false;
            info.cubemap = null;

            if (parameter != null) {
                format = parameter.format;
                info.cubemap = parameter.cubemap;
            }

            if (fileName.contains(".ktx") || fileName.contains(".zktx")) {
                info.data = new KTXTextureData(file, genMipMaps);
            }
        } else {
            info.data = parameter.cubemapData;
            info.cubemap = parameter.cubemap;
        }
        if (!info.data.isPrepared())
            info.data.prepare();
    }

    @Override
    public OwnCubemap loadSync(AssetManager manager, String fileName, FileHandle file, CubemapParameter parameter) {
        if (info == null)
            return null;
        OwnCubemap cubemap = info.cubemap;
        if (cubemap != null) {
            cubemap.load(info.data);
        } else {
            cubemap = new OwnCubemap(info.data);
        }
        if (parameter != null) {
            cubemap.setFilter(parameter.minFilter, parameter.magFilter);
            cubemap.setWrap(parameter.wrapU, parameter.wrapV);
        }
        return cubemap;
    }

    @Override
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, CubemapParameter parameter) {
        return null;
    }

    static public class CubemapLoaderInfo {
        String filename;
        CubemapData data;
        OwnCubemap cubemap;
    }

    static public class CubemapParameter extends AssetLoaderParameters<OwnCubemap> {
        /** the format of the final Texture. Uses the source images format if null **/
        public Format format = null;
        /** The texture to put the {@link TextureData} in, optional. **/
        public OwnCubemap cubemap = null;
        /** CubemapData for textures created on the fly, optional. When set, all format and genMipMaps are ignored */
        public CubemapData cubemapData = null;
        public TextureFilter minFilter = TextureFilter.Nearest;
        public TextureFilter magFilter = TextureFilter.Nearest;
        public TextureWrap wrapU = TextureWrap.ClampToEdge;
        public TextureWrap wrapV = TextureWrap.ClampToEdge;
    }
}
