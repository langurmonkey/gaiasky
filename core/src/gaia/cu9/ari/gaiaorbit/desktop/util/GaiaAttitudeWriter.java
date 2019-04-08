/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.desktop.util;

import gaia.cu9.ari.gaiaorbit.util.coord.Coordinates;
import gaia.cu9.ari.gaiaorbit.util.gaia.Attitude;
import gaia.cu9.ari.gaiaorbit.util.gaia.GaiaAttitudeServer;
import gaia.cu9.ari.gaiaorbit.util.gaia.Satellite;
import gaia.cu9.ari.gaiaorbit.util.math.Quaterniond;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;

import java.io.*;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Writes Gaia attitude to a file. Assumes attitude has been initialised.
 * @author tsagrista
 *
 */
public class GaiaAttitudeWriter {
    enum OutputType {
        UP_VECTOR, FOV_VECTORS
    }

    private static final float BAM_2 = (float) Satellite.BASICANGLE_DEGREE / 2f;

    public static void attitudeZAxisToFile(OutputType type) {
        Calendar iniCal = GregorianCalendar.getInstance();
        // 2014-01-15T15:43:04
        iniCal.set(2014, 01, 15, 15, 43, 04);
        Date ini = iniCal.getTime();
        // 2019-06-20T06:20:05
        iniCal.set(2019, 06, 20, 05, 40, 00);
        Date end = iniCal.getTime();

        long tini = ini.getTime();
        long tend = end.getTime();
        Vector3d v1 = new Vector3d();
        Vector3d v2 = new Vector3d();
        Vector3d sph1 = new Vector3d();
        Vector3d sph2 = new Vector3d();

        String filenameEcl = "Gaia-ecliptic-Z-" + System.currentTimeMillis() + ".csv";
        String filenameEq = "Gaia-equatorial-Z-" + System.currentTimeMillis() + ".csv";
        File fecl = new File(System.getProperty("java.io.tmpdir") + File.separator + filenameEcl);
        File feq = new File(System.getProperty("java.io.tmpdir") + File.separator + filenameEq);

        try {
            Writer writerEcl = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fecl), "utf-8"));
            Writer writerEq = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(feq), "utf-8"));

            if (type.equals(OutputType.UP_VECTOR)) {
                writerEcl.append("#EclLong[deg], EclLat[deg], t[ms-January_1_1970_00:00:00_GMT], current[date], attitudeFile\n");
                writerEq.append("#alpha[deg], delta[deg], t[ms-January_1_1970_00:00:00_GMT], current[date], attitudeFile\n");
            } else if (type.equals(OutputType.FOV_VECTORS)) {
                writerEcl.append("#EclLong-fov1[deg], EclLat-fov1[deg], EclLong-fov2[deg], EclLat-fov2[deg],t[ms-January_1_1970_00:00:00_GMT], current[date], attitudeFile\n");
                writerEq.append("#alpha-fov1[deg], delta-fov1[deg], alpha-fov2[deg], delta-fov2[deg],t[ms-January_1_1970_00:00:00_GMT], current[date], attitudeFile\n");
            }
            // 10 minutes
            long step = 10 * 60000;
            for (long t = tini; t <= tend; t += step) {
                Date current = new Date(t);

                try {
                    Attitude att = GaiaAttitudeServer.instance.getAttitude(current);
                    Quaterniond quat = att.getQuaternion();

                    if (type.equals(OutputType.UP_VECTOR)) {
                        // Up vector in Gaia coordinates
                        v1.set(0, 1, 0);

                        // Equatorial
                        v1.rotateVectorByQuaternion(quat);
                        Coordinates.cartesianToSpherical(v1, sph1);
                        writerEq.append(Math.toDegrees(sph1.x) + ", " + Math.toDegrees(sph1.y) + ", " + t + ", " + current + ", " + GaiaAttitudeServer.instance.getCurrentAttitudeName() + "\n");

                        //Ecliptic
                        v1.mul(Coordinates.eqToEcl());
                        Coordinates.cartesianToSpherical(v1, sph1);
                        writerEcl.append(Math.toDegrees(sph1.x) + ", " + Math.toDegrees(sph1.y) + ", " + t + ", " + current + ", " + GaiaAttitudeServer.instance.getCurrentAttitudeName() + "\n");
                    } else if (type.equals(OutputType.FOV_VECTORS)) {
                        // Fov 1 and Fov 2
                        v1.set(0, 0, 1).rotate(BAM_2, 0, 1, 0);
                        v2.set(0, 0, 1).rotate(-BAM_2, 0, 1, 0);

                        // Equatorial
                        v1.rotateVectorByQuaternion(quat);
                        v2.rotateVectorByQuaternion(quat);
                        Coordinates.cartesianToSpherical(v1, sph1);
                        Coordinates.cartesianToSpherical(v2, sph2);
                        writerEq.append(Math.toDegrees(sph1.x) + ", " + Math.toDegrees(sph1.y) + ", " + Math.toDegrees(sph2.x) + ", " + Math.toDegrees(sph2.y) + ", " + t + ", " + current + ", " + GaiaAttitudeServer.instance.getCurrentAttitudeName() + "\n");

                        //Ecliptic
                        v1.mul(Coordinates.eqToEcl());
                        v2.mul(Coordinates.eqToEcl());
                        Coordinates.cartesianToSpherical(v1, sph1);
                        Coordinates.cartesianToSpherical(v2, sph2);
                        writerEcl.append(Math.toDegrees(sph1.x) + ", " + Math.toDegrees(sph1.y) + ", " + Math.toDegrees(sph2.x) + ", " + Math.toDegrees(sph2.y) + ", " + t + ", " + current + ", " + GaiaAttitudeServer.instance.getCurrentAttitudeName() + "\n");
                    }

                } catch (Exception e) {
                    System.out.println("Error: t=" + current);
                }

            }

            writerEcl.close();
            writerEq.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
