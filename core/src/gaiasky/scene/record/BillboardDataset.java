/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

import com.badlogic.gdx.math.Vector3;
import gaiasky.data.group.PointDataProvider;
import gaiasky.render.BlendMode;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.util.*;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings.GraphicsQuality;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.tree.GenStatus;
import net.jafama.FastMath;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A dataset composed of a single set of billboard-like particles.
 */
public class BillboardDataset {
    private static final Log logger = Logger.getLogger(BillboardDataset.class);
    /**
     * Maximum number of supported colors.
     **/
    private static final int MAX_COLORS = 4;

    /**
     * Number of particles to generate with procedural generation.
     */
    public int particleCount = 0;
    /**
     * Path to the file containing the particle data. Ignored if this dataset is in a procedural billboard group.
     */
    public String file;
    /**
     * Unpacked file path. Ignored if this dataset is in a procedural billboard group.
     */
    public String fileUnpack;
    /**
     * List of particle records with the particles of this dataset. Ignored if this dataset is in a procedural billboard group.
     */
    public List<IParticleRecord> data;
    /**
     * Type of particle.
     */
    public ChannelType type = ChannelType.POINT;
    /**
     * Probability distribution, for procedural datasets.
     */
    public Distribution distribution = Distribution.DISK;
    /**
     * Base color(s) for the particles of this dataset. These colors will be used as base to generate the particle colors.
     * {@link #MAX_COLORS} RGBA colors are supported, so the size of this array must be {@link #MAX_COLORS} * 3.
     */
    public float[] baseColors = new float[MAX_COLORS * 3];
    /**
     * Color randomness to apply to the base colors to generate the final particle colors.
     */
    public float colorNoise = 0.08f;
    /**
     * Texture layers to use.
     */
    public int[] layers;
    /**
     * Array with completion rate per texture quality (to skip data).
     */
    public float[] completion;
    /**
     * Base radius of this dataset. 1 is the default radius, but you can make this component spread out wider or thinner by changing this.
     */
    public float baseRadius = 1;
    /**
     * Minimum radius to generate particles. Only available for spiral and density wave distributions.
     * Must be in [0,1], where 0 is the very center, and 1 is baseRadius.
     */
    public float minRadius = 0.01f;
    /**
     * Render particle size scale factor.
     */
    public float size = 1;
    /**
     * Use FBM perlin noise to mask size generation.
     */
    public boolean sizeMask = false;
    /**
     * Used to compute the size.
     * <ul>
     *     <li>&ge; 0 &mdash; sizeMask false, randomness of sizes</li>
     *     <li>&lt; 0 &mdash; sizeMask true, scale of FBM perlin noise</li>
     * </ul>
     */
    public float sizeNoise = 0.1f;

    /**
     * Translation vector for the particles of this dataset.
     */
    public Vector3 translation = new Vector3(0f, 0f, 0f);

    /**
     * Scale vector for the particles of this dataset.
     */
    public Vector3 scale = new Vector3(1f, 1f, 1f);

    /**
     * Euler rotations for the particles of this dataset.
     */
    public Vector3 rotation = new Vector3(0f, 0f, 0f);

    /**
     * Strength of the warp, for flat distributions.
     */
    public float warpStrength = 0.0f;
    /**
     * Height scale.
     */
    public float heightScale = 0.01f;
    /**
     * Height (vertical) profile.
     */
    public HeightProfile heightProfile = HeightProfile.CONSTANT;
    /**
     * Eccentricities for density wave [1] and ellipsoid [2].
     */
    public float[] eccentricity = new float[]{0.3f, 0f};
    /**
     * Bar aspect ratio, e.g. 0.3 = short bar, 1.0 = long bar.
     */
    public float aspect = 1.0f;

