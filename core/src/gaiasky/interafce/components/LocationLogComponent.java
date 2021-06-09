package gaiasky.interafce.components;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import gaiasky.GaiaSky;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.interafce.ControlsWindow;
import gaiasky.scenegraph.camera.CameraManager;
import gaiasky.script.EventScriptingInterface;
import gaiasky.util.I18n;
import gaiasky.util.LocationLogManager;
import gaiasky.util.LocationLogManager.LocationRecord;
import gaiasky.util.TextUtils;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnScrollPane;
import gaiasky.util.scene2d.OwnTextIconButton;
import gaiasky.util.scene2d.OwnTextTooltip;

import java.util.LinkedList;

public class LocationLogComponent extends GuiComponent implements IObserver {

    private VerticalGroup locations;

    public LocationLogComponent(Skin skin, Stage stage) {
        super(skin, stage);
        EventManager.instance.subscribe(this, Events.NEW_LOCATION_RECORD);
    }

    @Override
    public void initialize() {
        final float contentWidth = ControlsWindow.getContentWidth();

        locations = new VerticalGroup().align(Align.topLeft).columnAlign(Align.left).space(pad8);
        /*
         * ADD TO CONTENT
         */
        ScrollPane scrollPane = new OwnScrollPane(locations, skin, "minimalist-nobg");
        scrollPane.setName("bookmarks scroll");

        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);

        scrollPane.setHeight(260f);
        scrollPane.setWidth(contentWidth);

        VerticalGroup locationGroup = new VerticalGroup().align(Align.left).columnAlign(Align.left).space(pad12);
        locationGroup.addActor(scrollPane);

        component = locationGroup;
        refresh();
    }

    /**
     * Refreshes the locations list with the current data in the location log manager
     */
    private void refresh() {
        locations.clear();
        LinkedList<LocationRecord> locs = LocationLogManager.instance().getLocations();

        for (int i = locs.size() - 1; i >= 0; i--) {
            LocationRecord lr = locs.get(i);
            Table recordTable = new Table(skin);
            // Create location
            Label num = new OwnLabel(Integer.toString(locs.size() - i) + ":", skin, "default-blue");
            num.setWidth(30f);
            Label name = new OwnLabel(TextUtils.capString(lr.name, 14), skin, "default");
            name.addListener(new OwnTextTooltip(lr.name, skin));
            name.setWidth(165f);
            Label time = new OwnLabel("(" + lr.elapsedString() + ")", skin, "default-pink");
            time.addListener(new OwnTextTooltip(I18n.txt("gui.locationlog.visited", lr.entryTime), skin));
            time.setWidth(40f);

            OwnTextIconButton goToLoc = new OwnTextIconButton("", skin, "go-to");
            goToLoc.addListener(new OwnTextTooltip(I18n.txt("gui.locationlog.goto.location", lr.entryTime), skin));
            goToLoc.setSize(30f, 30f);
            goToLoc.addListener((event) -> {
                if (event instanceof ChangeEvent) {
                    EventManager.instance.post(Events.CAMERA_MODE_CMD, CameraManager.CameraMode.FREE_MODE);
                    EventManager.instance.post(Events.CAMERA_POS_CMD, lr.position.valuesd());
                    EventManager.instance.post(Events.CAMERA_DIR_CMD, lr.direction.values());
                    EventManager.instance.post(Events.CAMERA_UP_CMD, lr.up.values());
                    EventManager.instance.post(Events.TIME_CHANGE_CMD, lr.simulationTime);

                    return true;
                }
                return false;
            });

            OwnTextIconButton goToObj = new OwnTextIconButton("", skin, "land-on");
            goToObj.addListener(new OwnTextTooltip(I18n.txt("gui.locationlog.goto.object", lr.entryTime), skin));
            goToObj.setSize(30f, 30f);
            goToObj.addListener((event) -> {
                if (event instanceof ChangeEvent) {
                    GaiaSky.postRunnable(() -> EventScriptingInterface.instance().setCameraFocusInstantAndGo(lr.name, false));
                    return true;
                }
                return false;
            });

            recordTable.add(num).left().padRight(pad8);
            recordTable.add(name).left().padRight(pad8);
            recordTable.add(time).left();

            Table mainTable = new Table(skin);
            mainTable.add(recordTable).left().padRight(pad12 * 1.5f);
            mainTable.add(goToLoc).left().padRight(pad8);
            mainTable.add(goToObj).left().padRight(pad8);

            locations.addActor(mainTable);
        }

    }

    @Override
    public void notify(Events event, Object... data) {
        if (event == Events.NEW_LOCATION_RECORD) {
            refresh();
        }
    }

    @Override
    public void dispose() {

    }
}
