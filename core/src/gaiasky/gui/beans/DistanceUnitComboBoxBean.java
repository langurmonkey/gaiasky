package gaiasky.gui.beans;

import gaiasky.util.Nature;
import gaiasky.util.i18n.I18n;

public class DistanceUnitComboBoxBean {
    public String name;
    public DistanceUnit unit;

    public DistanceUnitComboBoxBean(DistanceUnit unit) {
        this.unit = unit;
        this.name = unit.text();
    }

    @Override
    public String toString() {
        return name;
    }

    public static DistanceUnitComboBoxBean[] defaultBeans() {
        int i = 0;
        var beans = new DistanceUnitComboBoxBean[DistanceUnit.values().length];
        for (DistanceUnit unit : DistanceUnit.values()) {
            beans[i++] = new DistanceUnitComboBoxBean(unit);
        }
        return beans;
    }

    public enum DistanceUnit {
        M(1e-3, I18n.msg("gui.unit.m")),
        KM(1, I18n.msg("gui.unit.km")),
        AU(Nature.AU_TO_KM, I18n.msg("gui.unit.au")),
        LY(Nature.LY_TO_KM, I18n.msg("gui.unit.ly")),
        PC(Nature.PC_TO_KM, I18n.msg("gui.unit.pc"));
        private double toKm;
        private String text;

        DistanceUnit(double toKm, String text) {
            this.toKm = toKm;
            this.text = text;
        }

        public String text() {
            return text;
        }

        public double toKm(double value) {
            return value * toKm;
        }
    }
}
