/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.orbit;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import gaiasky.data.util.OrbitDataLoader.OrbitDataLoaderParameter;
import gaiasky.data.util.PointCloudData;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.util.Settings;

/**
 * Reads an orbit file into an OrbitData object.
 */
public class OrbitFileDataEclipticJulianTimeProvider implements IOrbitDataProvider {
    PointCloudData data;

    @Override
    public void load(String file, OrbitDataLoaderParameter parameter) {
        FileDataLoaderEclipticJulianTime odl = new FileDataLoaderEclipticJulianTime();
        try {
            FileHandle f = Settings.settings.data.dataFileHandle(file);
            data = odl.load(f.read());
            EventManager.publish(Event.ORBIT_DATA_LOADED, this, data, file);
        } catch (Exception e) {
            Gdx.app.error(OrbitFileDataEclipticJulianTimeProvider.class.getName(), e.getMessage());
        }
    }

    @Override
    public void load(String file, OrbitDataLoaderParameter parameter, boolean newMethod) {
        load(file, parameter);
    }

    public PointCloudData getData() {
        return data;
    }

}