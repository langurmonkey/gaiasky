/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene;

import com.badlogic.ashley.core.Entity;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.scene.view.FocusView;
import gaiasky.util.Logger;
import gaiasky.util.i18n.I18n;

import java.util.Locale;

public class SceneObserver implements IObserver {
    private static final Logger.Log logger = Logger.getLogger(SceneObserver.class);

    private final FocusView view;

    public SceneObserver() {
        this.view = new FocusView();

        EventManager.instance.subscribe(this, Event.PER_OBJECT_VISIBILITY_CMD, Event.FORCE_OBJECT_LABEL_CMD, Event.LABEL_COLOR_CMD);
    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        switch (event) {
        case PER_OBJECT_VISIBILITY_CMD -> {
            if (data[0] instanceof FocusView focusView) {
                final String name = (String) data[1];
                final boolean state = (boolean) data[2];

                focusView.setVisible(state, name.toLowerCase());
                logger.info(I18n.msg("notif.visibility.object.set", focusView.getName(), I18n.msg("gui." + state)));
            } else if (data[0] instanceof Entity entity) {
                view.setEntity(entity);
                final String name = (String) data[1];
                final boolean state = (boolean) data[2];

                view.setVisible(state, name.toLowerCase());
                logger.info(I18n.msg("notif.visibility.object.set", view.getName(), I18n.msg("gui." + state)));
            } else {
                logger.warn("PER_OBJECT_VISIBILITY_CMD needs a FocusView or an Entity, got " + data[0].getClass().getSimpleName());
            }
        }
        case FORCE_OBJECT_LABEL_CMD -> {
            if (data[0] instanceof FocusView entity) {
                final String name = (String) data[1];
                final boolean state = (boolean) data[2];

                entity.setForceLabel(state, name.toLowerCase(Locale.ROOT));
                logger.info(I18n.msg("notif.object.flag", "forceLabel", entity.getName(), I18n.msg("gui." + state)));
            }
        }
        case LABEL_COLOR_CMD -> {
            final Entity entity = (Entity) data[0];
            String name = (String) data[1];
            float[] labelColor = (float[]) data[2];

            synchronized (view) {
                view.setEntity(entity);
                view.setLabelColor(labelColor, name);
            }
        }
        }

    }
}
