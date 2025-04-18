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
     * Solid angle limit for rendering at all, in radians. If angle is smaller than this
     * quantity, no rendering happens.
     */
    public double thresholdNone;

    /**
     * Solid angle limit for rendering as a billboard, in radians. If angle is any bigger, we
     * render as a model.
     */
    public double thresholdQuad;

    /**
     * Solid angle limit for rendering as point, in radians. If angle is any bigger, we
     * render as a billboard.
     */
    public double thresholdPoint;

    /** Minimum solid angle for rendering the label of this object, in radians. */
    public double thresholdLabel;
}
