/*
 * Copyright (c) 2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.desktop.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import com.badlogic.gdx.files.FileHandle;
import gaiasky.data.orbit.OrbitFileDataProvider;
import gaiasky.data.util.OrbitDataLoader;
import gaiasky.gui.ConsoleLogger;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Settings;
import gaiasky.util.SettingsManager;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.CatmullRomSplineDouble;
import gaiasky.util.math.Vector3d;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.Locale;

public class PathReSampler {
    private static final Logger.Log logger = Logger.getLogger(PathReSampler.class);
    private static final int TARGET_POINTS = 5000;

    public static void main(String[] args) throws IOException {
        I18n.locale = Locale.getDefault();
        new ConsoleLogger();
        Gdx.files = new Lwjgl3Files();
        try {
            SettingsManager.initialize(false);
        } catch (Exception e) {
            Logger.getLogger(Positions2DExtractor.class).error(e);
        }
        String ASSETS_LOC = Settings.ASSETS_LOC + "/";
        I18n.initialize(new FileHandle(ASSETS_LOC + "i18n/gsbundle"), new FileHandle(ASSETS_LOC + "i18n/objects"));

        if (args.length == 0) {
            logger.error("An input file is needed to run.");
            System.exit(-1);
        }
        String dataFile = args[0];
        PathReSampler reSampler = new PathReSampler();
        reSampler.process(dataFile);
    }

    public void process(String dataFile) {
        OrbitFileDataProvider provider = new OrbitFileDataProvider();
        OrbitDataLoader.OrbitDataLoaderParameters params = new OrbitDataLoader.OrbitDataLoaderParameters(provider.getClass());
        params.multiplier = 1;

        provider.load(dataFile, params);
        var data = provider.getData();
        logger.info("Data loaded from " + data.getStart() + " to " + data.getEnd() + " with " + data.x.size() + " points.");

        Vector3d[] points = new Vector3d[data.x.size()];
        for (int i = 0; i < data.x.size(); i++) {
            points[i] = new Vector3d(data.getX(i), data.getY(i), data.getZ(i));
        }

        CatmullRomSplineDouble<Vector3d> spline = new CatmullRomSplineDouble<>(points, true);
        long start = data.time.get(0).toEpochMilli();
        long end = data.time.get(data.time.size() - 1).toEpochMilli();

        int n = TARGET_POINTS;
        Vector3d[] fp = new Vector3d[n];
        long[] ft = new long[n];

        double vStep = 1.0 / n;
        long tStep = (end - start) / n;
        for (int i = 0; i < n; i++) {
            double v = vStep * i;

            // Get XYZ
            Vector3d out = new Vector3d();
            spline.valueAt(out, v);
            out.scl(Constants.U_TO_KM);

            // Get time
            long t = start + tStep * i;

            fp[i] = out;
            ft[i] = t;
        }


        String outputFileName = System.getProperty("user.home") + "/temp/gaiasky/euclid/orb.EUCLID." + n + ".dat";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName, false))) {
            writer.write("# time X Y Z");
            writer.newLine();
            for (int i = 0; i < n; i++) {
                var t = ft[i];
                var v = fp[i];

                var inst = Instant.ofEpochMilli(t);
                var instStr = inst.toString().replace('T', '_').replace("Z", "");
                writer.write(instStr + " " + v.x + " " + v.y + " " + v.z);
                writer.newLine();
            }
            logger.info("File written to " + outputFileName);
        } catch (IOException ex) {
            logger.error(ex);
        }
    }
}
