/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.interfce.components;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import gaia.cu9.ari.gaiaorbit.GaiaSky;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.interfce.DateDialog;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.format.DateFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.format.DateFormatFactory.DateType;
import gaia.cu9.ari.gaiaorbit.util.format.IDateFormat;
import gaia.cu9.ari.gaiaorbit.util.format.INumberFormat;
import gaia.cu9.ari.gaiaorbit.util.format.NumberFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnImageButton;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnLabel;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnTextTooltip;

import java.time.Instant;
import java.time.ZoneOffset;

public class TimeComponent extends GuiComponent implements IObserver {

    /** Date format **/
    private IDateFormat dfdate, dftime;
    /** Decimal format **/
    private INumberFormat nf, nfsci;

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
        nf = NumberFormatFactory.getFormatter("#########.###");
        nfsci = NumberFormatFactory.getFormatter("0.#E0");
        EventManager.instance.subscribe(this, Events.TIME_CHANGE_INFO, Events.TIME_CHANGE_CMD, Events.PACE_CHANGED_INFO);
    }

    @Override
    public void initialize() {
        // Time
        date = new OwnLabel("date UT", skin, "mono");
        date.setName("label date");
        date.setWidth(150 * GlobalConf.SCALE_FACTOR);

        time = new OwnLabel("time UT", skin, "mono");
        time.setName("label time");
        time.setWidth(150 * GlobalConf.SCALE_FACTOR);

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
        plus.addListener(new OwnTextTooltip(I18n.txt("gui.tooltip.timewarpplus"), skin));

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
        minus.addListener(new OwnTextTooltip(I18n.txt("gui.tooltip.timewarpminus"), skin));

        timeWarp = new OwnLabel(getFormattedTimeWrap(), skin, "warp");
        timeWarp.setName("time warp");
        Container<Label> wrapWrapper = new Container<Label>(timeWarp);
        wrapWrapper.width(80f * GlobalConf.SCALE_FACTOR);
        wrapWrapper.align(Align.center);

        VerticalGroup timeGroup = new VerticalGroup().align(Align.left).columnAlign(Align.left).space(3 * GlobalConf.SCALE_FACTOR).padTop(3 * GlobalConf.SCALE_FACTOR);

        HorizontalGroup dateGroup = new HorizontalGroup();
        dateGroup.space(4 * GlobalConf.SCALE_FACTOR);
        VerticalGroup datetimeGroup = new VerticalGroup();
        datetimeGroup.addActor(date);
        datetimeGroup.addActor(time);
        dateGroup.addActor(datetimeGroup);
        dateGroup.addActor(dateEdit);
        timeGroup.addActor(dateGroup);

        HorizontalGroup paceGroup = new HorizontalGroup();
        paceGroup.space(3 * GlobalConf.SCALE_FACTOR);
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
                this.timeWarp.setText(getFormattedTimeWarp((double) data[0]));
            break;
        default:
            break;
        }

    }

    private String getFormattedTimeWarp(double warp) {
        if (warp > 0.9 || warp < -0.9) {
            // Remove decimals
            warp = Math.round(warp);
        } else {
            // Round to 2 decimal places
            warp = Math.round(warp * 1000.0) / 1000.0;
        }
        if (warp > 99999 || warp < -99999) {
            return "x" + nfsci.format(warp);
        } else {
            return "x" + nf.format(warp);
        }
    }

    private String getFormattedTimeWrap() {
        return getFormattedTimeWarp(GaiaSky.instance.time.getWarpFactor());
    }

    @Override
    public void dispose() {
        EventManager.instance.removeAllSubscriptions(this);
    }
}
