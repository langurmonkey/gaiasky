/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import gaiasky.GaiaSky;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.scenegraph.IFocus;
import gaiasky.scenegraph.SceneGraphNode;
import gaiasky.scenegraph.camera.CameraManager.CameraMode;
import gaiasky.util.GlobalConf;
import gaiasky.util.I18n;
import gaiasky.util.TextUtils;
import gaiasky.util.format.DateFormatFactory;
import gaiasky.util.format.DateFormatFactory.DateType;
import gaiasky.util.format.IDateFormat;
import gaiasky.util.scene2d.OwnLabel;

import java.time.Instant;

/**
 * The HUD UI at the top of the regular view
 */
public class TopInfoInterface extends TableGuiInterface implements IObserver {

    /** Date format **/
    private final IDateFormat dfdate;
    private final IDateFormat dftime;

    private final int maxNameLen = 15;

    private final OwnLabel date;
    private final OwnLabel time;
    private final OwnLabel pace;
    private final OwnLabel closest;
    private final OwnLabel focus;
    private final OwnLabel home;
    private final OwnLabel s1;
    private final OwnLabel s2;
    private String lastFocusName;

    public TopInfoInterface(Skin skin) {
        super(skin);
        this.setBackground("table-bg");

        float pad = 18f;

        dfdate = DateFormatFactory.getFormatter(I18n.locale, DateType.DATE);
        dftime = DateFormatFactory.getFormatter(I18n.locale, DateType.TIME);

        date = new OwnLabel(I18n.txt("gui.top.date.ut"), skin, "mono");
        date.setName("label date tii");

        time = new OwnLabel(I18n.txt("gui.top.time.ut"), skin, "mono");
        time.setName("label time tii");

        pace = new OwnLabel("(" + (GlobalConf.runtime.TIME_ON ? TextUtils.getFormattedTimeWarp() : I18n.txt("gui.top.time.off")) + ")", skin, "mono");
        pace.setName("pace tii");

        focus = new OwnLabel("", skin, "mono");
        focus.setName("focus tii");
        focus.setColor(0.2f, 1f, 0.4f, 1f);

        s1 = new OwnLabel("|", skin, "mono");

        closest = new OwnLabel("", skin, "mono");
        closest.setName("closest tii");
        closest.setColor(0.3f, 0.5f, 1f, 1f);

        s2 = new OwnLabel("|", skin, "mono");

        home = new OwnLabel(I18n.txt("gui.top.home", TextUtils.capString(GlobalConf.scene.STARTUP_OBJECT, maxNameLen)), skin, "mono");
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

        EventManager.instance.subscribe(this, Events.TIME_CHANGE_INFO, Events.TIME_CHANGE_CMD, Events.TIME_WARP_CHANGED_INFO, Events.TIME_STATE_CMD, Events.CAMERA_CLOSEST_INFO, Events.CAMERA_MODE_CMD, Events.FOCUS_CHANGE_CMD);
    }

    private void unsubscribe() {
        EventManager.instance.removeAllSubscriptions(this);
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
                pack();
            });

            break;
        case TIME_WARP_CHANGED_INFO:
            if (data.length == 1)
                pace.setText("(" + TextUtils.getFormattedTimeWarp((double) data[0]) + ")");
            break;
        case TIME_STATE_CMD:
            Boolean t = (Boolean) data[0];
            if (!t) {
                pace.setText("(" + I18n.txt("gui.top.time.off") + ")");
            } else {
                pace.setText("(" + TextUtils.getFormattedTimeWarp() + ")");
            }
            break;
        case CAMERA_CLOSEST_INFO:
            IFocus closestObject = (IFocus) data[0];
            if (closestObject != null) {
                closest.setText(TextUtils.capString(closestObject.getClosestName(), maxNameLen));
                closest.setText(I18n.txt("gui.top.closest", closest.getText()));
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
                focus.setText(I18n.txt("gui.top.focus", lastFocusName));
                s1.setText("|");
            }
            break;
        case FOCUS_CHANGE_CMD:
            IFocus f = null;
            if (data[0] instanceof String) {
                SceneGraphNode sgn = GaiaSky.instance.sg.getNode((String) data[0]);
                if (sgn instanceof IFocus)
                    f = (IFocus) sgn;
            } else {
                f = (IFocus) data[0];
            }
            if (f != null) {
                String candidate = f.getCandidateName();
                lastFocusName = TextUtils.capString(candidate, maxNameLen);
                focus.setText(I18n.txt("gui.top.focus", lastFocusName));
                s1.setText("|");
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

    @Override
    public void update() {

    }

}
