/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

import com.badlogic.gdx.utils.Disposable;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.StdRandom;
import net.jafama.FastMath;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public sealed abstract class NamedComponent implements IComponent, Disposable permits AtmosphereComponent, CloudComponent, MaterialComponent, ModelComponent, NoiseComponent, VirtualTextureComponent {
    protected String name;

    @Override
    public void initialize(String name) {
        if (name != null)
            this.name = name.toLowerCase(Locale.ROOT).replaceAll("\\s+", "_");
    }

    protected Map<String, Object> createUVSphereParameters(long quality, double diameter, boolean flip) {
        Map<String, Object> params = new HashMap<>();
        params.put("quality", quality);
        params.put("diameter", diameter);
        params.put("flip", flip);
        return params;
    }

    protected Map<String, Object> createIcoSphereParameters(long recursion, double diameter, boolean flip) {
        Map<String, Object> params = new HashMap<>();
        params.put("recursion", recursion);
        params.put("diameter", diameter);
        params.put("flip", flip);
        return params;
    }

    protected Map<String, Object> createOctahedronSphereParameters(long divisions, double diameter, boolean flip) {
        Map<String, Object> params = new HashMap<>();
        params.put("divisions", divisions);
        params.put("diameter", diameter);
        params.put("flip", flip);
        return params;
    }

    protected Map<String, Object> createCubeSphereParameters(long divisions, double diameter, boolean flip) {
        Map<String, Object> params = new HashMap<>();
        params.put("divisions", divisions);
        params.put("diameter", diameter);
        params.put("flip", flip);
        return params;
    }

    protected double gaussian(Random rand, double mean, double sigma) {
        return rand.nextGaussian() * sigma + mean;
    }

    protected double gaussian(Random rand, double mean, double sigma, double min) {
        return FastMath.max(min, rand.nextGaussian() * sigma + mean);
    }

    protected double gaussian(Random rand, double mean, double sigma, double min, double max) {
        return MathUtilsDouble.clamp(rand.nextGaussian() * sigma + mean, min, max);
    }

    protected double uniform(Random rand, double min, double max) {
        if (!(min < max))
            throw new IllegalArgumentException("Invalid range");
        return min + rand.nextDouble() * (max - min);
    }
}
