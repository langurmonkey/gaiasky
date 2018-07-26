package gaia.cu9.ari.gaiaorbit.data.orbit;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

import gaia.cu9.ari.gaiaorbit.assets.OrbitDataLoader.OrbitDataLoaderParameter;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.util.Logger;

/**
 * Reads an orbit file into an OrbitData object.
 * 
 * @author Toni Sagrista
 *
 */
public class OrbitFileDataProvider implements IOrbitDataProvider {
    PolylineData data;

    @Override
    public void load(String file, OrbitDataLoaderParameter parameter) {
        FileDataLoader odl = new FileDataLoader();
        try {
            FileHandle f = Gdx.files.internal(file);
            data = odl.load(f.read());
            if (parameter.multiplier != 1f) {
                int n = data.x.size;
                for (int i = 0; i < n; i++) {
                    data.x.set(i, data.x.get(i) * parameter.multiplier);
                    data.y.set(i, data.y.get(i) * parameter.multiplier);
                    data.z.set(i, data.z.get(i) * parameter.multiplier);
                }
            }
            EventManager.instance.post(Events.ORBIT_DATA_LOADED, data, file);
        } catch (Exception e) {
            Logger.error(e, OrbitFileDataProvider.class.getName());
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
