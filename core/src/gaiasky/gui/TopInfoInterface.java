/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.api.IFocus;
import gaiasky.scene.camera.CameraManager.CameraMode;
import gaiasky.scene.view.FocusView;
import gaiasky.util.Settings;
import gaiasky.util.TextUtils;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.OwnLabel;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;

/**
 * The HUD UI at the top of the regular view
 */
public class TopInfoInterface extends TableGuiInterface implements IObserver {

    private final ZoneId timeZone;
    /** Date format **/
    private final DateTimeFormatter dfDate;
    private final DateTimeFormatter dfEra;
    private final DateTimeFormatter dfTime;

    private final int maxNameLen = 15;

    private final OwnLabel date;
    private final OwnLabel time;
    private final OwnLabel pace;
    private final OwnLabel closest;
    private final OwnLabel focus;
    private final OwnLabel home;
    private final OwnLabel s1;
    private final OwnLabel s2;
    private final Scene scene;
    private final FocusView view;
    private String lastFocusName;

    public TopInfoInterface(Skin skin, Scene scene) {
        super(skin);
        this.setBackground("table-bg");

        float pad = 18f;

        this.scene = scene;
        view = new FocusView();

        timeZone = Settings.settings.program.timeZone.getTimeZone();
        dfDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(I18n.locale).withZone(timeZone);
        dfEra = DateTimeFormatter.ofPattern("G").withLocale(I18n.locale).withZone(timeZone);
        dfTime = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM).withLocale(I18n.locale).withZone(timeZone);

        date = new OwnLabel(I18n.msg("gui.top.date.ut"), skin, "mono");
        date.setName("label date tii");

        time = new OwnLabel(I18n.msg("gui.top.time.ut"), skin, "mono");
        time.setName("label time tii");

        pace = new OwnLabel("(" + (Settings.settings.runtime.timeOn ? TextUtils.getFormattedTimeWarp() : I18n.msg("gui.top.time.off")) + ")", skin, "mono");
        pace.setName("pace tii");

        focus = new OwnLabel("", skin, "mono");
        focus.setName("focus tii");
        focus.setColor(0.2f, 1f, 0.4f, 1f);

        s1 = new OwnLabel("|", skin, "mono");

        closest = new OwnLabel("", skin, "mono");
        closest.setName("closest tii");
        closest.setColor(0.3f, 0.5f, 1f, 1f);

        s2 = new OwnLabel("|", skin, "mono");

        home = new OwnLabel(I18n.msg("gui.top.home", TextUtils.capString(Settings.settings.scene.homeObject, maxNameLen)), skin, "mono");
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

        EventManager.instance.subscribe(this, Event.TIME_CHANGE_INFO, Event.TIME_CHANGE_CMD, Event.TIME_WARP_CHANGED_INFO, Event.TIME_STATE_CMD, Event.CAMERA_CLOSEST_INFO, Event.CAMERA_MODE_CMD, Event.FOCUS_CHANGE_CMD);
    }

    private void unsubscribe() {
        EventManager.instance.removeAllSubscriptions(this);
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        switch (event) {
        case TIME_CHANGE_INFO:
        case TIME_CHANGE_CMD:
            // Update input time
            Instant datetime = (Instant) data[0];
            GaiaSky.postRunnable(() -> {
                date.setText(dfDate.format(datetime) + " " + dfEra.format(datetime));
                time.setText(dfTime.format(datetime) + " " + timeZone.getDisplayName(TextStyle.SHORT, I18n.locale));
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
                pace.setText("(" + I18n.msg("gui.top.time.off") + ")");
            } else {
                pace.setText("(" + TextUtils.getFormattedTimeWarp() + ")");
            }
            break;
        case CAMERA_CLOSEST_INFO:
            IFocus closestObject = (IFocus) data[0];
            if (closestObject != null) {
                closest.setText(TextUtils.capString(closestObject.getClosestName(), maxNameLen));
                closest.setText(I18n.msg("gui.top.closest", closest.getText()));
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
                focus.setText(I18n.msg("gui.top.focus", lastFocusName));
                s1.setText("|");
            }
            break;
        case FOCUS_CHANGE_CMD:
            IFocus f = null;
            Entity e;
            if (data[0] instanceof String) {
                e = scene.getEntity((String) data[0]);
            } else if (data[0] instanceof FocusView) {
                e = ((FocusView) data[0]).getEntity();
            } else {
                e = (Entity) data[0];
            }
            if (Mapper.focus.has(e)) {
                view.setEntity(e);
                f = view;
            }
            if (f != null) {
                String candidate = f.getCandidateName();
                lastFocusName = TextUtils.capString(candidate, maxNameLen);
                focus.setText(I18n.msg("gui.top.focus", lastFocusName));
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