    /**
     * Spiral arm pitch angle in degrees.
     * <p>
     * When using {@link Distribution#SPIRAL_LOG}, this controls how tightly the spiral arms wind around the galactic center.
     * Lower values (≈5–10°) produce tightly wound Sa-type spirals,
     * while higher values (≈25–40°) yield open Sc–Sd morphologies.
     * This parameter maps directly to the logarithmic spiral pitch angle
     * used in the compute shader.
     * <p>
     * When using {@link Distribution#SPIRAL}, this controls the total rotation of the concentric ellipses.
     */
    public float baseAngle = 6.0f;
    /**
     * Displacement of the ellipses in the {@link Distribution#SPIRAL} mode.
     */
    public float[] spiralDeltaPos = new float[]{0.0f, 0.0f};
    /**
     * Number of spiral arms.
     */
    public int numArms = 4;

    /**
     * Standard deviation across arm.
     */
    public float armSigma = 0.4f;

    /**
     * The intensity factor.
     */
    public float intensity = 1;
    /**
     * Whether to allow depth writes when rendering.
     */
    public boolean depthMask = false;
    /**
     * The blending mode.
     */
    public BlendMode blending = BlendMode.ADDITIVE;

    /**
     * Maximum particle size for each texture quality mode. It has 4 entries, from LOW to ULTRA.
     * See {@link GraphicsQuality}.
     **/
    public double[] maxSizes = new double[GraphicsQuality.values().length];

    /** Current CPU generation status for this dataset. **/
    public AtomicReference<GenStatus> genStatus = new AtomicReference<>(GenStatus.NOT_STARTED);

    public BillboardDataset() {
        super();
    }

    public GenStatus getGenStatus() {
        return genStatus.get();
    }

    public void setGenStatus(GenStatus genStatus) {
        this.genStatus.set(genStatus);
    }

    public boolean initialize(PointDataProvider provider, boolean reload) {
        if (file != null && !file.isBlank()) {
            Pair<List<IParticleRecord>, String> p;
            p = reloadFile(provider, file, fileUnpack, data);
            reload = reload || !p.getSecond().equals(fileUnpack);
            data = p.getFirst();
            fileUnpack = p.getSecond();
            return reload;
        }
        return false;
    }

    private Pair<List<IParticleRecord>, String> reloadFile(PointDataProvider prov, String src, String srcUpk, List<IParticleRecord> curr) {
        String upk = GlobalResources.unpackAssetPath(Settings.settings.data.dataFile(src));
        if (srcUpk == null || !srcUpk.equals(upk)) {
            return new Pair<>(prov.loadData(upk), upk);
        } else {
            return new Pair<>(curr, srcUpk);
        }
    }

    public void setParticleCount(Long n) {
        this.particleCount = n.intValue();
    }

    public void setFile(String file) {
        this.file = file;
    }

    public void setSize(Double size) {
        this.size = size.floatValue();
    }

    public void setSizeMask(Boolean b) {
        this.sizeMask = b;
    }

    public void setSizeNoise(Double sizeNoise) {
        this.sizeNoise = MathUtilsDouble.clamp(sizeNoise.floatValue(), 0f, 1f);
        this.sizeMask = false;
    }

    public void setSizeNoiseScale(Double scale) {
        this.sizeNoise = (float) MathUtilsDouble.clamp(Math.abs(scale), 0f, 100f);
        this.sizeMask = true;
    }

    public void setIntensity(Double intensity) {
        this.intensity = intensity.floatValue();
    }

    public void setIntensity(Long intensity) {
        this.intensity = intensity.floatValue();
    }

    public void setBaseRadius(Double baseRadius) {
        this.baseRadius = baseRadius.floatValue();
    }

    public void setBaseRadius(Long baseRadius) {
        this.baseRadius = baseRadius.floatValue();
    }

    public void setMinRadius(Double minRadius) {
        this.minRadius = minRadius.floatValue();
    }

    public void setMinRadius(Long minRadius) {
        this.minRadius = minRadius.floatValue();
    }

    public void setTranslation(double[] d) {
        this.translation.set((float) d[0], (float) d[1], (float) d[2]);
    }

