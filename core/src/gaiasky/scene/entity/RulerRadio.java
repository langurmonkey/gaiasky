/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.entity;

import com.badlogic.ashley.core.Entity;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.scene.Mapper;

public class RulerRadio extends EntityRadio {

    public RulerRadio(Entity entity) {
        super(entity);
        EventManager.instance.subscribe(this, Event.RULER_ATTACH_0, Event.RULER_ATTACH_1, Event.RULER_CLEAR);
    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        var ruler = Mapper.ruler.get(entity);

        switch (event) {
            case RULER_ATTACH_0 -> {
                String name = (String) data[0];
                ruler.setName0(name);
            }
            case RULER_ATTACH_1 -> {
                String name = (String) data[0];
                ruler.setName1(name);
            }
            case RULER_CLEAR -> {
                ruler.setName0(null);
                ruler.setName1(null);
            }
            default -> {
            }
        }

    }
}
