/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.scene2d;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

public abstract class TabSelectionChangeListener extends ChangeListener {

    @Override
    public void changed(ChangeEvent event, Actor actor) {
        if (event.getListenerActor() == event.getTarget()) {
            tabSelectionChanged(event, actor);
        }
    }

    public abstract void tabSelectionChanged(ChangeEvent event, Actor actor);
}
