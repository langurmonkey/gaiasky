/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.iface;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.utils.ObjectMap;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.util.Constants;
import gaiasky.util.scene2d.OwnProgressBar;

/**
 * Interface that displays progress bars at the bottom with load progress information.
 * Since {@link Event#UPDATE_LOAD_PROGRESS} events modify the UI, we need to make sure
 * that those are processed in the main thread. OpenGL is pesky, and its context
 * can only operate in the thread it was created.
 */
public class LoadProgressInterface extends TableGuiInterface implements IObserver {
    private static final int MAX_PROGRESS_BARS = 5;

    private final ObjectMap<String, OwnProgressBar> progress;
    private final Skin skin;
    private final VerticalGroup stack;
    private final float width;

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
            var threadName = Thread.currentThread().getName();
            if (!threadName.equals(Constants.MAIN_THREAD_NAME)) {
                // Post runnable to main thread.
                GaiaSky.postRunnable(() -> updateProgress(name, val));
            } else {
                // Run directly.
                updateProgress(name, val);
            }

        }
    }

    private synchronized void updateProgress(String name, float val) {
        OwnProgressBar p = null;
        // Create or get progress bar.
        if (!progress.containsKey(name) && val >= 0f && val < 1f) {
            if (progress.size < MAX_PROGRESS_BARS) {
                p = new OwnProgressBar(0f, 100f, 0.1f, false, skin, "small-horizontal");
                p.setTitle(name, skin);
                p.setPrefWidth(width);
                progress.put(name, p);
                this.stack.addActor(p);
            }
        } else {
            p = progress.get(name);
        }

        if (p != null) {
            p.setValue(val * 100f);
            // Deactivate if value is over 1.
            if (val >= 1) {
                p.setVisible(false);
                progress.remove(name);
                this.stack.removeActor(p, true);
            }
        }
    }
}
