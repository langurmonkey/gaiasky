/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.components;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.gui.KeyBindings;
import gaiasky.util.Settings;
import gaiasky.util.TextUtils;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.*;
import gaiasky.util.time.GlobalClock;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;

public class TimeComponent extends GuiComponent implements IObserver {

    private final ZoneId timeZone;
    /**
     * Date format
     **/
    private final DateTimeFormatter dfDate;
    private final DateTimeFormatter dfEra;
    private final DateTimeFormatter dfTime;
    private final int warpSteps;
    protected OwnLabel date;
    protected OwnLabel time;
    protected OwnLabel warp, warpForward, warpBackward;
    protected OwnImageButton playPause, stepForward, stepBackward;
    protected OwnTextIconButton dateEdit;
    protected OwnSliderPlus warpSlider;
    protected double[] timeWarpVector;
    // Guard to know when to fire warp events
    protected boolean warpGuard = false;

    public TimeComponent(Skin skin, Stage stage) {
        super(skin, stage);
        timeZone = Settings.settings.program.timeZone.getTimeZone();
        dfDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(I18n.locale).withZone(timeZone);
        dfEra = DateTimeFormatter.ofPattern("G").withLocale(I18n.locale).withZone(timeZone);
        dfTime = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM).withLocale(I18n.locale).withZone(timeZone);
        warpSteps = ((GlobalClock) GaiaSky.instance.time).warpSteps;
        EventManager.instance.subscribe(this, Event.TIME_CHANGE_INFO, Event.TIME_CHANGE_CMD,
                Event.TIME_WARP_CHANGED_INFO, Event.TIME_WARP_CMD, Event.TIME_STATE_CMD);
    }

    @Override
    public void initialize(float componentWidth) {
        KeyBindings kb = KeyBindings.instance;
        boolean redTheme = skin.getAtlas().getTextures().first().toString().contains("night-red");

        // Time
        date = new OwnLabel("date UT", skin, "msg-33");
        date.setWidth(componentWidth - 65);
        date.setAlignment(Align.center);
        if (redTheme) {
            date.setColor(Color.RED);
        }
        date.setName("label date");

        time = new OwnLabel("time UT", skin, "msg-33");
        time.setWidth(componentWidth - 65);
        time.setAlignment(Align.center);
        if (redTheme) {
            time.setColor(Color.RED);
        }
        time.setName("label time");

        dateEdit = new OwnTextIconButton("", skin, "edit");
        dateEdit.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.SHOW_DATE_TIME_EDIT_ACTION, this);
            }
            return false;
        });
        dateEdit.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.dateedit"), skin));

        // BWD label.
        warpBackward = new OwnLabel(I18n.msg("gui.time.bwd"), skin, "msg-15");
        warpBackward.setAlignment(Align.left);
        warpBackward.setColor(redTheme ? Color.RED : Color.GRAY);

        // FWD label.
        warpForward = new OwnLabel(I18n.msg("gui.time.fwd"), skin, "msg-15");
        warpForward.setAlignment(Align.right);
        warpForward.setColor(redTheme ? Color.RED : Color.GRAY);

        // Warp factor.
        warp = new OwnLabel("", skin, "big");
        warp.setText(TextUtils.secondsToTimeUnit(GaiaSky.instance.time.getWarpFactor()) + "/" + I18n.msg("gui.unit.second"));
        warp.setAlignment(Align.center);
        warp.setWidth(componentWidth - 160f);
        warp.addListener(new OwnTextTooltip(I18n.msg("gui.warp.tooltip"), skin));

        // Warp slider.
        timeWarpVector = ((GlobalClock) GaiaSky.instance.time).generateTimeWarpVector();
        warpSlider = new OwnSliderPlus("", -warpSteps, warpSteps, 1, skin, "time-warp");
        warpSlider.setValueLabelTransform((value) -> TextUtils.getFormattedTimeWarp(timeWarpVector[value.intValue() + warpSteps]));
        warpSlider.setValue(getWarpIndex(GaiaSky.instance.time.getWarpFactor()) - warpSteps);
        warpSlider.setWidth(componentWidth);
        warpSlider.addListener(new OwnTextTooltip(I18n.msg("gui.warp"), skin));
        warpSlider.addListener((event) -> {
            if (event instanceof ChangeEvent && !warpGuard) {
                int index = (int) warpSlider.getValue();
                double newWarp = timeWarpVector[index + warpSteps];
                EventManager.publish(Event.TIME_WARP_CMD, warpSlider, newWarp);
            }
            return false;
        });

        stepForward = new OwnImageButton(skin, "media-skip-forward");
        stepForward.setName("plus");
        stepForward.addListener(event -> {
            if (event instanceof ChangeEvent) {
                // Plus pressed
                EventManager.publish(Event.TIME_WARP_INCREASE_CMD, stepForward);

                return true;
            }
            return false;
        });
        stepForward.addListener(new OwnTextHotkeyTooltip(I18n.msg("gui.tooltip.timewarpplus"), kb.getStringKeys("action.doubletime", true), skin));

        playPause = new OwnImageButton(skin, "media-play-pause");
        playPause.setChecked(Settings.settings.runtime.timeOn);
        playPause.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.TIME_STATE_CMD, playPause, playPause.isChecked());
                return true;
            }
            return false;
        });
        String[] timeHotkey = KeyBindings.instance.getStringKeys("action.pauseresume", true);
        playPause.addListener(new OwnTextHotkeyTooltip(I18n.msg("gui.tooltip.playstop"), timeHotkey, skin));

        stepBackward = new OwnImageButton(skin, "media-skip-backward");
        stepBackward.setName("minus");
        stepBackward.addListener(event -> {
            if (event instanceof ChangeEvent) {
                // Minus pressed
                EventManager.publish(Event.TIME_WARP_DECREASE_CMD, stepBackward);
                return true;
            }
            return false;
        });
        stepBackward.addListener(new OwnTextHotkeyTooltip(I18n.msg("gui.tooltip.timewarpminus"), kb.getStringKeys("action.dividetime", true), skin));

        /* Reset time */
        OwnTextIconButton resetTime = new OwnTextIconButton(I18n.msg("gui.resettime"), skin, "reset");
        resetTime.align(Align.center);
        resetTime.setWidth(componentWidth);
        resetTime.addListener(new OwnTextTooltip(I18n.msg("gui.resettime.tooltip"), skin));
        resetTime.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager m = EventManager.instance;
                m.post(Event.TIME_CHANGE_CMD, resetTime, Instant.now());
                m.post(Event.TIME_WARP_CMD, resetTime, 1d);
                return true;
            }
            return false;
        });

        Table timeGroup = new Table(skin);

        // Date time.
        Table dateGroup = new Table(skin);
        dateGroup.setWidth(componentWidth);
        Table datetimeGroup = new Table(skin);
        datetimeGroup.add(date).left().pad(pad4).row();
        datetimeGroup.add(time).left().pad(pad4);
        dateGroup.add(datetimeGroup).left().padRight(pad12).padLeft(pad9);
        dateGroup.add(dateEdit).right();

        // Time warp and controls.
        Table textGroup = new Table(skin);
        textGroup.add(warpBackward).left().padRight(pad4);
        textGroup.add(warp).center().growX().padRight(pad4);
        textGroup.add(warpForward).right();

        Table controlsGroup = new Table(skin);
        controlsGroup.add(stepBackward).center().pad(pad3).padLeft(pad12 * 2.2f).padRight(pad6);
        controlsGroup.add(playPause).center().pad(pad3).padRight(pad6);
        controlsGroup.add(stepForward).center().pad(pad3).padRight(pad6);
        controlsGroup.add(resetTime).center().pad(pad3);

        Table warpGroup = new Table(skin);
        warpGroup.add(controlsGroup).center().row();
        warpGroup.add(warpSlider).center().pad(0, pad9, 0, pad9).row();
        warpGroup.add(textGroup).center().pad(0, pad9, 0, pad9).row();

        // Add to table.
        timeGroup.add(dateGroup).left().padBottom(pad9).row();
        timeGroup.add(new Separator(skin, "gray")).center().growX().padBottom(pad20).padLeft(pad20).padRight(pad20).row();
        timeGroup.add(warpGroup).left().padBottom(pad20 * 1.5f).row();
        timeGroup.add(resetTime).center();

        timeGroup.pack();

        component = timeGroup;
    }

    /**
     * Generate the time warp vector.
     *
     * @param steps The number of steps per side (positive and negative)
     * @return The vector
     */
    private double[] generateTimeWarpVector(int steps) {
        double[] warp = new double[steps * 2 + 1];
        warp[steps] = 0;
        // Positive
        double w = 0;
        for (int i = steps + 1; i < warp.length; i++) {
            warp[i] = increaseWarp(w);
            w = warp[i];
        }
        // Negative
        w = 0;
        for (int i = steps - 1; i >= 0; i--) {
            warp[i] = decreaseWarp(w);
            w = warp[i];
        }
        return warp;
    }

    private double increaseWarp(double timeWarp) {
        if (timeWarp == 0) {
            return 0.125;
        } else if (timeWarp == -0.125) {
            return 0;
        } else if (timeWarp < 0) {
            return timeWarp / 2.0;
        } else {
            return timeWarp * 2.0;
        }
    }

    private double decreaseWarp(double timeWarp) {
        if (timeWarp == 0.125) {
            return 0;
        } else if (timeWarp == 0) {
            return -0.125;
        } else if (timeWarp < 0) {
            return timeWarp * 2.0;
        } else {
            return timeWarp / 2.0;
        }
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        switch (event) {
            case TIME_CHANGE_INFO, TIME_CHANGE_CMD -> {
                // Update input time
                Instant datetime = (Instant) data[0];
                GaiaSky.postRunnable(() -> {
                    date.setText(dfDate.format(datetime) + " " + dfEra.format(datetime));
                    time.setText(dfTime.format(datetime) + " " + timeZone.getDisplayName(TextStyle.SHORT, I18n.locale));
                });
            }
            case TIME_WARP_CHANGED_INFO, TIME_WARP_CMD -> {
                if (source != warpSlider) {
                    double newWarp = (double) data[0];
                    int index = getWarpIndex(newWarp);

                    if (index >= 0) {
                        warpGuard = true;
                        warpSlider.setValue(index - warpSteps);
                        warpGuard = false;
                    }
                    warp.setText(TextUtils.secondsToTimeUnit(newWarp) + "/" + I18n.msg("gui.unit.second"));
                }
            }
            case TIME_STATE_CMD -> {
                if (source != playPause) {
                    playPause.setCheckedNoFire((Boolean) data[0]);
                }
            }
            default -> {
            }
        }
    }

    private int getWarpIndex(double warpValue) {
        int index = -1;
        double prev = Double.MIN_VALUE;
        for (int i = 0; i < timeWarpVector.length; i++) {
            if (warpValue == timeWarpVector[i]) {
                index = i;
                break;
            } else if (warpValue > prev && warpValue < timeWarpVector[i]) {
                index = i;
                break;
            }
            prev = timeWarpVector[i];
        }
        return index;

    }

    @Override
    public void dispose() {
        EventManager.instance.removeAllSubscriptions(this);
    }
}
