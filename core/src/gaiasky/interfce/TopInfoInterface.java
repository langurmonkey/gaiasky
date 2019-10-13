/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interfce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.scenegraph.IFocus;
import gaiasky.scenegraph.camera.CameraManager.CameraMode;
import gaiasky.util.GlobalConf;
import gaiasky.util.I18n;
import gaiasky.util.TextUtils;
import gaiasky.util.format.DateFormatFactory;
import gaiasky.util.format.DateFormatFactory.DateType;
import gaiasky.util.format.IDateFormat;
import gaiasky.util.scene2d.OwnLabel;

import java.time.Instant;

public class TopInfoInterface extends Table implements IObserver, IGuiInterface {

    /** Date format **/
    private IDateFormat dfdate, dftime;

    private final int maxNameLen = 15;

    private OwnLabel date, time, pace, closest, focus, home, s1, s2;
    private String lastFocusName;

    public TopInfoInterface(Skin skin) {
        super(skin);
        this.setBackground("table-bg");

        float pad = 15f * GlobalConf.UI_SCALE_FACTOR;

        dfdate = DateFormatFactory.getFormatter(I18n.locale, DateType.DATE);
        dftime = DateFormatFactory.getFormatter(I18n.locale, DateType.TIME);

        date = new OwnLabel("date UT", skin, "mono");
        date.setName("label date tii");

        time = new OwnLabel("time UT", skin, "mono");
        time.setName("label time tii");

        pace = new OwnLabel("(" + (GlobalConf.runtime.TIME_ON ? TextUtils.getFormattedTimeWarp() : "time off") + ")", skin, "mono");
        pace.setName("pace tii");

        focus = new OwnLabel("", skin, "mono");
        focus.setName("focus tii");
        focus.setColor(0.2f, 1f, 0.4f, 1f);

        s1 = new OwnLabel("|", skin, "mono");

        closest = new OwnLabel("", skin, "mono");
        closest.setName("closest tii");
        closest.setColor(0.3f, 0.5f, 1f, 1f);

        s2 = new OwnLabel("|", skin, "mono");

        home = new OwnLabel("home: " + TextUtils.capString(GlobalConf.scene.STARTUP_OBJECT, maxNameLen), skin, "mono");
        home.setName("home tii");
        home.setColor(1f, 0.7f, 0.1f, 1f);

        this.add(date).left().padRight(pad);
        this.add(time).left().padRight(pad);
        this.add(pace).left().padRight(pad * 2f);
        this.add(focus).left().padRight(pad);
        this.add(s1).left().padRight(pad);
        this.add(closest).left().padRight(pad);
        this.add(s2).left().padRight(pad);
        this.add(home).left();

        pack();

        EventManager.instance.subscribe(this, Events.TIME_CHANGE_INFO, Events.TIME_CHANGE_CMD, Events.PACE_CHANGED_INFO, Events.TIME_STATE_CMD, Events.CAMERA_CLOSEST_INFO, Events.CAMERA_MODE_CMD, Events.FOCUS_CHANGE_CMD);
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
            if (!t) {
                pace.setText("(time off)");
            } else {
                pace.setText("(" + TextUtils.getFormattedTimeWarp() + ")");
            }
            break;
        case CAMERA_CLOSEST_INFO:
            IFocus closestObject = (IFocus) data[0];
            if (closestObject != null) {
                closest.setText(TextUtils.capString(closestObject.getClosestName(), maxNameLen));
                closest.setText("closest: " + closest.getText());
            } else {
                closest.setText("");
            }
            break;
        case CAMERA_MODE_CMD:
            CameraMode mode = (CameraMode) data[0];
            if (!mode.isFocus()) {
                focus.setText("");
                s1.setText("");
            } else {
                focus.setText("focus: " + lastFocusName);
                s1.setText("|");
            }
            break;
        case FOCUS_CHANGE_CMD:
            IFocus f = (IFocus) data[0];
            String candidate = f.getCandidateName();
            lastFocusName = TextUtils.capString(candidate, maxNameLen);
            focus.setText("focus: " + lastFocusName);
            s1.setText("|");
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
