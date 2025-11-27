package gaiasky.scene.record;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonValue.ValueType;
import gaiasky.render.BlendMode;
import gaiasky.render.ComponentTypes;
import gaiasky.scene.Mapper;
import gaiasky.scene.record.BillboardDataset.HeightProfile;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Pair;
import gaiasky.util.coord.StaticCoordinates;
import gaiasky.util.math.StdRandom;
import net.jafama.FastMath;

import java.util.Objects;
import java.util.Random;

import static gaiasky.scene.record.BillboardDataset.ChannelType;
import static gaiasky.scene.record.BillboardDataset.ChannelType.*;
import static gaiasky.scene.record.BillboardDataset.Distribution;
import static gaiasky.scene.record.BillboardDataset.Distribution.*;

/**
 * Generates galaxies as lists of {@link BillboardDataset} objects.
 */
public class GalaxyGenerator {
    private static final Logger.Log logger = Logger.getLogger(GalaxyGenerator.class);

    /** Adjectives for random name generation. **/
    private static final String[] adjectives = {
            "Nebulous", "Radiant", "Distant", "Ancient", "Fading",
            "Eternal", "Frozen", "Mystic", "Vast", "Cosmic",
            "Luminous", "Fierce", "Silent", "Shadowed", "Eclipsing",
            "Majestic", "Crimson", "Whirling", "Burning", "Spiral",
            "Twilight", "Endless", "Glowing", "Lost", "Twinkling",
            "Mysterious", "Fragmented", "Golden", "Vibrant", "Dark"
    };

    public static String generateGalaxyName() {
        StdRandom.setSeed(System.currentTimeMillis());
        return adjectives[StdRandom.uniform(adjectives.length)] + " galaxy " + StdRandom.uniform(2, 100);
    }

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
     * @param gm   The galaxy morphology.
     * @param seed The RNG seed.
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

                var distribution = rand.nextFloat() > 0.3f ? ELLIPSOID : SPHERE_GAUSS;
                // Horizontal flattening.
                double eccY = generateEccentricity(gm);
                // Vertical flattening.
                double eccX = eccY * rand.nextDouble(0.1f, 0.6f);

                // Stars
                var stars = generateBase(STAR, distribution);
                stars.setEccentricityX(eccX);
                stars.setEccentricityY(eccY);
                stars.setMinRadius(0.0);
                if (distribution == SPHERE_GAUSS) {
                    stars.setScaleY(1.0 + eccY);
                }

                // Dust (50% chance of being included)
                var dust = generateBase(DUST, distribution);
                dust.setEccentricityX(eccX);
                dust.setEccentricityY(eccY);
                dust.setSize(rand.nextDouble(20.0, 60.9));
                dust.setIntensity(rand.nextDouble(0.005, 0.009));
                if (distribution == SPHERE_GAUSS) {
                    dust.setScaleY(1.0 + eccY);
                }

                // Gas
                var gas = generateBase(GAS, distribution);
                gas.setParticleCount((long) (gas.particleCount * 0.6));
                gas.setEccentricityX(eccX);
                gas.setEccentricityY(eccY);
                gas.setMinRadius(0.0);
                gas.setSize(gas.size * (distribution == ELLIPSOID ? 1.5 : 1.0));
                gas.setColorNoise(rand.nextDouble(0.01, 0.4));
                if (distribution == SPHERE_GAUSS) {
                    gas.setScaleY(1.0 + eccY);
                }


