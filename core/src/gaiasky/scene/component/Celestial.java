/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;

public class Celestial implements Component, ICopy {

    /** Name to pull info from wikipedia **/
    public String wikiName;

    /** Red, green and blue colors and their revamped cousins **/
    public float[] colorPale;

    /** B-V color index **/
    public float colorBv;

    /** Inner radius for billboard rendering **/
    public double innerRad;

    public void setWikiname(String wikiName) {
        setWikiName(wikiName);
    }

    public void setWikiName(String wikiName) {
        this.wikiName = wikiName;
    }

    public void setColorBV(Double colorBv) {
        this.colorBv = colorBv.floatValue();
    }

    public void setColorBv(Double colorBv) {
        setColorBV(colorBv);
    }

    public void setColorIndex(Double colorBv) {
        setColorBV(colorBv);
    }

    public void setColorbv(Double colorBv) {
        setColorBV(colorBv);
    }

    @Override
    public Component getCopy(Engine engine) {
        return engine.createComponent(this.getClass());
    }
}
