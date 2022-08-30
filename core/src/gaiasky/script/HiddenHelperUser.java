/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.scene.Scene;
import gaiasky.scene.view.FocusView;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This guy implements high level operations which run concurrently to the main
 * thread by starting new threads
 */
public class HiddenHelperUser implements IObserver {

    private static HiddenHelperUser hhu;

    public static HiddenHelperUser instance() {
        if (hhu == null)
            hhu = new HiddenHelperUser();
        return hhu;
    }

    public static void initialize() {
        instance();
    }

    private final Array<HelperTask> currentTasks;

    private long lastCommandTime;

    private Scene scene;

    private FocusView view;

    private HiddenHelperUser() {
        super();
        currentTasks = new Array<>(false, 5);
        lastCommandTime = -1;
        view = new FocusView();
        EventManager.instance.subscribe(this, Event.SCENE_LOADED, Event.NAVIGATE_TO_OBJECT, Event.LAND_ON_OBJECT, Event.LAND_AT_LOCATION_OF_OBJECT, Event.INPUT_EVENT);
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        switch (event) {
        case SCENE_LOADED:
            this.scene = (Scene) data[0];
            break;
        case NAVIGATE_TO_OBJECT:
            FocusView body;
            if (data[0] instanceof String) {
                var entity = scene.findFocus((String) data[0]);
                view.setEntity(entity);
                body = view;
            } else {
                body = ((FocusView) data[0]);
            }

            GoToObjectTask gotoTask = new GoToObjectTask(body.getEntity(), currentTasks);
            Thread gotoT = new Thread(gotoTask);
            gotoT.start();
            currentTasks.add(gotoTask);
            lastCommandTime = TimeUtils.millis();
            break;
        case LAND_ON_OBJECT:
            if (data[0] instanceof String) {
                var entity = scene.findFocus((String) data[0]);
                view.setEntity(entity);
                body = view;
            } else {
                body = ((FocusView) data[0]);
            }

            LandOnObjectTask landOnTask = new LandOnObjectTask(body.getEntity(), currentTasks);
            Thread landonT = new Thread(landOnTask);
            landonT.start();
            currentTasks.add(landOnTask);
            lastCommandTime = TimeUtils.millis();

            break;
        case LAND_AT_LOCATION_OF_OBJECT:
            if (data[0] instanceof String) {
                var entity = scene.findFocus((String) data[0]);
                view.setEntity(entity);
                body = view;
            } else {
                body = ((FocusView) data[0]);
            }

            HelperTask landAtTask;
            if (data[1] instanceof String) {
                String locname = (String) data[1];
                landAtTask = new LandAtLocationTask(body.getEntity(), locname, currentTasks);
            } else {
                Double lon = (Double) data[1];
                Double lat = (Double) data[2];
                landAtTask = new LandAtLocationTask(body.getEntity(), lon, lat, currentTasks);
            }
            Thread landAtLoc = new Thread(landAtTask);
            landAtLoc.start();
            currentTasks.add(landAtTask);
            lastCommandTime = TimeUtils.millis();
            break;
        case INPUT_EVENT:
            // More than one second after the command is given to be able to
            // stop
            if (TimeUtils.millis() - lastCommandTime > 1000) {

                // Stop all current threads
                for (HelperTask tsk : currentTasks) {
                    tsk.stop();
                }
                currentTasks.clear();
            }
            break;
        default:
            break;
        }

    }

    private abstract class HelperTask implements Runnable {
        protected AtomicBoolean stop;
        Array<HelperTask> currentTasks;

        HelperTask(Array<HelperTask> currentTasks) {
            this.stop = new AtomicBoolean(false);
            this.currentTasks = currentTasks;
        }

        public void stop() {
            this.stop.set(true);
        }
    }

    private class GoToObjectTask extends HelperTask {
        Entity body;

        GoToObjectTask(Entity entity, Array<HelperTask> currentTasks) {
            super(currentTasks);
            this.body = entity;
        }

        @Override
        public void run() {
            ((EventScriptingInterface) GaiaSky.instance.scripting()).goToObject(body, 20, 1, stop);
            currentTasks.removeValue(this, true);
        }

    }

    private class LandOnObjectTask extends HelperTask {
        Entity body;

        LandOnObjectTask(Entity body, Array<HelperTask> currentTasks) {
            super(currentTasks);
            this.body = body;
        }

        @Override
        public void run() {
            ((EventScriptingInterface) GaiaSky.instance.scripting()).landOnObject(body, stop);
            currentTasks.removeValue(this, true);
        }

    }

    private class LandAtLocationTask extends HelperTask {
        Entity body;
        String locName;
        Double lon, lat;

        LandAtLocationTask(Entity body, String locName, Array<HelperTask> currentTasks) {
            super(currentTasks);
            this.body = body;
            this.locName = locName;
        }

        LandAtLocationTask(Entity body, double lon, double lat, Array<HelperTask> currentTasks) {
            super(currentTasks);
            this.body = body;
            this.lon = lon;
            this.lat = lat;
        }

        @Override
        public void run() {
            if (locName == null)
                ((EventScriptingInterface) GaiaSky.instance.scripting()).landAtObjectLocation(body, lon, lat, stop);
            else
                ((EventScriptingInterface) GaiaSky.instance.scripting()).landAtObjectLocation(body, locName, stop);
            currentTasks.removeValue(this, true);
        }

    }

}
