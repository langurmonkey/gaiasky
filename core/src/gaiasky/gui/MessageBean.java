/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui;

import gaiasky.util.i18n.I18n;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class MessageBean {
    private static final String TAG_SEPARATOR = " - ";
    private static final DateTimeFormatter df = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM).withLocale(I18n.locale).withZone(ZoneOffset.UTC);
    String msg;
    Instant date;

    public MessageBean(Instant date, String msg) {
        this.msg = msg;
        this.date = date;
    }

    public MessageBean(String msg) {
        this.msg = msg;
        this.date = Instant.now();
    }

    /** Has the message finished given the timeout? **/
    public boolean finished(long timeout) {
        return Instant.now().toEpochMilli() - date.toEpochMilli() > timeout;
    }

    @Override
    public String toString() {
        return formatMessage(true);
    }

    public String formatMessage(boolean writeDates) {
        return (writeDates ? df.format(this.date) + TAG_SEPARATOR : "") + this.msg;
    }

}
