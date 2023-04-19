/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;

public class Magnitude implements Component, ICopy {
    /** Absolute magnitude, m = -2.5 log10(flux), with the flux at 10 pc **/
    public float absMag = Float.NaN;
    /** Apparent magnitude, m = -2.5 log10(flux) **/
    public float appMag = Float.NaN;

    public void setAbsMag(Double absMag) {
        this.absMag = absMag.floatValue();
    }

    public void setAbsmag(Double absMag) {
        setAbsMag(absMag);
    }

    public void setAbsoluteMagnitude(Double absMag) {
        setAbsMag(absMag);
    }

    public void setAppMag(Double appMag) {
        this.appMag = appMag.floatValue();
    }

    public void setAppmag(Double appMag) {
        setAppMag(appMag);
    }

    public void setApparentMagnitude(Double appMag) {
        setAppMag(appMag);
    }
    @Override
    public Component getCopy(Engine engine) {
        var copy = engine.createComponent(this.getClass());
        copy.absMag = absMag;
        copy.appMag = appMag;
        return copy;
    }
}
