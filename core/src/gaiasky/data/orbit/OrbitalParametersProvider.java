/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.orbit;

import com.badlogic.ashley.core.Entity;
import gaiasky.data.api.IOrbitDataProvider;
import gaiasky.data.util.OrbitDataLoader.OrbitDataLoaderParameters;
import gaiasky.data.util.PointCloudData;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.scene.component.Trajectory;
import gaiasky.scene.record.OrbitComponent;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector3d;
import net.jafama.FastMath;

import java.time.Instant;

public class OrbitalParametersProvider implements IOrbitDataProvider {
    PointCloudData data;

    public OrbitalParametersProvider() {
        super();
    }


    @Override
    public void initialize(Entity entity, Trajectory trajectory) {

    }

    @Override
    public void load(String file, OrbitDataLoaderParameters parameter) {
        load(file, parameter, false);
    }

    @Override
    public void load(String file, OrbitDataLoaderParameters parameter, boolean newMethod) {
        if (newMethod) {
            OrbitComponent params = parameter.orbitalParamaters;
            Vector3d out = new Vector3d();
            try {
                double period = params.period; // in days
                double epoch = params.epoch; // in days

                data = new PointCloudData();
                data.period = period;

                // Step time in days, a full period over number of samples starting at epoch
                double t_step = period / (parameter.numSamples - 1.0);
                double t = 0.0;

                for (int n = 0; n < parameter.numSamples; n++) {
                    params.loadDataPoint(out, t);

                    if (n == parameter.numSamples - 1) {
                        // Close orbit
                        double sx = data.getX(0);
                        double sy = data.getY(0);
                        double sz = data.getZ(0);
                        data.x.add(sx);
                        data.y.add(sy);
                        data.z.add(sz);
                    } else {
                        // Add point
                        data.x.add(out.x);
                        data.y.add(out.y);
                        data.z.add(out.z);
                    }
                    data.time.add(AstroUtils.julianDateToInstant(epoch + t));

                    t += t_step;
                }

                EventManager.publish(Event.ORBIT_DATA_LOADED, this, data, parameter.name);
            } catch (Exception e) {
                Logger.getLogger(this.getClass()).error(e);
            }
        } else {
            loadOld(file, parameter);
        }
    }

    public void loadOld(String file, OrbitDataLoaderParameters parameter) {
        OrbitComponent params = parameter.orbitalParamaters;
        try {
            // Parameters of the ellipse
            double a = params.semimajoraxis;
            double f = params.e * params.semimajoraxis;
            double b = FastMath.sqrt(Math.pow(a, 2) - FastMath.pow(f, 2));

            int nSamples = FastMath.min(Math.max(50, (int) (a * 0.01)), 100);
            double step = 360d / nSamples;
            Vector3d[] samples = new Vector3d[nSamples + 1];
            int i = 0;
            for (double angledeg = 0; angledeg < 360; angledeg += step) {
                double angleRad = FastMath.toRadians(angledeg);
                Vector3d point = new Vector3d(b * FastMath.sin(angleRad), 0d, a * FastMath.cos(angleRad));
                samples[i] = point;
                i++;
            }
            // Last, to close the orbit.
            samples[i] = samples[0].cpy();

            Matrix4d transform = new Matrix4d();
            transform.scl(Constants.KM_TO_U);
            data = new PointCloudData();
            for (Vector3d point : samples) {
                point.mul(transform);
                data.x.add(point.x);
                data.y.add(point.y);
                data.z.add(point.z);
                data.time.add(Instant.now());
            }
            EventManager.publish(Event.ORBIT_DATA_LOADED, this, data, parameter.name);
        } catch (Exception e) {
            Logger.getLogger(this.getClass()).error(e);
        }
    }

    public PointCloudData getData() {
        return data;
    }

}
