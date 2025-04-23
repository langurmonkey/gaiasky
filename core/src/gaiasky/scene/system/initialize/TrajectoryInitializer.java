/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import gaiasky.data.api.IOrbitDataProvider;
import gaiasky.data.util.OrbitDataLoader.OrbitDataLoaderParameters;
import gaiasky.render.RenderGroup;
import gaiasky.scene.Mapper;
import gaiasky.scene.entity.TrajectoryUtils;
import gaiasky.scene.system.render.draw.line.LineEntityRenderSystem;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector3d;
import org.lwjgl.opengl.GL20;

public class TrajectoryInitializer extends AbstractInitSystem {

    /**
     * The trajectory utils instance.
     */
    private final TrajectoryUtils utils;

    public TrajectoryInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
        utils = new TrajectoryUtils();
        TrajectoryUtils.initRefresher(utils);
    }

    @Override
    public void initializeEntity(Entity entity) {
        var base = Mapper.base.get(entity);
        var body = Mapper.body.get(entity);
        var trajectory = Mapper.trajectory.get(entity);
        var verts = Mapper.verts.get(entity);
        var line = Mapper.line.get(entity);

        if (trajectory.bodyRepresentation.isOrbit()) {
            if (trajectory.provider != null) {
                try {
                    trajectory.providerClass = (Class<? extends IOrbitDataProvider>) ClassReflection.forName(trajectory.provider);
                    // Orbit data
                    try {
                        trajectory.providerInstance = ClassReflection.newInstance(trajectory.providerClass);
                        trajectory.providerInstance.initialize(entity, trajectory);
                        trajectory.providerInstance.load(trajectory.oc.source, new OrbitDataLoaderParameters(base.names[0],
                                trajectory.providerClass,
                                trajectory.oc,
                                trajectory.multiplier,
                                trajectory.numSamples,
                                trajectory.sampling),
                                trajectory.newMethod);
                        verts.pointCloudData = trajectory.providerInstance.getData();

                        // Transform data using affine transformations.
                        var affine = Mapper.affine.get(entity);
                        if (affine != null && !affine.isEmpty()) {
                            var data = verts.pointCloudData;
                            var v = new Vector3d();
                            var mat = new Matrix4d();
                            affine.apply(mat);
                            for (int i = 0; i < data.getNumPoints(); i++) {
                                // Get.
                                data.loadPoint(v, i);
                                // Transform.
                                v.mul(mat);
                                // Set.
                                data.setPoint(v, i);
                            }

                        }
                    } catch (Exception e) {
                        logger.error(e);
                    }
                } catch (ReflectionException e) {
                    logger.error(e);
                }
            }
        }
        // All trajectories have the same primitive and render group.
        verts.glPrimitive = GL20.GL_LINE_STRIP;
        verts.renderGroup = RenderGroup.LINE_GPU;

        line.renderConsumer = LineEntityRenderSystem::renderTrajectory;

        // Initialize default colors if needed.
        if (body.color == null) {
            body.color = new float[]{0.8f, 0.8f, 0.8f, 1f};
        }
        if (trajectory.bodyColor == null) {
            trajectory.bodyColor = new float[]{0.8f, 0.8f, 0.8f, 0.6f};
        }
    }

    @Override
    public void setUpEntity(Entity entity) {
        var body = Mapper.body.get(entity);
        var graph = Mapper.graph.get(entity);
        var transform = Mapper.transform.get(entity);
        var trajectory = Mapper.trajectory.get(entity);
        var verts = Mapper.verts.get(entity);

        trajectory.alpha = body.color[3];
        utils.initOrbitMetadata(body, trajectory, verts);
        verts.primitiveSize = 1.1f;

        if (trajectory.body != null) {
            var bodyBase = Mapper.base.get(trajectory.body);
            trajectory.params = new OrbitDataLoaderParameters(bodyBase.names[0], null, trajectory.oc.period, 600, trajectory.sampling);
            trajectory.params.entity = entity;
        }

        utils.initializeTransformMatrix(trajectory, graph, transform);

        trajectory.isInOrbitalElementsGroup = graph.parent != null && Mapper.orbitElementsSet.has(graph.parent);
    }
}
