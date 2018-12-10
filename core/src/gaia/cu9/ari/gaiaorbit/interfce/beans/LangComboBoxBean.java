package gaia.cu9.ari.gaiaorbit.interfce.beans;

import java.util.Locale;

import gaia.cu9.ari.gaiaorbit.util.TextUtils;

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
