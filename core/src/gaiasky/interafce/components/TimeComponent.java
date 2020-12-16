/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce.components;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import gaiasky.GaiaSky;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.interafce.DateDialog;
import gaiasky.interafce.KeyBindings;
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
    protected Label timeWarp;
    protected OwnTextIconButton dateEdit;
    protected DateDialog dateDialog;

    public TimeComponent(Skin skin, Stage stage) {
        super(skin, stage);

        dfdate = DateFormatFactory.getFormatter(I18n.locale, DateType.DATE);
        dftime = DateFormatFactory.getFormatter(I18n.locale, DateType.TIME);
        EventManager.instance.subscribe(this, Events.TIME_CHANGE_INFO, Events.TIME_CHANGE_CMD, Events.PACE_CHANGED_INFO);
    }

    @Override
    public void initialize() {
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
        Label paceLabel = new Label(I18n.txt("gui.pace"), skin);
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

        timeWarp = new OwnLabel(TextUtils.getFormattedTimeWarp(), skin, "warp");
        timeWarp.setName("time warp");
        timeWarp.setAlignment(Align.center);
        Container<Label> wrapWrapper = new Container<>(timeWarp);
        wrapWrapper.width(192f);
        wrapWrapper.align(Align.center);

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
        paceGroup.add(paceLabel).left().padRight(pad12 * 3f);
        paceGroup.add(minus).left().padRight(pad8);
        paceGroup.add(wrapWrapper).left().padRight(pad8);
        paceGroup.add(plus).left().padRight(pad8);

        // Add to table
        timeGroup.add(dateGroup).left().padBottom(pad8).row();
        timeGroup.add(paceGroup).left();

        timeGroup.pack();

        component = timeGroup;
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
        case PACE_CHANGED_INFO:
            if (data.length == 1)
                this.timeWarp.setText(TextUtils.getFormattedTimeWarp((double) data[0]));
            break;
        default:
            break;
        }

    }

    @Override
    public void dispose() {
        EventManager.instance.removeAllSubscriptions(this);
    }
}
