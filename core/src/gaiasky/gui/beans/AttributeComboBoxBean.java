/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.beans;

import gaiasky.util.filter.attrib.IAttribute;

public class AttributeComboBoxBean {
    public String name;
    public IAttribute attr;

    public AttributeComboBoxBean(IAttribute attr) {
        this.attr = attr;
        this.name = attr.toString();
    }

    @Override
    public String toString() {
        return name;
    }
}
