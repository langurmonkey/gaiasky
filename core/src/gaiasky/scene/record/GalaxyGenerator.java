package gaiasky.scene.record;

import gaiasky.render.BlendMode;
import gaiasky.util.Pair;

import java.util.Random;

import static gaiasky.scene.record.BillboardDataset.Distribution.*;
import static gaiasky.scene.record.BillboardDataset.ParticleType.*;

/**
 * Generates galaxies as lists of {@link BillboardDataset} objects.
 */
public class GalaxyGenerator {

    /** RNG. **/
    private final Random rand;
    /** Array with base gas colors. **/
    private final double[][] gasColors = {
            // O III / high-ionization teals
            {0.20, 0.85, 0.78},
            {0.10, 0.73, 0.82},
            {0.32, 0.88, 0.94},

            // Reflection nebula blues
            {0.34, 0.54, 0.93},
            {0.42, 0.66, 0.94},
            {0.25, 0.44, 0.81},

            // Dust-reddened hydrogen / diffuse WIM (not HII pink!)
            {0.94, 0.50, 0.28},
            {0.88, 0.37, 0.22},
            {0.76, 0.25, 0.18},

            // Composite spiral-arm gas emission
            {0.78, 0.60, 0.85},
            {0.63, 0.78, 0.86},
            {0.85, 0.68, 0.72}
    };
    /** HII colors. **/
    double[][] hiiColors = {
            // Classic bright Hα-dominated pinks
            {0.96, 0.38, 0.50},
            {0.89, 0.32, 0.44},

            // More saturated magenta mixes (Hβ + OIII influence)
            {0.78, 0.23, 0.52},
            {0.82, 0.18, 0.58},

            // Softer, dust-attenuated hydrogen emission
            {0.88, 0.46, 0.54},
            {0.77, 0.34, 0.46},

            // Deep ionized-gas magenta (starburst-like)
            {0.73, 0.12, 0.48},
            {0.67, 0.09, 0.42}
    };
    /** Star colors, with proper distribution. **/
    double[][] starColors = {
            // ---- Rare blue stars (~5%) ----
            {0.60, 0.75, 1.00},  // Hot B-star blue
            {0.50, 0.68, 1.00},  // Slightly cooler A/B mix

            // ---- Yellow stars (~20%) ----
            {1.00, 0.95, 0.80},  // Sun-like G
            {1.00, 0.90, 0.70},
            {0.98, 0.88, 0.65},
            {0.96, 0.84, 0.55},

            // ---- Red stars (~75%) ----
            {1.00, 0.72, 0.55},  // K-type warm orange
            {1.00, 0.60, 0.45},
            {1.00, 0.52, 0.40},
            {0.97, 0.48, 0.35},
            {0.94, 0.44, 0.30},
            {0.91, 0.40, 0.27},
            {0.88, 0.36, 0.24},
            {0.85, 0.33, 0.22},
            {0.80, 0.30, 0.20},
            {0.76, 0.27, 0.18},
            {0.72, 0.24, 0.16},
            {0.68, 0.22, 0.15},
            {0.64, 0.20, 0.14},
            {0.60, 0.18, 0.12}
    };
    /** Bulge colors. Older, redder stars. **/
    double[][] bulgeColors = {
            // ---- Blue stars (~10%) ----
            {0.60, 0.75, 1.00},  // Hot B-type blue
            {0.50, 0.68, 1.00},  // Cooler A/B mix

            // ---- Yellow stars (~20%) ----
            {1.00, 0.95, 0.80},  // Sun-like G
            {1.00, 0.90, 0.70},
            {0.98, 0.88, 0.65},
            {0.96, 0.84, 0.55},

            // ---- Red stars (~70%) ----
            {1.00, 0.72, 0.55},  // K-type warm orange
            {1.00, 0.60, 0.45},
            {1.00, 0.52, 0.40},
            {0.97, 0.48, 0.35},
            {0.94, 0.44, 0.30},
            {0.91, 0.40, 0.27},
            {0.88, 0.36, 0.24},
            {0.85, 0.33, 0.22},
            {0.80, 0.30, 0.20},
            {0.76, 0.27, 0.18},
            {0.72, 0.24, 0.16},
            {0.68, 0.22, 0.15}
    };
    /** Dust colors. **/
    double[][] dustColors = {
            // Dark reddish-brown tones
            {0.50, 0.25, 0.15},  // Dark red-brown, typical of dense dust clouds
            {0.60, 0.30, 0.20},  // Slightly lighter brown
            {0.70, 0.35, 0.25},  // Warm brown, typical for scattered dust lanes

            // Muted brownish-greys
            {0.60, 0.50, 0.40},  // Dusty brown-grey
            {0.65, 0.55, 0.45},  // Slightly lighter dust
            {0.75, 0.60, 0.50},  // Light dusty brown

            // Subtle yellowish dust
            {0.85, 0.75, 0.55},  // Yellow-brown, softer, less dense dust
            {0.90, 0.80, 0.60},  // More yellowish, less saturated

            // Darker brown hues
            {0.45, 0.30, 0.20},  // Deeper brown, almost black
            {0.40, 0.25, 0.15},  // Darker, more intense brown
            {0.55, 0.40, 0.25},  // Dark dusty brown, almost shadow-like
            {0.50, 0.35, 0.20}   // Another dark brown variant
    };