    public void setTranslation(int[] d) {
        this.translation.set((float) d[0], (float) d[1], (float) d[2]);
    }

    public void setScale(double[] s) {
        this.scale.set((float) s[0], (float) s[1], (float) s[2]);
    }

    public void setScale(int[] s) {
        this.scale.set((float) s[0], (float) s[1], (float) s[2]);
    }

    public void setScaleX(double s) {
        this.scale.x = (float) s;
    }

    public void setScaleY(double s) {
        this.scale.y = (float) s;
    }

    public void setScaleZ(double s) {
        this.scale.z = (float) s;
    }

    public void setRotation(double[] r) {
        this.rotation.set((float) r[0], (float) r[1], (float) r[2]);
    }

    public void setRotation(int[] r) {
        this.rotation.set((float) r[0], (float) r[1], (float) r[2]);
    }

    public void setRotationX(double r) {
        this.rotation.x = (float) r;
    }

    public void setRotationY(double r) {
        this.rotation.y = (float) r;
    }

    public void setRotationZ(double r) {
        this.rotation.z = (float) r;
    }

    public void setEccentricity(Double eccentricity) {
        this.eccentricity[0] = eccentricity.floatValue();
    }

    public void setEccentricity(Long eccentricity) {
        this.eccentricity[0] = eccentricity.floatValue();
    }

    public void setEccentricityX(Double eccentricity) {
        setEccentricity(eccentricity);
    }

    public void setEccentricityX(Long eccentricity) {
        setEccentricity(eccentricity);
    }

    public void setEccentricityY(Double eccentricity) {
        this.eccentricity[1] = eccentricity.floatValue();
    }

    public void setEccentricityY(Long eccentricity) {
        this.eccentricity[1] = eccentricity.floatValue();
    }

    public void setEccentricity(double[] eccentricity) {
        this.eccentricity[0] = (float) eccentricity[0];
        this.eccentricity[1] = (float) eccentricity[1];
    }

    public void setWarpStrength(Double warpStrength) {
        this.warpStrength = warpStrength.floatValue();
    }

    public void setWarpStrength(Long warpStrength) {
        this.warpStrength = warpStrength.floatValue();
    }

    public void setHeightScale(Double heightScale) {
        this.heightScale = heightScale.floatValue();
    }

    public void setHeightScale(Long heightScale) {
        this.heightScale = heightScale.floatValue();
    }

    public void setHeightProfile(String profile) {
        if (profile != null && !profile.isBlank()) {
            this.heightProfile = HeightProfile.valueOf(profile.toUpperCase(Locale.ROOT));
        }
    }

    public void setHeightProfile(HeightProfile p) {
        this.heightProfile = p;
    }

    public void setHeightParameters(Double warpStrength, Double heightScale, HeightProfile profile) {
        setWarpStrength(warpStrength);
        setHeightScale(heightScale);
        setHeightProfile(profile);
    }


    public void setBaseAngle(Double baseAngle) {
        this.baseAngle = baseAngle.floatValue();
    }

    public void setBaseAngle(Long baseAngle) {
        this.baseAngle = baseAngle.floatValue();
    }

    public void setNumArms(Long numArms) {
        this.numArms = numArms.intValue();
    }

    public void setSpiralArms(Long spiralArms) {
        setNumArms(spiralArms);
    }

    public void setArmSigma(Double armSigma) {
        this.armSigma = armSigma.floatValue();
    }

    public void setArmSigma(Long armSigma) {
        this.armSigma = armSigma.floatValue();
    }

    public void setAspect(Double aspect) {
        this.aspect = aspect.floatValue();
    }

    public void setAspect(Long aspect) {
        this.aspect = aspect.floatValue();
    }

    public void setType(ChannelType type) {
        this.type = type;

    }

    public void setType(String type) {
        if (type != null && !type.isBlank()) {
            this.type = ChannelType.valueOf(type.toUpperCase(Locale.ROOT));
        }
    }

    public void setDistribution(Distribution distribution) {
        this.distribution = distribution;
    }

