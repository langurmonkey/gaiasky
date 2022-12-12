package gaiasky.gui.beans;

import gaiasky.util.i18n.I18n;

public class PrimitiveComboBoxBean {

    public String name;
    public Primitive primitive;

    public PrimitiveComboBoxBean(Primitive primitive) {
        this.primitive = primitive;
        this.name = I18n.msg("gui.shape.primitive." + primitive.name().toLowerCase());
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
