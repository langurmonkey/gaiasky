/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
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
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.record.BillboardDataset;
import gaiasky.util.Logger;
import gaiasky.util.Nature;
import gaiasky.util.coord.StaticCoordinates;

public class ProceduralTriggerUpdater extends AbstractUpdateSystem {
    protected static final Logger.Log logger = Logger.getLogger(ProceduralTriggerUpdater.class);

    private final Scene scene;

    public ProceduralTriggerUpdater(Scene scene, Family family, int priority) {
        super(family, priority);
        this.scene = scene;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        updateEntity(entity, deltaTime);
    }

    @Override
    public void updateEntity(Entity entity, float deltaTime) {
        var base = Mapper.base.get(entity);
        var body = Mapper.body.get(entity);
        var trigger = Mapper.trigger.get(entity);

        if (trigger.proceduralGeneration &&
                trigger.billboardGroup == null &&
                body.distToCamera < body.size * 70) {

            logger.info("Procedurally generating galaxy: " + base.getName());

            // Create billboard group entity and add it to scene graph.
            var archetype = scene.archetypes().get("BillboardGroup");
            var bbg = archetype.createEntity();

            var bbgBase = Mapper.base.get(bbg);
            bbgBase.setName(base.getName() + " Procedural Half");
            bbgBase.setComponentType(ComponentType.Galaxies);

            var bbgBody = Mapper.body.get(bbg);
            bbgBody.setSize((double) body.size / 2);

            var coord = Mapper.coordinates.get(bbg);
            var coordinates = new StaticCoordinates();
            coordinates.setPosition(body.pos);
            coord.coordinates = coordinates;

            var fade = Mapper.fade.get(bbg);
            fade.setFadeOut(new double[]{bbgBody.size * 5, bbgBody.size * 10});
            fade.setFadeObjectName(bbgBase.getName());

            var graph = Mapper.graph.get(bbg);
            graph.setParent(Scene.ROOT_NAME);

            var focus = Mapper.focus.get(bbg);
            focus.focusable = true;

            var render = Mapper.render.get(bbg);
            render.halfResolutionBuffer = true;

            var bbSet = Mapper.billboardSet.get(bbg);
            bbSet.procedural = true;
            bbSet.setTextures(new String[]{"$data/default-data/galaxy/sprites"});

            // Generate Galaxy
            var spiralAngle = 360.0;
            var eccentricity = 0.3;
            var minRadius = 0.13;
            var displacement = new double[]{0.2, 0.1};

            // Dust
            var dust = new BillboardDataset();
            dust.setType("dust");
            dust.setDistribution("density");
            dust.setSpiralAngle(spiralAngle);
            dust.setEccentricity(eccentricity);
            dust.setMinRadius(minRadius);
            dust.setDisplacement(displacement);
            dust.setBaseColor(new double[]{0.8, 0.8, 1.0});
            dust.setParticleCount(45000L);
            dust.setSize(29.0);
            dust.setIntensity(0.022);
            dust.setBlending("subtractive");
            dust.setDepthMask(false);
            dust.setLayers(new int[]{1, 2});
            dust.setMaxSize(20.0);

            // Gas
            var gas = new BillboardDataset();
            gas.setType("gas");
            gas.setDistribution("density");
            gas.setSpiralAngle(spiralAngle);
            gas.setEccentricity(eccentricity);
            gas.setMinRadius(minRadius);
            gas.setDisplacement(displacement);
            gas.setBaseColors(new double[]{
                    0.802,
                    0.808,
                    0.979,
                    0.747,
                    0.734,
                    0.899,
                    0.8,
                    0.8,
                    1.0

            });
            gas.setParticleCount(5500L);
            gas.setColorNoise(0.07);
            gas.setSize(80.0);
            gas.setIntensity(0.007);
            gas.setLayers(new int[]{0, 1, 2, 3, 4});
            gas.setMaxSize(20.0);

            // Bulge
            var bulge = new BillboardDataset();
            bulge.setType("bulge");
            bulge.setDistribution("sphere");
            bulge.setBaseRadius(minRadius + 0.05);
            bulge.setBaseColor(new double[]{1.0, 0.93, 0.8});
            bulge.setParticleCount(12L);
            bulge.setColorNoise(0.09);
            bulge.setSize(95.0);
            bulge.setIntensity(0.6);
            bulge.setLayers(new int[]{0, 1, 2});
            bulge.setMaxSize(50.0);

            bbSet.setData(new BillboardDataset[]{dust, gas, bulge});


            GaiaSky.postRunnable(()->{
                scene.initializeEntity(bbg);
                scene.setUpEntity(bbg);
                EventManager.instance.post(Event.SCENE_ADD_OBJECT_CMD, this, bbg, true);
                trigger.billboardGroup = bbg;
            });
        }
    }
}
