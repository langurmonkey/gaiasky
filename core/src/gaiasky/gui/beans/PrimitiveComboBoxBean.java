/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.beans;

import gaiasky.util.i18n.I18n;

import java.util.Locale;

public class PrimitiveComboBoxBean {

    public String name;
    public Primitive primitive;

    public PrimitiveComboBoxBean(Primitive primitive) {
        this.primitive = primitive;
        this.name = I18n.msg("gui.shape.primitive." + primitive.name().toLowerCase(Locale.ROOT));
    }

    public static PrimitiveComboBoxBean[] defaultShapes() {
        int i = 0;
        var shapes = new PrimitiveComboBoxBean[Primitive.values().length];
        for (Primitive s : Primitive.values()) {
            shapes[i++] = new PrimitiveComboBoxBean(s);
        }
        return shapes;
    }

    public String toString() {
        return name;
    }

    public enum Primitive {
        LINES,
        TRIANGLES
    }
}
