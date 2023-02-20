/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.loader;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.glutils.FileTextureData;
import com.badlogic.gdx.utils.Array;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.gdx.loader.OwnTextureLoader.OwnTextureParameter;

public class PFMTextureLoader extends AsynchronousAssetLoader<Texture, PFMTextureLoader.PFMTextureParameter> {
    private static final Log logger = Logger.getLogger(PFMTextureLoader.class);
    PFMTextureLoader.TextureLoaderInfo info = new PFMTextureLoader.TextureLoaderInfo();

    public PFMTextureLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file, PFMTextureParameter parameter) {
        info.filename = fileName;
        if (parameter == null || parameter.textureData == null) {
            info.texture = null;

            if (parameter != null) {
                info.texture = parameter.texture;
            }
            logger.info("Loading PFM: " + file.path());
            assert parameter != null;
            if (parameter.internalFormat == GL20.GL_FLOAT) {
                info.data = PFMReader.readPFMTextureData(file, parameter.invert);
            } else {
                Pixmap pixmap = PFMReader.readPFMPixmap(file, parameter.invert);
                info.data = new FileTextureData(file, pixmap, parameter.format, parameter.genMipMaps);
            }
        } else {
            info.data = parameter.textureData;
            info.texture = parameter.texture;
        }
        if (!info.data.isPrepared())
            info.data.prepare();
    }

    @Override
    public Texture loadSync(AssetManager manager, String fileName, FileHandle file, PFMTextureParameter parameter) {
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

    @SuppressWarnings("rawtypes")
    @Override
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, PFMTextureParameter parameter) {
        return null;
    }

    static public class TextureLoaderInfo {
        String filename;
        TextureData data;
        Texture texture;
    }

    static public class PFMTextureParameter extends OwnTextureParameter {
        /** Whether to compute the inverse mapping **/
        public boolean invert = false;
        /** Either GL_RGB or GL_FLOAT **/
        public int internalFormat = GL20.GL_RGB;

        public PFMTextureParameter() {
        }
        public PFMTextureParameter(OwnTextureParameter other) {
            this.format = other.format;
            this.genMipMaps = other.genMipMaps;
            this.magFilter = other.magFilter;
            this.minFilter = other.minFilter;
            this.wrapU = other.wrapU;
            this.wrapV = other.wrapV;
        }
    }

}
