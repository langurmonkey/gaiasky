/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputEvent.Type;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import gaiasky.event.EventManager;
import gaiasky.event.Event;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;

/**
 * A dialog to pick a date.
 */
public class DateDialog extends CollapsibleWindow {
    private final OwnWindow me;
    private final Stage stage;

    private final OwnTextField day, year, hour, min, sec;
    private final SelectBox<String> month;
    private final Color defaultColor;

    public DateDialog(Stage stage, Skin skin) {
        super(I18n.msg("gui.pickdate"), skin);
        this.me = this;
        this.stage = stage;

        float inputWidth = 96f;
        float pad = 8f;

        // SET NOW
        OwnTextButton setNow = new OwnTextButton(I18n.msg("gui.pickdate.setcurrent"), skin);
        setNow.addListener(event -> {
            if (event instanceof ChangeEvent) {
                updateTime(Instant.now(), ZoneOffset.UTC);
                return true;
            }
            return false;
        });
        setNow.setSize(388f, 28f);
        add(setNow).center().colspan(2).padTop(pad).padBottom(pad * 3f);
        row();

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
        year.setMaxLength(5);
        year.setWidth(inputWidth);
        year.addListener(event -> {
            if (event instanceof InputEvent) {
                InputEvent ie = (InputEvent) event;
                if (ie.getType() == Type.keyTyped) {
                    checkField(year, -100000, 100000);
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

        add(new OwnLabel(I18n.msg("gui.time.date") + " (" + I18n.msg("gui.time.date.format") + ":", skin)).pad(pad, pad, 0, pad * 2).right();
        add(dayGroup).pad(pad, 0, 0, pad);
        row();

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

        add(new OwnLabel(I18n.msg("gui.time.time")+ " (" + I18n.msg("gui.time.time.format")+ ":", skin)).pad(pad, pad, 0, pad * 2).right();
        add(hourGroup).pad(pad, 0, pad, pad);
        row();

        // BUTTONS
        HorizontalGroup buttonGroup = new HorizontalGroup();
        buttonGroup.pad(pad);
        buttonGroup.space(pad);
        TextButton ok = new OwnTextButton(I18n.msg("gui.ok"), skin, "default");
        ok.addListener(event -> {
            if (event instanceof ChangeEvent) {

                boolean cool = checkField(day, 1, 31);
                cool = checkField(year, -100000, 100000) && cool;
                cool = checkField(hour, 0, 23) && cool;
                cool = checkField(min, 0, 59) && cool;
                cool = checkField(sec, 0, 59) && cool;

                if (cool) {
                    // Set the date
                    LocalDateTime date = LocalDateTime.of(Integer.parseInt(year.getText()), month.getSelectedIndex() + 1, Integer.parseInt(day.getText()), Integer.parseInt(hour.getText()), Integer.parseInt(min.getText()), Integer.parseInt(sec.getText()));

                    // Send time change command
                    EventManager.publish(Event.TIME_CHANGE_CMD, ok, date.toInstant(ZoneOffset.UTC));

                    me.remove();
                }

                return true;
            }

            return false;
        });
        TextButton cancel = new OwnTextButton(I18n.msg("gui.cancel"), skin, "default");
        cancel.addListener(event -> {
            if (event instanceof ChangeEvent) {
                me.remove();
                return true;
            }

            return false;
        });
        buttonGroup.addActor(ok);
        ok.setSize(112f, 32f);
        buttonGroup.addActor(cancel);
        cancel.setSize(112f, 32f);
        buttonGroup.align(Align.right).space(10);

        add(buttonGroup).colspan(2).pad(pad).bottom().right();
        getTitleTable().align(Align.left);

        pack();

        defaultColor = day.getColor().cpy();

        this.setPosition(Math.round(stage.getWidth() / 2f - this.getWidth() / 2f), Math.round(stage.getHeight() / 2f - this.getHeight() / 2f));
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
        int year = date.get(ChronoField.YEAR_OF_ERA);
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

    public void display() {
        if (!stage.getActors().contains(me, true))
            stage.addActor(me);
    }

}
