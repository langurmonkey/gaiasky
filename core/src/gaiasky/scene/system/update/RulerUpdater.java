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
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.scene.Mapper;
import gaiasky.util.GlobalResources;
import gaiasky.util.Pair;
import gaiasky.util.Settings;

import java.text.DecimalFormat;

public class RulerUpdater extends AbstractUpdateSystem {

    private final DecimalFormat nf;

    public RulerUpdater(Family family, int priority) {
        super(family, priority);
        nf = new DecimalFormat("0.#########E0");
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        updateEntity(entity, deltaTime);
    }

    @Override
    public void updateEntity(Entity entity, float deltaTime) {
        var body = Mapper.body.get(entity);
        var graph = Mapper.graph.get(entity);
        var ruler = Mapper.ruler.get(entity);

        // Update positions
        ruler.rulerOk = (GaiaSky.instance.scene.getObjectPosition(ruler.name0, ruler.pos0) != null);
        ruler.rulerOk = ruler.rulerOk && (GaiaSky.instance.scene.getObjectPosition(ruler.name1, ruler.pos1) != null);

        if (ruler.rulerOk) {
            var parentGraph = Mapper.graph.get(graph.parent);
            ruler.p0.set(ruler.pos0).add(parentGraph.translation);
            ruler.p1.set(ruler.pos1).add(parentGraph.translation);
            // Mid-point
            ruler.m.set(ruler.p1).sub(ruler.p0).scl(0.5).add(ruler.p0);
            body.pos.set(ruler.m).sub(parentGraph.translation);
            graph.translation.set(parentGraph.translation).add(body.pos);
            // Distance in internal units
            double dst = ruler.p0.dst(ruler.p1);
            Pair<Double, String> d = GlobalResources.doubleToDistanceString(dst, Settings.settings.program.ui.distanceUnits);
            ruler.dist = nf.format(d.getFirst()) + " " + d.getSecond();

            GaiaSky.postRunnable(() -> EventManager.publish(Event.RULER_DIST, entity, dst, ruler.dist));
        } else {
            ruler.dist = null;
        }
    }
}
