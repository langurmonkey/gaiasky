/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import gaiasky.scene.record.CloudComponent;

public class Cloud implements Component {

    public CloudComponent cloud;

    public void setCloud(String diffuseCloud) {
        if (this.cloud != null) {
            this.cloud.setDiffuse(diffuseCloud);
        }
    }

    public void updateCloud(CloudComponent cloud) {
        if(this.cloud != null) {
            this.cloud.updateWith(cloud);
        } else {
            this.cloud = cloud;
        }
    }
}
