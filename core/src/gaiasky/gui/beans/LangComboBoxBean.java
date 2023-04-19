/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.beans;

import gaiasky.util.TextUtils;

import java.util.Locale;

public class LangComboBoxBean implements Comparable<LangComboBoxBean> {
    public Locale locale;
    public String name;

    public LangComboBoxBean(Locale locale) {
        super();
        this.locale = locale;
        this.name = TextUtils.capitalise(locale.getDisplayName(locale));
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(LangComboBoxBean o) {
        return this.name.compareTo(o.name);
    }

}
