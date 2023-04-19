/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.util;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import gaiasky.data.attitude.IAttitudeServer;
import gaiasky.data.util.AttitudeLoader.AttitudeLoaderParameters;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;

public class AttitudeLoader extends AsynchronousAssetLoader<IAttitudeServer, AttitudeLoaderParameters> {
    private static final Log logger = Logger.getLogger(AttitudeLoader.class);

    IAttitudeServer server;

    public AttitudeLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file, AttitudeLoaderParameters parameter) {
        try {
            String className = parameter.loaderClass;
            if (className == null || className.isBlank()) {
                logger.error("Attitude loader class name is null or blank");
                throw new RuntimeException("Attitude loader class name is null or blank");
            }
            Class<? extends IAttitudeServer> c = (Class<? extends IAttitudeServer>) Class.forName(className);
            server = c.getDeclaredConstructor(String.class).newInstance(fileName);
        } catch (Exception e) {
            logger.error("Error creating attitude server from class: " + parameter.loaderClass);
            throw new RuntimeException(e);
        }
    }

    @Override
    public IAttitudeServer loadSync(AssetManager manager, String fileName, FileHandle file, AttitudeLoaderParameters parameter) {
        return server;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, AttitudeLoaderParameters parameter) {
        return null;
    }

    static public class AttitudeLoaderParameters extends AssetLoaderParameters<IAttitudeServer> {

        public String loaderClass;

        public AttitudeLoaderParameters(String loaderClass) {
            this.loaderClass = loaderClass;
        }
    }
}
