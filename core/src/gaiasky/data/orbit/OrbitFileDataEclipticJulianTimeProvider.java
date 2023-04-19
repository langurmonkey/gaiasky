/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.orbit;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import gaiasky.data.api.IOrbitDataProvider;
import gaiasky.data.util.OrbitDataLoader.OrbitDataLoaderParameters;
import gaiasky.data.util.PointCloudData;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.util.Settings;

public class OrbitFileDataEclipticJulianTimeProvider implements IOrbitDataProvider {
    PointCloudData data;

    @Override
    public void load(String file, OrbitDataLoaderParameters parameter) {
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
    public void load(String file, OrbitDataLoaderParameters parameter, boolean newMethod) {
        load(file, parameter);
    }

    public PointCloudData getData() {
        return data;
    }

}