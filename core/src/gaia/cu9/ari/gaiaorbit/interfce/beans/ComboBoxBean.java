/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.interfce.beans;

public class ComboBoxBean {
    public String name;
    public int value;

    public ComboBoxBean(String name, int samples) {
        super();
        this.name = name;
        this.value = samples;
    }

    @Override
    public String toString() {
        return name;
    }

}