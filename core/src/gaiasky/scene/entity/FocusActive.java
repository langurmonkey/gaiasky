/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.entity;

import com.badlogic.ashley.core.Entity;
import gaiasky.GaiaSky;
import gaiasky.scene.Mapper;
import gaiasky.scene.api.IFocus;
import gaiasky.scene.component.Base;

public class FocusActive {

    public boolean isFocusActiveTrue(Entity entity, Base base) {
        return true;
    }

    public boolean isFocusActiveFalse(Entity entity, Base base) {
        return true;
    }

    public boolean isFocusActiveCtOpacity(Entity entity, Base base) {
        return GaiaSky.instance.isOn(base.ct) && base.opacity > 0;
    }

    public boolean isFocusActiveGroup(Entity entity, Base base) {
        if (Mapper.starSet.has(entity)) {
            return Mapper.starSet.get(entity).focusIndex >= 0;
        } else if (Mapper.particleSet.has(entity)) {
            return Mapper.particleSet.get(entity).focusIndex >= 0;
        } else {
            return false;
        }
    }
}
