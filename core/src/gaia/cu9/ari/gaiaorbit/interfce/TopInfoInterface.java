/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.interfce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.TextUtils;
import gaia.cu9.ari.gaiaorbit.util.format.DateFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.format.DateFormatFactory.DateType;
import gaia.cu9.ari.gaiaorbit.util.format.IDateFormat;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnLabel;

import java.time.Instant;

public class TopInfoInterface extends Table implements IObserver, IGuiInterface {

    /** Date format **/
    private IDateFormat dfdate, dftime;

    private OwnLabel date, time, pace;

    public TopInfoInterface(Skin skin) {
        super(skin);
        this.setBackground("table-bg");

        float pad = 15f * GlobalConf.SCALE_FACTOR;

        dfdate = DateFormatFactory.getFormatter(I18n.locale, DateType.DATE);
        dftime = DateFormatFactory.getFormatter(I18n.locale, DateType.TIME);

        date = new OwnLabel("date UT", skin, "mono");
        date.setName("label date tii");

        time = new OwnLabel("time UT", skin, "mono");
        time.setName("label time tii");

        pace = new OwnLabel("(" + (GlobalConf.runtime.TIME_ON ? TextUtils.getFormattedTimeWarp() : "time off") + ")", skin, "mono");
        pace.setName("pace tii");

        this.add(date).left().padRight(pad);
        this.add(time).left().padRight(pad);
        this.add(pace);

        pack();

        EventManager.instance.subscribe(this, Events.TIME_CHANGE_INFO, Events.TIME_CHANGE_CMD, Events.PACE_CHANGED_INFO, Events.TIME_STATE_CMD);
    }

    private void unsubscribe() {
        EventManager.instance.removeAllSubscriptions(this);
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
                pack();
            });

            break;
        case PACE_CHANGED_INFO:
            if (data.length == 1)
                pace.setText("(" + TextUtils.getFormattedTimeWarp((double) data[0]) + ")");
            break;
        case TIME_STATE_CMD:
            Boolean t = (Boolean) data[0];
            if(!t){
                pace.setText("(time off)");
            } else {
                pace.setText("(" + TextUtils.getFormattedTimeWarp() + ")");
            }
            break;
        default:
            break;
        }
    }

    @Override
    public void dispose() {
        unsubscribe();
    }

}
