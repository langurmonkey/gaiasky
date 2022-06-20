package gaiasky.gui.beans;

import gaiasky.util.i18n.I18n;

public class ShapeComboBoxBean {

    public String name;
    public Shape shape;

    public ShapeComboBoxBean(Shape shape) {
        this.shape = shape;
        this.name = I18n.msg("gui.shape." + shape.name().toLowerCase());
    }

    public String toString() {
        return name;
    }

    public static ShapeComboBoxBean[] defaultShapes() {
        int i = 0;
        var shapes = new ShapeComboBoxBean[Shape.values().length];
        for (Shape s : Shape.values()) {
            shapes[i++] = new ShapeComboBoxBean(s);
        }
        return shapes;
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
