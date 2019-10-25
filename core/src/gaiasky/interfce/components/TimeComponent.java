/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interfce.components;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import gaiasky.GaiaSky;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.interfce.DateDialog;
import gaiasky.interfce.KeyBindings;
import gaiasky.util.GlobalConf;
import gaiasky.util.I18n;
import gaiasky.util.TextUtils;
import gaiasky.util.format.DateFormatFactory;
import gaiasky.util.format.DateFormatFactory.DateType;
import gaiasky.util.format.IDateFormat;
import gaiasky.util.scene2d.OwnImageButton;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnTextHotkeyTooltip;
import gaiasky.util.scene2d.OwnTextTooltip;

import java.time.Instant;
import java.time.ZoneOffset;

public class TimeComponent extends GuiComponent implements IObserver {

    /** Date format **/
    private IDateFormat dfdate, dftime;

    protected OwnLabel date;
    protected OwnLabel time;
    protected ImageButton plus, minus;
    protected Label timeWarp;
    protected ImageButton dateEdit;
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

        // Time
        date = new OwnLabel("date UT", skin, "mono");
        date.setName("label date");
        date.setWidth(150f * GlobalConf.UI_SCALE_FACTOR);

        time = new OwnLabel("time UT", skin, "mono");
        time.setName("label time");
        time.setWidth(150f * GlobalConf.UI_SCALE_FACTOR);

        dateEdit = new OwnImageButton(skin, "edit");
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
        Label paceLabel = new Label(I18n.txt("gui.pace") + " ", skin);
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
        Container<Label> wrapWrapper = new Container<>(timeWarp);
        wrapWrapper.width(80f * GlobalConf.UI_SCALE_FACTOR);
        wrapWrapper.align(Align.center);

        VerticalGroup timeGroup = new VerticalGroup().align(Align.left).columnAlign(Align.left).space(3 * GlobalConf.UI_SCALE_FACTOR).padTop(3 * GlobalConf.UI_SCALE_FACTOR);

        HorizontalGroup dateGroup = new HorizontalGroup();
        dateGroup.space(4f * GlobalConf.UI_SCALE_FACTOR);
        VerticalGroup datetimeGroup = new VerticalGroup();
        datetimeGroup.addActor(date);
        datetimeGroup.addActor(time);
        dateGroup.addActor(datetimeGroup);
        dateGroup.addActor(dateEdit);
        timeGroup.addActor(dateGroup);

        HorizontalGroup paceGroup = new HorizontalGroup();
        paceGroup.space(3f * GlobalConf.UI_SCALE_FACTOR);
        paceGroup.addActor(paceLabel);
        paceGroup.addActor(minus);
        paceGroup.addActor(wrapWrapper);
        paceGroup.addActor(plus);

        timeGroup.addActor(paceGroup);
        timeGroup.pack();

        component = timeGroup;
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case TIME_CHANGE_INFO:
        case TIME_CHANGE_CMD:
            // Update input time
            Instant datetime = (Instant) data[0];
            Gdx.app.postRunnable(() -> {
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
