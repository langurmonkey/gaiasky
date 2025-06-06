/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.iface;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.util.scene2d.OwnButton;
import gaiasky.util.scene2d.OwnImageButton;
import gaiasky.util.scene2d.OwnLabel;

/**
 * The notifications interface shows messages as pop-up notifications at the top-right of the screen. Clicking on
 * notifications closes them.
 */
public class PopupNotificationsInterface extends TableGuiInterface implements IObserver {
    protected final Table me;
    protected final VerticalGroup stack;
    protected Skin skin;
    protected float pad5, pad15;
    protected float defaultSeconds;

    public PopupNotificationsInterface(Skin skin) {
        super(skin);
        pad5 = 5f;
        pad15 = pad5 * 3f;
        this.defaultSeconds = 8f;

        this.skin = skin;
        this.me = this;
        this.stack = new VerticalGroup();
        this.stack.space(pad5);
        this.stack.bottom().columnRight();
        me.add(stack).top().right().pad(pad15);

        EventManager.instance.subscribe(this, Event.POST_POPUP_NOTIFICATION);
    }

    /**
     * Adds a new notification with the given message. The notification
     * stays on screen for the given amount of seconds. If that amount is negative,
     * the notification is not closed automatically.
     *
     * @param message The text message.
     * @param seconds The amount of time the notification remains on screen. Negative for unlimited.
     */
    public void addNotification(String message, float seconds) {

        Table t = new Table(skin);
        t.pad(pad5, pad15, pad5, pad15);
        OwnLabel label = new OwnLabel(message, skin, "big", 60);
        // Add to table.
        t.add(label).left().padRight(pad5);

        OwnButton notification = new OwnButton(t, skin, "dataset", true);
        notification.setWidth(350f);
        notification.pad(pad15);
        notification.right();

        notification.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // Close notification.
                removeNotification(notification);
            }
        });

        if (seconds <= 0) {
            // Add close button.
            Button close = new OwnImageButton(skin, "clear");
            close.setName("quit");
            close.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    // Close notification.
                    removeNotification(notification);
                }
                return false;
            });
            // Add to table.
            t.add(close).right().top().padLeft(pad15);
        }

        // Add to stack.
        stack.addActor(notification);
        notification.setColor(notification.getColor().r, notification.getColor().g, notification.getColor().b, 0f);
        notification.addAction(Actions.fadeIn(0.5f));
        stack.pack();

        // Timer to remove notification.
        if (seconds > 0) {
            Task closeTask = new Task() {
                @Override
                public void run() {
                    removeNotification(notification);
                }
            };
            Timer.schedule(closeTask, seconds);
        }
    }

    private void removeNotification(Actor notification) {
        if (stack != null && notification != null && notification.hasParent() && notification.getParent() == stack) {
            notification.addAction(Actions.sequence(Actions.fadeOut(0.5f), Actions.removeActor(notification)));
        }
    }

    @Override
    public void dispose() {
        EventManager.instance.removeAllSubscriptions(this);
    }

    @Override
    public void update() {

    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        if (event == Event.POST_POPUP_NOTIFICATION) {
            String message = (String) data[0];
            float seconds = defaultSeconds;
            if (data.length > 1) {
                try {
                    seconds = ((Number) data[1]).floatValue();
                } catch (ClassCastException ignored) {
                    // Item [1] should always be a number!
                }
            }
            addNotification(message, seconds);
        }
    }
}
