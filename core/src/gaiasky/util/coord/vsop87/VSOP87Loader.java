/*
 * Copyright (c) 2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.coord.vsop87;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import gaiasky.util.Logger;
import gaiasky.util.Settings;

/**
 * Loads the VSOP87 binary files and initializes the main class.
 */
public class VSOP87Loader extends AsynchronousAssetLoader<VSOP87Binary, VSOP87Loader.VSOP87LoaderParameters> {
    private static final Logger.Log logger = Logger.getLogger(VSOP87Loader.class);

    private VSOP87Binary vsop87;

    public VSOP87Loader(FileHandleResolver resolver) {
        super(resolver);
    }

    @Override
    public void loadAsync(AssetManager assetManager, String s, FileHandle fileHandle, VSOP87LoaderParameters vsop87LoaderParameters) {
        try {
            var fullPath = Settings.settings.data.dataFile(s);
            vsop87 = new VSOP87Binary(fullPath, vsop87LoaderParameters.percentSkipped);
        } catch (Exception e) {
            logger.error(e);
        }
    }

    @Override
    public VSOP87Binary loadSync(AssetManager assetManager, String s, FileHandle fileHandle, VSOP87LoaderParameters vsop87LoaderParameters) {
        return vsop87;
    }

    @Override
    public Array<AssetDescriptor> getDependencies(String s, FileHandle fileHandle, VSOP87LoaderParameters vsop87LoaderParameters) {
        return null;
    }

    static public class VSOP87LoaderParameters extends AssetLoaderParameters<VSOP87Binary> {
        public double percentSkipped;

        /**
         * Creates a new instance with the percentage of terms to skip.
         * @param percentSkipped The percentage of terms to skip, in [0,1].
         */
        public VSOP87LoaderParameters(double percentSkipped) {
            this.percentSkipped = percentSkipped;
        }
    }
}
