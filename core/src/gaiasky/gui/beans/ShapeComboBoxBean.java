/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.beans;

import gaiasky.util.i18n.I18n;

public class ShapeComboBoxBean {

    public String name;
    public Shape shape;

    public ShapeComboBoxBean(Shape shape) {
        this.shape = shape;
        this.name = I18n.msg("gui.shape." + shape.name().toLowerCase());
    }

    public static ShapeComboBoxBean[] defaultShapes() {
        int i = 0;
        var shapes = new ShapeComboBoxBean[Shape.values().length];
        for (Shape s : Shape.values()) {
            shapes[i++] = new ShapeComboBoxBean(s);
        }
        return shapes;
    }

    public String toString() {
        return name;
    }

    public enum Shape {
        SPHERE,
        ICOSPHERE,
        OCTAHEDRONSPHERE,
        CYLINDER,
        RING,
        CONE
    }
}
