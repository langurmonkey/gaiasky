/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import gaiasky.util.filter.attrib.IAttribute;

public class Highlight implements Component {
    /**
     * Is it highlighted?
     */
    public boolean highlighted = false;
    // Plain color for highlighting
    public boolean hlplain = false;
    // Highlight color
    public float[] hlc = new float[4];
    // Highlight all visible
    public boolean hlallvisible = true;
    // Highlight colormap index
    public int hlcmi;
    // Color map alpha value.
    public float hlcmAlpha = 1f;
    // Highlight colormap attribute
    public IAttribute hlcma;
    // Highlight colormap min
    public double hlcmmin;
    // Highlight colormap max
    public double hlcmmax;
    // Point size scaling
    public float pointscaling = 1;

    public boolean isHighlighted() {
        return highlighted;
    }

    public boolean isHlplain() {
        return hlplain;
    }

    public int getHlcmi() {
        return hlcmi;
    }

    public float getHlcmAlpha() {
        return hlcmAlpha;
    }

    public IAttribute getHlcma() {
        return hlcma;
    }

    public double getHlcmmin() {
        return hlcmmin;
    }

    public double getHlcmmax() {
        return hlcmmax;
    }

    public boolean isHlAllVisible() {
        return hlallvisible;
    }

    public void setPointScaling(Double pointScaling) {
        this.pointscaling = pointScaling.floatValue();
    }
}
