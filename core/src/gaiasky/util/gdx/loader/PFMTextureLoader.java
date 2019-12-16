/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.loader;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.glutils.FileTextureData;
import com.badlogic.gdx.utils.Array;

public class PFMTextureLoader extends TextureLoader {
    static public class TextureLoaderInfo {
        String filename;
        TextureData data;
        Texture texture;
    }

    PFMTextureLoader.TextureLoaderInfo info = new PFMTextureLoader.TextureLoaderInfo();

    public PFMTextureLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file, TextureParameter parameter) {
        info.filename = fileName;
        if (parameter == null || parameter.textureData == null) {
            info.texture = null;

            if (parameter != null) {
                info.texture = parameter.texture;
            }

            Pixmap pixmap = PFMReader.readPFMPixmap(file, true);
            info.data = new FileTextureData(file, pixmap, parameter.format, parameter.genMipMaps);
            //info.data = PFMReader.readPFMTextureData(file);
        } else {
            info.data = parameter.textureData;
            info.texture = parameter.texture;
        }
        if (!info.data.isPrepared())
            info.data.prepare();
    }

    @Override
    public Texture loadSync(AssetManager manager, String fileName, FileHandle file, TextureParameter parameter) {
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
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, TextureParameter parameter) {
        return null;
    }

}
