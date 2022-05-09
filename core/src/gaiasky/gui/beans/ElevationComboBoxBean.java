/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.beans;

import gaiasky.util.Settings.ElevationType;

public class ElevationComboBoxBean {

    public String name;
    public ElevationType type;

    public ElevationComboBoxBean(String name, ElevationType type){
        this.name = name;
        this.type = type;
    }

    @Override
    public String toString(){
        return name;
    }
}
