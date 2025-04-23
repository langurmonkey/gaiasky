/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.util;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import gaiasky.data.api.IOrbitDataProvider;
import gaiasky.scene.component.Trajectory.OrbitSamplingStrategy;
import gaiasky.scene.record.OrbitComponent;
import gaiasky.util.Logger;

import java.util.Date;

public class OrbitDataLoader extends AsynchronousAssetLoader<PointCloudData, OrbitDataLoader.OrbitDataLoaderParameters> {

    PointCloudData data;

    public OrbitDataLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, OrbitDataLoaderParameters parameters) {
        return null;
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file, OrbitDataLoaderParameters parameters) {
        IOrbitDataProvider provider;
        try {
            provider = ClassReflection.newInstance(parameters.providerClass);
            provider.load(fileName, parameters);
            data = provider.getData();
        } catch (Exception e) {
            Logger.getLogger(this.getClass()).error(e);
        }
    }

    public PointCloudData loadSync(AssetManager manager, String fileName, FileHandle file, OrbitDataLoaderParameters parameter) {
        return data;
    }

    static public class OrbitDataLoaderParameters extends AssetLoaderParameters<PointCloudData> {

        public Date ini;
        public double orbitalPeriod;
        public double multiplier = 1;
        public int numSamples;
        public OrbitSamplingStrategy sampling;
        public String name;
        public OrbitComponent orbitalParamaters;
        public Entity entity;
        public boolean force = false;
        Class<? extends IOrbitDataProvider> providerClass;

        public OrbitDataLoaderParameters(Class<? extends IOrbitDataProvider> providerClass) {
            this.providerClass = providerClass;
        }

        public OrbitDataLoaderParameters(String name,
                                         Class<? extends IOrbitDataProvider> providerClass,
                                         double orbitalPeriod, int numSamples,
                                         OrbitSamplingStrategy sampling) {
            this(providerClass);
            this.name = name;
            this.orbitalPeriod = orbitalPeriod;
            this.numSamples = numSamples;
            this.sampling = sampling;
        }

        public OrbitDataLoaderParameters(String name,
                                         Class<? extends IOrbitDataProvider> providerClass,
                                         OrbitComponent orbitalParameters,
                                         double multiplier) {
            this(providerClass);
            this.name = name;
            this.orbitalParamaters = orbitalParameters;
            this.multiplier = multiplier;
        }

        public OrbitDataLoaderParameters(String name,
                                         Class<? extends IOrbitDataProvider> providerClass,
                                         OrbitComponent orbitalParameters,
                                         double multiplier,
                                         int numSamples,
                                         OrbitSamplingStrategy sampling) {
            this(providerClass);
            this.name = name;
            this.orbitalParamaters = orbitalParameters;
            this.multiplier = multiplier;
            this.numSamples = numSamples;
            this.sampling = sampling;
        }

        public OrbitDataLoaderParameters(Class<? extends IOrbitDataProvider> providerClass,
                                         String name,
                                         Date ini,
                                         double orbitalPeriod,
                                         int numSamples,
                                         OrbitSamplingStrategy sampling) {
            this(providerClass);
            this.name = name;
            this.ini = ini;
            this.orbitalPeriod = orbitalPeriod;
            this.numSamples = numSamples;
            this.sampling = sampling;
        }

        public OrbitDataLoaderParameters(Class<? extends IOrbitDataProvider> providerClass,
                                         String name,
                                         Date ini,
                                         double orbitalPeriod) {
            this(providerClass, name, ini, orbitalPeriod, -1, OrbitSamplingStrategy.TIME);
        }

        public void setIni(Date date) {
            this.ini = date;
        }

        public void setOrbitalPeriod(double period) {
            this.orbitalPeriod = period;
        }

        public void setForce(Boolean force) {
            this.force = force;
        }
    }
}
