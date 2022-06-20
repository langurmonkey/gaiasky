/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.beans;

public class FloatComboBoxBean {
    public float value;
    public FloatComboBoxBean(float value){
        this.value = value;
    }

    @Override
    public String toString(){
        return Float.toString(value);
    }
}
