/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.group;

import gaiasky.render.ComponentTypes.ComponentType;

public class DatasetOptions {

    public DatasetLoadType type;
    // Particles
    public double profileDecay;
    public double[] particleColor;
    public double particleColorNoise;
    public double particleSize;
    /** Minimum and maximum solid angle (in radians) to represent each particle. **/
    public double[] particleSizeLimits;
    public String modelType = "quad";
    public String modelPrimitive = "GL_TRIANGLES";
    public String[] textures;
    public ComponentType ct;
    // Stars
    public double magnitudeScale;
    // Star clusters
    public String catalogName;
    // All
    public double[] labelColor;
    public double[] fadeIn;
    public double[] fadeOut;
    public int numLabels;
    public boolean renderSetLabel = false;
    public boolean initializeCatalogInfo = true;

    public static DatasetOptions getStarDatasetOptions(String datasetName,
                                                       double magnitudeScale,
                                                       double[] labelColor,
                                                       double[] fadeIn,
                                                       double[] fadeOut) {
        DatasetOptions datasetOptions = new DatasetOptions();
        datasetOptions.type = DatasetLoadType.STARS;
        datasetOptions.catalogName = datasetName;
        datasetOptions.labelColor = labelColor;
        datasetOptions.magnitudeScale = magnitudeScale;
        datasetOptions.fadeIn = fadeIn;
        datasetOptions.fadeOut = fadeOut;
        return datasetOptions;
    }

    public static DatasetOptions getParticleDatasetOptions(String datasetName,
                                                           double profileDecay,
                                                           double[] particleColor,
                                                           double colorNoise,
                                                           double[] labelColor,
                                                           double particleSize,
                                                           double[] particleSizeLimits,
                                                           ComponentType ct,
                                                           double[] fadeIn,
                                                           double[] fadeOut) {
        DatasetOptions datasetOptions = new DatasetOptions();
        datasetOptions.type = DatasetLoadType.PARTICLES;
        datasetOptions.catalogName = datasetName;
        datasetOptions.profileDecay = profileDecay;
        datasetOptions.particleColor = particleColor;
        datasetOptions.particleColorNoise = colorNoise;
        datasetOptions.labelColor = labelColor;
        datasetOptions.particleSize = particleSize;
        datasetOptions.particleSizeLimits = particleSizeLimits;
        datasetOptions.ct = ct;
        datasetOptions.fadeIn = fadeIn;
        datasetOptions.fadeOut = fadeOut;
        return datasetOptions;
    }

    public static DatasetOptions getStarClusterDatasetOptions(String datasetName,
                                                              double[] particleColor,
                                                              double[] labelColor,
                                                              ComponentType ct,
                                                              double[] fadeIn,
                                                              double[] fadeOut) {
        DatasetOptions datasetOptions = new DatasetOptions();
        datasetOptions.type = DatasetLoadType.PARTICLES_EXT;
        datasetOptions.catalogName = datasetName;
        datasetOptions.particleColor = particleColor;
        datasetOptions.labelColor = labelColor;
        datasetOptions.ct = ct;
        datasetOptions.particleSizeLimits = new double[] { 0.0, 1.57 };
        datasetOptions.fadeIn = fadeIn;
        datasetOptions.fadeOut = fadeOut;
        datasetOptions.modelType = "icosphere";
        datasetOptions.modelPrimitive = "GL_LINES";
        return datasetOptions;
    }

    public static DatasetOptions getVariableStarDatasetOptions(String datasetName,
                                                               double magnitudeScale,
                                                               double[] labelColor,
                                                               ComponentType ct,
                                                               double[] fadeIn,
                                                               double[] fadeOut) {
        DatasetOptions datasetOptions = new DatasetOptions();
        datasetOptions.type = DatasetLoadType.VARIABLES;
        datasetOptions.catalogName = datasetName;
        datasetOptions.labelColor = labelColor;
        datasetOptions.magnitudeScale = magnitudeScale;
        datasetOptions.ct = ct;
        datasetOptions.fadeIn = fadeIn;
        datasetOptions.fadeOut = fadeOut;
        return datasetOptions;
    }

    public enum DatasetLoadType {
        PARTICLES,
        PARTICLES_EXT,
        STARS,
        CLUSTERS,
        VARIABLES;

        public boolean isSelectable() {
            return this != PARTICLES;
        }
    }
}
