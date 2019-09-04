/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.interfce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import gaia.cu9.ari.gaiaorbit.GaiaSky;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.scenegraph.IFocus;
import gaia.cu9.ari.gaiaorbit.scenegraph.IStarFocus;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.TextUtils;
import gaia.cu9.ari.gaiaorbit.util.format.DateFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.format.DateFormatFactory.DateType;
import gaia.cu9.ari.gaiaorbit.util.format.IDateFormat;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnLabel;

import java.time.Instant;

public class TopInfoInterface extends Table implements IObserver, IGuiInterface {

    /** Date format **/
    private IDateFormat dfdate, dftime;
    private Vector3d aux;

    private OwnLabel date, time, pace, closest;

    public TopInfoInterface(Skin skin) {
        super(skin);
        this.setBackground("table-bg");

        this.aux = new Vector3d();

        float pad = 15f * GlobalConf.SCALE_FACTOR;

        dfdate = DateFormatFactory.getFormatter(I18n.locale, DateType.DATE);
        dftime = DateFormatFactory.getFormatter(I18n.locale, DateType.TIME);

        date = new OwnLabel("date UT", skin, "mono");
        date.setName("label date tii");

        time = new OwnLabel("time UT", skin, "mono");
        time.setName("label time tii");

        pace = new OwnLabel("(" + (GlobalConf.runtime.TIME_ON ? TextUtils.getFormattedTimeWarp() : "time off") + ")", skin, "mono");
        pace.setName("pace tii");

        closest = new OwnLabel("closest: " + GlobalConf.scene.STARTUP_OBJECT, skin, "mono");
        closest.setName("closest tii");

        this.add(date).left().padRight(pad);
        this.add(time).left().padRight(pad);
        this.add(pace).left().padRight(pad * 2f);
        this.add(closest);

        pack();

        EventManager.instance.subscribe(this, Events.TIME_CHANGE_INFO, Events.TIME_CHANGE_CMD, Events.PACE_CHANGED_INFO, Events.TIME_STATE_CMD, Events.CAMERA_CLOSEST_INFO);
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
            final int maxLen = 15;
            if (GaiaSky.instance.getICamera().getPos().len() > 1E8 * Constants.PC_TO_U) {
                closest.setText("");
            } else {
                IFocus closestBody = (IFocus) data[0];
                IStarFocus closestStar = (IStarFocus) data[1];
                if (closestBody != null || closestStar != null) {
                    if (closestBody == null) {
                        // Use star
                        closest.setText(TextUtils.capString(closestStar.getClosestName(), maxLen));
                    } else if (closestStar == null) {
                        // Use body
                        closest.setText(TextUtils.capString(closestBody.getName(), maxLen));
                    } else {
                        // Use closest of two
                        String n;
                        if (closestBody.getDistToCamera() < closestStar.getClosestDist()) {
                            // Body
                            n = closestBody.getName();
                        } else {
                            // Star
                            n = closestStar.getClosestName();
                        }
                        closest.setText(TextUtils.capString(n, maxLen));
                    }
                    closest.setText("closest: " + closest.getText());
                }
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
