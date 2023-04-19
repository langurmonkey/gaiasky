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

import java.util.List;

public class BillboardDataset {
    private static final Log logger = Logger.getLogger(BillboardDataset.class);

    // The source file
    public String file;
    // Unpacked file path
    public String fileUnpack;
    // The data points
    public List<IParticleRecord> data;
    // Type of data
    public ParticleType type;
    // Texture layers to use
    public int[] layers;
    // Modulus (to skip data)
    public int modulus = 0;
    // Render size factor
    public float size = 1;
    // The intensity factor
    public float intensity = 1;
    // Whether to allow depth writes when rendering
    public boolean depthMask = false;
    // The blending mode
    public BlendMode blending = BlendMode.ADDITIVE;

    /**
     * Maximum particle size for each graphics quality mode. It has 4 entries, from LOW to ULTRA.
     * See {@link gaiasky.util.Settings.GraphicsQuality}.
     **/
    public double[] maxSizes;

    public BillboardDataset() {
        super();
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
        for (int i = 0; i < layers.length; i++) {
            this.layers[i] = layers[i];
        }
    }

    public void setModulus(Long modulus) {
        this.modulus = modulus.intValue();
    }

    public void setDepthmask(Boolean depthMask) {
        this.depthMask = depthMask;
    }

    public void setBlending(String blending) {
        this.blending = BlendMode.valueOf(blending.toUpperCase());
    }

    /**
     * Sets the maximum size as a solid angle [deg].
     * The same setting is used for all graphics quality settings.
     *
     * @param maxSize The maximum size in degrees.
     */
    public void setMaxsize(Double maxSize) {
        this.maxSizes = new double[GraphicsQuality.values().length];
        double val = Math.tan(Math.toRadians(maxSize));
        for (int i = 0; i < GraphicsQuality.values().length; i++) {
            this.maxSizes[i] = val;
        }
    }

    /**
     * Sets the maximum size as a list of solid angles [deg], one
     * for each of the graphics qualities LOW, NORMAL, HIGH and ULTRA.
     *
     * @param maxSizes The maximum size per graphics quality, in degrees.
     */
    public void setMaxsizes(double[] maxSizes) {
        int len = GraphicsQuality.values().length;
        if (maxSizes.length == len) {
            this.maxSizes = new double[GraphicsQuality.values().length];
            for (int i = 0; i < GraphicsQuality.values().length; i++) {
                this.maxSizes[i] = Math.tan(Math.toRadians(maxSizes[i]));
            }
        } else {
            // What to do?
            logger.warn("The length of the maximum sizes array must be 4, got " + maxSizes.length);
        }
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
