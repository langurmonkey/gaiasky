/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.orbit;

import com.badlogic.gdx.files.FileHandle;
import gaiasky.data.util.OrbitDataLoader.OrbitDataLoaderParameters;
import gaiasky.data.util.PointCloudData;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.util.Logger;
import gaiasky.util.Settings;

public class OrbitFileDataProvider implements IOrbitDataProvider {
    PointCloudData data;

    @Override
    public void load(String file, OrbitDataLoaderParameters parameter) {
        if (file != null) {
            FileDataLoader odl = new FileDataLoader();
            try {
                FileHandle f = Settings.settings.data.dataFileHandle(file);
                data = odl.load(f.read());
                if (parameter.multiplier != 1f) {
                    int n = data.x.size();
                    for (int i = 0; i < n; i++) {
                        data.x.set(i, data.x.get(i) * parameter.multiplier);
                        data.y.set(i, data.y.get(i) * parameter.multiplier);
                        data.z.set(i, data.z.get(i) * parameter.multiplier);
                    }
                }
                EventManager.publish(Event.ORBIT_DATA_LOADED, this, data, file);
            } catch (Exception e) {
                Logger.getLogger(this.getClass()).error(e);
            }
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
