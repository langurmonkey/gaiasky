/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.utils.Array;
import gaiasky.event.Event;
import gaiasky.event.EventManager;

public class OrbitElementsSet implements Component {
    public Array<Entity> alwaysUpdate;
    public boolean initialUpdate = true;

    public void markForUpdate(Render render) {
        EventManager.publish(Event.GPU_DISPOSE_ORBITAL_ELEMENTS, render);
    }
}
