/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.data;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglFiles;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.desktop.format.DesktopDateFormatFactory;
import gaia.cu9.ari.gaiaorbit.desktop.format.DesktopNumberFormatFactory;
import gaia.cu9.ari.gaiaorbit.desktop.util.DesktopConfInit;
import gaia.cu9.ari.gaiaorbit.interfce.ConsoleLogger;
import gaia.cu9.ari.gaiaorbit.util.ConfInit;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.format.DateFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.format.NumberFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.math.StdRandom;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;

import java.io.*;

public class OortGenerator {

    /** Whether to write the results to disk **/
    private static final boolean writeFile = true;

    /** Outer radius in AU **/
    private static float outer_radius = 15000;

    /** Number of particles **/
    private static int N = 10000;

    public static void main(String[] args) {
        try {
            Gdx.files = new LwjglFiles();

            // Initialize number format
            NumberFormatFactory.initialize(new DesktopNumberFormatFactory());

            // Initialize date format
            DateFormatFactory.initialize(new DesktopDateFormatFactory());

            ConfInit.initialize(new DesktopConfInit(new FileInputStream(new File("../android/assets/conf/global.properties")), new FileInputStream(new File("../android/assets/data/dummyversion"))));

            I18n.initialize(new FileHandle("/home/tsagrista/git/gaiasky/android/assets/i18n/gsbundle"));

            // Add notif watch
            new ConsoleLogger();

            Array<double[]> oort = null;

            oort = generateOort();

            if (writeFile) {
                writeToDisk(oort, "/tmp/");
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Generates random Oort cloud particles
     * 
     * @throws IOException
     */
    private static Array<double[]> generateOort() throws IOException, RuntimeException {
        StdRandom.setSeed(100l);

        Array<double[]> particles = new Array<double[]>(N);

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
