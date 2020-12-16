/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce.components;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import gaiasky.GaiaSky;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.interafce.ControlsWindow;
import gaiasky.interafce.DateDialog;
import gaiasky.interafce.KeyBindings;
import gaiasky.util.Constants;
import gaiasky.util.I18n;
import gaiasky.util.TextUtils;
import gaiasky.util.format.DateFormatFactory;
import gaiasky.util.format.DateFormatFactory.DateType;
import gaiasky.util.format.IDateFormat;
import gaiasky.util.scene2d.*;

import java.time.Instant;
import java.time.ZoneOffset;

public class TimeComponent extends GuiComponent implements IObserver {

    /** Date format **/
    private final IDateFormat dfdate;
    private final IDateFormat dftime;

    protected OwnLabel date;
    protected OwnLabel time;
    protected ImageButton plus, minus;
    protected OwnTextIconButton dateEdit;
    protected DateDialog dateDialog;
    protected OwnSliderPlus warp;
    private OwnTextIconButton resetTime;

    // Warp steps per side + 0, 0.125, 0.250, 0.5
    private final int warpSteps = Constants.WARP_STEPS + 4;
    protected double[] timeWarpVector;
    // Guard to know when to fire warp events
    protected boolean warpGuard = false;

    public TimeComponent(Skin skin, Stage stage) {
        super(skin, stage);

        dfdate = DateFormatFactory.getFormatter(I18n.locale, DateType.DATE);
        dftime = DateFormatFactory.getFormatter(I18n.locale, DateType.TIME);
        EventManager.instance.subscribe(this, Events.TIME_CHANGE_INFO, Events.TIME_CHANGE_CMD, Events.TIME_WARP_CHANGED_INFO, Events.TIME_WARP_CMD);
    }

    @Override
    public void initialize() {
        float contentWidth = ControlsWindow.getContentWidth();
        KeyBindings kb = KeyBindings.instance;

        float dateTimeWidth = 280f;

        // Time
        date = new OwnLabel("date UT", skin, "mono");
        date.setName("label date");
        date.setWidth(dateTimeWidth);

        time = new OwnLabel("time UT", skin, "mono");
        time.setName("label time");
        time.setWidth(dateTimeWidth);

        dateEdit = new OwnTextIconButton("", skin, "edit");
        dateEdit.addListener(event -> {
            if (event instanceof ChangeEvent) {
                // Left button click
                if (dateDialog == null) {
                    dateDialog = new DateDialog(stage, skin);
                }
                dateDialog.updateTime(GaiaSky.instance.time.getTime(), ZoneOffset.UTC);
                dateDialog.display();
            }
            return false;
        });
        dateEdit.addListener(new OwnTextTooltip(I18n.txt("gui.tooltip.dateedit"), skin));

        // Pace
        timeWarpVector = generateTimeWarpVector(warpSteps);
        warp = new OwnSliderPlus(I18n.txt("gui.warp"), -warpSteps, warpSteps, 1, skin, "big-horizontal-arrow", "menuitem-shortcut");
        warp.setValueLabelTransform((value) -> TextUtils.getFormattedTimeWarp(timeWarpVector[value.intValue() + warpSteps]));
        warp.setValue(getWarpIndex(GaiaSky.instance.time.getWarpFactor()) - warpSteps);
        warp.setWidth(300f);
        warp.addListener((event) -> {
            if (event instanceof ChangeEvent && !warpGuard) {
                int index = (int) warp.getValue();
                double newWarp = timeWarpVector[index + warpSteps];
                EventManager.instance.post(Events.TIME_WARP_CMD, newWarp, true);
            }
            return false;
        });

        plus = new OwnImageButton(skin, "plus");
        plus.setName("plus");
        plus.addListener(event -> {
            if (event instanceof ChangeEvent) {
                // Plus pressed
                EventManager.instance.post(Events.TIME_WARP_INCREASE_CMD);

                return true;
            }
            return false;
        });
        plus.addListener(new OwnTextHotkeyTooltip(I18n.txt("gui.tooltip.timewarpplus"), kb.getStringKeys("action.doubletime"), skin));

        minus = new OwnImageButton(skin, "minus");
        minus.setName("minus");
        minus.addListener(event -> {
            if (event instanceof ChangeEvent) {
                // Minus pressed
                EventManager.instance.post(Events.TIME_WARP_DECREASE_CMD);
                return true;
            }
            return false;
        });
        minus.addListener(new OwnTextHotkeyTooltip(I18n.txt("gui.tooltip.timewarpminus"), kb.getStringKeys("action.dividetime"), skin));

        /** Reset time **/
        resetTime = new OwnTextIconButton(I18n.txt("gui.resettime"), skin, "reset");
        resetTime.align(Align.center);
        resetTime.setWidth(contentWidth);
        resetTime.addListener(new OwnTextTooltip(I18n.txt("gui.resettime.tooltip"), skin));
        resetTime.addListener(event -> {
            if (event instanceof ChangeEvent) {
                // Events
                EventManager m = EventManager.instance;
                m.post(Events.TIME_CHANGE_CMD, Instant.now());
                m.post(Events.TIME_WARP_CMD, 1d, false);
                return true;
            }
            return false;
        });

        Table timeGroup = new Table(skin);

        // Date time
        Table dateGroup = new Table(skin);
        Table datetimeGroup = new Table(skin);
        datetimeGroup.add(date).padBottom(pad8).row();
        datetimeGroup.add(time);
        dateGroup.add(datetimeGroup).left().padRight(pad8);
        dateGroup.add(dateEdit).left();

        // Pace
        Table paceGroup = new Table(skin);
        paceGroup.add(minus).left().padRight(pad1);
        paceGroup.add(warp).left().padRight(pad1);
        paceGroup.add(plus).left();

        // Add to table
        timeGroup.add(dateGroup).left().padBottom(pad12).row();
        timeGroup.add(paceGroup).left().padBottom(pad12).row();
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
    public void notify(final Events event, final Object... data) {
        switch (event) {
        case TIME_CHANGE_INFO:
        case TIME_CHANGE_CMD:
            // Update input time
            Instant datetime = (Instant) data[0];
            GaiaSky.postRunnable(() -> {
                date.setText(dfdate.format(datetime));
                time.setText(dftime.format(datetime) + " UTC");
            });
            break;
        case TIME_WARP_CHANGED_INFO:
        case TIME_WARP_CMD:
            double newWarp = (double) data[0];
            boolean ui = data.length > 1 ? (Boolean) data[1] : false;
            if (!ui) {
                int index = getWarpIndex(newWarp);

                if (index >= 0) {
                    warpGuard = true;
                    warp.setValue(index - warpSteps);
                    warpGuard = false;
                }
            }

            break;
        default:
            break;
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
