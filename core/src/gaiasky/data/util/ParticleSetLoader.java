/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
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
import gaiasky.data.api.IParticleGroupDataProvider;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.util.UpdaterHelper;

import java.util.List;

/**
 * Loads data for particle and star sets using {@link IParticleGroupDataProvider} and {@link gaiasky.data.api.IStarGroupDataProvider}.
 */
public class ParticleSetLoader extends AsynchronousAssetLoader<ParticleSetData, ParticleSetLoader.ParticleSetLoaderParameters> {

    private ParticleSetData data;

    public ParticleSetLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file, ParticleSetLoaderParameters parameter) {
        var provider = parameter.provider;

        var list = provider.loadData(parameter.file, parameter.factor,
                                     () -> EventManager.publish(Event.UPDATE_LOAD_PROGRESS, this, parameter.file, 0f),
                                     (current, count) ->
                                             EventManager.publish(Event.UPDATE_LOAD_PROGRESS,
                                                                  this,
                                                                  parameter.file,
                                                                  (float) current / (float) count),
                                     () -> EventManager.publish(Event.UPDATE_LOAD_PROGRESS, this, parameter.file, 2f)
        );

        data = new ParticleSetData(list, provider);
    }

    @Override
    public ParticleSetData loadSync(AssetManager manager, String fileName, FileHandle file, ParticleSetLoaderParameters parameter) {
        return data;
    }

    @Override
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, ParticleSetLoaderParameters parameter) {
        return null;
    }

    static public class ParticleSetLoaderParameters extends AssetLoaderParameters<ParticleSetData> {
        public IParticleGroupDataProvider provider;
        public String file;
        public Double factor;

        public ParticleSetLoaderParameters(IParticleGroupDataProvider provider, String file, Double factor) {
            this.provider = provider;
            this.file = file;
            this.factor = factor;
        }
    }
}
