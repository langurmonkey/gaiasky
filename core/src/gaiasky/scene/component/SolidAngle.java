/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;

public class SolidAngle implements Component {
    /**
     * radius/distance limit for rendering at all. If angle is smaller than this
     * quantity, no rendering happens.
     */
    public double thresholdNone;

    /**
     * radius/distance limit for rendering as shader. If angle is any bigger, we
     * render as a model.
     */
    public double thresholdQuad;

    /**
     * radius/distance limit for rendering as point. If angle is any bigger, we
     * render with shader.
     */
    public double thresholdPoint;

    /** Minimum solid angle for rendering the lable of this object. */
    public double thresholdLabel;
}
