/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.loader;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.PixmapLoader.PixmapParameter;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.assets.loaders.TextureLoader.TextureParameter;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.glutils.ETC1TextureData;
import com.badlogic.gdx.graphics.glutils.FileTextureData;
import com.badlogic.gdx.graphics.glutils.KTXTextureData;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import com.badlogic.gdx.utils.Array;
import gaiasky.util.gdx.graphics.BufferedImageTextureData;

public class OwnTextureLoader extends AsynchronousAssetLoader<Texture, TextureParameter> {
    static public class TextureLoaderInfo {
        String filename;
        TextureData data;
        Texture texture;
    }

    TextureLoaderInfo info = new TextureLoaderInfo();

    public OwnTextureLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file, TextureParameter parameter) {
        info.filename = fileName;
        if (parameter == null || parameter.textureData == null) {
            Format format = null;
            boolean genMipMaps = false;
            boolean pixmapBacked = false;
            info.texture = null;

            if (parameter != null) {
                format = parameter.format;
                genMipMaps = parameter.genMipMaps;
                if (parameter instanceof OwnTextureParameter) {
                    pixmapBacked = ((OwnTextureParameter) parameter).pixmapBacked;
                }
                info.texture = parameter.texture;
            }

            info.data = Factory.loadFromFile(file, format, genMipMaps, pixmapBacked);
        } else {
            info.data = parameter.textureData;
            info.texture = parameter.texture;
        }
        if (!info.data.isPrepared())
            info.data.prepare();
    }

    @Override
    public void unloadAsync(AssetManager manager, String fileName, FileHandle file, TextureParameter parameter) {
        if (parameter.texture != null) {
            parameter.texture.dispose();
        }
        if (parameter.textureData != null) {
            parameter.textureData.disposePixmap();
        }
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

    static public class OwnTextureParameter extends TextureLoader.TextureParameter {
        /**
         * Create a pixmap-backed texture which can be modified programmatically in the CPU.
         * Warning, slow!
         **/
        public boolean pixmapBacked = false;
    }

    public static class Factory {

        public static TextureData loadFromFile(FileHandle file, boolean useMipMaps) {
            return loadFromFile(file, null, useMipMaps, false);
        }

        private static TextureData loadFromFile(FileHandle file, Format format, boolean useMipMaps, boolean pixmapBacked) {
            if (file == null) {
                return null;
            } else if (file.name().endsWith(".cim")) {
                return new FileTextureData(file, PixmapIO.readCIM(file), format, useMipMaps);
            } else if (file.name().endsWith(".etc1")) {
                return new ETC1TextureData(file, useMipMaps);
            } else if (file.name().endsWith(".ktx") || file.name().endsWith(".zktx")) {
                return new KTXTextureData(file, useMipMaps);
            } else if (pixmapBacked) {
                return new PixmapTextureData(loadPixmap(file), format, useMipMaps, false, false);
            } else if (file.name().endsWith(".jxl")) {
                return new BufferedImageTextureData(file, useMipMaps);
            } else {
                return new FileTextureData(file, loadPixmap(file), format, useMipMaps);
            }
        }

        private static Pixmap loadPixmap(FileHandle file) {
            var pixLoader = new OwnPixmapLoader(null);
            pixLoader.loadAsync(null, file.name(), file, new PixmapParameter());
            return pixLoader.loadSync(null, null, null, null);
        }

    }
}
