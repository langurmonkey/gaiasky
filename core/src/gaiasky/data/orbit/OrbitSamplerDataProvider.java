/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.orbit;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import com.badlogic.gdx.files.FileHandle;
import gaiasky.data.api.IOrbitDataProvider;
import gaiasky.data.util.OrbitDataLoader.OrbitDataLoaderParameters;
import gaiasky.data.util.PointCloudData;
import gaiasky.gui.ConsoleLogger;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Trajectory;
import gaiasky.util.Logger;
import gaiasky.util.Settings;
import gaiasky.util.SettingsManager;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.MathManager;
import gaiasky.util.math.Vector3b;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Date;

/**
 * Samples orbits using the classical algorithms (VSOP87, Moon AA, Pluto) and converts them to point cloud
 * data objects. Used to update the orbit objects of planets when they get outdated.
 *
 * @deprecated We use {@link OrbitBodyDataProvider} instead, which uses the actual coordinates provider of the
 * attached body.
 */
@Deprecated
public class OrbitSamplerDataProvider implements IOrbitDataProvider {
    private static final String writeDataPath = "/tmp/";
    private static boolean writeData = false;
    private final Vector3b ecl = new Vector3b();
    PointCloudData data;

    public static void main(String[] args) {
        try {
            // Assets location
            String ASSETS_LOC = Settings.ASSETS_LOC;

            // Logger
            new ConsoleLogger();

            Gdx.files = new Lwjgl3Files();

            SettingsManager.initialize(new FileInputStream(ASSETS_LOC + "/conf/config.yaml"), new FileInputStream(ASSETS_LOC + "/dummyversion"));

            I18n.initialize(new FileHandle(ASSETS_LOC + "/i18n/gsbundle"), new FileHandle(ASSETS_LOC + "/i18n/objects"));

            // Initialize math manager
            MathManager.initialize();

            OrbitSamplerDataProvider.writeData = true;
            OrbitSamplerDataProvider me = new OrbitSamplerDataProvider();

            Date now = new Date();
            String[] bodies = new String[]{"Mercury", "Venus", "Earth", "Mars", "Jupiter", "Saturn", "Uranus", "Neptune", "Moon", "Pluto"};
            double[] periods = new double[]{87.9691, 224.701, 365.256363004, 686.971, 4332.59, 10759.22, 30799.095, 60190.03, 27.321682, 90560.0};
            for (int i = 0; i < bodies.length; i++) {
                String b = bodies[i];
                double period = periods[i];
                OrbitDataLoaderParameters param = new OrbitDataLoaderParameters(me.getClass(), b, now, period, 500);
                me.load(null, param);

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void initialize(Entity entity, Trajectory trajectory) {

    }

    @Override
    public void load(String file,
                     OrbitDataLoaderParameters parameter) {
        // Sample using VSOP87
        // If num samples is not defined, we use 300 samples per year of period

        // Prevent overlapping by rescaling the period
        double period = parameter.orbitalPeriod * 0.999d;
        int numSamples = parameter.numSamples > 0 ? parameter.numSamples : (int) (300.0 * period / 365.0);
        numSamples = Math.max(200, Math.min(2000, numSamples));
        data = new PointCloudData();
        String bodyDesc = parameter.name;

        // Milliseconds of this orbit in one revolution
        double orbitalMs = period * 86400000.0;
        double stepMs = orbitalMs / (double) numSamples;

        var trajectory = parameter.entity != null ? Mapper.trajectory.get(parameter.entity) : null;
        Instant d;
        if (parameter.force) {
            // Forcing, use orbit starting now.
            d = Instant.ofEpochMilli(parameter.ini.getTime());
            parameter.setForce(false);
        } else if (trajectory != null && trajectory.refreshRate >= 0) {
            // User-defined refresh rate.
            d = Instant.ofEpochMilli(parameter.ini.getTime() - (long) (orbitalMs * trajectory.refreshRate));
        } else if (period > 40000) {
            // For long-period, it is better to recompute more often because they can deviate significantly.
            d = Instant.ofEpochMilli(parameter.ini.getTime() - (long) (orbitalMs * 0.8));
        } else if (parameter.entity != null && Mapper.base.get(parameter.entity).ct.isEnabled(ComponentType.Moons)) {
            // For moon orbits, it is better to recompute more often because they can deviate significantly.
            d = Instant.ofEpochMilli(parameter.ini.getTime() - (long) (orbitalMs * 0.4));
        } else {
            // Shorter period orbits don't deviate enough to be noticeable.
            d = Instant.ofEpochMilli(parameter.ini.getTime());
        }


        var coordinates = Mapper.coordinates.get(parameter.entity);
        // Load orbit data
        for (int i = 0; i <= numSamples; i++) {
            coordinates.coordinates.getEclipticCartesianCoordinates(d, ecl);
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

        Logger.getLogger(this.getClass()).info(I18n.msg("notif.orbitdataof.loaded", parameter.name, data.getNumPoints()));

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