    public void setDistribution(String distribution) {
        if (distribution != null && !distribution.isBlank()) {
            this.distribution = Distribution.valueOf(distribution.toUpperCase(Locale.ROOT));
        }
    }

    public void setSpiralDeltaPos(double[] d) {
        if (d.length == 1) {
            this.spiralDeltaPos[0] = (float) d[0];
            this.spiralDeltaPos[1] = (float) d[0];
        } else {
            this.spiralDeltaPos[0] = (float) d[0];
            this.spiralDeltaPos[1] = (float) d[1];
        }
    }

    public void setSpiralDeltaPos(int[] d) {
        if (d.length == 1) {
            this.spiralDeltaPos[0] = (float) d[0];
            this.spiralDeltaPos[1] = (float) d[0];
        } else {
            this.spiralDeltaPos[0] = (float) d[0];
            this.spiralDeltaPos[1] = (float) d[1];
        }
    }

    public void setDisplacement(Double d) {
        this.spiralDeltaPos[0] = d.floatValue();
        this.spiralDeltaPos[1] = d.floatValue();
    }

    /**
     * Sets the given color to the given index in the colors array.
     *
     * @param r   The red.
     * @param g   The green.
     * @param b   The blue.
     * @param idx The index of the color, in [0,MAX_COLORS-1]
     */
    private void setColor(double r, double g, double b, int idx) {
        assert idx >= 0 && idx < MAX_COLORS;
        this.baseColors[idx * 3] = (float) r;
        this.baseColors[idx * 3 + 1] = (float) g;
        this.baseColors[idx * 3 + 2] = (float) b;
    }

    public void setBaseColors(float[] baseColors) {
        this.baseColors = Arrays.copyOf(baseColors, baseColors.length);
    }

    /**
     * Sets the base colors. Four RGB colors are supported, so the size of the array must be one of [3, 6, 9, 12].
     *
     * @param baseColors The base colors.
     */
    public void setBaseColors(double[] baseColors) {
        assert baseColors.length == 3 || baseColors.length == 6 || baseColors.length == 9 || baseColors.length == 12;
        switch (baseColors.length) {
            case 3 -> {
                setColor(baseColors[0], baseColors[1], baseColors[2], 0);
                setColor(baseColors[0], baseColors[1], baseColors[2], 1);
                setColor(baseColors[0], baseColors[1], baseColors[2], 2);
                setColor(baseColors[0], baseColors[1], baseColors[2], 3);
            }
            case 6 -> {
                setColor(baseColors[0], baseColors[1], baseColors[2], 0);
                setColor(baseColors[0], baseColors[1], baseColors[2], 1);
                setColor(baseColors[3], baseColors[4], baseColors[5], 2);
                setColor(baseColors[3], baseColors[4], baseColors[5], 3);
            }
            case 9 -> {
                setColor(baseColors[0], baseColors[1], baseColors[2], 0);
                setColor(baseColors[3], baseColors[4], baseColors[5], 1);
                setColor(baseColors[6], baseColors[7], baseColors[8], 2);
                setColor(baseColors[0], baseColors[1], baseColors[2], 3);
            }
            default -> {
                setColor(baseColors[0], baseColors[1], baseColors[2], 0);
                setColor(baseColors[3], baseColors[4], baseColors[5], 1);
                setColor(baseColors[6], baseColors[7], baseColors[8], 2);
                setColor(baseColors[9], baseColors[10], baseColors[11], 3);
            }
        }
        for (int i = 0; i < baseColors.length; i++) {
            this.baseColors[i] = (float) baseColors[i];
        }
    }

    public void setBaseColor(double[] baseColors) {
        setBaseColors(baseColors);
    }

    public float[] getColorRGBA(int i) {
        var c = new float[4];
        c[0] = baseColors[i * 3];
        c[1] = baseColors[i * 3 + 1];
        c[2] = baseColors[i * 3 + 2];
        c[3] = 1f;
        return c;
    }

