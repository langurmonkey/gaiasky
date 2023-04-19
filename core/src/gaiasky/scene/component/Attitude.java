/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import gaiasky.data.attitude.IAttitudeServer;
import gaiasky.util.gaia.IAttitude;
import gaiasky.util.math.Vector3d;

public class Attitude implements Component {

    // Attitude provider
    public String provider;
    public String attitudeLocation;
    public IAttitudeServer attitudeServer;
    public IAttitude attitude;

    public Vector3d nonRotatedPos;

    public void setAttitudeProvider(String provider) {
        this.provider = provider;
    }

    public void setProvider(String provider) {
        setAttitudeProvider(provider);
    }

}
