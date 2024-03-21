/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import gaiasky.scene.record.RotationComponent;

public class RigidRotation implements Cloneable {

    /** Holds information about the rotation of the body **/
    public RotationComponent rc;

    /**
     * Sets the rotation period in hours
     */
    public void setRotation(RotationComponent rc) {
        this.rc = rc;
    }

    public void updateRotation(RotationComponent rc) {
        if (this.rc != null) {
            this.rc.updateWith(rc);
        } else {
            this.rc = rc;
        }
    }

    @Override
    public RigidRotation clone() {
        try {
            var copy = (RigidRotation) super.clone();
            copy.rc = rc;
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

}