    public void setColorRGBA(float[] rgba, int i) {
        baseColors[i * 3] = rgba[0];
        baseColors[i * 3 + 1] = rgba[1];
        baseColors[i * 3 + 2] = rgba[2];
    }

    public void setColorNoise(Double colorNoise) {
        this.colorNoise = MathUtilsDouble.clamp(colorNoise.floatValue(), 0f, 1f);
    }

    /**
     * Sets the texture layers of this dataset.
     *
     * @param layers The layers.
     */
    public void setLayers(int[] layers) {
        this.layers = new int[layers.length];
        System.arraycopy(layers, 0, this.layers, 0, layers.length);
    }

    /**
     * Sets the completion rate to skip particles, in [0..1].
     *
     * @param completion The completion rate, applied to all graphics qualities.
     */
    public void setCompletion(Double completion) {
        float c = (float) MathUtilsDouble.saturate(completion);
        this.completion = new float[]{c, c, c, c};
    }

    /**
     * Sets the completion rate array per graphics quality.
     *
     * @param completion Array with the completion rate for each quality setting.
     */
    public void setCompletion(double[] completion) {
        int len = GraphicsQuality.values().length;
        if (completion.length == len) {
            var c0 = (float) MathUtilsDouble.saturate(completion[0]);
            var c1 = (float) MathUtilsDouble.saturate(completion[1]);
            var c2 = (float) MathUtilsDouble.saturate(completion[2]);
            var c3 = (float) MathUtilsDouble.saturate(completion[0]);
            this.completion = new float[]{c0, c1, c2, c3};
        } else {
            // What to do?
            logger.warn("The length of the completion array must be " + len + ", got " + completion.length);
        }
    }

    public void setDepthMask(Boolean depthMask) {
        this.depthMask = depthMask;
    }

    public void setDepthmask(Boolean depthMask) {
        setDepthMask(depthMask);
    }

    public void setBlending(BlendMode blending) {
        this.blending = blending;
    }

    public void setBlending(String blending) {
        this.blending = BlendMode.valueOf(blending.toUpperCase(Locale.ROOT));
    }

    /**
     * Set the maximum size as a solid angle [deg].
     * The same setting is used for all graphics quality settings.
     *
     * @param maxSize The maximum size in degrees.
     */
    public void setMaxSize(Double maxSize) {
        this.maxSizes = new double[GraphicsQuality.values().length];
        double val = FastMath.tan(Math.toRadians(maxSize));
        for (int i = 0; i < GraphicsQuality.values().length; i++) {
            this.maxSizes[i] = val;
        }
    }

    /**
     * Alias to {@link #setMaxSize(Double)}.
     *
     * @param maxSize The maximum size in degrees.
     *
     * @deprecated Use {@link #setMaxSize(Double)} instead.
     */
    @Deprecated
    public void setMaxsize(Double maxSize) {
        setMaxSize(maxSize);
    }

    /**
     * Set the maximum size as a list of solid angles [deg], one
     * for each of the graphics qualities [LOW, MED, HIGH, ULTRA].
     *
     * @param maxSizes The maximum size per graphics quality, in degrees.
     */
    public void setMaxSizes(double[] maxSizes) {
        int len = GraphicsQuality.values().length;
        if (maxSizes.length == len) {
            this.maxSizes = new double[GraphicsQuality.values().length];
            for (int i = 0; i < GraphicsQuality.values().length; i++) {
                this.maxSizes[i] = FastMath.tan(Math.toRadians(maxSizes[i]));
            }
        } else {
            // What to do?
            logger.warn("The length of the completion array must be " + len + ", got " + maxSizes.length);
        }
    }

    /**
     * Alias to {@link #setMaxSizes(double[])}.
     *
     * @param maxSizes The maximum size per graphics quality, in degrees.
     *
     * @deprecated Use {@link #setMaxSizes(double[])} instead.
     */
    @Deprecated
    public void setMaxsizes(double[] maxSizes) {
        setMaxSizes(maxSizes);
    }

