/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.desktop.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import gaiasky.gui.ConsoleLogger;
import gaiasky.util.Logger;
import gaiasky.util.SettingsManager;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.StdRandom;
import gaiasky.util.math.Vector3d;

import java.io.*;

/**
 * Generates particles that make up the Oort cloud.
 */
public class OortGenerator {

    /** Whether to write the results to disk **/
    private static final boolean writeFile = true;

    /** Outer radius in AU **/
    private static final float outer_radius = 15000;

    /** Number of particles **/
    private static final int N = 10000;

    public static void main(String[] args) {
        try {
            Gdx.files = new Lwjgl3Files();

            SettingsManager.initialize(new FileInputStream("../assets/conf/config.yaml"), new FileInputStream("../assets/data/dummyversion"));

            I18n.initialize(new FileHandle(System.getenv("PROJECTS") + "/gaiasky/android/assets/i18n/gsbundle"),
                    new FileHandle(System.getenv("PROJECTS") + "/gaiasky/android/assets/i18n/objects"));

            // Add notif watch
            new ConsoleLogger();

            Array<double[]> oort = null;

            oort = generateOort();

            if (writeFile) {
                writeToDisk(oort, "/tmp/");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Generates random Oort cloud particles
     */
    private static Array<double[]> generateOort() throws RuntimeException {
        StdRandom.setSeed(100l);

        Array<double[]> particles = new Array<>(false, N);

        Vector3d particle = new Vector3d();
        int n = 0;
        // Generate only in z, we'll randomly rotate later
        while (n < N) {
            double x = (StdRandom.gaussian()) * outer_radius * 2;
            double y = (StdRandom.gaussian()) * outer_radius * 2;
            double z = (StdRandom.gaussian()) * outer_radius * 2;

            particle.set(x, y, z);

            // if (particle.len() <= outer_radius) {

            // // Rotation around X
            // double xAngle = StdRandom.uniform() * 360;
            // particle.rotate(xAxis, xAngle);
            //
            // // Rotation around Y
            // double yAngle = StdRandom.uniform() * 180 - 90;
            // particle.rotate(yAxis, yAngle);

            particles.add(new double[] { particle.x, particle.y, particle.z });
            n++;
            // }
        }

        return particles;
    }

    private static void writeToDisk(Array<double[]> oort, String dir) throws IOException {
        String filePath = dir + "oort_";
        filePath += N + "particles.dat";

        FileHandle fh = new FileHandle(filePath);
        File f = fh.file();
        if (fh.exists() && f.isFile()) {
            fh.delete();
        }

        if (fh.isDirectory()) {
            throw new RuntimeException("File is directory: " + filePath);
        }
        f.createNewFile();

        FileWriter fw = new FileWriter(filePath);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write("#X Y Z");
        bw.newLine();

        for (int i = 0; i < oort.size; i++) {
            double[] particle = oort.get(i);
            bw.write(particle[0] + " " + particle[1] + " " + particle[2]);
            bw.newLine();
        }

        bw.close();

        Logger.getLogger(OortGenerator.class).info("File written to " + filePath);
    }

}
