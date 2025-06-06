/*
 * Copyright (c) 2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.iface;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.util.Settings;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.OwnLabel;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;

/**
 * An interface that displays the current simulation time. Listens to {@link Event#TIME_CHANGE_INFO} events
 * to update the time.
 */
public class TimeGuiInterface extends TableGuiInterface implements IObserver {

    private final ZoneId timeZone;
    /** Date format **/
    private final DateTimeFormatter dfDate;
    private final DateTimeFormatter dfEra;
    private final DateTimeFormatter dfTime;
    private final OwnLabel date;
    private final OwnLabel time;

    public TimeGuiInterface(final Skin skin) {
        super(skin);

        timeZone = Settings.settings.program.timeZone.getTimeZone();
        dfDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(I18n.locale).withZone(timeZone);
        dfEra = DateTimeFormatter.ofPattern("G").withLocale(I18n.locale).withZone(timeZone);
        dfTime = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM).withLocale(I18n.locale).withZone(timeZone);
        this.setBackground("bg-pane");
        date = new OwnLabel("", skin, "main-title");
        time = new OwnLabel("", skin, "main-title");
        add(time).right().pad(15f).padRight(30f);
        add(date).right().pad(15f);

        pack();

        EventManager.instance.subscribe(this, Event.TIME_CHANGE_INFO);
    }


    @Override
    public void notify(Event event, Object source, Object... data) {
        if (event == Event.TIME_CHANGE_INFO) {
            var datetime = (Instant) data[0];
            GaiaSky.postRunnable(() -> {
                date.setText(dfDate.format(datetime) + " " + dfEra.format(datetime));
                time.setText(dfTime.format(datetime) + " " + timeZone.getDisplayName(TextStyle.SHORT, I18n.locale));
                pack();
            });
        }

    }

    @Override
    public void dispose() {

    }

    @Override
    public void update() {

    }
}
