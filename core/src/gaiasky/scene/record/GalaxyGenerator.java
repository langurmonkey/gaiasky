package gaiasky.scene.record;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonValue.ValueType;
import com.badlogic.gdx.utils.JsonWriter;
import gaiasky.render.BlendMode;
import gaiasky.render.ComponentTypes;
import gaiasky.scene.Mapper;
import gaiasky.scene.record.GalaxyAttributes.HeightProfile;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Pair;
import gaiasky.util.coord.StaticCoordinates;
import gaiasky.util.math.StdRandom;
import net.jafama.FastMath;

import java.util.Locale;
import java.util.Objects;
import java.util.Random;

import static gaiasky.scene.record.GalaxyAttributes.ChannelType;
import static gaiasky.scene.record.GalaxyAttributes.ChannelType.*;
import static gaiasky.scene.record.GalaxyAttributes.Distribution;
import static gaiasky.scene.record.GalaxyAttributes.Distribution.*;

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
     * Generates galaxies given a morphology, as billboard datasets only.
     *
     * @param gm   The galaxy morphology.
     * @param seed The RNG seed.
     *
     * @return A couple of {@link BillboardDataset} arrays, the first being the full-resolution channel, and the second being the half-resolution
     *         channel.
     */
    public Pair<BillboardDataset[], BillboardDataset[]> generateGalaxy(GalaxyMorphology gm, long seed) {
        var spec = generateGalaxySpec(gm, seed);
        var result = new Pair<BillboardDataset[], BillboardDataset[]>();
        result.set(spec.full, spec.half);
        return result;
    }

    /**
     * Generates the complete description of a galaxy given a morphology: its billboard datasets and
     * the volume fields that describe the same gas, bulge and dust analytically.
     * <p>
     * Both come from a single seeded draw, so the volume of a hybrid galaxy is the one its billboards
     * describe. No extra random numbers are consumed when building the volume fields - they are
     * copies of the descriptors this draw already produced - which is why a seed yields the same
     * billboards as it did before the volume path existed.
     *
     * @param gm   The galaxy morphology. If null, one is picked at random and recorded in the spec.
     * @param seed The RNG seed.
     *
     * @return The spec, which is never null.
     */
    public GalaxySpec generateGalaxySpec(GalaxyMorphology gm, long seed) {
        rand.setSeed(seed);
        if (gm == null) {
            gm = GalaxyMorphology.values()[rand.nextInt(GalaxyMorphology.values().length)];
        }

        BillboardDataset[] specFull = null;
        BillboardDataset[] specHalf = null;
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


                specFull = new BillboardDataset[]{stars};
                specHalf = rand.nextBoolean() ? new BillboardDataset[]{gas, dust} : new BillboardDataset[]{gas};
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
                // baseRadius is a fraction of the object radius, and a volume is clipped by it, so
                // the gas fills the object instead of spilling past it.
                gas.setBaseRadius(1.0);
                gas.setMinRadius(0.0);
                gas.setScale(new double[]{1.0, rand.nextDouble(0.1, 2.0), 1.0});

                specFull = new BillboardDataset[]{stars};
                specHalf = rand.nextFloat() > 0.3 ? new BillboardDataset[]{gas, dust} : new BillboardDataset[]{gas};
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
                var armSigma = rand.nextDouble(0.25, 0.45);
                // The gas is the broad component: sharing the stars' sigma gives arms narrower than
                // the voxel grid, and the gas renders as thin lanes instead of as a diffuse arm.
                var gasArmSigma = rand.nextDouble(0.22, 0.5);

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
                gas.setArmSigma(gasArmSigma);
                // The gas has no inner edge: a hole in the middle is the bulge's presence, and the
                // bulge is not always there. The arms still fade in over an inner disc, which is an
                // anti-aliasing term of the field and not a physical cut.
                gas.setMinRadius(0.0);
                gas.setHeightParameters(warpStrength, heightScale, heightProfile);

                // BULGE
                var bulge = generateBase(BULGE, SPHERE);
                bulge.setMinRadius(0.0);
                bulge.setBaseRadius(minRadius + 0.05);
                bulge.setColorNoise(0.22);


                specFull = new BillboardDataset[]{stars, hii};
                specHalf = dustDistribution != SPIRAL_LOG ? new BillboardDataset[]{gas, dust, bulge} : new BillboardDataset[]{gas, dust, dustF, bulge};
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
                var armSigma = rand.nextDouble(0.25, 0.45);
                // See the spiral branch: the gas gets its own, broader sigma.
                var gasArmSigma = rand.nextDouble(0.22, 0.5);

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
                gas.setArmSigma(gasArmSigma);
                // See the spiral branch: the gas keeps its centre, the bar is what may be there.
                gas.setMinRadius(0.0);
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


                specFull = new BillboardDataset[]{stars, hii};
                specHalf = dustDistribution != SPIRAL_LOG ? new BillboardDataset[]{gas, dust, bar} : new BillboardDataset[]{gas, dust, dustF, bar};
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
                // The gas of an irregular has no inner edge either; the sky is not behind a bulge.
                gas.setMinRadius(0.0);
                gas.setScale(scale);
                // A fraction of the object radius, as everywhere else.
                gas.setBaseRadius(rand.nextDouble(0.7, 1.0));
                gas.setSizeNoiseScale(sizeScale);
                gas.setSize(gas.size * rand.nextDouble(3.2, 5.0));

                // Dust (random dust clouds, more chaotic)
                var dust = generateBase(DUST, IRREGULAR);
                dust.setIntensity(dust.intensity * rand.nextDouble(0.5, 2.2));
                dust.setSize(rand.nextDouble(15.0, 35.0));
                dust.setScale(scale);
                dust.setSizeNoiseScale(sizeScale);

                // Creating the final dataset
                specFull = new BillboardDataset[]{stars};
                specHalf = new BillboardDataset[]{gas, dust};
            }
        }

        return new GalaxySpec(gm, seed, specFull, specHalf);
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
        if (type == GAS || type == DUST) {
            // The diffuse channels always get clumped: an analytic field without it is a smooth
            // mathematical surface, which is what makes these galaxies read as synthetic.
            var mask = rand.nextDouble(0.55, 0.85);
            bd.setNoiseMask(mask);
            bd.setNoiseMaskFreq(rand.nextDouble(5.0, 12.0));
            bd.setNoiseMaskOctaves((long) rand.nextInt(3, 5));
            // The mask removes density where the noise is in a trough, so it lowers the mean; the
            // intensity has to make up for that or the clumped field comes out dimmer.
            bd.setIntensity(bd.intensity * (1.0 + mask * 0.5));
        } else if (type == BULGE) {
            // A stellar population is smoother than the gas, but not a solid: at the resolution of a
            // volume the crest of the bulge is thousands of pixels across, and an exact spheroid
            // there is the clearest tell of a synthetic galaxy. Milder and coarser than the gas.
            bd.setNoiseMask(rand.nextDouble(0.15, 0.3));
            bd.setNoiseMaskFreq(rand.nextDouble(3.0, 7.0));
            bd.setNoiseMaskOctaves((long) rand.nextInt(2, 4));
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
            case BULGE -> rand.nextDouble(0.8, 2.2);
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
        // Upper part of the range: a warp near zero is invisible next to a 0.03-thick disk.
        var w = rand.nextDouble(GAS.warpStrength[0] + 0.25 * (GAS.warpStrength[1] - GAS.warpStrength[0]),
                                GAS.warpStrength[1]);
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
        // The pitch is 0.1 * the stored value, so the floor of 500 - 50 degrees - is the flattest
        // arm the generator draws. Below it the arms are open enough to read as spokes rather than
        // as spiral arms, in both the field and the particles.
        if (d == SPIRAL) {
            return switch (m) {
                case Sc, SBc -> rand.nextDouble(500.0, 600.0);
                case Sb, SBb -> rand.nextDouble(600.0, 750.0);
                case Sa, SBa -> rand.nextDouble(750.0, 850.0);
                default -> rand.nextDouble(500.0, 850.0);
            };
        } else if (d == Distribution.SPIRAL_LOG) {
            return switch (m) {
                case Sc -> rand.nextDouble(500.0, 630.0);
                case Sb -> rand.nextDouble(630.0, 770.0);
                case Sa -> rand.nextDouble(750.0, 850.0);
                default -> rand.nextDouble(500.0, 850.0);
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

    /**
     * Converts a hybrid galaxy into a JSON descriptor: the billboard entity with its compact emitters,
     * and the child volume entity that carries the density fields of the diffuse channels.
     *
     * @param billboard The billboard entity of the pair.
     * @param volume    The child volume entity of the pair.
     *
     * @return The descriptor, ready to be printed.
     */
    public JsonValue convertToHybridJson(Entity billboard,
                                         Entity volume) {
        var top = new JsonValue(ValueType.object);
        var array = new JsonValue(ValueType.array);
        top.addChild("objects", array);

        array.addChild(convertToJson(billboard));
        array.addChild(convertVolumeToJson(volume));
        return top;
    }

    /**
     * Converts the volume half of a hybrid galaxy into a JSON object.
     * <p>
     * A volume entity is a plain {@code Volume} archetype: a unit sphere scaled by the body size,
     * with the galaxy shader and the field list that {@code VolumeInitializer} voxelises at load
     * time. No material is written: a procedural volume without one gets a default material, which is
     * exactly what this galaxy was built with.
     *
     * @param e The volume entity.
     *
     * @return The descriptor of the entity.
     */
    public JsonValue convertVolumeToJson(Entity e) {
        var obj = new JsonValue(ValueType.object);

        var base = Mapper.base.get(e);
        var body = Mapper.body.get(e);
        var graph = Mapper.graph.get(e);
        var coord = Mapper.coordinates.get(e);
        var trf = Mapper.transform.get(e);
        var fade = Mapper.fade.get(e);
        var focus = Mapper.focus.get(e);
        var modelComponent = Mapper.model.get(e);
        var model = modelComponent != null ? modelComponent.model : null;
        var render = Mapper.render.get(e);
        var volume = Mapper.volume.get(e);

        // Base
        if (base.names.length == 1) {
            obj.addChild("name", new JsonValue(base.getName()));
        } else {
            addArray(obj, "names", base.names);
        }
        obj.addChild("componentType", new JsonValue(ComponentTypes.ComponentType.values()[base.ct.getFirstOrdinal()].name()));
        obj.addChild("archetype", new JsonValue("Volume"));
        if (graph.parent != null) {
            obj.addChild("parent", new JsonValue(Mapper.base.get(graph.parent).getName()));
        }
        // Body: same key and same meaning as the billboard half. The volume keeps its size as a
        // diameter in internal units (sizeIsRadiusFlag cleared), while sizePc is a radius, so the
        // factor has to be applied here or the two halves of the galaxy come out at different sizes.
        double radiusPc = body.size * (body.sizeIsRadiusFlag ? 1.0 : 0.5) * Constants.U_TO_PC;
        obj.addChild("sizePc", new JsonValue(radiusPc));
        obj.addChild("cameraCollision", new JsonValue(body.cameraCollision));
        if (body.labelColor != null) {
            addArray(obj, "labelColor", body.labelColor);
        }
        // Fade: with the galaxy, not with its own offset.
        if (fade.fadePositionObjectName != null) {
            obj.addChild("fadeObjectName", new JsonValue(fade.fadePositionObjectName));
        }
        if (fade.fadeOut != null) {
            addArray(obj, "fadeOut", toPc(fade.fadeOut.values()));
        }
        // The volume is the only child of a galaxy whose brightness is multiplied by Base#opacity
        // (ModelEntityRenderSystem#renderVolume); the billboards ignore it. Inheriting the parent's
        // opacity therefore lets an ancestor's distance fade erase the volume: NBG's nbg-hook fades
        // in between 1 and 8 kpc from the Sun, so every volume under it was invisible from inside
        // the Milky Way. 'inheritOpacity' makes it follow the parent's visibility instead, which
        // keeps it drawn while the ancestor is faded out and hides it when the ancestor is off.
        obj.addChild("inheritOpacity", new JsonValue(false));
        // Focus: the volume is never a focus target, its parent is.
        obj.addChild("focusable", new JsonValue(focus.focusable));
        obj.addChild("halfResolutionBuffer", new JsonValue(render.halfResolutionBuffer));
        // Model
        if (model != null) {
            var modelObj = new JsonValue(ValueType.object);
            if (model.modelType != null) {
                modelObj.addChild("type", new JsonValue(model.modelType.name().toLowerCase(Locale.ROOT)));
            }
            if (model.modelParams != null) {
                var paramsObj = new JsonValue(ValueType.object);
                for (var param : model.modelParams.entrySet()) {
                    paramsObj.addChild(param.getKey(), toJsonValue(param.getValue()));
                }
                modelObj.addChild("params", paramsObj);
            }
            modelObj.addChild("culling", new JsonValue(model.culling));
            modelObj.addChild("useColor", new JsonValue(model.isUseColor()));
            if (model.getBlendMode() != null) {
                modelObj.addChild("blendMode", new JsonValue(model.getBlendMode().name()));
            }
            obj.addChild("model", modelObj);
        }
        // Volume. Its attributes are flat in the object, not nested: that is how the attribute map
        // declares them for the Volume component, and a "volume" block would be an unknown key.
        obj.addChild("vertexShader", new JsonValue(volume.vertexShader));
        obj.addChild("fragmentShader", new JsonValue(volume.fragmentShader));
        obj.addChild("writeDepth", new JsonValue(volume.writeDepth));
        addArray(obj, "iterations", volume.iterations);
        var fields = fieldsToJson(volume.fields);
        if (fields != null) {
            obj.addChild("fields", fields);
        }
        // Reference system transform: the volume has to be written with the same tilt the billboard
        // half carries, or the gas is drawn in a different orientation from its own stars.
        if (trf != null) {
            if (trf.transformName != null) {
                obj.addChild("transformName", new JsonValue(trf.transformName));
            } else if (trf.matrix != null) {
                addArray(obj, "transformMatrix", trf.matrix.val);
            }
        }
        // Coordinates: a child's position is an offset from its parent's.
        var coordsObj = new JsonValue(ValueType.object);
        var stc = (StaticCoordinates) coord.coordinates;
        coordsObj.addChild("impl", new JsonValue(stc.getClass().getName()));
        if (stc.getTransformName() != null) {
            coordsObj.addChild("transformName", new JsonValue(stc.getTransformName()));
        }
        if (stc.getPosition() != null) {
            addArray(coordsObj, "positionPc", stc.getPosition().cpy().scl(Constants.U_TO_PC).valuesD());
        }
        obj.addChild("coordinates", coordsObj);

        return obj;
    }

    /**
     * Serialises the density fields of a volume, each of them a complete {@link GalaxyAttributes}
     * entry.
     * <p>
     * Every attribute is written, not only the ones that differ from the defaults: a field is a
     * handful of numbers, and an export that reads back the same whichever defaults the next version
     * of the generator happens to pick is worth the few extra lines.
     *
     * @param fields The fields, possibly null.
     *
     * @return The JSON array, or null if there is nothing to write.
     */
    /**
     * Serialises a descriptor for export.
     * <p>
     * Deliberately not {@link JsonValue#toJson(JsonWriter.OutputType)}: that path prints a whole
     * double as an integer literal ({@code 0.0} becomes {@code 0}, see
     * {@code JsonValue.toJson(OutputType, Writer)}), so {@code positionPc} and every other
     * {@code double} attribute comes back from the file as a {@code Long}/{@code int[]} and the
     * loader finds no {@code setPositionPc(double[])}. Pretty printing keeps the decimal point,
     * and is easier to read and hand-edit besides.
     *
     * @param json The descriptor.
     *
     * @return The JSON text.
     */
    public static String toJsonString(JsonValue json) {
        return json.prettyPrint(JsonWriter.OutputType.json, 120);
    }

    public static JsonValue fieldsToJson(GalaxyAttributes[] fields) {
        if (fields == null || fields.length == 0) {
            return null;
        }
        var array = new JsonValue(ValueType.array);
        for (var f : fields) {
            var o = new JsonValue(ValueType.object);
            o.addChild("impl", new JsonValue(GalaxyAttributes.class.getName()));
            o.addChild("type", new JsonValue(f.type.name()));
            o.addChild("distribution", new JsonValue(f.distribution.name()));
            addArray(o, "baseColors", f.baseColors);
            o.addChild("colorNoise", new JsonValue(f.colorNoise));
            o.addChild("baseRadius", new JsonValue(f.baseRadius));
            o.addChild("minRadius", new JsonValue(f.minRadius));
            addArray(o, "translation", f.translation);
            addArray(o, "scale", f.scale);
            o.addChild("warpStrength", new JsonValue(f.warpStrength));
            o.addChild("heightScale", new JsonValue(f.heightScale));
            o.addChild("heightProfile", new JsonValue(f.heightProfile.name()));
            addArray(o, "eccentricity", f.eccentricity);
            o.addChild("aspect", new JsonValue(f.aspect));
            o.addChild("baseAngle", new JsonValue(f.baseAngle));
            addArray(o, "spiralDeltaPos", f.spiralDeltaPos);
            o.addChild("numArms", new JsonValue(f.numArms));
            o.addChild("armSigma", new JsonValue(f.armSigma));
            o.addChild("laneWarp", new JsonValue(f.laneWarp));
            o.addChild("laneRough", new JsonValue(f.laneRough));
            o.addChild("laneNoiseScale", new JsonValue(f.laneNoiseScale));
            o.addChild("noiseMask", new JsonValue(f.noiseMask));
            // These two use 0 as "the default", and 0 is what an untouched field carries. The octave
            // setter clamps to [1,8], so writing the sentinel would turn the default three octaves
            // into one; leaving the key out keeps the field's own default, which is the same thing.
            if (f.noiseMaskFreq != 0) {
                o.addChild("noiseMaskFreq", new JsonValue(f.noiseMaskFreq));
            }
            if (f.noiseMaskOctaves != 0) {
                o.addChild("noiseMaskOctaves", new JsonValue(f.noiseMaskOctaves));
            }
            o.addChild("intensity", new JsonValue(f.intensity));
            o.addChild("occlusionThreshold", new JsonValue(f.occlusionThreshold));
            array.addChild(o);
        }
        return array;
    }

    /**
     * Converts a distance in internal units to parsecs. The fade distances are stored in internal
     * units, so writing them raw would multiply them by {@link Constants#PC_TO_U} again on load.
     *
     * @param values The distances, in internal units.
     *
     * @return The same distances, in parsecs.
     */
    private static double[] toPc(double[] values) {
        var out = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = values[i] * Constants.U_TO_PC;
        }
        return out;
    }

    /**
     * Converts a model parameter to JSON, keeping the number type it was created with: the model
     * builders read some of them as {@link Long} and cast, so a whole number written as a decimal
     * would come back as a {@link Double} and throw.
     *
     * @param value The value.
     *
     * @return The JSON value.
     */
    private static JsonValue toJsonValue(Object value) {
        return switch (value) {
            case null -> new JsonValue(ValueType.nullValue);
            case Boolean b -> new JsonValue(b);
            case Long l -> new JsonValue(l);
            case Number n -> new JsonValue(n.doubleValue());
            case String s -> new JsonValue(s);
            default -> new JsonValue(value.toString());
        };
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
            obj.addChild("fadeObjectName", new JsonValue(fade.fadePositionObjectName));
        }
        if (fade.fadeIn != null) {
            addArray(obj, "fadeIn", toPc(fade.fadeIn.values()));
        }
        if (fade.fadeOut != null) {
            addArray(obj, "fadeOut", toPc(fade.fadeOut.values()));
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

    private static <T> void addArray(JsonValue parent, String name, T arr) {
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
