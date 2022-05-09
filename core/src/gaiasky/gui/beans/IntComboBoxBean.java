/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.beans;

public class IntComboBoxBean {
    public int value;
    public IntComboBoxBean(int value){
        this.value = value;
    }

    @Override
    public String toString(){
        return Integer.toString(value);
    }
}
