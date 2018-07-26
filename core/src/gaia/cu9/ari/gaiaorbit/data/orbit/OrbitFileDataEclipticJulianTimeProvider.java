package gaia.cu9.ari.gaiaorbit.data.orbit;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

import gaia.cu9.ari.gaiaorbit.assets.OrbitDataLoader.OrbitDataLoaderParameter;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;

/**
 * Reads an orbit file into an OrbitData object.
 * @author Toni Sagrista
 *
 */
public class OrbitFileDataEclipticJulianTimeProvider implements IOrbitDataProvider {
    PolylineData data;

    @Override
    public void load(String file, OrbitDataLoaderParameter parameter) {
        FileDataLoaderEclipticJulianTime odl = new FileDataLoaderEclipticJulianTime();
        try {
            FileHandle f = Gdx.files.internal(file);
            data = odl.load(f.read());
            EventManager.instance.post(Events.ORBIT_DATA_LOADED, data, file);
        } catch (Exception e) {
            Gdx.app.error(OrbitFileDataEclipticJulianTimeProvider.class.getName(), e.getMessage());
        }
    }

    @Override
    public void load(String file, OrbitDataLoaderParameter parameter, boolean newmethod) {
        load(file, parameter);
    }

    public PolylineData getData() {
        return data;
    }

}