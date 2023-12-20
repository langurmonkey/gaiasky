/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.entity;

import com.badlogic.ashley.core.Entity;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.BillboardSet;
import gaiasky.scene.system.initialize.BillboardSetInitializer;
import gaiasky.util.tree.LoadStatus;

public class BillboardSetRadio extends EntityRadio {

    private final BillboardSetInitializer initializer;
    private final BillboardSet billboardSet;

    public BillboardSetRadio(Entity entity) {
        super(entity);
        billboardSet = Mapper.billboardSet.get(entity);

        initializer = new BillboardSetInitializer(false, null, 0);
    }

    @Override
    public void notify(Event event,
                       Object source,
                       Object... data) {
        if (event == Event.GRAPHICS_QUALITY_UPDATED) {
            // Reload data files with new graphics setting
            GaiaSky.postRunnable(() -> {
                boolean reloaded = initializer.reloadData(billboardSet);
                if (reloaded) {
                    initializer.transformData(entity);
                    EventManager.publish(Event.GPU_DISPOSE_BILLBOARD_DATASET, entity);
                    billboardSet.status.set(LoadStatus.NOT_LOADED);
                }
            });
        }
    }
}