    /**
     * Sets the spiral parameters all in one go.
     *
     * @param baseAngle The base angle.
     * @param ec        The eccentricity.
     * @param deltaPos  The delta pos.
     * @param numArms   The number of arms.
     * @param armSigma  The arm spread.
     */
    public void setSpiralData(double baseAngle, double ec, double[] deltaPos, long numArms, double armSigma) {
        setBaseAngle(baseAngle);
        setEccentricity(ec);
        setSpiralDeltaPos(deltaPos);
        setNumArms(numArms);
        setArmSigma(armSigma);
    }

    /**
     * Channel types. Particle types are essentially parameter
     * ranges for all parameters of a billboard dataset.
     */
    public enum ChannelType implements LocalizedEnum {
        DUST(new String[]{"density", "disk", "sphere", "ellipse", "gauss"},
             new int[]{0, 30_000},
             new int[]{0, 1, 2},
             new float[]{0f, 100f},
             new float[]{0f, 30f},
             new float[]{0.0f, 0.08f},
             null,
             null,
             null,
             null,
             null,
             null,
             null),
        BULGE(new String[]{"sphere", "bar", "ellipse", "gauss"},
              new int[]{0, 100},
              new int[]{0, 1, 2},
              new float[]{0f, 300f},
              new float[]{0f, 30f},
              new float[]{0.0f, 2.0f},
              new float[]{0.0f, 0.05f},
              new float[]{0.05f, 0.25f},
              null,
              null,
              null,
              null,
              null),
        STAR(new String[]{"gauss", "disk", "ellipse", "sphere"},
             new int[]{0, 100_000},
             new int[]{0, 1, 2},
             new float[]{0.0f, 8.0f},
             new float[]{0.05f, 0.2f},
             new float[]{0.0f, 6.0f},
             null,
             null,
             null,
             null,
             null,
             null,
             null),
        GAS(new String[]{"density", "disk", "sphere", "ellipse", "gauss"},
            new int[]{0, 30_000},
            new int[]{0, 1, 2, 3},
            new float[]{0f, 100f},
            new float[]{0f, 30f},
            new float[]{0.0f, 0.05f},
            null,
            null,
            null,
            null,
            null,
            null,
            null),
        HII(new String[]{"density", "disk", "sphere", "ellipse", "gauss"},
            new int[]{0, 10_000},
            new int[]{4},
            new float[]{0f, 10f},
            new float[]{0f, 30f},
            new float[]{0.0f, 6.0f},
            null,
            null,
            null,
            null,
            null,
            null,
            null),
        POINT(new String[]{"density", "disk", "sphere", "bar", "ellipse", "gauss"},
              new int[]{0, 50_000},
              new int[]{1},
              new float[]{0f, 1000f},
              new float[]{0f, 30f},
              new float[]{0.0f, 1.0f},
              null,
              null,
              null,
              null,
              null,
              null,
              null);

        /** Available distributions. **/
        public String[] distributions;
        /** Number of particles range. **/
        public int[] nParticles;
        /** Particle layers range. **/
        public int[] layers;
        /** Particle size range. **/
        public float[] size;
        /** Particle max size range. **/
        public float[] maxSize;
        /** Particle intensity range. **/
        public float[] intensity;
        /** Minimum radius range. **/
        public float[] minRadius = new float[]{0.0f, 0.4f};
        /** Base radius range. **/
        public float[] baseRadius = new float[]{0.8f, 2.0f};
        /** Warp strength range. **/
        public final float[] warpStrength = new float[]{-0.1f, 0.1f};

