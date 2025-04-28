/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

import gaiasky.data.group.PointDataProvider;
import gaiasky.render.BlendMode;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.util.GlobalResources;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Pair;
import gaiasky.util.Settings;
import gaiasky.util.Settings.GraphicsQuality;
import gaiasky.util.math.Vector3dTransformer;
import net.jafama.FastMath;

import java.util.List;

public class BillboardDataset {
    private static final Log logger = Logger.getLogger(BillboardDataset.class);

    // The source file.
    public String file;
    // Unpacked file path.
    public String fileUnpack;
    // The data points.
    public List<IParticleRecord> data;
    // Type of data.
    public ParticleType type;
    // Texture layers to use.
    public int[] layers;
    // Array with completion rate per texture quality (to skip data).
    public float[] completion;
    // Render size factor
    public float size = 1;
    // The intensity factor
    public float intensity = 1;
    // Whether to allow depth writes when rendering
    public boolean depthMask = false;
    // The blending mode
    public BlendMode blending = BlendMode.ADDITIVE;

    /**
     * Maximum particle size for each texture quality mode. It has 4 entries, from LOW to ULTRA.
     * See {@link GraphicsQuality}.
     **/
    public double[] maxSizes;

    public BillboardDataset() {
        super();
    }

    public boolean initialize(PointDataProvider provider, Vector3dTransformer tr, boolean reload) {
        if (file != null && !file.isBlank()) {
            Pair<List<IParticleRecord>, String> p;
            p = reloadFile(provider, file, fileUnpack, tr, data);
            reload = reload || !p.getSecond()
                    .equals(fileUnpack);
            data = p.getFirst();
            fileUnpack = p.getSecond();
            return reload;
        }
        return false;
    }

    private Pair<List<IParticleRecord>, String> reloadFile(PointDataProvider prov, String src, String srcUpk,
                                                           Vector3dTransformer tr, List<IParticleRecord> curr) {
        String upk = GlobalResources.unpackAssetPath(Settings.settings.data.dataFile(src));
        if (srcUpk == null || !srcUpk.equals(upk)) {
            prov.setVector3dTransformer(tr);
            return new Pair<>(prov.loadData(upk), upk);
        } else {
            return new Pair<>(curr, srcUpk);
        }
    }

    public void setFile(String file) {
        this.file = file;
    }

    public void setSize(Double size) {
        this.size = size.floatValue();
    }

    public void setIntensity(Double intensity) {
        this.intensity = intensity.floatValue();
    }

    public void setType(String type) {
        if (type != null && !type.isBlank()) {
            this.type = ParticleType.valueOf(type.toUpperCase());
        }
    }

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
        float c = completion.floatValue();
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
            this.completion = new float[]{(float) completion[0], (float) completion[1], (float) completion[2], (float) completion[3]};
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

    public void setBlending(String blending) {
        this.blending = BlendMode.valueOf(blending.toUpperCase());
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

    public enum ParticleType {
        DUST,
        BULGE,
        STAR,
        GAS,
        HII,
        GALAXY,
        POINT,
        OTHER;
    }
}
