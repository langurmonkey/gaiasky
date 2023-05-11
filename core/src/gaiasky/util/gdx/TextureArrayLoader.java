/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.TextureArray;
import com.badlogic.gdx.graphics.TextureArrayData;
import com.badlogic.gdx.utils.Array;
import gaiasky.util.gdx.TextureArrayLoader.TextureArrayParameter;

public class TextureArrayLoader extends AsynchronousAssetLoader<TextureArray, TextureArrayParameter> {

    public TextureArrayLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    private TextureArrayData data;

    @Override
    public void loadAsync(AssetManager manager,
                          String fileName,
                          FileHandle file,
                          TextureArrayParameter parameter) {
        if (parameter.files != null) {
            FileHandle[] textureFiles = new FileHandle[parameter.files.length];
            int i = 0;
            for (String f : parameter.files) {
                FileHandle fh = resolve(f);
                textureFiles[i++] = fh;
            }
            data = TextureArrayData.Factory.loadFromFiles(Format.RGBA8888, false, textureFiles);
        }
    }

    @Override
    public TextureArray loadSync(AssetManager manager,
                                 String fileName,
                                 FileHandle file,
                                 TextureArrayParameter parameter) {
        if (data != null) {
            return new TextureArray(data);
        }
        return null;
    }

    @Override
    public Array<AssetDescriptor> getDependencies(String fileName,
                                                  FileHandle file,
                                                  TextureArrayParameter parameter) {
        return null;
    }

    static public class TextureArrayParameter extends AssetLoaderParameters<TextureArray> {
        public String[] files;

        public TextureArrayParameter(String[] files) {
            this.files = files;
        }

        public TextureArrayParameter(Array<String> files) {
            if (files != null && !files.isEmpty()) {
                this.files = new String[files.size];
                for (int i = 0; i < files.size; i++) {
                    this.files[i] = files.get(i);
                }
            }
        }
    }
}
