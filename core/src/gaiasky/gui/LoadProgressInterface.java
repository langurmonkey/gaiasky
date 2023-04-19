/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.utils.ObjectMap;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.util.scene2d.OwnProgressBar;

public class LoadProgressInterface extends TableGuiInterface implements IObserver {
    private static int MAX_PROGRESS_BARS = 5;

    private ObjectMap<String, OwnProgressBar> progress;
    private Skin skin;
    private VerticalGroup stack;
    private float width;

    public LoadProgressInterface(float width, Skin skin) {
        super(skin);
        progress = new ObjectMap<>(10);
        this.skin = skin;
        this.stack = new VerticalGroup();
        this.width = width;
        add(this.stack).center();
        EventManager.instance.subscribe(this, Event.UPDATE_LOAD_PROGRESS);
    }

    @Override
    public void dispose() {
    }

    @Override
    public void update() {
    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        if (event == Event.UPDATE_LOAD_PROGRESS) {
            String name = (String) data[0];
            float val = (Float) data[1];

            OwnProgressBar p = null;
            if (!progress.containsKey(name) && val < 1f) {
                if (progress.size < MAX_PROGRESS_BARS) {
                    p = new OwnProgressBar(0f, 100f, 0.1f, false, skin, "small-horizontal");
                    p.setTitle(name, skin);
                    p.setPrefWidth(width);
                    progress.put(name, p);
                    this.stack.addActor(p);
                    this.pack();
                }
            } else {
                p = progress.get(name);
            }
            if (p != null) {
                p.setValue(val * 100f);
                if (val >= 1) {
                    p.setVisible(false);
                    progress.remove(name);
                    this.stack.removeActor(p, true);
                    this.pack();
                }
            }
        }
    }
}
