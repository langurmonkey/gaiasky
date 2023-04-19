/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.update;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.GaiaSky;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.tag.TagNoProcess;

public class DatasetDescriptionUpdater extends AbstractUpdateSystem {

    public DatasetDescriptionUpdater(Family family, int priority) {
        super(family, priority);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        updateEntity(entity, deltaTime);
    }

    @Override
    public void updateEntity(Entity entity, float deltaTime) {
        var base = Mapper.base.get(entity);
        var ds = Mapper.datasetDescription.get(entity);

        float alpha = GaiaSky.instance.alpha(base.ct);

        if (alpha == 0 && ds.previousAlpha != 0) {
            // Turn off children.
            enableChildrenProcessing(entity, false);
        } else if (alpha > 0 && ds.previousAlpha == 0) {
            // Turn on children.
            enableChildrenProcessing(entity, true);
        }

        ds.previousAlpha = alpha;
    }

    public void enableChildrenProcessing(Entity entity, boolean enable) {
        var graph = Mapper.graph.get(entity);
        if (graph != null && graph.children != null && graph.children.size > 0) {
            for (var child : graph.children) {
                if (enable) {
                    // Remove no-process tag.
                    child.remove(TagNoProcess.class);
                } else {
                    // Add no-process tag.
                    var engine = getEngine();
                    child.add(engine != null ? engine.createComponent(TagNoProcess.class) : new TagNoProcess());
                }
            }
        }
    }

}
