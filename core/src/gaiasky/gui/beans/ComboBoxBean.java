/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.beans;

public class ComboBoxBean {
    public String name;
    public int value;

    public ComboBoxBean(String name) {
        super();
        this.name = name;
    }

    public ComboBoxBean(String name, int value) {
        super();
        this.name = name;
        this.value = value;
    }

    @Override
    public String toString() {
        return name;
    }

}