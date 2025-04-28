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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static gaiasky.util.math.MathUtilsDouble.PI2;

/**
 * Provider for orbital elements based data.
 */
public class OrbitalParametersProvider implements IOrbitDataProvider {
    /** The data that holds the orbit. **/
    private PointCloudData data;

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
            try {
                switch (parameter.sampling) {
                    case TIME -> sampleOrbitInTime(parameter);
                    case NU -> sampleOrbitInNu(parameter);
                }
            } catch (Exception e) {
                Logger.getLogger(this.getClass()).error(e);
            }
        } else {
            loadOld(file, parameter);
        }
    }

    /**
     * Samples points in the orbit uniformly in time. The number of points to sample is in {@link OrbitDataLoaderParameters#numSamples}.
     *
     * @param parameter The orbital parameters.
     */
    private void sampleOrbitInTime(OrbitDataLoaderParameters parameter) {
        OrbitComponent params = parameter.orbitalParamaters;
        Vector3d out = new Vector3d();
        double period = params.period; // in days
        double epoch = params.epoch; // in days

        data = new PointCloudData();
        data.period = period;

        // Step time in days, a full period over number of samples starting at epoch
        double tStep = period / (parameter.numSamples - 1.0);
        double t = 0.0;

        for (int i = 0; i < parameter.numSamples; i++) {
            params.loadDataPoint(out, t);

            var time = AstroUtils.julianDateToInstant(epoch + t);
            if (i == parameter.numSamples - 1) {
                // Close orbit
                double sx = data.getX(0);
                double sy = data.getY(0);
                double sz = data.getZ(0);
                data.addPoint(sx, sy, sz, time);
            } else {
                // Add point
                data.addPoint(out.x, out.y, out.z, time);
            }

            t += tStep;
        }
    }

    /**
     * Samples points in the orbit uniformly in true anomaly (nu). The number of points to sample is in {@link OrbitDataLoaderParameters#numSamples}.
     *
     * @param parameter The orbital parameters.
     */
    private void sampleOrbitInNu(OrbitDataLoaderParameters parameter) {
        OrbitComponent params = parameter.orbitalParamaters;
        Vector3d out = new Vector3d();
        double period = params.period; // in days

        data = new PointCloudData();
        data.period = period;

        // Find true anomaly at epoch.
        double nu0 = params.timeToTrueAnomaly(0);

        // Step in nu.
        double nuStep = PI2 / (parameter.numSamples - 1);
        double nu = nu0;

        for (int i = 0; i < parameter.numSamples; i++) {
            params.loadDataPointNu(out, nu);

            var dtDays = params.trueAnomalyToTime(nu);

            var time = AstroUtils.julianDateToInstant(params.epoch + dtDays);
            if (i == parameter.numSamples - 1) {
                // Close orbit
                double sx = data.getX(0);
                double sy = data.getY(0);
                double sz = data.getZ(0);
                data.addPoint(sx, sy, sz, time);
            } else {
                // Add point
                data.addPoint(out.x, out.y, out.z, time);
            }

            nu += nuStep;
        }

        // Sort samples by time.
        data.samples.sort(Comparator.comparing(PointCloudData.PointSample::time));
    }

    public void loadOld(String file, OrbitDataLoaderParameters parameter) {
        OrbitComponent params = parameter.orbitalParamaters;
        if (params == null)
            return;
        try {
            // Parameters of the ellipse
            double a = params.semiMajorAxis;
            double f = params.e * params.semiMajorAxis;
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
                data.addPoint(point.x, point.y, point.z, Instant.now());
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
