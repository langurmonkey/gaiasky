/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.desktop.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import com.badlogic.gdx.files.FileHandle;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import gaiasky.gui.ConsoleLogger;
import gaiasky.scene.record.BillboardDataset;
import gaiasky.util.Logger;
import gaiasky.util.SettingsManager;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.StdRandom;
import gaiasky.util.math.Vector3d;
import net.jafama.FastMath;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class GalaxyGenerator {
    private static final Logger.Log logger = Logger.getLogger(GalaxyGenerator.class);

    private static final String separator = " ";

    /** Whether to write the results to disk **/
    private static final boolean writeFile = true;

    enum GalaxyType {
        spiral, milkyway, uniform, bulge
    }

    /** Number of particles **/
    private static final int N = 3000;

    /** Number of spiral arms **/
    private static final int Narms = 8;

    /** Does the galaxy have a bar? **/
    private static final boolean bar = true;

    /** The length of the bar, if it has one **/
    private static final double barLength = 0.8;

    /** Radius of the galaxy **/
    private static final double radius = 2.5;

    /** Ratio radius/armWidth **/
    private static final double armWidthRatio = 0.04;

    /** Ratio radius/armHeight **/
    private static final double armHeightRatio = 0.02;

    /** Maximum spiral rotation (end of arm) in degrees **/
    private static final double maxRotation = 100;

    private static final boolean radialDensity = true;

    /** CLI arguments. **/
    private static CLIArgs cliArgs;

    private static class CLIArgs {
        @Parameter(names = {"-h", "--help"}, description = "Show program options and usage information.", help = true, order = 0)
        private boolean help = false;

        @Parameter(names = {"--galaxytype"}, required = true, description = "The galaxy type to use.", order = 1)
        private GalaxyType galaxyType = GalaxyType.uniform;

        @Parameter(names = {"--particletype"}, required = true, description = "The particle type to use.", order = 2)
        private BillboardDataset.ParticleType particleType = BillboardDataset.ParticleType.GAS;

        @Parameter(names = {"-o", "--ouptut"}, description = "The output directory.", required = true, order = 2)
        private String outputDir = "/home/tsagrista/.local/share/gaiasky/data/galaxy/";

        @Parameter(names = {"-s", "--skip-welcome"}, description = "Skip the welcome screen if possible (base-data package must be present).", order = 2)
        private boolean skipWelcome = false;

    }

    public static void main(String[] args) {
        try {
            Gdx.files = new Lwjgl3Files();
            cliArgs = new CLIArgs();
            JCommander jc = JCommander.newBuilder().addObject(cliArgs).build();
            jc.setProgramName("galaxy-generator");
            try {
                jc.parse(args);

                if (cliArgs.help) {
                    jc.usage();
                    return;
                }
            } catch (Exception e) {
                logger.error("galaxy-generator: bad program arguments\n\n");
                jc.usage();
                return;
            }

            SettingsManager.initialize(new FileInputStream("assets/conf/config.yaml"), new FileInputStream("assets/dummyversion"));

            I18n.initialize(new FileHandle("assets/i18n/gsbundle"), new FileHandle("assets/i18n/objects"));

            // Add notifications watch
            new ConsoleLogger();

            // Seed RNG
            StdRandom.setSeed(System.currentTimeMillis());

            List<double[]> gal;

            gal = switch (cliArgs.galaxyType) {
                case spiral -> generateGalaxySpiral();
                case milkyway -> generateMilkyWay();
                case uniform -> generateUniform();
                case bulge -> generateBulge();
            };

            if (writeFile) {
                writeToDisk(gal, cliArgs.outputDir);
            }

        } catch (Exception e) {
            logger.error(e);
        }
    }

    private static double generateNewSize() {
        return switch (cliArgs.particleType) {
            case STAR, DUST -> FastMath.abs(StdRandom.uniform() * 20.0 + StdRandom.uniform() * 3.0);
            case BULGE -> FastMath.abs(StdRandom.uniform() * 40.0 + StdRandom.uniform() * 6.0);
            case HII -> FastMath.abs(StdRandom.uniform() * 70.0 + StdRandom.uniform() * 30.0);
            case GAS -> FastMath.abs(StdRandom.uniform() * 100.0 + StdRandom.uniform() * 50.0);
            default -> 1;
        };
    }

    // Clamp
    private static double[] cl(double r, double g, double b) {
        return new double[]{MathUtilsDouble.clamp(r, 0, 1), MathUtilsDouble.clamp(g, 0, 1), MathUtilsDouble.clamp(b, 0, 1)};
    }

    private static double[] generateNewColor() {
        double r = StdRandom.gaussian();
        switch (cliArgs.particleType) {
            case STAR:
                r *= 0.15;
                if (StdRandom.uniform(2) == 0) {
                    // Blue/white star
                    return cl(0.95 - r, 0.8 - r, 0.6);
                } else {
                    // Red/white star
                    return cl(0.95, 0.8 - r, 0.6 - r);
                }
            case BULGE:
                return new double[]{0.9, 0.9, 0.8};
            case HII:
                return new double[]{0.78, 0.31, 0.55};
            case GAS:
                r *= 0.1;
                return cl(0.068 + r, 0.06 + r, 0.2 + r * 1.3);
            case DUST:
            default:
                return null;
        }
    }

    private static void addMWParticle(double x, double y, double z, Vector3d aux, List<double[]> particles) {
        aux.set(x, y, z);
        double size = generateNewSize();
        double[] color = generateNewColor();

        if (color == null || color.length < 3)
            particles.add(new double[]{x, y, z, size});
        else
            particles.add(new double[]{x, y, z, size, color[0], color[1], color[2]});
    }

    private static List<double[]> generateUniform() {
        return generateUniformBlob(1.0 / 7.0, 1.0 / 7.0, 1.0 / 40.0);
    }

    private static List<double[]> generateBulge() {
        return generateUniformBlob(0.15, 0.15, 1.0 / 20.0);
    }

    private static List<double[]> generateUniformBlob(double xExtent, double yExtent, double zExtent) {
        Vector3d aux = new Vector3d();
        // x, y, z, size
        List<double[]> particles = new ArrayList<>(N);

        for (int i = 0; i < N; i++) {
            double x = 0.5 * StdRandom.gaussian(0, 10) * xExtent;
            double y = 0.5 * StdRandom.gaussian(0, 10) * yExtent;
            double z = StdRandom.gaussian() * zExtent;

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
     */
    private static List<double[]> generateMilkyWay() {
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
        if (bar && Narms % 2 == 1) {
            throw new RuntimeException("Galaxies with bars can only have an even number of arms");
        }

        double totalLength = Narms * radius + (bar ? barLength : 0);
        double armOverTotal = radius / totalLength;
        double barOverTotal = (bar ? barLength / totalLength : 0);

        long NperArm = FastMath.round(N * armOverTotal);
        long Nbar = FastMath.round(N * barOverTotal);

        double armWidth = radius * armWidthRatio;
        double armHeight = radius * armHeightRatio;

        // x, y, z, size
        List<double[]> particles = new ArrayList<>(N);

        double stepAngle = bar ? 60.0 / FastMath.max(1.0, ((Narms / 2.0) - 1.0)) : 360.0 / Narms;
        double angle = bar ? 10.0 : 0.0;

        Vector3d rotAxis = new Vector3d(0, 1, 0);

        // Generate bar
        for (long j = 0; j < Nbar; j++) {
            double z = StdRandom.uniform() * barLength - barLength / 2.0;
            double x = StdRandom.gaussian() * armWidth;
            double y = StdRandom.gaussian() * armHeight;

            particles.add(new double[]{x, y, z, FastMath.abs(StdRandom.gaussian())});
        }

        // Generate arms
        for (int i = 0; i < Narms; i++) {
            logger.info("Generating arm " + (i + 1));
            double zPlus = bar ? barLength / 2.0 * (i < Narms / 2.0 ? 1.0 : -1.0) : 0.0;

            angle = bar && i == Narms / 2.0 ? 190.0 : angle;

            for (int j = 0; j < NperArm; j++) {
                double x, y, z;
                if (!radialDensity) {
                    z = StdRandom.uniform() * radius;
                } else {
                    z = FastMath.abs(StdRandom.gaussian()) * radius;
                }
                x = StdRandom.gaussian() * armWidth;
                y = StdRandom.gaussian() * armHeight;

                Vector3d particle = new Vector3d(x, y, z);
                particle.rotate(rotAxis, angle);

                // Differential rotation
                particle.rotate(rotAxis, maxRotation * particle.len() / radius);

                particle.add(0.0, 0.0, zPlus);

                particles.add(new double[]{particle.x, particle.y, particle.z, FastMath.abs(StdRandom.gaussian())});
            }
            angle += stepAngle;
        }

        return particles;
    }

    private static void writeToDisk(List<double[]> gal, String dir) throws IOException {
        // Sort in x
        gal.sort(Comparator.comparingDouble(f -> f[0]));

        String filePath = dir + "galaxy_";
        if (cliArgs.galaxyType == GalaxyType.spiral) {
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
        if (f.createNewFile()) {

            final BufferedWriter bw = getBufferedWriter(gal, filePath);

            bw.close();
            logger.info(I18n.msg("notif.written", gal.size(), filePath));
        } else {
            logger.error("Error creating file: " + f.getAbsolutePath());
        }

    }

    private static BufferedWriter getBufferedWriter(List<double[]> gal, String filePath) throws IOException {
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(filePath))));
        bw.write("X" + separator + "Y" + separator + "Z" + separator + "size" + separator + "r" + separator + "g" + separator + "b");
        bw.newLine();

        for (double[] star : gal) {
            StringBuilder sb = new StringBuilder();
            sb.append(star[0]);
            for (int j = 1; j < star.length; j++) {
                sb.append(separator);
                sb.append(star[j]);
            }

            bw.write(sb.toString());
            bw.newLine();
        }
        return bw;
    }

}
