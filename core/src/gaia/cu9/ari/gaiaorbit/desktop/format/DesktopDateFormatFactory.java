package gaia.cu9.ari.gaiaorbit.desktop.format;

import gaia.cu9.ari.gaiaorbit.util.format.DateFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.format.IDateFormat;

import java.util.Locale;

public class DesktopDateFormatFactory extends DateFormatFactory {

    @Override
    protected IDateFormat getDateFormatter(String pattern) {
        return new DesktopDateFormat(pattern);
    }

    @Override
    protected IDateFormat getDateFormatter(Locale loc) {
        return new DesktopDateFormat(loc, true, false);
    }

    @Override
    protected IDateFormat getTimeFormatter(Locale loc) {
        return new DesktopDateFormat(loc, false, true);
    }

    @Override
    protected IDateFormat getDateTimeFormatter(Locale loc) {
        return new DesktopDateFormat(loc, true, true);
    }

}
