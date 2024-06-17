package gaiasky.gui.beans;

import gaiasky.util.i18n.I18n;

public class OrientationComboBoxBean {

    public String name;
    public ShapeOrientation orientation;

    public OrientationComboBoxBean(ShapeOrientation orientation) {
        this.orientation = orientation;
        this.name = I18n.msg("gui.shape.orientation." + orientation.name().toLowerCase());
    }

    public static OrientationComboBoxBean[] defaultOrientations() {
        int i = 0;
        var orientations = new OrientationComboBoxBean[ShapeOrientation.values().length];
        for (ShapeOrientation s : ShapeOrientation.values()) {
            orientations[i++] = new OrientationComboBoxBean(s);
        }
        return orientations;
    }

    public String toString() {
        return name;
    }

    public enum ShapeOrientation {
        CAMERA,
        EQUATORIAL,
        ECLIPTIC,
        GALACTIC
    }
}
