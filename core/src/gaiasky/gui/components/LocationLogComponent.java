/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.components;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.scene.camera.CameraManager;
import gaiasky.script.EventScriptingInterface;
import gaiasky.util.LocationLogManager;
import gaiasky.util.LocationLogManager.LocationRecord;
import gaiasky.util.TextUtils;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnScrollPane;
import gaiasky.util.scene2d.OwnTextIconButton;
import gaiasky.util.scene2d.OwnTextTooltip;

import java.util.LinkedList;

public class LocationLogComponent extends GuiComponent implements IObserver {

    private VerticalGroup locations;

    public LocationLogComponent(Skin skin, Stage stage) {
        super(skin, stage);
        EventManager.instance.subscribe(this, Event.NEW_LOCATION_RECORD);
    }

    @Override
    public void initialize(float componentWidth) {

        locations = new VerticalGroup().align(Align.topLeft).columnAlign(Align.left).space(pad8);
        /*
         * ADD TO CONTENT
         */
        ScrollPane scrollPane = new OwnScrollPane(locations, skin, "minimalist-nobg");
        scrollPane.setName("location log scroll");

        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);

        scrollPane.setHeight(360f);
        scrollPane.setWidth(componentWidth);

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
        LinkedList<LocationRecord> locations = LocationLogManager.instance().getLocations();

        for (int i = locations.size() - 1; i >= 0; i--) {
            LocationRecord lr = locations.get(i);
            Table recordTable = new Table(skin);
            // Create location
            Label num = new OwnLabel(Integer.toString(locations.size() - i) + ":", skin, "default-blue");
            num.setWidth(30f);
            Label name = new OwnLabel(TextUtils.capString(lr.name, 14), skin, "default");
            name.addListener(new OwnTextTooltip(lr.name, skin));
            name.setWidth(165f);
            Label time = new OwnLabel("(" + lr.elapsedString() + ")", skin, "msg-17");
            time.setColor(Color.CORAL);
            time.addListener(new OwnTextTooltip(I18n.msg("gui.locationlog.visited", lr.entryTime), skin));
            time.setWidth(60f);

            OwnTextIconButton goToLoc = new OwnTextIconButton("", skin, "go-to");
            goToLoc.addListener(new OwnTextTooltip(I18n.msg("gui.locationlog.goto.location", lr.entryTime), skin));
            goToLoc.setSize(30f, 30f);
            goToLoc.addListener((event) -> {
                if (event instanceof ChangeEvent) {
                    EventManager.publish(Event.CAMERA_MODE_CMD, goToLoc, CameraManager.CameraMode.FREE_MODE);
                    EventManager.publish(Event.CAMERA_POS_CMD, goToLoc, lr.position.valuesd());
                    EventManager.publish(Event.CAMERA_DIR_CMD, goToLoc, lr.direction.values());
                    EventManager.publish(Event.CAMERA_UP_CMD, goToLoc, lr.up.values());
                    EventManager.publish(Event.TIME_CHANGE_CMD, goToLoc, lr.simulationTime);

                    return true;
                }
                return false;
            });

            OwnTextIconButton goToObj = new OwnTextIconButton("", skin, "land-on");
            goToObj.addListener(new OwnTextTooltip(I18n.msg("gui.locationlog.goto.object", lr.entryTime), skin));
            goToObj.setSize(30f, 30f);
            goToObj.addListener((event) -> {
                if (event instanceof ChangeEvent) {
                    GaiaSky.postRunnable(() -> ((EventScriptingInterface) GaiaSky.instance.scripting()).setCameraFocusInstantAndGo(lr.name, false));
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

            this.locations.addActor(mainTable);
        }

        if (locations.size() == 0) {
            this.locations.addActor(new OwnLabel(I18n.msg("gui.locationlog.empty"), skin));
        }

    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        if (event == Event.NEW_LOCATION_RECORD) {
            refresh();
        }
    }

    @Override
    public void dispose() {

    }
}
