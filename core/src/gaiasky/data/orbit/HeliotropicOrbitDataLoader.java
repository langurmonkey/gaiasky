/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.orbit;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import com.badlogic.gdx.files.FileHandle;
import gaiasky.data.util.PointCloudData;
import gaiasky.gui.main.ConsoleLogger;
import gaiasky.util.*;
import gaiasky.util.Logger.Log;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.Vector3D;

import java.io.*;
import java.time.Instant;
import java.util.Calendar;

public class HeliotropicOrbitDataLoader {
    static Log logger = Logger.getLogger(HeliotropicOrbitDataLoader.class);
    int count = 0;
    // Maximum time between accepted samples
    long maxMsSep = (long) (12d * Nature.H_TO_MS);

    public HeliotropicOrbitDataLoader() {
        super();

    }

    public static void main(String[] args) {
        HeliotropicOrbitDataLoader l = new HeliotropicOrbitDataLoader();
        try {
            // Assets location
            String ASSETS_LOC = Settings.ASSETS_LOC;

            // Logger
            new ConsoleLogger();

            Gdx.files = new Lwjgl3Files();

            SettingsManager.initialize(new FileInputStream(ASSETS_LOC + "/conf/config.yaml"), new FileInputStream(ASSETS_LOC + "/dummyversion"));

            I18n.initialize(new FileHandle(ASSETS_LOC + "/i18n/gsbundle"), new FileHandle(ASSETS_LOC + "/i18n/objects"));

            String inputFile = System.getProperty("user.home") + "/Downloads/orbit.JWST.heliotropic.csv";
            String outputFile = System.getProperty("user.home") + "/Downloads/orbit.JWST.dat";

            PointCloudData od = l.load(new FileInputStream(inputFile));
            logger.info("Loaded and converted " + od.getNumPoints() + " orbit data points: " + inputFile);

            OrbitDataWriter.writeOrbitData(outputFile, od);
            logger.info("Results written successfully: " + outputFile);

        } catch (Exception e) {
            logger.error(e);
        }
    }

    /**
     * Loads the data in the input stream and transforms it into Cartesian
     * <b>ecliptic</b> coordinates. The reference system of the data goes as
     * follows:
     * <ul>
     * <li>Origin of frame : Earth</li>
     * <li>X and Y axis in the EQUATORIAL PLANE with X pointing in the direction
     * of vernal equinox.</li>
     * <li>Z perpendicular to the the EQUATORIAL PLANE in the north direction
     * </li>
     * <li>The Y direction is defined to have (X,Y,Z) as a "three axis"
     * positively oriented.</li>
     * </ul>
     * <p>
     * The simulation reference system:
     * <ul>
     * <li>- XZ lies in the ECLIPTIC PLANE, with Z pointing to the vernal
     * equinox.</li>
     * <li>- Y perpendicular to the ECLIPTIC PLANE pointing north.</li>
     * </ul>
     *
     * @param data The input stream with the data to load
     */
    public PointCloudData load(InputStream data) throws Exception {
        PointCloudData orbitData = new PointCloudData();

        BufferedReader br = new BufferedReader(new InputStreamReader(data));
        String line;

        Instant previousAddedTime = null;

        int lineNum = 1;
        while ((line = br.readLine()) != null) {
            // Skip header (first line)
            if (lineNum > 1 && !line.isEmpty() && !line.startsWith("#")) {
                String[] tokens = line.split(",");
                try {
                    // Valid data line
                    // Julian date
                    double jd = parsed(tokens[0]);
                    Instant time = AstroUtils.julianDateToInstant(jd);

                    /*
                     * From Data coordinates to OpenGL world coordinates Z -> -X
                     * X -> Y Y -> Z
                     */
                    Vector3D pos = new Vector3D(parsed(tokens[2]), parsed(tokens[3]), -parsed(tokens[1]));

                    // Transform to heliotropic using the Sun's ecliptic longitude
                    Vector3D posHel = correctSunLongitude(pos, time);

                    // To ecliptic again
                    pos.mul(Coordinates.eqToEcl());
                    posHel.mul(Coordinates.eqToEcl());

                    boolean add = count == 0 || previousAddedTime == null || (time.toEpochMilli() - previousAddedTime.toEpochMilli() >= maxMsSep);

                    if (add) {
                        orbitData.addPoint(posHel.x * Constants.KM_TO_U, posHel.y * Constants.KM_TO_U, posHel.z * Constants.KM_TO_U, time);
                        previousAddedTime = time;
                    }
                    count++;
                } catch (Exception e) {
                    logger.error("Error loading line: " + count);
                }

            }
            lineNum++;
        }

        br.close();

        return orbitData;
    }

    protected float parsef(String str) {
        return Float.parseFloat(str);
    }

    protected double parsed(String str) {
        return Double.parseDouble(str);
    }

    protected int parsei(String str) {
        return Integer.parseInt(str);
    }

    /**
     * Transforms the given vector to a heliotropic system using the given time.
     *
     * @param pos Position vector
     * @param t   Time
     *
     * @return Vector3 with the position in the heliotropic reference frame
     */
    protected Vector3D correctSunLongitude(final Vector3D pos, Instant t) {
        Vector3D upDirection = new Vector3D(0, 1, 0);
        // We get the Up direction of the ecliptic in equatorial coordinates
        upDirection.mul(Coordinates.eclToEq());
        return pos.cpy()
                .rotate(upDirection, AstroUtils.getSunLongitude(t) - (float) 0);
    }

    private float getYearFraction(int year, int month, int day, int hour, int min, int sec) {
        return year + month / 12f + day / 365.242f + hour / 8765.81f + min / 525949f + sec / 31556940f;
    }

    private float getYearFraction(long time) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int min = cal.get(Calendar.MINUTE);
        int sec = cal.get(Calendar.SECOND);

        return getYearFraction(year, month, day, hour, min, sec);
    }

    /**
     * Writes a file under the given path with the distance data
     */
    public void writeDistVsTimeData(String filePath, PointCloudData data) throws Exception {
        File file = new File(filePath);
        if (file.exists()) {
            if (!file.delete()) {
                logger.warn("Could not delete file: " + file);
            }
        }
        if (!file.exists()) {
            if (!file.createNewFile()) {
                logger.warn("Could not create file: " + file);
            }
        }
        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write("#time[ms] time[year] dist[km]");
        bw.newLine();
        long iniTime = -1;

        int n = data.samples.size();
        for (int i = 0; i < n; i++) {
            var p = data.samples.get(i);
            Vector3D pos = new Vector3D(p.x(), p.y(), p.z());
            long t = p.toEpochMilli();

            long time = iniTime < 0 ? 0 : t - iniTime;
            if (time == 0) {
                iniTime = t;
            }
            float timeYearFraction = getYearFraction(iniTime + time);

            bw.write(time + " " + timeYearFraction + " " + pos.len());
            bw.newLine();

        }

        bw.close();
    }

}
