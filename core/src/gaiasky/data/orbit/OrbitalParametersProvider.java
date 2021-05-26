/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.orbit;

import gaiasky.assets.OrbitDataLoader.OrbitDataLoaderParameter;
import gaiasky.data.util.PointCloudData;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.scenegraph.component.OrbitComponent;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Nature;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector3d;

import java.time.Instant;

/**
 * Reads an orbit file into an OrbitData object.
 *
 * @author Toni Sagrista
 */
public class OrbitalParametersProvider implements IOrbitDataProvider {
    PointCloudData data;

    public OrbitalParametersProvider() {
        super();
    }

    @Override
    public void load(String file, OrbitDataLoaderParameter parameter) {
        load(file, parameter, false);
    }

    @Override
    public void load(String file, OrbitDataLoaderParameter parameter, boolean newmethod) {
        if (newmethod) {
            OrbitComponent params = parameter.orbitalParamaters;
            try {
                // See https://downloads.rene-schwarz.com/download/M001-Keplerian_Orbit_Elements_to_Cartesian_State_Vectors.pdf
                double period = params.period; // in days
                double epoch = params.epoch; // in days
                double a = params.semimajoraxis * 1000d; // km to m
                double e = params.e;
                double i = params.i * MathUtilsd.degRad;
                double omega_lan = params.ascendingnode * MathUtilsd.degRad;
                double omega_ap = params.argofpericenter * MathUtilsd.degRad;
                double M0 = params.meananomaly * MathUtilsd.degRad;
                double mu = params.mu;

                data = new PointCloudData();
                data.period = period;

                // Step time in days, a full period over number of samples starting at epoch
                double t_step = period / (parameter.numSamples - 1.0);
                double t = 0.0;

                for (int n = 0; n < parameter.numSamples; n++) {
                    // 1
                    double dt = t * Nature.D_TO_S;
                    double M = M0 + dt * Math.sqrt(mu / Math.pow(a, 3.0));

                    // 2
                    double E = M;
                    for (int j = 0; j < 2; j++) {
                        E = E - ((E - e * Math.sin(E) - M) / (1.0 - e * Math.cos(E)));
                    }
                    double E_t = E;

                    // 3
                    double nu_t = 2.0 * Math.atan2(Math.sqrt(1.0 + e) * Math.sin(E_t / 2.0), Math.sqrt(1.0 - e) * Math.cos(E_t / 2.0));

                    // 4
                    double rc_t = a * (1.0 - e * Math.cos(E_t));

                    // 5
                    double ox = rc_t * Math.cos(nu_t);
                    double oy = rc_t * Math.sin(nu_t);

                    // 6
                    double sinomega = Math.sin(omega_ap);
                    double cosomega = Math.cos(omega_ap);
                    double sinOMEGA = Math.sin(omega_lan);
                    double cosOMEGA = Math.cos(omega_lan);
                    double cosi = Math.cos(i);
                    double sini = Math.sin(i);

                    double x = ox * (cosomega * cosOMEGA - sinomega * cosi * sinOMEGA) - oy * (sinomega * cosOMEGA + cosomega * cosi * sinOMEGA);
                    double y = ox * (cosomega * sinOMEGA + sinomega * cosi * cosOMEGA) + oy * (cosomega * cosi * cosOMEGA - sinomega * sinOMEGA);
                    double z = ox * (sinomega * sini) + oy * (cosomega * sini);

                    // 7
                    x *= Constants.M_TO_U;
                    y *= Constants.M_TO_U;
                    z *= Constants.M_TO_U;

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
                        data.x.add(y);
                        data.y.add(z);
                        data.z.add(x);
                    }
                    data.time.add(AstroUtils.julianDateToInstant(epoch + t));

                    t += t_step;
                }

                EventManager.instance.post(Events.ORBIT_DATA_LOADED, data, parameter.name);
            } catch (Exception e) {
                Logger.getLogger(this.getClass()).error(e);
            }
        } else {
            loadOld(file, parameter);
        }
    }

    public void loadOld(String file, OrbitDataLoaderParameter parameter) {
        OrbitComponent params = parameter.orbitalParamaters;
        try {
            // Parameters of the ellipse
            double a = params.semimajoraxis;
            double f = params.e * params.semimajoraxis;
            double b = Math.sqrt(Math.pow(a, 2) - Math.pow(f, 2));

            int nSamples = Math.min(Math.max(50, (int) (a * 0.01)), 100);
            double step = 360d / nSamples;
            Vector3d[] samples = new Vector3d[nSamples + 1];
            int i = 0;
            for (double angledeg = 0; angledeg < 360; angledeg += step) {
                double angleRad = Math.toRadians(angledeg);
                Vector3d point = new Vector3d(b * Math.sin(angleRad), 0d, a * Math.cos(angleRad));
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
            EventManager.instance.post(Events.ORBIT_DATA_LOADED, data, parameter.name);
        } catch (Exception e) {
            Logger.getLogger(this.getClass()).error(e);
        }
    }

    public PointCloudData getData() {
        return data;
    }

}
