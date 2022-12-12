/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.beans;

import gaiasky.scene.camera.CameraManager.CameraMode;

public class CameraComboBoxBean {

    public String name;
    public CameraMode mode;

    public CameraComboBoxBean(String name, CameraMode mode) {
        this.name = name;
        this.mode = mode;
    }

    @Override
    public String toString() {
        return name;
    }
}
