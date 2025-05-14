/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.math.Vector3;
import gaiasky.scene.Mapper;
import gaiasky.scene.system.render.draw.text.LabelEntityRenderSystem;
import gaiasky.scene.system.update.GraphUpdater;
import gaiasky.scene.view.LabelView;
import gaiasky.util.Constants;
import gaiasky.util.math.Vector3Q;

public class LocInitializer extends AbstractInitSystem {

    public LocInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
    }

    @Override
    public void initializeEntity(Entity entity) {
        var base = Mapper.base.get(entity);
        var body = Mapper.body.get(entity);
        var graph = Mapper.graph.get(entity);
        var loc = Mapper.loc.get(entity);
        var label = Mapper.label.get(entity);

        graph.mustUpdateFunction = GraphUpdater::mustUpdateLoc;
        graph.positionUpdaterConsumer = GraphUpdater::updatePositionLocationMark;

        label.label = true;
        label.labelMax = 1;
        label.textScale = 1e-7f;
        label.labelFactor = 0.15f;
        label.renderConsumer = LabelEntityRenderSystem::renderLocation;
        label.depthBufferConsumer = LabelView::noTextDepthBuffer;
        label.renderFunction = LabelView::renderTextLocation;
        label.labelPosition = new Vector3Q();

        if (body.color == null) {
            // Default maker color.
            body.color = new float[]{0.7f, 0.6f, 0.0f, 0.4f};
        }
        body.size *= (float) Constants.KM_TO_U;

        loc.location3d = new Vector3();
        loc.sizeKm = (float) (body.size * Constants.U_TO_KM);
        loc.displayName = "  " + base.getLocalizedName();
    }

    @Override
    public void setUpEntity(Entity entity) {
        var loc = Mapper.loc.get(entity);
        var graph = Mapper.graph.get(entity);

        // Initialize location type if needed.
        if (loc.locationType == null || loc.locationType.isEmpty()) {
            // Set type equal to parent name.
            Entity papa = graph.parent;
            if (papa != null) {
                var pBase = Mapper.base.get(papa);
                loc.locationType = pBase.getLocalizedName();
            }
        }
    }
}
