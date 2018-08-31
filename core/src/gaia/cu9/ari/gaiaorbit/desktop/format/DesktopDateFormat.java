package gaia.cu9.ari.gaiaorbit.desktop.format;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.util.Locale;

import gaia.cu9.ari.gaiaorbit.util.format.IDateFormat;

public class DesktopDateFormat implements IDateFormat {
    private DateTimeFormatter df;

    public DesktopDateFormat(String pattern) {
        df = DateTimeFormatter.ofPattern(pattern).withLocale(Locale.US).withZone(ZoneOffset.UTC);
    }

    public DesktopDateFormat(Locale loc, boolean date, boolean time) {
        assert date || time : "Formatter must include date or time";
        if (date && !time) {
            df = DateTimeFormatter.ofPattern("MMM dd uuuu").withLocale(Locale.US).withZone(ZoneOffset.UTC);
        } else if (!date && time)
            df = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM).withLocale(Locale.US).withZone(ZoneOffset.UTC);
        else if (date && time)
            df = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.MEDIUM).withLocale(Locale.US).withZone(ZoneOffset.UTC);
    }

    @Override
    public String format(Instant date) {
        return df.format(date);
    }

    @Override
    public Instant parse(String date) {
        try {
            LocalDateTime ldt = LocalDateTime.parse(date, df);
            return ldt.toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            throw new RuntimeException(e);
        }
    }

}
