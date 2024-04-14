/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.orbit;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.files.FileHandle;
import gaiasky.data.api.IOrbitDataProvider;
import gaiasky.data.util.OrbitDataLoader.OrbitDataLoaderParameters;
import gaiasky.data.util.PointCloudData;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.scene.component.Trajectory;
import gaiasky.util.Logger;
import gaiasky.util.Settings;
import gaiasky.util.io.GzipUtils;

import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public class OrbitFileDataProvider implements IOrbitDataProvider {
    private PointCloudData data;

    @Override
    public void initialize(Entity entity,
                           Trajectory trajectory) {

    }

    @Override
    public void load(String file,
                     OrbitDataLoaderParameters parameter) {
        if (file != null) {
            FileDataLoader odl = new FileDataLoader();
            FileHandle f = Settings.settings.data.dataFileHandle(file);
            try {
                final InputStream is;
                var isGzip = false;
                try (var fis = f.read()){
                    isGzip = GzipUtils.isGZipped(fis);
                }
                var fis = f.read();
                if (isGzip) {
                    is = new GZIPInputStream(fis);
                } else {
                    is = fis;
                }
                data = odl.load(is);
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
    public void load(String file,
                     OrbitDataLoaderParameters parameter,
                     boolean newMethod) {
        load(file, parameter);
    }

    public PointCloudData getData() {
        return data;
    }

}
