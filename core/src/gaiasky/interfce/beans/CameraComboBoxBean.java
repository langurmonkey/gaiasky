/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.interfce.beans;

import gaia.cu9.ari.gaiaorbit.scenegraph.camera.CameraManager.CameraMode;

public class CameraComboBoxBean {

    public String name;
    public CameraMode mode;

    public CameraComboBoxBean(String name, CameraMode mode){
        this.name = name;
        this.mode = mode;
    }

    @Override
    public String toString() {
        return name;
    }
}
