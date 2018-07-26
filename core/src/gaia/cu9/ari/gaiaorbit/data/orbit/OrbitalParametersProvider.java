package gaia.cu9.ari.gaiaorbit.data.orbit;

import java.time.Instant;

import gaia.cu9.ari.gaiaorbit.assets.OrbitDataLoader.OrbitDataLoaderParameter;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.scenegraph.component.OrbitComponent;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.coord.AstroUtils;
import gaia.cu9.ari.gaiaorbit.util.math.MathUtilsd;
import gaia.cu9.ari.gaiaorbit.util.math.Matrix4d;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;

/**
 * Reads an orbit file into an OrbitData object.
 * @author Toni Sagrista
 *
 */
public class OrbitalParametersProvider implements IOrbitDataProvider {
    PolylineData data;

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

                data = new PolylineData();

                // Step time in days, a full period over number of samples starting at epoch
                double t_step = period / parameter.numSamples;
                for (double t = 0; t <= period; t += t_step) {
                    // 1
                    double deltat = t * Constants.D_TO_S;
                    double M = M0 + deltat * Math.sqrt(mu / Math.pow(a, 3d));

                    // 2
                    double E = M;
                    for (int j = 0; j < 2; j++) {
                        E = E - ((E - e * Math.sin(E) - M) / (1 - e * Math.cos(E)));
                    }
                    double E_t = E;

                    // 3
                    double nu_t = 2d * Math.atan2(Math.sqrt(1d + e) * Math.sin(E_t / 2d), Math.sqrt(1d - e) * Math.cos(E_t / 2d));

                    // 4
                    double rc_t = a * (1d - e * Math.cos(E_t));

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

                    data.x.add(y);
                    data.y.add(z);
                    data.z.add(x);
                    data.time.add(AstroUtils.julianDateToInstant(epoch + t));
                }
                data.x.add(data.getX(0));
                data.y.add(data.getY(0));
                data.z.add(data.getZ(0));
                data.time.add(data.getDate(0));

                EventManager.instance.post(Events.ORBIT_DATA_LOADED, data, parameter.name);
            } catch (Exception e) {
                Logger.error(e);
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

            int nsamples = Math.min(Math.max(50, (int) (a * 0.01)), 100);
            double step = 360d / nsamples;
            Vector3d[] samples = new Vector3d[nsamples + 1];
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
            data = new PolylineData();
            for (Vector3d point : samples) {
                point.mul(transform);
                data.x.add(point.x);
                data.y.add(point.y);
                data.z.add(point.z);
                data.time.add(Instant.now());
            }
            EventManager.instance.post(Events.ORBIT_DATA_LOADED, data, parameter.name);
        } catch (Exception e) {
            Logger.error(e);
        }
    }

    public PolylineData getData() {
        return data;
    }

}
