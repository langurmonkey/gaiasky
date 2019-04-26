/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.data.galaxy;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import com.badlogic.gdx.files.FileHandle;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class GalaxyGenerator {

    private static final String separator = " ";

    /** Whether to write the results to disk **/
    private static final boolean writeFile = true;

    /** spiral | milkyway | uniform | bulge **/
    private static String GALAXY_TYPE = "bulge";

    /** star | dust | hii | bulge **/
    private static String PARTICLE_TYPE = "bulge";

    /** Number of spiral arms **/
    private static int Narms = 8;

    /** Does the galaxy have a bar? **/
    private static boolean bar = true;

    /** The length of the bar, if it has one **/
    private static double barLength = 0.8;

    /** Radius of the galaxy **/
    private static double radius = 2.5;

    /** Number of particles **/
    private static int N = 5000;

    /** Ratio radius/armWidth **/
    private static double armWidthRatio = 0.04;

    /** Ratio radius/armHeight **/
    private static double armHeightRatio = 0.02;

    /** Maximum spiral rotation (end of arm) in degrees **/
    private static double maxRotation = 100;


    private static boolean radialDensity = true;

    public static void main(String[] args) {
        try {
            Gdx.files = new Lwjgl3Files();

            // Initialize number format
            NumberFormatFactory.initialize(new DesktopNumberFormatFactory());

            // Initialize date format
            DateFormatFactory.initialize(new DesktopDateFormatFactory());

            ConfInit.initialize(new DesktopConfInit(new FileInputStream(new File("assets/conf/global.properties")), new FileInputStream(new File("assets/dummyversion"))));

            I18n.initialize(new FileHandle("assets/i18n/gsbundle"));

            // Add notif watch
            new ConsoleLogger();

            List<double[]> gal;

            if (GALAXY_TYPE.equals("spiral")) {
                gal = generateGalaxySpiral();
            } else if (GALAXY_TYPE.equals("milkyway")) {
                gal = generateMilkyWay();
            } else if(GALAXY_TYPE.equals("uniform")){
                gal = generateUniform();
            } else if(GALAXY_TYPE.equals("bulge")){
                gal = generateBulge();
            } else{
                System.out.println("Wrong galaxy type: " + GALAXY_TYPE);
                return;
            }


            if (writeFile) {
                writeToDisk(gal, "/home/tsagrista/.local/share/gaiasky/data/galaxy/");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static double generateNewSize() {
        switch (PARTICLE_TYPE) {
        case "star":
        case "dust":
            return Math.abs(StdRandom.uniform() * 20.0 + StdRandom.uniform() * 3.0);
        case "bulge":
            return Math.abs(StdRandom.uniform() * 40.0 + StdRandom.uniform() * 6.0);
        case "hii":
            return Math.abs(StdRandom.uniform() * 70.0 + StdRandom.uniform() * 30.0);
        default:
            return 1;
        }
    }

    private static double[] generateNewColor() {
        double r = StdRandom.gaussian() * 0.15;
        switch (PARTICLE_TYPE) {
        case "star":
            if (StdRandom.uniform(2) == 0) {
                // Blue/white star
                return new double[] { 0.95 - r, 0.8 - r, 0.6 };
            } else {
                // Red/white star
                return new double[] { 0.95, 0.8 - r, 0.6 - r };
            }
        case "bulge":
            return new double[] { 0.9, 0.9, 0.8 };
        case "dust":
            return null;
        case "hii":
            return new double[] { 0.78, 0.31, 0.55 };
        default:
            return null;
        }
    }

    private static void addMWParticle(double x, double y, double z, Vector3d aux, List<double[]> particles) {
        aux.set(x, y, z);
        double size = generateNewSize();
        double[] color = generateNewColor();

        if (color == null || color.length < 3)
            particles.add(new double[] { x, y, z, size });
        else
            particles.add(new double[] { x, y, z, size, color[0], color[1], color[2] });
    }

    private static List<double[]> generateUniform() {
        return generateUniformBlob(0.6, 0.6, 1.0/40.0);
    }

    private static List<double[]> generateBulge() {
        return generateUniformBlob(0.15, 0.15, 1.0/20.0);
    }

    private static List<double[]> generateUniformBlob(double xExtent, double yExtent, double zExtent) {
        StdRandom.setSeed(100l);

        Vector3d aux = new Vector3d();
        // x, y, z, size
        List<double[]> particles = new ArrayList<>(N);

        for (int i = 0; i < N; i++) {
            double x = StdRandom.gaussian() * xExtent;
            double y = StdRandom.gaussian() * yExtent;
            double z = StdRandom.gaussian(0, zExtent);

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
     * @throws RuntimeException
     */
    private static List<double[]> generateMilkyWay() {
        StdRandom.setSeed(100l);

        Vector3d aux = new Vector3d();
        // x, y, z, size
        List<double[]> particles = new ArrayList<>(N);

        int Nbar = N / 10;
        int Nbulge = N / 6;
        int Nrest = 7 * N / 6;

        // BAR
        for (int i = 0; i < Nbar; i++) {
            double x = StdRandom.gaussian(0, 0.18);
            double y = StdRandom.gaussian(0, 0.03);
            double z = StdRandom.gaussian(0, 1.0 / 24.0);

            addMWParticle(x, y, z, aux, particles);
        }

        // BULGE
        for (int i = 0; i < Nbulge; i++) {
            double x = StdRandom.gaussian(0, 0.18);
            double y = StdRandom.gaussian(0, 0.18);
            double z = StdRandom.gaussian(0, 1.0 / 24.0);

            addMWParticle(x, y, z, aux, particles);
        }

        // REST
        for (int i = 0; i < Nrest; i++) {
            double x = StdRandom.gaussian();
            double y = StdRandom.gaussian();
            // 1/30 is the relation diameter/height of the galaxy (diameter=30
            // Kpc, height=0.3-1 Kpc)
            double z = StdRandom.gaussian(0, 1.0 / 30.0);

            addMWParticle(x, y, z, aux, particles);
        }

        // Rotate to align bar
        for (double[] particle : particles) {
            aux.set(particle[0], particle[1], particle[2]);
            aux.rotate(-45, 0, 0, 1);
            particle[0] = aux.x;
            particle[1] = aux.y;
            particle[2] = aux.z;
        }

        return particles;
    }

    /**
     * Generates a galaxy (particle positions) with spiral arms and so on. The
     * galactic plane is XZ and Y points to the galactic north pole.
     */
    private static List<double[]> generateGalaxySpiral() throws RuntimeException {
        StdRandom.setSeed(100l);

        if (bar && Narms % 2 == 1) {
            throw new RuntimeException("Galaxies with bars can only have an even number of arms");
        }

        double totalLength = Narms * radius + (bar ? barLength : 0);
        double armOverTotal = radius / totalLength;
        double barOverTotal = (bar ? barLength / totalLength : 0);

        long NperArm = Math.round(N * armOverTotal);
        long Nbar = Math.round(N * barOverTotal);

        double armWidth = radius * armWidthRatio;
        double armHeight = radius * armHeightRatio;

        // x, y, z, size
        List<double[]> particles = new ArrayList<>(N);

        double stepAngle = bar ? 60.0 / Math.max(1.0, ((Narms / 2.0) - 1.0)) : 360.0 / Narms;
        double angle = bar ? 10.0 : 0.0;

        Vector3d rotAxis = new Vector3d(0, 1, 0);

        // Generate bar
        for (long j = 0; j < Nbar; j++) {
            double z = StdRandom.uniform() * barLength - barLength / 2.0;
            double x = StdRandom.gaussian() * armWidth;
            double y = StdRandom.gaussian() * armHeight;

            particles.add(new double[] { x, y, z, Math.abs(StdRandom.gaussian()) });
        }

        // Generate arms
        for (int i = 0; i < Narms; i++) {
            Logger.getLogger(GalaxyGenerator.class).info("Generating arm " + (i + 1));
            double zplus = bar ? barLength / 2.0 * (i < Narms / 2.0 ? 1.0 : -1.0) : 0.0;

            angle = bar && i == Narms / 2.0 ? 190.0 : angle;

            for (int j = 0; j < NperArm; j++) {
                double x, y, z;
                if (!radialDensity) {
                    z = StdRandom.uniform() * radius;
                } else {
                    z = Math.abs(StdRandom.gaussian()) * radius;
                }
                x = StdRandom.gaussian() * armWidth;
                y = StdRandom.gaussian() * armHeight;

                Vector3d particle = new Vector3d(x, y, z);
                particle.rotate(rotAxis, angle);

                // Differential rotation
                particle.rotate(rotAxis, maxRotation * particle.len() / radius);

                particle.add(0.0, 0.0, zplus);

                particles.add(new double[] { particle.x, particle.y, particle.z, Math.abs(StdRandom.gaussian()) });
            }
            angle += stepAngle;
        }

        return particles;
    }

    private static void writeToDisk(List<double[]> gal, String dir) throws IOException {
        // Sort in x
        gal.sort(Comparator.comparingDouble(f -> f[0]));

        String filePath = dir + "galaxy_";
        if (GALAXY_TYPE.equals("spiral")) {
            filePath += (bar ? "bar" + barLength + "_" : "nobar_") + Narms + "arms_" + N + "particles_" + radius + "radius_" + armWidthRatio + "ratio_" + maxRotation + "deg.dat.gz";
        } else {
            filePath += N + "particles.dat.gz";
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

        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(filePath))));
        bw.write("X" + separator + "Y" + separator + "Z" + separator + "size" + separator + "r" + separator + "g" + separator +"b");
        bw.newLine();

        for (int i = 0; i < gal.size(); i++) {
            double[] star = gal.get(i);
            StringBuilder sb = new StringBuilder();
            sb.append(star[0]);
            for(int j = 1; j < star.length; j++) {
                sb.append(separator);
                sb.append(star[j]);
            }

            bw.write(sb.toString());
            bw.newLine();
        }

        bw.close();

        Logger.getLogger(GalaxyGenerator.class).info("File written to " + filePath);
    }

}
