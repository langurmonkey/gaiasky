/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.window;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputEvent.Type;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.util.Logger;
import gaiasky.util.Nature;
import gaiasky.util.Settings;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnTextButton;
import gaiasky.util.scene2d.OwnTextField;
import gaiasky.util.validator.DoubleValidator;
import gaiasky.util.validator.IntValidator;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.util.concurrent.atomic.AtomicBoolean;

public class DateDialog extends GenericDialog {
    private static final Logger.Log logger = Logger.getLogger(DateDialog.class);

    private OwnTextField day, year, hour, min, sec, jd;
    private OwnLabel jdLabel, dateTimeLabel;
    private SelectBox<String> month;
    private final ZoneId timeZone;
    private final AtomicBoolean initialized;
    private Instant current;

    private final DateTimeFormatter dfDate;
    private final DateTimeFormatter dfEra;
    private final DateTimeFormatter dfTime;

    enum DateDialogTab {
        TIME, JD
    }

    private DateDialogTab tab = DateDialogTab.TIME;

    public DateDialog(Stage stage, Skin skin) {
        super(I18n.msg("gui.pickdate"), skin, stage);
        this.timeZone = Settings.settings.program.timeZone.getTimeZone();
        this.initialized = new AtomicBoolean(false);
        this.current = Instant.now();

        dfDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(I18n.locale)
                .withZone(timeZone);
        dfEra = DateTimeFormatter.ofPattern("G")
                .withLocale(I18n.locale)
                .withZone(timeZone);
        dfTime = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)
                .withLocale(I18n.locale)
                .withZone(timeZone);

        setAcceptText(I18n.msg("gui.ok"));
        setCancelText(I18n.msg("gui.cancel"));