        /** Base angle [deg] range, for {@link Distribution#SPIRAL}, {@link Distribution#SPIRAL_LOG}, and {@link Distribution#CONE}. **/
        public float[] baseAngle = new float[]{0f, 1000f};
        /** Eccentricity range, for {@link Distribution#SPIRAL} and {@link Distribution#ELLIPSOID}. **/
        public float[] eccentricity = new float[]{0.0f, 1.0f};
        /** Delta pos (in X and Y) range, for {@link Distribution#SPIRAL}. **/
        public float[] spiralDeltaPos = new float[]{-0.5f, 0.5f};
        /** Aspect ratio range, for {@link Distribution#BAR}. **/
        public float[] aspect = new float[]{0.0f, 0.3f};
        /** Standard deviation across arm. **/
        public float[] armSigma = new float[]{0.0f, 1.0f};

        ChannelType(String[] distributions,
                    int[] nParticles,
                    int[] layers,
                    float[] size,
                    float[] maxSize,
                    float[] intensity,
                    float[] minRadius,
                    float[] baseRadius,
                    float[] baseAngle,
                    float[] eccentricity,
                    float[] displacement,
                    float[] aspect,
                    float[] armSigma) {
            if (distributions != null) {
                this.distributions = distributions;
            }
            if (nParticles != null) {
                this.nParticles = nParticles;
            }
            if (layers != null) {
                this.layers = layers;
            }
            if (size != null) {
                this.size = size;
            }
            if (maxSize != null) {
                this.maxSize = maxSize;
            }
            if (intensity != null) {
                this.intensity = intensity;
            }
            if (minRadius != null) {
                this.minRadius = minRadius;
            }
            if (baseRadius != null) {
                this.baseRadius = baseRadius;
            }
            if (baseAngle != null) {
                this.baseAngle = baseAngle;
            }
            if (eccentricity != null) {
                this.eccentricity = eccentricity;
            }
            if (displacement != null) {
                this.spiralDeltaPos = displacement;
            }
            if (aspect != null) {
                this.aspect = aspect;
            }
            if (armSigma != null) {
                this.armSigma = armSigma;
            }
        }

        @Override
        public String localizedName() {
            return I18n.msg("gui.galaxy.ds.type." + name().toLowerCase(Locale.ROOT));
        }
    }

    /** Particle distributions. **/
    public enum Distribution implements LocalizedEnum {
        /** Simple uniformly distributed sphere. **/
        SPHERE,
        /** Simple uniformly distributed disk. **/
        DISK,
        /** Density wave distribution, producing natural spirals. **/
        SPIRAL,
        /** An artificial logarithmic spiral. **/
        SPIRAL_LOG,
        /** A simple bar. **/
        BAR,
        /** An ellipse, with an eccentricity. **/
        ELLIPSOID,
        /** Gaussian distribution in a disk, with an overdense center. **/
        DISK_GAUSS,
        /** Gaussian distribution in a sphere. **/
        SPHERE_GAUSS,
        /** A cone distribution. **/
        CONE,
        /** An irregular distribution. **/
        IRREGULAR;

        public boolean isFlat() {
            return this == DISK || this == DISK_GAUSS || this == SPIRAL || this == SPIRAL_LOG;
        }

        public boolean isAnySpiral() {
            return isDensityWaveSpiral() || isLogarithmicSpiral();
        }

        public boolean isDensityWaveSpiral() {
            return this == SPIRAL;
        }

        public boolean isLogarithmicSpiral() {
            return this == SPIRAL;
        }

        public boolean isEllipsoid() {
            return this == ELLIPSOID;
        }

        public boolean isBar() {
            return this == BAR;
        }

        @Override
        public String localizedName() {
            return I18n.msg("gui.galaxy.ds.distribution." + name().toLowerCase(Locale.ROOT));
        }
    }

    /** Vertical dispersion profile, with {@link #heightScale} as base. **/
    public enum HeightProfile {
        /** Constant over radius. **/
        CONSTANT,
        /** Increasing linearly with radius. **/
        LINEAR_INC,
        /** Decreasing linearly with radius. **/
        LINEAR_DEC,
        /** Increasing smoothly with radius (smoothstep). **/
        SMOOTH_INC,
        /** Decreasing smoothly with radius (smoothstep). **/
        SMOOTH_DEC,
    }
}
