/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.data.galaxy;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector3;
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

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class GalaxyGenerator {

    /** Whether to write the results to disk **/
    private static final boolean writeFile = true;

    /** spiral | milkyway | uniform **/
    private static String TYPE = "spiral";

    /** Number of spiral arms **/
    private static int Narms = 8;

    /** Does the galaxy have a bar? **/
    private static boolean bar = true;

    /** The length of the bar, if it has one **/
    private static float barLength = 0.8f;

    /** Radius of the galaxy **/
    private static float radius = 2.5f;

    /** Number of particles **/
    private static int N = 15000;

    /** Ratio radius/armWidth **/
    private static float armWidthRatio = 0.04f;

    /** Ratio radius/armHeight **/
    private static float armHeightRatio = 0.02f;

    /** Maximum spiral rotation (end of arm) in degrees **/
    private static float maxRotation = 100;

    private static boolean radialDensity = true;

    public static void main(String[] args) {
        try {
            Gdx.files = new Lwjgl3Files();

            // Initialize number format
            NumberFormatFactory.initialize(new DesktopNumberFormatFactory());

            // Initialize date format
            DateFormatFactory.initialize(new DesktopDateFormatFactory());

            ConfInit.initialize(new DesktopConfInit(new FileInputStream(new File("../android/assets/conf/global.properties")), new FileInputStream(new File("../android/assets/data/dummyversion"))));

            I18n.initialize(new FileHandle("/home/tsagrista/git/gaiasky/android/assets/i18n/gsbundle"));

            // Add notif watch
            new ConsoleLogger();

            List<float[]> gal = null;

            if (TYPE.equals("spiral")) {
                gal = generateGalaxySpiral();
            } else if (TYPE.equals("milkyway")) {
                gal = generateMilkyWay();
            } else {
                gal = generateUniform();
            }

            if (writeFile) {
                writeToDisk(gal, "/home/tsagrista/Documents/");
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<float[]> generateUniform() throws IOException, RuntimeException {
        StdRandom.setSeed(100l);

        Vector3 aux = new Vector3();
        // x, y, z, size
        List<float[]> particles = new ArrayList<float[]>(N);

        for (int i = 0; i < N; i++) {
            float x = ((float) StdRandom.uniform() - 0.5f) * 4f;
            float y = ((float) StdRandom.uniform() - 0.5f) * 4f;
            float z = (float) StdRandom.gaussian(0, 1.0 / 24.0);

            addMWParticle(x, y, z, aux, particles);
        }
        return particles;
    }

    /**
     * Generates the Milky Way with the following parameters: radius: 15 Kpc
     * thin disk height: 0.3 Kpc thick disk height: 1.5 Kpc density profile:
     * normal with sigma^2 = 0.2 The normalisation factor is 1/30 units/Kpc
     * 
     * @return The list of stars
     * @throws IOException
     * @throws RuntimeException
     */
    private static List<float[]> generateMilkyWay() throws IOException, RuntimeException {
        StdRandom.setSeed(100l);

        Vector3 aux = new Vector3();
        // x, y, z, size
        List<float[]> particles = new ArrayList<float[]>(N);

        int Nbar = N / 10;
        int Nbulge = N / 6;
        int Nrest = 7 * N / 6;

        // BAR
        for (int i = 0; i < Nbar; i++) {
            float x = (float) StdRandom.gaussian(0, 0.18f);
            float y = (float) StdRandom.gaussian(0, 0.03f);
            float z = (float) StdRandom.gaussian(0, 1.0 / 24.0);

            addMWParticle(x, y, z, aux, particles);
        }

        // BULGE
        for (int i = 0; i < Nbulge; i++) {
            float x = (float) StdRandom.gaussian(0, 0.18f);
            float y = (float) StdRandom.gaussian(0, 0.18f);
            float z = (float) StdRandom.gaussian(0, 1.0 / 24.0);

            addMWParticle(x, y, z, aux, particles);
        }

        // REST
        for (int i = 0; i < Nrest; i++) {
            float x = (float) StdRandom.gaussian();
            float y = (float) StdRandom.gaussian();
            // 1/30 is the relation diameter/height of the galaxy (diameter=30
            // Kpc, height=0.3-1 Kpc)
            float z = (float) StdRandom.gaussian(0, 1.0 / 30.0);

            addMWParticle(x, y, z, aux, particles);
        }

        // Rotate to align bar
        for (float[] particle : particles) {
            aux.set(particle[0], particle[1], particle[2]);
            aux.rotate(-45, 0, 0, 1);
            particle[0] = aux.x;
            particle[1] = aux.y;
            particle[2] = aux.z;
        }

        return particles;
    }

    private static void addMWParticle(float x, float y, float z, Vector3 aux, List<float[]> particles) {
        aux.set(x, y, z);
        float len = aux.len();

        float size = (float) Math.abs(StdRandom.gaussian(0, 0.4f) * (4f - len));

        particles.add(new float[] { x, y, z, size });
    }

    /**
     * Generates a galaxy (particle positions) with spiral arms and so on. The
     * galactic plane is XZ and Y points to the galactic north pole.
     * 
     * @throws IOException
     */
    private static List<float[]> generateGalaxySpiral() throws IOException, RuntimeException {
        StdRandom.setSeed(100l);

        if (bar && Narms % 2 == 1) {
            throw new RuntimeException("Galaxies with bars can only have an even number of arms");
        }

        float totalLength = Narms * radius + (bar ? barLength : 0f);
        float armOverTotal = radius / totalLength;
        float barOverTotal = (bar ? barLength / totalLength : 0f);

        int NperArm = Math.round(N * armOverTotal);
        int Nbar = Math.round(N * barOverTotal);

        float armWidth = radius * armWidthRatio;
        float armHeight = radius * armHeightRatio;

        // x, y, z, size
        List<float[]> particles = new ArrayList<float[]>(N);

        float stepAngle = bar ? 60f / Math.max(1f, ((Narms / 2f) - 1)) : 360f / Narms;
        float angle = bar ? 10f : 0;

        Vector3 rotAxis = new Vector3(0, 1, 0);

        // Generate bar
        for (int j = 0; j < Nbar; j++) {
            float z = (float) StdRandom.uniform() * barLength - barLength / 2f;
            float x = (float) (StdRandom.gaussian() * armWidth);
            float y = (float) (StdRandom.gaussian() * armHeight);

            particles.add(new float[] { x, y, z, (float) Math.abs(StdRandom.gaussian()) });
        }

        // Generate arms
        for (int i = 0; i < Narms; i++) {
            Logger.getLogger(GalaxyGenerator.class).info("Generating arm " + (i + 1));
            float zplus = bar ? barLength / 2f * (i < Narms / 2 ? 1f : -1f) : 0f;

            angle = bar && i == Narms / 2 ? 190f : angle;

            for (int j = 0; j < NperArm; j++) {
                float x, y, z;
                if (!radialDensity) {
                    z = (float) StdRandom.uniform() * radius;
                } else {
                    z = (float) Math.abs(StdRandom.gaussian()) * radius;
                }
                x = (float) (StdRandom.gaussian() * armWidth);
                y = (float) (StdRandom.gaussian() * armHeight);

                Vector3 particle = new Vector3(x, y, z);
                particle.rotate(rotAxis, angle);

                // Differential rotation
                particle.rotate(rotAxis, maxRotation * particle.len() / radius);

                particle.add(0f, 0f, zplus);

                particles.add(new float[] { particle.x, particle.y, particle.z, (float) Math.abs(StdRandom.gaussian()) });
            }
            angle += stepAngle;
        }

        return particles;
    }

    private static void writeToDisk(List<float[]> gal, String dir) throws IOException {
        String filePath = dir + "galaxy_";
        if (TYPE.equals("spiral")) {
            filePath += (bar ? "bar" + barLength + "_" : "nobar_") + Narms + "arms_" + N + "particles_" + radius + "radius_" + armWidthRatio + "ratio_" + maxRotation + "deg.txt";
        } else {
            filePath += N + "particles.dat";
        }

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

        for (int i = 0; i < gal.size(); i++) {
            float[] star = gal.get(i);
            bw.write(star[0] + " " + star[1] + " " + star[2]);
            bw.newLine();
        }

        bw.close();

        Logger.getLogger(GalaxyGenerator.class).info("File written to " + filePath);
    }

}