    public GalaxyGenerator() {
        super();
        rand = new Random();
    }

    /**
     * Galaxy morphologies according to Edwin Hubble's classification.
     */
    public enum GalaxyMorphology {
        E0, E3, E5, E7, // Ellipticals (0-spherical, 7-highly elliptical)
        S0, // Lenticular (large bulb, surrounded with disk).
        Sa, Sb, Sc, // Spirals (a-low angle, c-high angle)
        SBa, SBb, SBc, // Barred spirals
        Im // Irregulars
    }

    /**
     * Generates galaxies given a morphology.
     *
     * @param gm The galaxy morphology.
     *
     * @return A couple of {@link BillboardDataset} arrays, the first being the full-resolution channel, and the second being the half-resolution
     *         channel.
     */
    public Pair<BillboardDataset[], BillboardDataset[]> generateGalaxy(GalaxyMorphology gm, final long seed) {
        rand.setSeed(seed);
        if (gm == null) {
            gm = GalaxyMorphology.values()[rand.nextInt(GalaxyMorphology.values().length)];
        }

        Pair<BillboardDataset[], BillboardDataset[]> result = new Pair<>();
        switch (gm) {
            // Ellipticals
            // No HII, old and cool stars.
            case E0, E3, E5, E7 -> {

                // Eccentricity in X and Y.
                double eccY = generateEccentricity(gm);
                double eccX = eccY * rand.nextDouble(0.1f, 0.6f);

                // Stars
                var stars = new BillboardDataset();
                stars.setType(STAR);
                stars.setDistribution(BillboardDataset.Distribution.ELLIPSOID);
                stars.setEccentricityX(eccX);
                stars.setEccentricityY(eccY);
                stars.setBaseColors(generateColors(STAR));
                stars.setParticleCount(generateCount(STAR));
                stars.setMinRadius(0.0);
                stars.setSize(0.2);
                stars.setSizeNoise(0.7);
                stars.setIntensity(2.0);
                stars.setLayers(new int[]{0, 1, 2, 4});
                stars.setMaxSize(0.15);

                // Dust (50% chance of being included)
                var dust = new BillboardDataset();
                dust.setType(DUST);
                dust.setDistribution(BillboardDataset.Distribution.ELLIPSOID);
                dust.setEccentricityX(eccX);
                dust.setEccentricityY(eccY);
                dust.setBaseColors(generateColors(DUST));
                dust.setParticleCount(generateCount(DUST));
                dust.setSize(rand.nextDouble(1.0, 4.9));
                dust.setIntensity(rand.nextDouble(0.005, 0.01));
                dust.setBlending(BlendMode.SUBTRACTIVE);
                dust.setDepthMask(false);
                dust.setLayers(new int[]{0, 1, 2});
                dust.setMaxSize(20.0);

                // Gas
                var gas = new BillboardDataset();
                gas.setType(GAS);
                gas.setDistribution(BillboardDataset.Distribution.ELLIPSOID);
                gas.setEccentricityX(eccX);
                gas.setEccentricityY(eccY);
                gas.setMinRadius(0.0);
                gas.setBaseColors(generateColors(GAS));
                gas.setColorNoise(rand.nextDouble(0.01, 0.4));
                gas.setParticleCount(generateCount(GAS));
                gas.setSize(rand.nextDouble(50.0, 90.0));
                gas.setSizeNoise(0.09);
                gas.setIntensity(rand.nextDouble(0.009, 0.012));
                gas.setLayers(new int[]{0, 1, 2, 3});
                gas.setMaxSize(20.0);


                var full = new BillboardDataset[]{stars};
                var half = rand.nextBoolean() ? new BillboardDataset[]{gas, dust} : new BillboardDataset[]{gas};
                result.set(full, half);
            }
            // Lenticulars
            case S0 -> {

            }
            // Spirals
            case Sa, Sb, Sc -> {
                var dustDistribution = generateSpiralDistribution(gm);
                var gasDistribution = dustDistribution == SPIRAL ? SPIRAL : DISK;
                var spiralAngle = generateSpiralAngle(gm, dustDistribution);
                var eccentricity = rand.nextDouble(0.1, 0.4);
                var minRadius = rand.nextDouble(0.08, 0.15);
                var spiralDeltaPos = new double[]{rand.nextGaussian() * 0.1f, rand.nextGaussian() * 0.1f};
                var armSigma = rand.nextDouble(0.3, 0.55);
                var numArms = rand.nextFloat() > 0.2f ? 2L : 4L;
                var heightScale = rand.nextDouble(0.05, 0.15);

                // Stars
                var stars = new BillboardDataset();
                stars.setType(STAR);
                stars.setDistribution(BillboardDataset.Distribution.GAUSS);
                stars.setBaseColors(generateColors(STAR));
                stars.setParticleCount(generateCount(STAR));
                stars.setMinRadius(minRadius);
                stars.setHeightScale(heightScale);
                stars.setSize(0.2);
                stars.setSizeNoise(0.7);
                stars.setIntensity(2.0);
                stars.setLayers(new int[]{0, 1, 2, 4});
                stars.setMaxSize(0.15);

                // HII
                var hii = new BillboardDataset();
                hii.setType(HII);
                hii.setDistribution(DISK);
                hii.setBaseColors(generateColors(HII));
                hii.setParticleCount(generateCount(HII));
                hii.setMinRadius(minRadius);
                hii.setHeightScale(heightScale);
                hii.setSize(0.2);
                hii.setSizeNoise(0.7);
                hii.setIntensity(2.0);
                hii.setLayers(new int[]{0, 1, 2, 4});
                hii.setMaxSize(0.15);

                // DUST
                var dust = new BillboardDataset();
                dust.setType(DUST);
                dust.setDistribution(dustDistribution);
                dust.setBaseColors(generateColors(DUST));
                dust.setParticleCount(generateCount(DUST));
                dust.setMinRadius(minRadius);
                dust.setSize(rand.nextDouble(10.0, 25.0));
                dust.setIntensity(rand.nextDouble(0.01, 0.045));
                dust.setSizeMask(true);
                dust.setSizeNoise(-rand.nextDouble(10.0, 25.0));
                dust.setEccentricity(eccentricity);
                dust.setBaseAngle(spiralAngle);
                dust.setSpiralDeltaPos(spiralDeltaPos);
                dust.setNumArms(numArms);
                dust.setArmSigma(armSigma);
                dust.setBlending(BlendMode.SUBTRACTIVE);
                dust.setDepthMask(false);
                dust.setLayers(new int[]{0, 1, 2});
                dust.setMaxSize(20.0);
                // DUST (field)
                var dustF = new BillboardDataset();
                dustF.setType(DUST);
                dustF.setDistribution(DISK);
                dustF.setBaseColors(dust.baseColors);
                dustF.setParticleCount(5000L);
                dustF.setMinRadius(minRadius);
                dustF.setSize(dust.size * 0.8);
                dustF.setSizeMask(true);
                dustF.setSizeNoise(-rand.nextDouble(10.0, 25.0));
                dustF.setIntensity((double) dust.intensity);
                dustF.setBlending(BlendMode.SUBTRACTIVE);
                dustF.setDepthMask(false);
                dustF.setLayers(new int[]{0, 1, 2});
                dustF.setMaxSize(20.0);

                // GAS
                var gas = new BillboardDataset();
                gas.setType(GAS);
                gas.setDistribution(gasDistribution);
                gas.setBaseColors(generateColors(GAS));
                gas.setParticleCount(generateCount(GAS));
                gas.setMinRadius(minRadius);
                gas.setSize(rand.nextDouble(50.0, 90.0));
                gas.setSizeNoise(0.09);
                gas.setIntensity(rand.nextGaussian(0.008, 0.0004));
                gas.setEccentricity(eccentricity);
                gas.setBaseAngle(spiralAngle);
                gas.setSpiralDeltaPos(spiralDeltaPos);
                gas.setNumArms(numArms);
                gas.setArmSigma(armSigma);
                gas.setLayers(new int[]{0, 1, 2, 3});
                gas.setMaxSize(20.0);

                // BULGE
                var bulge = new BillboardDataset();
                bulge.setType(BillboardDataset.ParticleType.BULGE);
                bulge.setDistribution(BillboardDataset.Distribution.SPHERE);
                bulge.setMinRadius(0.0);
                bulge.setBaseRadius(minRadius + 0.05);
                bulge.setBaseColor(generateColors(BULGE));
                bulge.setParticleCount(generateCount(BULGE));
                bulge.setColorNoise(0.09);
                bulge.setSize(90.0);
                bulge.setIntensity(rand.nextDouble(0.5, 1.2));
                bulge.setLayers(new int[]{0, 1, 2});
                bulge.setMaxSize(50.0);


                var full = new BillboardDataset[]{stars, hii};
                var half = dustDistribution != SPIRAL_LOG ? new BillboardDataset[]{gas, dust, bulge} : new BillboardDataset[]{gas, dust, dustF, bulge};
                result.set(full, half);
            }
            // Barred spirals
            case SBa, SBb, SBc -> {

            }
            // Irregulars
            case Im -> {

            }
        }

        return result;
    }

