package gaiasky.util.gdx.loader;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AssetLoader;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.TextureLoader.TextureParameter;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.glutils.ETC1TextureData;
import com.badlogic.gdx.graphics.glutils.FileTextureData;
import com.badlogic.gdx.graphics.glutils.KTXTextureData;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import com.badlogic.gdx.utils.Array;
import gaiasky.util.gdx.loader.OwnTextureLoader.OwnTextureParameter;

/**
 * {@link AssetLoader} for {@link Texture} instances. The pixel data is loaded asynchronously. The texture is then created on the
 * rendering thread, synchronously. Passing a {@link TextureParameter} to
 * {@link AssetManager#load(String, Class, AssetLoaderParameters)} allows one to specify parameters as can be passed to the
 * various Texture constructors, e.g. filtering, whether to generate mipmaps and so on.
 *
 * @author mzechner
 */
public class OwnTextureLoader extends AsynchronousAssetLoader<Texture, OwnTextureParameter> {
    static public class TextureLoaderInfo {
        String filename;
        TextureData data;
        Texture texture;
    }

    ;

    TextureLoaderInfo info = new TextureLoaderInfo();

    public OwnTextureLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file, OwnTextureParameter parameter) {
        info.filename = fileName;
        if (parameter == null || parameter.textureData == null) {
            Format format = null;
            boolean genMipMaps = false;
            boolean pixmapBacked = false;
            info.texture = null;

            if (parameter != null) {
                format = parameter.format;
                genMipMaps = parameter.genMipMaps;
                pixmapBacked = parameter.pixmapBacked;
                info.texture = parameter.texture;
            }

            info.data = loadFromFile(file, format, genMipMaps, pixmapBacked);
        } else {
            info.data = parameter.textureData;
            info.texture = parameter.texture;
        }
        if (!info.data.isPrepared())
            info.data.prepare();
    }

    private TextureData loadFromFile(FileHandle file, Format format, boolean useMipMaps, boolean pixmapBacked) {
        if (file == null) {
            return null;
        } else if (file.name().endsWith(".cim")) {
            return new FileTextureData(file, PixmapIO.readCIM(file), format, useMipMaps);
        } else if (file.name().endsWith(".etc1")) {
            return new ETC1TextureData(file, useMipMaps);
        } else if (file.name().endsWith(".ktx") || file.name().endsWith(".zktx")) {
            return new KTXTextureData(file, useMipMaps);
        } else if (pixmapBacked) {
            return new PixmapTextureData(new Pixmap(file), format, useMipMaps, false, false);
        } else {
            return new FileTextureData(file, new Pixmap(file), format, useMipMaps);
        }
    }

    @Override
    public void unloadAsync(AssetManager manager, String fileName, FileHandle file, OwnTextureParameter parameter) {
        if (parameter.texture != null) {
            parameter.texture.dispose();
        }
        if(parameter.textureData != null) {
            parameter.textureData.disposePixmap();
        }
    }

    @Override
    public Texture loadSync(AssetManager manager, String fileName, FileHandle file, OwnTextureParameter parameter) {
        if (info == null)
            return null;
        Texture texture = info.texture;
        if (texture != null) {
            texture.load(info.data);
        } else {
            texture = new Texture(info.data);
        }
        if (parameter != null) {
            texture.setFilter(parameter.minFilter, parameter.magFilter);
            texture.setWrap(parameter.wrapU, parameter.wrapV);
        }
        return texture;
    }

    @Override
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, OwnTextureParameter parameter) {
        return null;
    }

    static public class OwnTextureParameter extends AssetLoaderParameters<Texture> {
        /** the format of the final Texture. Uses the source images format if null **/
        public Format format = null;
        /** whether to generate mipmaps **/
        public boolean genMipMaps = false;
        /**
         * Create a pixmap-backed texture which can be modified programmatically in the CPU.
         * Warning, slow!
         **/
        public boolean pixmapBacked = false;
        /** The texture to put the {@link TextureData} in, optional. **/
        public Texture texture = null;
        /** TextureData for textures created on the fly, optional. When set, all format and genMipMaps are ignored */
        public TextureData textureData = null;
        public TextureFilter minFilter = TextureFilter.Nearest;
        public TextureFilter magFilter = TextureFilter.Nearest;
        public TextureWrap wrapU = TextureWrap.ClampToEdge;
        public TextureWrap wrapV = TextureWrap.ClampToEdge;
    }
}
