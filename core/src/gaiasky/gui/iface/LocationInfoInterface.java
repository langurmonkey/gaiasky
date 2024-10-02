/*
 * Copyright (c) 2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.iface;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.scene.component.LocationMark;
import gaiasky.util.TextUtils;
import gaiasky.util.scene2d.Link;
import gaiasky.util.scene2d.OwnLabel;

/**
 * This interface manages the pop-up that appears when the mouse hovers over a location marker.
 * This class gets events of type {@link Event#LOCATION_HOVER_INFO} every frame when the mouse is
 * on a location marker. We fake a tooltip at a given position.
 */
public class LocationInfoInterface extends TableGuiInterface implements IObserver {

    private final Skin skin;
    private static final double TIMEOUT_SECS = 1.0;
    private double lastUpdateTime = -1;
    private String currentLocation;
    private final Table content;

    public LocationInfoInterface(Skin skin) {
        super(skin);
        this.skin = skin;
        this.content = new Table(skin);
        this.content.setBackground("default-round");
        this.content.pad(15f);
        EventManager.instance.subscribe(this, Event.LOCATION_HOVER_INFO);
    }

    @Override
    public void dispose() {

    }

    @Override
    public void update() {
        var currentTime = GaiaSky.instance.getRunTimeSeconds();
        if (currentTime - lastUpdateTime > TIMEOUT_SECS) {
            // Remove.
            currentLocation = null;
            content.clear();
            clear();
        }

    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        if (event == Event.LOCATION_HOVER_INFO) {
            var x = (Integer) data[0];
            var y = (Integer) data[1];
            var loc = (LocationMark) data[2];
            if (currentLocation != null) {
                if (!loc.displayName.equals(currentLocation)) {
                    // Remove current, create.
                    content.clear();
                    clear();
                    addLocationDialog(x, y, loc);
                }
            } else {
                // Create loc.
                addLocationDialog(x, y, loc);
            }

            lastUpdateTime = GaiaSky.instance.getRunTimeSeconds();
        }
    }

    private void addLocationDialog(int x, int y, LocationMark loc) {
        var description = loc.tooltipText;
        description = TextUtils.breakCharacters(description, 50);
        content.add(new OwnLabel(loc.displayName, skin, "header-s")).left().padBottom(12f).row();
        if (description != null) {
            content.add(new OwnLabel(description, skin)).left().padBottom(12f).row();
        }
        if (loc.link != null) {
            content.add(new Link(TextUtils.capString(loc.link, 50), skin, loc.link)).left();
        }
        content.pack();
        add(content).bottom().center();
        currentLocation = loc.displayName;

        // Position fake toolkit.
        var v = new Vector2(x, y);
        getStage().screenToStageCoordinates(v);
        setPosition(v.x + (content.getWidth() / 2f) + 5f, v.y - (content.getHeight() / 2f) - 5f);
    }
}
