/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import gaiasky.scene.record.AtmosphereComponent;

public class Atmosphere implements Component {
    public AtmosphereComponent atmosphere;

    public void updateAtmosphere(AtmosphereComponent atmosphere) {
        if(this.atmosphere != null) {
            this.atmosphere.updateWith(atmosphere);
        } else {
            this.atmosphere = atmosphere;
        }
    }
}
