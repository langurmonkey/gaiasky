/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;

public class Title implements Component {

    public float scale = 1f;
    public int align;
    public boolean lines = false;
    public float lineHeight = 0f;

    public void setScale(Double scale) {
        this.scale = scale.floatValue();
    }

    public void setLines(Boolean lines) {
        this.lines = lines;
    }

    public void setLines(String linesText) {
        this.lines = Boolean.parseBoolean(linesText);
    }

    public void setAlign(Long align) {
        this.align = align.intValue();
    }
}