                var full = new BillboardDataset[]{stars};
                var half = rand.nextBoolean() ? new BillboardDataset[]{gas, dust} : new BillboardDataset[]{gas};
                result.set(full, half);
            }
            // Lenticulars
            case S0 -> {
                var distribution = SPHERE_GAUSS;
                var warpStrength = generateWarpStrength(gm);
                var heightScale = generateHeightScale();
                var heightProfile = generateHeightProfile(SPIRAL, heightScale);

                // Stars
                var stars = generateBase(STAR, distribution);
                stars.setMinRadius(0.0);
                stars.setHeightParameters(warpStrength, heightScale, heightProfile);

                // Dust
                var dust = generateBase(DUST, SPIRAL);
                dust.setIntensity(dust.intensity * 0.5);
                dust.setBaseAngle(rand.nextGaussian(990.0, 10.0));
                dust.setMinRadius(rand.nextGaussian(0.2, 0.01));
                dust.setHeightParameters(warpStrength, heightScale, heightProfile);

                // Gas
                var gas = generateBase(GAS, distribution);
                gas.setBaseRadius(rand.nextDouble(1.5, 2.2));
                gas.setMinRadius(0.0);
                gas.setScale(new double[]{1.0, rand.nextDouble(0.1, 2.0), 1.0});

                var full = new BillboardDataset[]{stars};
                var half = rand.nextFloat() > 0.3 ? new BillboardDataset[]{gas, dust} : new BillboardDataset[]{gas};
                result.set(full, half);
            }
            // Spirals
            case Sa, Sb, Sc -> {
                var dustDistribution = generateSpiralDistribution(gm);
                var gasDistribution = dustDistribution == SPIRAL ? SPIRAL : DISK;
                var starDistribution = dustDistribution == SPIRAL ? SPIRAL : DISK_GAUSS;
                var warpStrength = generateWarpStrength(gm);
                var heightScale = generateHeightScale();
                var heightProfile = generateHeightProfile(dustDistribution, heightScale);
                var spiralAngle = generateSpiralAngle(gm, dustDistribution);
                var eccentricity = rand.nextDouble(0.2, 0.3);
                var minRadius = rand.nextDouble(0.08, 0.15);
                var spiralDeltaPos = generateSpiralDeltaPos(dustDistribution);
                var numArms = (rand.nextFloat() > 0.35f ? 2L : 4L) * (gm == GalaxyMorphology.Sc ? 2L : 1L);
                var armSigma = rand.nextDouble(0.25, 0.45) / numArms;

                // Stars
                var stars = generateBase(STAR, starDistribution);
                stars.setSpiralData(spiralAngle, eccentricity, spiralDeltaPos, numArms, armSigma);
                stars.setMinRadius(minRadius);
                stars.setHeightParameters(warpStrength, heightScale, heightProfile);

                // HII
                var hii = generateBase(HII, gasDistribution);
                hii.setSpiralData(spiralAngle, eccentricity, spiralDeltaPos, numArms, armSigma);
                hii.setMinRadius(minRadius);
                hii.setHeightParameters(warpStrength, heightScale, heightProfile);

                // DUST
                var dust = generateBase(DUST, dustDistribution);
                dust.setSpiralData(spiralAngle, eccentricity, spiralDeltaPos, numArms, armSigma);
                dust.setMinRadius(minRadius);
                dust.setHeightParameters(warpStrength, heightScale, heightProfile);

                // DUST (field)
                var dustF = generateBase(DUST, DISK);
                dustF.setBaseColors(dust.baseColors);
                dustF.setIntensity((double) dust.intensity);
                dustF.setParticleCount(5000L);
                dustF.setMinRadius(minRadius);
                dustF.setSize(dust.size * 0.8);
                dustF.setHeightParameters(warpStrength, heightScale, heightProfile);

                // GAS
                var gas = generateBase(GAS, gasDistribution);
                gas.setSpiralData(spiralAngle, eccentricity, spiralDeltaPos, numArms, armSigma);
                gas.setMinRadius(minRadius);
                gas.setHeightParameters(warpStrength, heightScale, heightProfile);

                // BULGE
                var bulge = generateBase(BULGE, SPHERE);
                bulge.setMinRadius(0.0);
                bulge.setBaseRadius(minRadius + 0.05);
                bulge.setColorNoise(0.09);


                var full = new BillboardDataset[]{stars, hii};
                var half = dustDistribution != SPIRAL_LOG ? new BillboardDataset[]{gas, dust, bulge} : new BillboardDataset[]{gas, dust, dustF, bulge};
                result.set(full, half);
            }
            // Barred spirals
            case SBa, SBb, SBc -> {
                var dustDistribution = generateSpiralDistribution(gm);
                var gasDistribution = dustDistribution == SPIRAL ? SPIRAL : DISK;
                var starDistribution = dustDistribution == SPIRAL ? SPIRAL : DISK_GAUSS;
                var warpStrength = generateWarpStrength(gm);
                var heightScale = generateHeightScale();
                var heightProfile = generateHeightProfile(dustDistribution, heightScale);
                var spiralAngle = generateSpiralAngle(gm, dustDistribution);
                var minRadius = rand.nextDouble(0.25, 0.4);
                var eccentricity = rand.nextDouble(0.2, 0.3);
                var spiralDeltaPos = generateSpiralDeltaPos(dustDistribution);
                var numArms = (rand.nextFloat() > 0.35f ? 2L : 4L) * (gm == GalaxyMorphology.SBc ? 2L : 1L);
                var armSigma = rand.nextDouble(0.25, 0.45) / numArms;

                // Stars
                var stars = generateBase(STAR, starDistribution);
                stars.setSpiralData(spiralAngle, eccentricity, spiralDeltaPos, numArms, armSigma);
                stars.setMinRadius(minRadius);
                stars.setHeightParameters(warpStrength, heightScale, heightProfile);

                // HII
                var hii = generateBase(HII, gasDistribution);
                hii.setSpiralData(spiralAngle, eccentricity, spiralDeltaPos, numArms, armSigma);
                hii.setMinRadius(minRadius);
                hii.setHeightParameters(warpStrength, heightScale, heightProfile);

                // DUST
                var dust = generateBase(DUST, dustDistribution);
                dust.setSpiralData(spiralAngle, eccentricity, spiralDeltaPos, numArms, armSigma);
                dust.setMinRadius(minRadius / 2.0);
                dust.setHeightParameters(warpStrength, heightScale, heightProfile);

                // DUST (field)
                var dustF = generateBase(DUST, DISK);
                dustF.setMinRadius(minRadius / 2.0);
                dustF.setBaseColors(dust.baseColors);
                dustF.setIntensity((double) dust.intensity);
                dustF.setParticleCount(5000L);
                dustF.setSize(dust.size * 0.8);
                dustF.setHeightParameters(warpStrength, heightScale, heightProfile);

                // GAS
                var gas = generateBase(GAS, gasDistribution);
                gas.setSpiralData(spiralAngle, eccentricity, spiralDeltaPos, numArms, armSigma);
                gas.setMinRadius(minRadius / 2.0);
                gas.setHeightParameters(warpStrength, heightScale, heightProfile);

                // BAR
                var bar = generateBase(BULGE, ELLIPSOID);
                bar.setMinRadius(0.0);
                bar.setBaseRadius(minRadius - 0.1);
                bar.setParticleCount(70L);
                bar.setSize(bar.size * 0.4);
                bar.setIntensity(bar.intensity * 0.5);
                bar.setEccentricityX(rand.nextDouble(0.4, 0.8));
                bar.setEccentricityY(rand.nextDouble(0.4, 0.8));
                bar.setBaseColors(gas.baseColors);
                bar.setColorNoise(0.09);
                bar.setRotationY(-36.0);


                var full = new BillboardDataset[]{stars, hii};
                var half = dustDistribution != SPIRAL_LOG ? new BillboardDataset[]{gas, dust, bar} : new BillboardDataset[]{gas, dust, dustF, bar};
                result.set(full, half);
            }
            // Irregulars
            case Im -> {
                var sx = rand.nextDouble(0.1, 2.5);
                var sy = rand.nextDouble(0.1, 2.5);
                var sz = rand.nextDouble(0.1, 2.5);
                var scale = new double[]{sx, sy, sz};
                var sizeScale = rand.nextDouble(0.02, 0.4);

                // Stars (random distribution with clumps)
                var stars = generateBase(STAR, IRREGULAR);
                stars.setSize(0.8);
                stars.setIntensity(2.0);
                stars.setScale(scale);
                stars.setSizeNoiseScale(sizeScale);

                // Gas (scattered, clumpy distribution)
                var gas = generateBase(GAS, IRREGULAR);
                gas.setIntensity(gas.intensity * rand.nextDouble(3.0, 8.0));
                gas.setMinRadius(rand.nextDouble(0.1, 0.3));
                gas.setScale(scale);
                gas.setBaseRadius(rand.nextDouble(1.0, 2.0));
                gas.setSizeNoiseScale(sizeScale);
                gas.setSize(gas.size * rand.nextDouble(3.2, 5.0));

                // Dust (random dust clouds, more chaotic)
                var dust = generateBase(DUST, IRREGULAR);
                dust.setIntensity(dust.intensity * rand.nextDouble(0.5, 2.2));
                dust.setSize(rand.nextDouble(15.0, 35.0));
                dust.setScale(scale);
                dust.setSizeNoiseScale(sizeScale);

                // Creating the final dataset
                var full = new BillboardDataset[]{stars};
                var half = new BillboardDataset[]{gas, dust};
                result.set(full, half);
            }
        }

        return result;
    }

    private BillboardDataset generateBase(ChannelType type, Distribution distribution) {
        var bd = new BillboardDataset();
        bd.setType(type);
        bd.setDistribution(distribution);
        bd.setBaseColor(generateColors(type));
        bd.setParticleCount(generateCount(type));
        bd.setIntensity(generateIntensity(type));
        bd.setLayers(getLayers(type));
        bd.setSize(generateSize(type));
        bd.setSizeNoise(getSizeNoise(type));
        bd.setMaxSize(getMaxSize(type));

        if (type == DUST) {
            // Subtractive blending.
            bd.setBlending(BlendMode.SUBTRACTIVE);
            bd.setDepthMask(false);
            // Use FBM perlin noise.
            bd.setSizeNoiseScale(-rand.nextDouble(10.0, 25.0));
        }
        return bd;
    }

    private double getMaxSize(ChannelType type) {
        return switch (type) {
            case STAR -> 0.15;
            case HII -> 0.4;
            case DUST, GAS -> 25.0;
            case BULGE -> 35.0;
            case POINT -> 10.0;
        };
    }

    private int[] getLayers(ChannelType type) {
        return switch (type) {
            case STAR -> new int[]{0, 1, 2, 4};
            case HII -> new int[]{0, 3, 4, 5, 6, 7, 9, 10};
            case DUST -> new int[]{0, 1, 2, 3};
            case GAS -> new int[]{0, 1, 3};
            case BULGE -> new int[]{0, 1, 2};
            case POINT -> new int[]{0, 1, 2, 3, 4};
        };
    }

    private long generateCount(ChannelType gt) {
        return switch (gt) {
            case STAR -> rand.nextLong(26_000L, 33_000L);
            case HII -> rand.nextLong(100L, 500L);
            case GAS -> rand.nextLong(3_000L, 5_000L);
            case DUST -> rand.nextLong(9_000L, 14_500L);
            case BULGE -> rand.nextLong(5L, 18L);
            case POINT -> rand.nextLong(1000L, 50_000L);
        };
    }

    private double generateSize(ChannelType type) {
        return switch (type) {
            case STAR -> 0.3;
            case HII -> 2.2;
            case DUST -> rand.nextDouble(5.0, 10.0);
            case GAS -> rand.nextDouble(50.0, 80.0);
            case BULGE -> rand.nextDouble(70.0, 90.0);
            case POINT -> rand.nextDouble(1.0, 5.0);
        };
    }

    private double getSizeNoise(ChannelType type) {
        return switch (type) {
            case STAR -> 0.4;
            case HII -> 0.6;
            case DUST -> 0.3;
            case GAS -> 0.09;
            case BULGE -> 0.1;
            case POINT -> 0.15;
        };
    }

    private double generateIntensity(ChannelType type) {
        return switch (type) {
            case STAR -> 2.0;
            case HII -> 1.0;
            case DUST -> rand.nextDouble(0.03, 0.08);
            case GAS -> rand.nextGaussian(0.011, 0.0002);
            case BULGE -> rand.nextDouble(0.5, 1.2);
            case POINT -> rand.nextGaussian(0.01, 0.001);
        };
    }

    private Distribution generateSpiralDistribution(GalaxyMorphology m) {
        return switch (m) {
            case Sb -> rand.nextFloat() > 0.25f ? SPIRAL : SPIRAL_LOG;
            case Sa -> rand.nextBoolean() ? SPIRAL : SPIRAL_LOG;
            default -> SPIRAL;
        };
    }

    /**
     * The warp is actually very common, and depends on the morphology.
     * <ul>
     * <li>
     *      Spirals: 50%-70%
     * </li><li>
     *      Barred spirals: 30%-50%
     * </li><li>
     *      Lenticulars: 20%-30%
     * </li>
     * </ul>
     * <p>
     * We take the middle values, so 60% for spirals, 40% for barred spirals, and 25% for lenticulars.
     */
    private double generateWarpStrength(GalaxyMorphology m) {
        var w = rand.nextDouble(GAS.warpStrength[0] * 0.6f, GAS.warpStrength[1] * 0.6f);
        return switch (m) {
            case Sa, Sb, Sc -> rand.nextDouble() < 0.6 ? w : 0.0;
            case SBa, SBb, SBc -> rand.nextDouble() < 0.4 ? w : 0.0;
            case S0 -> rand.nextDouble() < 0.25 ? w : 0.0;
            default -> 0.0;
        };
    }

    private double generateHeightScale() {
        return rand.nextDouble(0.0, 0.03);
    }

    private HeightProfile generateHeightProfile(Distribution d, double heightScale) {

        return switch (d) {
            case SPHERE, SPHERE_GAUSS, ELLIPSOID, BAR, CONE, IRREGULAR -> HeightProfile.CONSTANT;
            case SPIRAL, SPIRAL_LOG, DISK, DISK_GAUSS -> {
                if (heightScale > 0.03) {
                    yield rand.nextDouble() > 0.4 ? HeightProfile.SMOOTH_INC : HeightProfile.LINEAR_INC;
                } else {
                    yield rand.nextDouble() > 0.8 ? (rand.nextBoolean() ? HeightProfile.SMOOTH_INC : HeightProfile.LINEAR_INC) : HeightProfile.CONSTANT;
                }
            }
        };
    }

    private double generateSpiralAngle(GalaxyMorphology m, Distribution d) {
        if (d == SPIRAL) {
            return switch (m) {
                case Sc, SBc -> rand.nextDouble(190.0, 300.0);
                case Sb, SBb -> rand.nextDouble(300.0, 500.0);
                case Sa, SBa -> rand.nextDouble(500.0, 1000.0);
                default -> rand.nextDouble(50.0, 1000.0);
            };
        } else if (d == Distribution.SPIRAL_LOG) {
            return switch (m) {
                case Sc -> rand.nextDouble(500.0, 630.0);
                case Sb -> rand.nextDouble(630.0, 770.0);
                case Sa -> rand.nextDouble(750.0, 870.0);
                default -> rand.nextDouble(50.0, 1000.0);
            };
        }
        return -1;
    }

    private double[] generateSpiralDeltaPos(Distribution d) {
        if (Objects.requireNonNull(d) == SPIRAL) {
            // Half of spirals have displacement.
            if (rand.nextBoolean()) {
                var dx = rand.nextGaussian() * 0.2f;
                var dy = rand.nextGaussian() * 0.2f;
                if (rand.nextBoolean()) {
                    // Only delta in X or Y.
                    if (rand.nextBoolean()) {
                        return new double[]{dx, 0.0};
                    } else {
                        return new double[]{0.0, dy};
                    }
                } else {
                    // Delta in both.
                    return new double[]{dx, dy};
                }
            } else {
                return new double[]{0.0, 0.0};
            }
        }
        return new double[]{0.0, 0.0};
    }

    private double[] generateColors(ChannelType gt) {
        return switch (gt) {
            case STAR -> getRandomColors(starColors);
            case HII -> getRandomColors(hiiColors);

            case GAS -> getRandomColors(gasColors);
            case DUST -> getRandomColors(dustColors);
            case BULGE -> getRandomColors(bulgeColors);
            case POINT -> getRandomColors(rand.nextBoolean() ? starColors : bulgeColors);
        };
    }

    public double[] getRandomColors(double[][] colorMatrix) {
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
            case E0 -> rand.nextDouble(0.0, 0.25);
            case E3 -> rand.nextDouble(0.25, 0.45);
            case E5 -> rand.nextDouble(0.45, 0.67);
            case E7 -> rand.nextDouble(0.67, 0.95);
            default -> 0f;
        };
    }


    public JsonValue convertToJson(Entity full, Entity half) {
        var top = new JsonValue(ValueType.object);
        var array = new JsonValue(ValueType.array);
        top.addChild("objects", array);

        var fullJ = convertToJson(full);
        var halfJ = convertToJson(half);
        array.addChild(fullJ);
        array.addChild(halfJ);
        return top;
    }

    public JsonValue convertToJson(Entity e) {
        var obj = new JsonValue(ValueType.object);

        var base = Mapper.base.get(e);
        var body = Mapper.body.get(e);
        var graph = Mapper.graph.get(e);
        var label = Mapper.label.get(e);
        var coord = Mapper.coordinates.get(e);
        var trf = Mapper.transform.get(e);
        var bb = Mapper.billboardSet.get(e);
        var focus = Mapper.focus.get(e);
        var fade = Mapper.fade.get(e);
        var render = Mapper.render.get(e);

        // Base
        if (base.names.length == 1) {
            obj.addChild("name", new JsonValue(base.getName()));
        } else {
            addArray(obj, "names", base.names);
        }
        if (body.color != null) {
            addArray(obj, "color", body.color);
        }
        if (body.labelColor != null) {
            addArray(obj, "labelColor", body.labelColor);
        }
        obj.addChild("sizePc", new JsonValue(body.size * Constants.U_TO_PC));
        obj.addChild("componentType", new JsonValue(ComponentTypes.ComponentType.values()[base.ct.getFirstOrdinal()].name()));
        obj.addChild("archetype", new JsonValue("BillboardGroup"));
        if (graph.parent != null) {
            obj.addChild("parent", new JsonValue(Mapper.base.get(graph.parent).getName()));
        }
        // Fade
        if (fade.fadePositionObjectName != null) {
            obj.addChild("fadeObjectname", new JsonValue(fade.fadePositionObjectName));
        }
        if (fade.fadeIn != null) {
            addArray(obj, "fadeIn", fade.fadeIn.values());
        }
        if (fade.fadeOut != null) {
            addArray(obj, "fadeOut", fade.fadeOut.values());
        }
        // Generation
        if (bb.procedural) {
            obj.addChild("procedural", new JsonValue(true));
        }
        if (bb.seed >= 0)
            obj.addChild("seed", new JsonValue(bb.seed));
        if (bb.morphology != null)
            obj.addChild("morphology", new JsonValue(bb.morphology.name()));
        obj.addChild("halfResolutionBuffer", new JsonValue(render.halfResolutionBuffer));
        addArray(obj, "textures", bb.textureFiles);
        // Misc
        obj.addChild("focusable", new JsonValue(focus.focusable));
        obj.addChild("renderLabel", new JsonValue(label.isDisplayAuto()));
        if (trf.transformName != null) {
            obj.addChild("transformName", new JsonValue(trf.transformName));
        } else if (trf.matrix != null) {
            addArray(obj, "transformMatrix", trf.matrix.val);
        }
        // Coordinates
        var coordsObj = new JsonValue(ValueType.object);
        var stc = (StaticCoordinates) coord.coordinates;
        coordsObj.addChild("impl", new JsonValue(stc.getClass().getName()));
        if (stc.getTransformName() != null)
            coordsObj.addChild("transformName", new JsonValue(stc.getTransformName()));
        if (stc.getPosition() != null)
            addArray(coordsObj, "positionPc", stc.getPosition().cpy().scl(Constants.U_TO_PC).valuesD());
        obj.addChild("coordinates", coordsObj);
        // Data
        var data = new JsonValue(ValueType.array);
        for (var bd : bb.datasets) {
            var dataset = new JsonValue(ValueType.object);
            dataset.addChild("impl", new JsonValue("gaiasky.scene.record.BillboardDataset"));
            dataset.addChild("type", new JsonValue(bd.type.name()));
            dataset.addChild("distribution", new JsonValue(bd.distribution.name()));
            dataset.addChild("blending", new JsonValue(bd.blending.name()));
            dataset.addChild("depthMask", new JsonValue(bd.depthMask));
            dataset.addChild("particleCount", new JsonValue(bd.particleCount));
            dataset.addChild("size", new JsonValue(bd.size));
            if (bd.sizeMask) {
                dataset.addChild("sizeNoiseScale", new JsonValue(bd.sizeNoise));
            } else {
                dataset.addChild("sizeNoise", new JsonValue(bd.sizeNoise));
            }
            addArray(dataset, "baseColors", bd.baseColors);
            dataset.addChild("maxSize", new JsonValue(Math.toDegrees(FastMath.atan(bd.maxSizes[0]))));
            dataset.addChild("intensity", new JsonValue(bd.intensity));
            dataset.addChild("heightScale", new JsonValue(bd.heightScale));
            dataset.addChild("minRadius", new JsonValue(bd.minRadius));
            dataset.addChild("baseRadius", new JsonValue(bd.baseRadius));
            addArray(dataset, "layers", bd.layers);
            if (!bd.translation.isZero())
                addArray(dataset, "translation", bd.translation);
            if (!bd.rotation.isZero())
                addArray(dataset, "rotation", bd.rotation);
            if (!(bd.scale.x == 1 && bd.scale.y == 1 && bd.scale.z == 1))
                addArray(dataset, "scale", bd.scale);
            if (bd.eccentricity[1] == 0)
                dataset.addChild("eccentricity", new JsonValue(bd.eccentricity[0]));
            else
                addArray(dataset, "eccentricity", bd.eccentricity);
            dataset.addChild("aspect", new JsonValue(bd.aspect));
            dataset.addChild("baseAngle", new JsonValue(bd.baseAngle));
            dataset.addChild("numArms", new JsonValue(bd.numArms));
            dataset.addChild("armSigma", new JsonValue(bd.armSigma));
            if (!(bd.spiralDeltaPos[0] == 0 && bd.spiralDeltaPos[1] == 0))
                addArray(dataset, "spiralDeltaPos", bd.spiralDeltaPos);

            data.addChild(dataset);
        }
        obj.addChild("data", data);

        return obj;
    }

    private <T> void addArray(JsonValue parent, String name, T arr) {
        var obj = new JsonValue(ValueType.array);

        switch (arr) {
            case int[] ints -> {
                // Handle int[] (primitive type)
                for (int n : ints) {
                    obj.addChild(new JsonValue(n));
                }
            }
            case float[] floats -> {
                // Handle float[] (primitive type)
                for (float n : floats) {
                    obj.addChild(new JsonValue(n, Float.toString(n)));
                }
            }
            case double[] doubles -> {
                // Handle double[] (primitive type)
                for (double n : doubles) {
                    obj.addChild(new JsonValue(n, Double.toString(n)));
                }
            }
            case long[] longs -> {
                // Handle long[] (primitive type)
                for (long n : longs) {
                    obj.addChild(new JsonValue(n));
                }
            }
            case short[] shorts -> {
                // Handle short[] (primitive type)
                for (short n : shorts) {
                    obj.addChild(new JsonValue(n));
                }
            }
            case byte[] bytes -> {
                // Handle byte[] (primitive type)
                for (byte n : bytes) {
                    obj.addChild(new JsonValue(n));
                }
            }
            case boolean[] booleans -> {
                // Handle boolean[] (primitive type)
                for (boolean n : booleans) {
                    obj.addChild(new JsonValue(n));
                }
            }
            case String[] booleans -> {
                // Handle String[]
                for (String n : booleans) {
                    obj.addChild(new JsonValue(n));
                }
            }
            case Vector3 vec3 -> {
                obj.addChild(new JsonValue(vec3.x));
                obj.addChild(new JsonValue(vec3.y));
                obj.addChild(new JsonValue(vec3.z));
            }
            default -> logger.warn("Unsupported array type: " + arr.getClass().getName());
        }

        parent.addChild(name, obj);
    }
}
