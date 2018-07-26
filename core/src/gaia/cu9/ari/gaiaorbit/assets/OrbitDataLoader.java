package gaia.cu9.ari.gaiaorbit.assets;

import java.util.Date;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.reflect.ClassReflection;

import gaia.cu9.ari.gaiaorbit.data.orbit.IOrbitDataProvider;
import gaia.cu9.ari.gaiaorbit.data.orbit.PolylineData;
import gaia.cu9.ari.gaiaorbit.scenegraph.component.OrbitComponent;
import gaia.cu9.ari.gaiaorbit.util.Logger;

/**
 * Abstract data loader to rule them all.
 * 
 * @author Toni Sagrista
 *
 */
public class OrbitDataLoader extends AsynchronousAssetLoader<PolylineData, OrbitDataLoader.OrbitDataLoaderParameter> {

    PolylineData data;

    public OrbitDataLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    @Override
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
            Logger.error(e, getClass().getSimpleName());
        }

    }

    /**
     * 
     */
    public PolylineData loadSync(AssetManager manager, String fileName, FileHandle file, OrbitDataLoaderParameter parameter) {
        return data;
    }

    static public class OrbitDataLoaderParameter extends AssetLoaderParameters<PolylineData> {

        Class<? extends IOrbitDataProvider> providerClass;
        public Date ini;
        public boolean forward;
        public float orbitalPeriod;
        public double multiplier;
        public int numSamples;
        public String name;
        public OrbitComponent orbitalParamaters;

        public OrbitDataLoaderParameter(Class<? extends IOrbitDataProvider> providerClass) {
            this.providerClass = providerClass;
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

        public OrbitDataLoaderParameter(Class<? extends IOrbitDataProvider> providerClass, String name, Date ini, boolean forward, float orbitalPeriod, int numSamples) {
            this(providerClass);
            this.name = name;
            this.ini = ini;
            this.forward = forward;
            this.orbitalPeriod = orbitalPeriod;
            this.numSamples = numSamples;
        }

        public OrbitDataLoaderParameter(Class<? extends IOrbitDataProvider> providerClass, String name, Date ini, boolean forward, float orbitalPeriod) {
            this(providerClass, name, ini, forward, orbitalPeriod, -1);
        }

        public void setIni(Date date) {
            this.ini = date;
        }

        public void setForward(boolean fwd) {
            this.forward = fwd;
        }

        public void setOrbitalPeriod(float period) {
            this.orbitalPeriod = period;
        }
    }
}
