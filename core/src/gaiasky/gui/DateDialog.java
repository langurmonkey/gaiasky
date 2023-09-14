/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputEvent.Type;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.util.Nature;
import gaiasky.util.Settings;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnTextButton;
import gaiasky.util.scene2d.OwnTextField;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;

public class DateDialog extends GenericDialog {

    private OwnTextField day, year, hour, min, sec;
    private SelectBox<String> month;
    private final Color defaultColor;
    private final ZoneId timeZone;

    public DateDialog(Stage stage, Skin skin) {
        super(I18n.msg("gui.pickdate"), skin, stage);
        this.timeZone = Settings.settings.program.timeZone.getTimeZone();

        setAcceptText(I18n.msg("gui.ok"));
        setCancelText(I18n.msg("gui.cancel"));

        buildSuper();

        defaultColor = day.getColor().cpy();
    }

    @Override
    protected void build() {
        float inputWidth = 96f;
        float pad = 8f;

        // SET NOW
        OwnTextButton setNow = new OwnTextButton(I18n.msg("gui.pickdate.setcurrent"), skin);
        setNow.addListener(event -> {
            if (event instanceof ChangeEvent) {
                updateTime(Instant.now(), timeZone);
                return true;
            }
            return false;
        });
        setNow.setSize(388f, 28f);
        content.add(setNow).center().colspan(2).padTop(pad).padBottom(pad * 3f);
        content.row();

        // DAY GROUP
        HorizontalGroup dayGroup = new HorizontalGroup();
        dayGroup.space(pad);
        day = new OwnTextField("", skin);
        day.setMaxLength(2);
        day.setWidth(inputWidth);
        day.addListener(event -> {
            if (event instanceof InputEvent) {
                InputEvent ie = (InputEvent) event;
                if (ie.getType() == Type.keyTyped) {
                    checkField(day, 1, 31);
                    return true;
                }
            }
            return false;
        });

        month = new SelectBox<>(skin);
        month.setItems(I18n.msg("gui.date.jan"), I18n.msg("gui.date.feb"), I18n.msg("gui.date.mar"), I18n.msg("gui.date.apr"), I18n.msg("gui.date.may"), I18n.msg("gui.date.jun"), I18n.msg("gui.date.jul"), I18n.msg("gui.date.aug"), I18n.msg("gui.date.sep"), I18n.msg("gui.date.oct"), I18n.msg("gui.date.nov"), I18n.msg("gui.date.dec"));
        month.setWidth(inputWidth);

        year = new OwnTextField("", skin);
        year.setMaxLength(8);
        year.setWidth(inputWidth);
        year.addListener(event -> {
            if (event instanceof InputEvent) {
                InputEvent ie = (InputEvent) event;
                if (ie.getType() == Type.keyTyped) {
                    checkField(year, (int) (Settings.settings.runtime.minTimeMs * Nature.MS_TO_S), (int) (Settings.settings.runtime.maxTimeMs * Nature.MS_TO_S));
                    return true;
                }
            }
            return false;
        });

        dayGroup.addActor(day);
        dayGroup.addActor(new OwnLabel("/", skin));
        dayGroup.addActor(month);
        dayGroup.addActor(new OwnLabel("/", skin));
        dayGroup.addActor(year);

        content.add(new OwnLabel(I18n.msg("gui.time.date") + " (" + I18n.msg("gui.time.date.format") + "):", skin)).pad(pad, pad, 0, pad * 2).right();
        content.add(dayGroup).pad(pad, 0, 0, pad);
        content.row();

        // HOUR GROUP
        HorizontalGroup hourGroup = new HorizontalGroup();
        hourGroup.space(pad);
        hour = new OwnTextField("", skin);
        hour.setMaxLength(2);
        hour.setWidth(inputWidth);
        hour.addListener(event -> {
            if (event instanceof InputEvent) {
                InputEvent ie = (InputEvent) event;
                if (ie.getType() == Type.keyTyped) {
                    checkField(hour, 0, 23);
                    return true;
                }
            }
            return false;
        });

        min = new OwnTextField("", skin);
        min.setMaxLength(2);
        min.setWidth(inputWidth);
        min.addListener(event -> {
            if (event instanceof InputEvent) {
                InputEvent ie = (InputEvent) event;
                if (ie.getType() == Type.keyTyped) {
                    checkField(min, 0, 59);
                    return true;
                }
            }
            return false;
        });

        sec = new OwnTextField("", skin);
        sec.setMaxLength(2);
        sec.setWidth(inputWidth);
        sec.addListener(event -> {
            if (event instanceof InputEvent) {
                InputEvent ie = (InputEvent) event;
                if (ie.getType() == Type.keyTyped) {
                    checkField(sec, 0, 59);
                    return true;
                }
            }
            return false;
        });

        hourGroup.addActor(hour);
        hourGroup.addActor(new OwnLabel(":", skin));
        hourGroup.addActor(min);
        hourGroup.addActor(new OwnLabel(":", skin));
        hourGroup.addActor(sec);

        content.add(new OwnLabel(I18n.msg("gui.time.time") + " (" + I18n.msg("gui.time.time.format") + "):", skin)).pad(pad, pad, 0, pad * 2).right();
        content.add(hourGroup).pad(pad, 0, pad, pad);
    }

    @Override
    protected boolean accept() {
        boolean cool = checkField(day, 1, 31);
        cool = checkField(year, -100000, 100000) && cool;
        cool = checkField(hour, 0, 23) && cool;
        cool = checkField(min, 0, 59) && cool;
        cool = checkField(sec, 0, 59) && cool;

        if (cool) {
            // Set the date
            LocalDateTime date = LocalDateTime.of(Integer.parseInt(year.getText()), month.getSelectedIndex() + 1, Integer.parseInt(day.getText()), Integer.parseInt(hour.getText()), Integer.parseInt(min.getText()), Integer.parseInt(sec.getText()));

            // Send time change command
            ZoneOffset offset = LocalDateTime.now().atZone(timeZone).getOffset();
            EventManager.publish(Event.TIME_CHANGE_CMD, this, date.toInstant(offset));
        }
        return true;
    }

    @Override
    protected void cancel() {

    }

    /**
     * Returns true if all is good
     *
     * @param f   The text field
     * @param min The minimum value
     * @param max The maximum value
     *
     * @return The boolean indicating whether the value in this field is between
     * min and max
     */
    public boolean checkField(TextField f, int min, int max) {
        try {
            int val = Integer.parseInt(f.getText());
            if (val < min || val > max) {
                f.setColor(1, 0, 0, 1);
                return false;
            }
        } catch (Exception e) {
            f.setColor(1, 0, 0, 1);
            return false;
        }
        f.setColor(defaultColor);
        return true;
    }

    /** Updates the time **/
    public void updateTime(Instant instant, ZoneId zid) {
        LocalDateTime date = LocalDateTime.ofInstant(instant, zid);
        int year = date.get(ChronoField.YEAR);
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

    @Override
    public void dispose() {

    }
}
