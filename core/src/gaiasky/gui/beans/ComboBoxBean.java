/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.beans;

import gaiasky.util.LocalizedEnum;

/**
 * A generic combo box bean.
 *
 * @param <T> The type of the value.
 */
public class ComboBoxBean<T> {
    public String name;
    public T value;

    public ComboBoxBean(String name, T value) {
        super();
        this.name = name;
        this.value = value;
    }

    @Override
    public String toString() {
        return name;
    }

    public static <T extends Enum<T> & LocalizedEnum> ComboBoxBean<T>[] getValues(Class<T> c) {
        T[] values = c.getEnumConstants();
        ComboBoxBean<T>[] result = new ComboBoxBean[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = new ComboBoxBean<>(values[i].localizedName(), values[i]);
        }
        return result;
    }

}