    private BillboardDataset.Distribution generateSpiralDistribution(GalaxyMorphology m) {
        return switch (m) {
            case Sb -> rand.nextFloat() > 0.3f ? SPIRAL : SPIRAL_LOG;
            case Sc -> rand.nextBoolean() ? SPIRAL : SPIRAL_LOG;
            default -> SPIRAL;
        };
    }

    private double generateSpiralAngle(GalaxyMorphology m, BillboardDataset.Distribution d) {
        if (d == SPIRAL) {
            return switch (m) {
                case Sa -> rand.nextDouble(100.0, 200.0);
                case Sb -> rand.nextDouble(200.0, 500.0);
                case Sc -> rand.nextDouble(500.0, 1000.0);
                default -> rand.nextDouble(50.0, 1000.0);
            };
        } else if (d == BillboardDataset.Distribution.SPIRAL_LOG) {
            return switch (m) {
                case Sa -> rand.nextDouble(500.0, 630.0);
                case Sb -> rand.nextDouble(630.0, 770.0);
                case Sc -> rand.nextDouble(750.0, 870.0);
                default -> rand.nextDouble(50.0, 1000.0);
            };
        }
        return -1;
    }

    private long generateCount(BillboardDataset.ParticleType gt) {
        return switch (gt) {
            case STAR -> rand.nextLong(20_000L, 55_000L);
            case HII -> rand.nextLong(50L, 200L);
            case GAS -> rand.nextLong(3000L, 15_000L);
            case DUST -> rand.nextLong(8000L, 19_500L);
            case BULGE -> rand.nextLong(5L, 18L);
            case POINT -> rand.nextLong(1000L, 50_000L);
        };
    }