        buildSuper();
    }

    @Override
    protected void build() {
        // Tabs
        HorizontalGroup tabGroup = new HorizontalGroup();
        tabGroup.align(Align.center);
        float tabWidth = 300f;
        final OwnTextButton tabUTC = new OwnTextButton(I18n.msg("gui.time.time.zone", timeZone.getDisplayName(TextStyle.SHORT, I18n.locale)), skin, "toggle-big");
        tabUTC.pad(pad10);
        tabUTC.setWidth(tabWidth);
        final OwnTextButton tabJD = new OwnTextButton(I18n.msg("gui.time.julian"), skin, "toggle-big");
        tabJD.pad(pad10);
        tabJD.setWidth(tabWidth);
        tabGroup.addActor(tabUTC);
        tabGroup.addActor(tabJD);
        content.add(tabGroup)
                .center()
                .padBottom(pad18)
                .expandX()
                .row();

        // Content
        final Table contentUTC = new Table(skin);
        contentUTC.align(Align.top);
        contentUTC.pad(pad18);

        final Table contentJD = new Table(skin);
        contentJD.align(Align.top);
        contentJD.pad(pad18);

        /* ADD ALL CONTENT */
        addTabContent(contentUTC);
        addTabContent(contentJD);
        content.add(tabStack)
                .expand()
                .fill()
                .row();
        // Listen to changes in the tab button checked states.
        // Set visibility of the tab content to match the checked state.
        ChangeListener tabListener = new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (tabUTC.isChecked()) {
                    tab = DateDialogTab.TIME;
                    if (initialized.get())
                        selectedTab = 0;
                    reloadUTC(contentUTC);
                }
                if (tabJD.isChecked()) {
                    tab = DateDialogTab.JD;
                    if (initialized.get())
                        selectedTab = 1;
                    reloadJD(contentJD);
                }
                contentUTC.setVisible(tabUTC.isChecked());
                contentJD.setVisible(tabJD.isChecked());

                // Set time from current.
                updateTime(current, timeZone);

                content.pack();
                setKeyboardFocus();
            }
        };
        tabUTC.addListener(tabListener);
        tabJD.addListener(tabListener);
        // Let only one tab button be checked at a time.
        ButtonGroup<Button> tabs = new ButtonGroup<>();
        tabs.setMinCheckCount(1);
        tabs.setMaxCheckCount(1);
        tabs.add(tabUTC);
        tabs.add(tabJD);

        initialized.set(true);

    }

    /**
     * Builds the tab to input a UTC time.
     *
     * @param content The content table.
     */
    private void reloadUTC(Table content) {
        float inputWidth = 96f;
        float pad = 8f;
        content.clear();

        // DAY GROUP
        HorizontalGroup dayGroup = new HorizontalGroup();
        dayGroup.space(pad);
        var dayValidator = new IntValidator(1, 31);
        day = new OwnTextField("", skin, dayValidator);
        day.setMaxLength(2);
        day.setWidth(inputWidth);
        day.addListener(event -> {
            if (event instanceof InputEvent ie) {
                if (ie.getType() == Type.keyTyped) {
                    return checkFullDate();
                }
            }
            return false;
        });

        month = new SelectBox<>(skin);
        month.setItems(I18n.msg("gui.date.jan"), I18n.msg("gui.date.feb"), I18n.msg("gui.date.mar"), I18n.msg("gui.date.apr"),
                       I18n.msg("gui.date.may"), I18n.msg("gui.date.jun"), I18n.msg("gui.date.jul"), I18n.msg("gui.date.aug"),
                       I18n.msg("gui.date.sep"), I18n.msg("gui.date.oct"), I18n.msg("gui.date.nov"), I18n.msg("gui.date.dec"));
        month.setWidth(inputWidth);

        var yearValidator = new IntValidator((int) (Settings.settings.runtime.minTimeMs * Nature.MS_TO_Y),
                                             (int) (Settings.settings.runtime.maxTimeMs * Nature.MS_TO_Y));
        year = new OwnTextField("", skin, yearValidator);
        year.setMaxLength(8);
        year.setWidth(inputWidth);
        year.addListener(event -> {
            if (event instanceof InputEvent ie) {
                if (ie.getType() == Type.keyTyped) {
                    return checkFullDate();
                }
            }
            return false;
        });

        dayGroup.addActor(day);
        dayGroup.addActor(new OwnLabel("/", skin));
        dayGroup.addActor(month);
        dayGroup.addActor(new OwnLabel("/", skin));
        dayGroup.addActor(year);

        content.add(new OwnLabel(I18n.msg("gui.time.date") + " (" + I18n.msg("gui.time.date.format") + "):", skin))
                .pad(pad, pad, 0, pad * 2)
                .right();
        content.add(dayGroup)
                .pad(pad, 0, 0, pad);
        content.row();

        // HOUR GROUP
        HorizontalGroup hourGroup = new HorizontalGroup();
        hourGroup.space(pad);
        var hoursValidator = new IntValidator(0, 23);
        hour = new OwnTextField("", skin, hoursValidator);
        hour.setMaxLength(2);
        hour.setWidth(inputWidth);
        hour.addListener(event -> {
            if (event instanceof InputEvent ie) {
                if (ie.getType() == Type.keyTyped) {
                    return checkFullDate();
                }
            }
            return false;
        });

        var minutesValidator = new IntValidator(0, 59);
        min = new OwnTextField("", skin, minutesValidator);
        min.setMaxLength(2);
        min.setWidth(inputWidth);
        min.addListener(event -> {
            if (event instanceof InputEvent ie) {
                if (ie.getType() == Type.keyTyped) {
                    return checkFullDate();
                }
            }
            return false;
        });

        var secondsValidator = new IntValidator(0, 59);
        sec = new OwnTextField("", skin, secondsValidator);
        sec.setMaxLength(2);
        sec.setWidth(inputWidth);
        sec.addListener(event -> {
            if (event instanceof InputEvent ie) {
                if (ie.getType() == Type.keyTyped) {
                    return checkFullDate();
                }
            }
            return false;
        });

        hourGroup.addActor(hour);
        hourGroup.addActor(new OwnLabel(":", skin));
        hourGroup.addActor(min);
        hourGroup.addActor(new OwnLabel(":", skin));
        hourGroup.addActor(sec);

        content.add(new OwnLabel(I18n.msg("gui.time.time") + " (" + I18n.msg("gui.time.time.format") + "):", skin))
                .pad(pad, pad, 0, pad * 2)
                .right();
        content.add(hourGroup)
                .pad(pad, 0, pad, pad)
                .row();

        // Julian date info label
        jdLabel = new OwnLabel("", skin);

        content.add(new OwnLabel(I18n.msg("gui.time.julian") + ":", skin))
                .pad(pad, pad, pad18, pad * 2)
                .right();
        content.add(jdLabel)
                .center()
                .pad(pad, 0, pad18, 0)
                .row();

        // Set current
        OwnTextButton setNow = new OwnTextButton(I18n.msg("gui.pickdate.setcurrent", timeZone.getDisplayName(TextStyle.SHORT, I18n.locale)), skin);
        setNow.addListener(event -> {
            if (event instanceof ChangeEvent) {
                updateTime(Instant.now(), timeZone);
                return true;
            }
            return false;
        });
        setNow.setSize(388f, 36f);
        setNow.pad(8f);
        content.add(setNow)
                .center()
                .colspan(2)
                .padTop(pad * 2f);

    }

    /**
     * Builds the tab to input a Julian date.
     *
     * @param content The content table.
     */
    private void reloadJD(Table content) {
        float inputWidth = 350f;
        float pad = 8f;
        content.clear();

        // Julian date
        var jdMin = AstroUtils.getJulianDateUTC((int) (Settings.settings.runtime.minTimeMs * Nature.MS_TO_Y), 1, 1, 0, 0, 0, 0);
        var jdMax = AstroUtils.getJulianDateUTC((int) (Settings.settings.runtime.maxTimeMs * Nature.MS_TO_Y), 1, 1, 0, 0, 0, 0);
        var jdValidator = new DoubleValidator(jdMin, jdMax);
        jd = new OwnTextField("", skin, jdValidator);
        jd.setWidth(inputWidth);
        jd.addListener(event -> {
            if (event instanceof InputEvent ie) {
                if (ie.getType() == Type.keyTyped) {
                    checkJulian();
                    return true;
                }
            }
            return false;
        });

        // Date
        dateTimeLabel = new OwnLabel("", skin);

        content.add(new OwnLabel(I18n.msg("gui.time.julian") + ":", skin))
                .pad(pad, pad, pad, pad * 2)
                .right();
        content.add(jd)
                .center()
                .pad(pad, 0, pad, 0)
                .row();

        // Time info label
        content.add(new OwnLabel(I18n.msg("gui.time.time.zone", timeZone.getDisplayName(TextStyle.SHORT, I18n.locale)) + ":", skin))
                .pad(pad, pad, pad18, pad * 2)
                .right();
        content.add(dateTimeLabel)
                .center()
                .pad(pad, 0, pad18, 0)
                .row();

        // Set current
        OwnTextButton setNow = new OwnTextButton(I18n.msg("gui.pickdate.julian.current"), skin);
        setNow.addListener(event -> {
            if (event instanceof ChangeEvent) {
                updateTime(Instant.now(), timeZone);
                return true;
            }
            return false;
        });
        setNow.setSize(388f, 32f);
        setNow.pad(8f);
        content.add(setNow)
                .center()
                .colspan(2)
                .padTop(pad * 2f);
        content.row();

    }

    @Override
    protected boolean accept() {
        switch (tab) {
            case TIME -> {
                boolean cool = year.isValid() && day.isValid() && hour.isValid() && min.isValid() && sec.isValid() && checkFullDate();

                if (cool) {
                    EventManager.publish(Event.TIME_CHANGE_CMD, this, current);
                    return true;
                } else {
                    return false;
                }
            }
            case JD -> {
                boolean cool = jd.isValid() && checkJulian();
                if (cool) {
                    EventManager.publish(Event.TIME_CHANGE_CMD, this, current);
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    @Override
    protected void cancel() {

    }

    /**
     * Checks that the current date in the input fields is plausible. If so, it updates the current instant.
     *
     * @return True if the date is ok.
     */
    public boolean checkFullDate() {
        try {
            var ldt = LocalDateTime.of(Integer.parseInt(year.getText()), month.getSelectedIndex() + 1, Integer.parseInt(day.getText()),
                                       Integer.parseInt(hour.getText()), Integer.parseInt(min.getText()), Integer.parseInt(sec.getText()));
            var instant = ldt.atZone(timeZone).toInstant();
            updateCurrent(instant);

            return true;
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage());
            // Probably error in leap years (Feb 29?).
            if (month.getSelectedIndex() == 1 && Integer.parseInt(day.getText()) == 29) {
                day.setColor(ColorUtils.gRedC);
            }
            return false;
        }
    }

    /**
     * Checks that the current Julian date is plausible. If so, updates the current instant.
     *
     * @return True if the Julian date is ok.
     */
    public boolean checkJulian() {
        try {
            var julianDate = Double.parseDouble(jd.getText());
            var instant = AstroUtils.julianDateToInstant(julianDate);
            updateCurrent(instant);
            return true;
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage());
            // Probably error in leap years (Feb 29?).
            if (month.getSelectedIndex() == 1 && Integer.parseInt(day.getText()) == 29) {
                day.setColor(ColorUtils.gRedC);
            }
            return false;
        }

    }

    /**
     * Updates the current instant with the given one, and updates the JD and date/time labels.
     *
     * @param instant The instant.
     */
    public void updateCurrent(Instant instant) {
        assert instant != null;

        this.current = instant;

        if (jdLabel != null) {
            var jd = AstroUtils.getJulianDate(instant);
            jdLabel.setText(Double.toString(jd));

        }
        if (dateTimeLabel != null) {
            var dateText = dfDate.format(current) + " " + dfEra.format(current) + " " + dfTime.format(current) + " " + timeZone.getDisplayName(
                    TextStyle.SHORT, I18n.locale);
            dateTimeLabel.setText(dateText);
        }
    }

    /** Updates the time **/
    public void updateTime(Instant instant, ZoneId zid) {

        switch (tab) {
            case TIME -> {
                var date = LocalDateTime.ofInstant(instant, zid);
                int year = date.getYear();
                int month = date.getMonthValue();
                int day = date.getDayOfMonth();

                int hour = date.getHour();
                int min = date.getMinute();
                int sec = date.getSecond();

                this.day.setText(String.valueOf(day));
                this.month.setSelectedIndex(month - 1);
                this.year.setText(String.valueOf(year));
                this.hour.setText(String.valueOf(hour));
                this.min.setText(String.valueOf(min));
                this.sec.setText(String.valueOf(sec));
            }
            case JD -> {
                var julian = AstroUtils.getJulianDate(instant);
                this.jd.setText(String.valueOf(julian));
            }
        }

        updateCurrent(instant);
    }

    @Override
    public void dispose() {

    }
}
