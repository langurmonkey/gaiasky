/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.orbit;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import com.badlogic.gdx.files.FileHandle;
import gaiasky.data.util.OrbitDataLoader.OrbitDataLoaderParameter;
import gaiasky.data.util.PointCloudData;
import gaiasky.desktop.format.DesktopDateFormatFactory;
import gaiasky.desktop.format.DesktopNumberFormatFactory;
import gaiasky.interafce.ConsoleLogger;
import gaiasky.util.i18n.I18n;
import gaiasky.util.Logger;
import gaiasky.util.Settings;
import gaiasky.util.SettingsManager;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.format.DateFormatFactory;
import gaiasky.util.format.NumberFormatFactory;
import gaiasky.util.math.MathManager;
import gaiasky.util.math.Vector3b;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Instant;
import java.util.Date;

/**
 * Samples an orbit for a particular Body.
 */
public class OrbitSamplerDataProvider implements IOrbitDataProvider {
    private static boolean writeData = false;
    private static final String writeDataPath = "/tmp/";
    PointCloudData data;

    public static void main(String[] args) {
        try {
            // Assets location
            String ASSETS_LOC = Settings.ASSETS_LOC;

            // Logger
            new ConsoleLogger();

            Gdx.files = new Lwjgl3Files();

            // Initialize number format
            NumberFormatFactory.initialize(new DesktopNumberFormatFactory());

            // Initialize date format
            DateFormatFactory.initialize(new DesktopDateFormatFactory());

            SettingsManager.initialize(new FileInputStream(ASSETS_LOC + "/conf/config.yaml"), new FileInputStream(ASSETS_LOC + "/dummyversion"));

            I18n.initialize(new FileHandle(ASSETS_LOC + "/i18n/gsbundle"));

            // Initialize math manager
            MathManager.initialize();

            OrbitSamplerDataProvider.writeData = true;
            OrbitSamplerDataProvider me = new OrbitSamplerDataProvider();

            Date now = new Date();
            String[] bodies = new String[] { "Mercury", "Venus", "Earth", "Mars", "Jupiter", "Saturn", "Uranus", "Neptune", "Moon", "Pluto" };
            double[] periods = new double[] { 87.9691, 224.701, 365.256363004, 686.971, 4332.59, 10759.22, 30799.095, 60190.03, 27.321682, 90560.0 };
            for (int i = 0; i < bodies.length; i++) {
                String b = bodies[i];
                double period = periods[i];
                OrbitDataLoaderParameter param = new OrbitDataLoaderParameter(me.getClass(), b, now, true, period, 500);
                me.load(null, param);

            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private final Vector3b ecl = new Vector3b();

    @Override
    public void load(String file, OrbitDataLoaderParameter parameter) {
        // Sample using VSOP
        // If num samples is not defined, we use 300 samples per year of period

        // Prevent overlapping by rescaling the period
        double period = parameter.orbitalPeriod * 0.99d;
        int numSamples = parameter.numSamples > 0 ? parameter.numSamples : (int) (300.0 * period / 365.0);
        numSamples = Math.max(100, Math.min(2000, numSamples));
        data = new PointCloudData();
        String bodyDesc = parameter.name;
        Instant d = Instant.ofEpochMilli(parameter.ini.getTime());
        double last = 0, accum = 0;

        // Milliseconds of this orbit in one revolution
        double orbitalMs = period * 86400000.0;
        double stepMs = orbitalMs / (double) numSamples;

        // Load orbit data
        for (int i = 0; i <= numSamples; i++) {
            AstroUtils.getEclipticCoordinates(bodyDesc, d, ecl, true);
            double eclx = ecl.x.doubleValue();

            if (last == 0) {
                last = Math.toDegrees(eclx);
            }

            accum += Math.toDegrees(eclx) - last;
            last = Math.toDegrees(eclx);

            if (accum > 360) {
                break;
            }

            Coordinates.sphericalToCartesian(ecl, ecl);
            ecl.mul(Coordinates.eclToEq()).scl(1);
            data.x.add(ecl.x.doubleValue());
            data.y.add(ecl.y.doubleValue());
            data.z.add(ecl.z.doubleValue());
            data.time.add(d);

            d = Instant.ofEpochMilli(d.toEpochMilli() + (long) stepMs);
        }

        // Close the circle
        data.x.add(data.x.get(0));
        data.y.add(data.y.get(0));
        data.z.add(data.z.get(0));
        d = Instant.ofEpochMilli(d.toEpochMilli() + (long) stepMs);
        data.time.add(Instant.ofEpochMilli(d.toEpochMilli()));

        if (writeData) {
            try {
                OrbitDataWriter.writeOrbitData(writeDataPath + "orb." + bodyDesc.toUpperCase() + ".dat", data);
            } catch (IOException e) {
                Logger.getLogger(this.getClass()).error(e);
            }
        }

        Logger.getLogger(this.getClass()).info(I18n.txt("notif.orbitdataof.loaded", parameter.name, data.getNumPoints()));

    }

    @Override
    public void load(String file, OrbitDataLoaderParameter parameter, boolean newMethod) {
        load(file, parameter);
    }

    public PointCloudData getData() {
        return data;
    }

}
