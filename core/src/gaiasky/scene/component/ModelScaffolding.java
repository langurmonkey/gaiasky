/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import gaiasky.render.ShadowMapImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ModelScaffolding implements Component {

    /** Multiplier for Loc view angle **/
    public float locVaMultiplier = 4f;
    /** ThresholdLabel for children locations. **/
    public float locThresholdLabel = 1f;

    /** Size factor, which can be set to scale model objects up or down **/
    public float sizeScaleFactor = 1f;

    /** Fade opacity, special to model bodies **/
    public float fadeOpacity;

    /** Shadow map properties **/
    public ShadowMapImpl shadowMap;

    /** State flag; whether to render the shadow (number of times left) **/
    public int shadow;

    /** Name of the reference plane for this object. Defaults to equator **/
    public String refPlane;
    /** Name of the transformation to the reference plane **/
    public String refPlaneTransform;
    public String inverseRefPlaneTransform;

    /**
     * Array with shadow camera distance, camera near and camera far as a function of
     * the radius of the object.
     */
    public double[] shadowMapValues;

    /** The seed for random components **/
    public List<Long> seed = List.of(1L);

    /** The components to randomize---possible values are ["model", "cloud", "atmosphere"] **/
    public List<String> randomize;

    /** The factor to apply to the size when rendered as billboard. **/
    public float billboardSizeFactor;

    /**
     * Whether shadows should be rendered for this object
     *
     * @return Whether shadows should be rendered for this object
     */
    public boolean isShadow() {
        return shadowMapValues != null;
    }

    /**
     * Sets the shadow mapping values for this object
     *
     * @param shadowMapValues The values
     */
    public void setShadowValues(double[] shadowMapValues) {
        this.shadowMapValues = shadowMapValues;
    }

    public void setShadowvalues(double[] shadowMapValues) {
        this.setShadowValues(shadowMapValues);
    }

    public void setSizeScaleFactor(Double sizescalefactor) {
        this.sizeScaleFactor = sizescalefactor.floatValue();
    }

    public void setSizescalefactor(Double sizescalefactor) {
        this.setSizeScaleFactor(sizescalefactor);
    }

    public void setRandomize(String[] randomize) {
        this.randomize = Arrays.asList(randomize);
    }

    public void setSeed(Long seed) {
        this.seed = Arrays.asList(seed);
    }

    public void setSeed(int[] seed) {
        this.seed = new ArrayList<>(seed.length);
        for (int s : seed) {
            this.seed.add((long) s);
        }
    }

    public void setRandomize(String randomize) {
        this.randomize = Arrays.asList(randomize);
    }

    public void setRefPlane(String refplane) {
        this.refPlane = refplane;
        this.refPlaneTransform = refplane + "toequatorial";
        this.inverseRefPlaneTransform = "equatorialto" + refplane;
    }

    public void setReferencePlane(String refplane) {
        this.setRefPlane(refplane);
    }

    public void setRefplane(String refplane) {
        this.setRefPlane(refplane);
    }

    public void setLocVaMultiplier(Double locvamultiplier) {
        this.locVaMultiplier = locvamultiplier.floatValue();
    }

    public void setLocvamultiplier(Double locvamultiplier) {
        this.setLocVaMultiplier(locvamultiplier);
    }

    public void setLocThOverFactor(Double locthoverfactor) {
        this.setLocThresholdLabel(locthoverfactor);
    }

    public void setLocthoverfactor(Double locthoverfactor) {
        this.setLocThresholdLabel(locthoverfactor);
    }

    public void setLocThresholdLabel(Double val) {
        this.locThresholdLabel = val.floatValue();
    }

    /**
     * Gets the seed corresponding to the given component by matching it using
     * the position in the randomize vector.
     *
     * @param component The component name.
     *
     * @return The seed.
     */
    public long getSeed(String component) {
        if (randomize != null && randomize.contains(component)) {
            int idx;
            if ((idx = randomize.indexOf(component)) >= 0 && seed.size() > idx) {
                return seed.get(idx);
            }
        }
        // Get first otherwise
        return this.seed.get(0);
    }
}