    private double[] generateColors(BillboardDataset.ParticleType gt) {
        return switch (gt) {
            case STAR -> getRandomColors(starColors);
            case HII -> getRandomColors(hiiColors);

            case GAS -> getRandomColors(gasColors);
            case DUST -> getRandomColors(dustColors);
            case BULGE -> getRandomColors(bulgeColors);
            case POINT -> getRandomColors(rand.nextBoolean() ? starColors : bulgeColors);
        };
    }

    public static double[] getRandomColors(double[][] colorMatrix) {
        Random rand = new Random();
        double[] result = new double[12];  // Will hold 4 RGB colors (4 * 3 = 12 values)

        for (int i = 0; i < 4; i++) {
            // Randomly choose a color from the matrix
            int randomIndex = rand.nextInt(colorMatrix.length);
            result[i * 3] = colorMatrix[randomIndex][0];  // Red
            result[i * 3 + 1] = colorMatrix[randomIndex][1];  // Green
            result[i * 3 + 2] = colorMatrix[randomIndex][2];  // Blue
        }

        return result;
    }

    private double generateEccentricity(GalaxyMorphology gm) {
        return switch (gm) {
            case E0 -> rand.nextDouble(0.0, 0.1);
            case E3 -> rand.nextDouble(0.1, 0.25);
            case E5 -> rand.nextDouble(0.25, 0.45);
            case E7 -> rand.nextDouble(0.45, 0.6);
            default -> 0f;
        };
    }


}
