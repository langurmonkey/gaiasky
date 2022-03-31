/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.util;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import gaiasky.data.orbit.IOrbitDataProvider;
import gaiasky.scenegraph.Orbit;
import gaiasky.scenegraph.component.OrbitComponent;
import gaiasky.util.Logger;

import java.util.Date;

/**
 * Abstract data loader to rule them all.
 */
public class OrbitDataLoader extends AsynchronousAssetLoader<PointCloudData, OrbitDataLoader.OrbitDataLoaderParameter> {

    PointCloudData data;

    public OrbitDataLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, OrbitDataLoaderParameter parameter) {
        return null;
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file, OrbitDataLoaderParameter parameter) {
        IOrbitDataProvider provider;
        try {
            provider = ClassReflection.newInstance(parameter.providerClass);
            provider.load(fileName, parameter);
            data = provider.getData();
        } catch (Exception e) {
            Logger.getLogger(this.getClass()).error(e);
        }

    }

    /**
     * 
     */
    public PointCloudData loadSync(AssetManager manager, String fileName, FileHandle file, OrbitDataLoaderParameter parameter) {
        return data;
    }

    static public class OrbitDataLoaderParameter extends AssetLoaderParameters<PointCloudData> {

        Class<? extends IOrbitDataProvider> providerClass;
        public Date ini;
        public boolean forward;
        public double orbitalPeriod;
        public double multiplier;
        public int numSamples;
        public String name;
        public OrbitComponent orbitalParamaters;
        public Orbit orbit;

        public OrbitDataLoaderParameter(Class<? extends IOrbitDataProvider> providerClass) {
            this.providerClass = providerClass;
        }

        public OrbitDataLoaderParameter(String name, Class<? extends IOrbitDataProvider> providerClass, double orbitalPeriod, int numSamples) {
            this(providerClass);
            this.name = name;
            this.orbitalPeriod = orbitalPeriod;
            this.numSamples = numSamples;
        }

        public OrbitDataLoaderParameter(String name, Class<? extends IOrbitDataProvider> providerClass, OrbitComponent orbitalParameters, double multiplier) {
            this(providerClass);
            this.name = name;
            this.orbitalParamaters = orbitalParameters;
            this.multiplier = multiplier;
        }

        public OrbitDataLoaderParameter(String name, Class<? extends IOrbitDataProvider> providerClass, OrbitComponent orbitalParameters, double multiplier, int numSamples) {
            this(providerClass);
            this.name = name;
            this.orbitalParamaters = orbitalParameters;
            this.multiplier = multiplier;
            this.numSamples = numSamples;
        }

        public OrbitDataLoaderParameter(Class<? extends IOrbitDataProvider> providerClass, String name, Date ini, boolean forward, double orbitalPeriod, int numSamples) {
            this(providerClass);
            this.name = name;
            this.ini = ini;
            this.forward = forward;
            this.orbitalPeriod = orbitalPeriod;
            this.numSamples = numSamples;
        }

        public OrbitDataLoaderParameter(Class<? extends IOrbitDataProvider> providerClass, String name, Date ini, boolean forward, double orbitalPeriod) {
            this(providerClass, name, ini, forward, orbitalPeriod, -1);
        }

        public void setIni(Date date) {
            this.ini = date;
        }

        public void setForward(boolean fwd) {
            this.forward = fwd;
        }

        public void setOrbitalPeriod(double period) {
            this.orbitalPeriod = period;
        }
    }
}
