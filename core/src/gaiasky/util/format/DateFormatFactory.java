/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.format;

import java.util.Locale;

public abstract class DateFormatFactory {
    private static DateFormatFactory instance;

    public enum DateType {
        DATE, TIME, DATETIME
    }

    public static void initialize(DateFormatFactory inst) {
        instance = inst;
    }

    public static IDateFormat getFormatter(String pattern) {
        return instance.getDateFormatter(pattern);
    }

    public static IDateFormat getFormatter(Locale loc, DateType type) {
        switch (type) {
        case DATE:
            return instance.getDateFormatter(loc);
        case TIME:
            return instance.getTimeFormatter(loc);
        case DATETIME:
            return instance.getDateTimeFormatter(loc);
        }
        return null;
    }

    protected abstract IDateFormat getDateFormatter(String pattern);

    protected abstract IDateFormat getDateFormatter(Locale loc);

    protected abstract IDateFormat getTimeFormatter(Locale loc);

    protected abstract IDateFormat getDateTimeFormatter(Locale loc);
}
