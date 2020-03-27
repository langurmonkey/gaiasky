/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.group;

import gaiasky.render.ComponentTypes.ComponentType;

public class DatasetOptions {

    public static DatasetOptions getStarDatasetOptions(double magnitudeScale, double[] labelColor, double[] fadeIn, double[] fadeOut){
       DatasetOptions dops = new DatasetOptions();
       dops.type = DatasetLoadType.STARS;
       dops.labelColor = labelColor;
       dops.magnitudeScale = magnitudeScale;
       dops.fadeIn = fadeIn;
       dops.fadeOut = fadeOut;
       return dops;
    }

    public static DatasetOptions getParticleDatasetOptions(double profileDecay, double[] particleColor, double colorNoise, double[] labelColor, double particleSize, double[] particleSizeLimits, ComponentType ct, double[] fadeIn, double[] fadeOut){
        DatasetOptions dops = new DatasetOptions();
        dops.type = DatasetLoadType.PARTICLES;
        dops.profileDecay = profileDecay;
        dops.particleColor = particleColor;
        dops.particleColorNoise = colorNoise;
        dops.labelColor = labelColor;
        dops.particleSize = particleSize;
        dops.particleSizeLimits = particleSizeLimits;
        dops.ct = ct;
        dops.fadeIn = fadeIn;
        dops.fadeOut = fadeOut;
        return dops;
    }

    public static DatasetOptions getStarClusterDatasetOptions(String catalogName, double[] particleColor, double[] labelColor, ComponentType ct, double[] fadeIn, double[] fadeOut){
        DatasetOptions dops = new DatasetOptions();
        dops.type = DatasetLoadType.CLUSTERS;
        dops.catalogName = catalogName;
        dops.particleColor = particleColor;
        dops.labelColor = labelColor;
        dops.ct = ct;
        dops.fadeIn = fadeIn;
        dops.fadeOut = fadeOut;
        return dops;
    }

    public enum DatasetLoadType {
        PARTICLES,
        STARS,
        CLUSTERS
    }

    public DatasetLoadType type;

    // Particles
    public double profileDecay;
    public double[] particleColor;
    public double particleColorNoise;
    public double particleSize;
    public double[] particleSizeLimits;
    public ComponentType ct;

    // Stars
    public double magnitudeScale;

    // Star clusters
    public String catalogName;

    // All
    public double[] labelColor;
    public double[] fadeIn;
    public double[] fadeOut;
}
