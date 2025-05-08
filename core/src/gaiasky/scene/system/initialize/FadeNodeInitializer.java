/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.scene.Index;
import gaiasky.scene.Mapper;
import gaiasky.util.math.Vector2D;

public class FadeNodeInitializer extends AbstractInitSystem {

    /** The index reference. **/
    private final Index index;

    public FadeNodeInitializer(final Index index, boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
        this.index = index;
    }

    @Override
    public void initializeEntity(Entity entity) {
        var fade = Mapper.fade.get(entity);

        // Initialize default mappings, if no mappings are set
        if (fade.fadeIn != null && fade.fadeInMap == null) {
            fade.fadeInMap = new Vector2D(0, 1);
        }
        if (fade.fadeOut != null && fade.fadeOutMap == null) {
            fade.fadeOutMap = new Vector2D(1, 0);
        }

    }

    @Override
    public void setUpEntity(Entity entity) {
        var fade = Mapper.fade.get(entity);
        if (fade.fadePositionObjectName != null) {
            fade.fadePositionObject = index.getEntity(fade.fadePositionObjectName);
        }
    }
}
