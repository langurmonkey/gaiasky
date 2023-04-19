/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Matrix4;
import gaiasky.util.gdx.model.IntModel;

public class Cluster implements Component, ICopy {

    // The texture, for when the cluster is far away
    public Texture clusterTex;

    // Distance of this cluster to Sol, in internal units
    public double dist;

    // Radius of this cluster in degrees
    public double radiusDeg;

    // Number of stars of this cluster
    public int numStars;

    // Years since epoch
    public double ySinceEpoch;

    /**
     * Fade alpha between quad and model. Attribute contains model opacity. Quad
     * opacity is <code>1-fadeAlpha</code>
     **/
    public float fadeAlpha;

    public IntModel model;
    public Matrix4 modelTransform;

    public void setNumStars(Integer numStars) {
        this.numStars = numStars;
    }

    @Override
    public Component getCopy(Engine engine) {
        var copy = engine.createComponent(this.getClass());
        copy.dist = dist;
        copy.radiusDeg = radiusDeg;
        copy.numStars = numStars;
        copy.modelTransform = new Matrix4(modelTransform);
        return copy;
    }
}
