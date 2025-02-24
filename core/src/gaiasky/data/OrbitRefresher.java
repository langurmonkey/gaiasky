/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.data.api.IOrbitDataProvider;
import gaiasky.data.orbit.OrbitBodyDataProvider;
import gaiasky.data.orbit.OrbitFileDataProvider;
import gaiasky.data.orbit.OrbitSamplerDataProvider;
import gaiasky.data.util.OrbitDataLoader.OrbitDataLoaderParameters;
import gaiasky.data.util.PointCloudData;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.scene.Mapper;
import gaiasky.scene.entity.TrajectoryUtils;
import gaiasky.scene.view.VertsView;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.concurrent.ServiceThread;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class OrbitRefresher implements IObserver {
    // Maximum number of pages to send to load every batch.
    protected static final int MAX_LOAD_CHUNK = 5;
    private static final Log logger = Logger.getLogger(OrbitRefresher.class);
    // Maximum size of load queue.
    private static final int LOAD_QUEUE_MAX_SIZE = 15;
    private final Queue<OrbitDataLoaderParameters> toLoadQueue;
    private final OrbitUpdaterThread daemon;
    private final TrajectoryUtils utils;

    public OrbitRefresher(String threadName, TrajectoryUtils utils) {
        super();
        toLoadQueue = new ArrayBlockingQueue<>(LOAD_QUEUE_MAX_SIZE);

        this.utils = utils;

        // Start daemon
        daemon = new OrbitUpdaterThread(this);
        daemon.setDaemon(true);
        daemon.setName(threadName);
        daemon.setPriority(Thread.MIN_PRIORITY);
        daemon.start();

        EventManager.instance.subscribe(this, Event.DISPOSE, Event.ORBIT_REFRESH_CMD);
    }

    public void queue(OrbitDataLoaderParameters params) {
        boolean loadingPaused = false;
        if (!loadingPaused && toLoadQueue.size() < LOAD_QUEUE_MAX_SIZE - 1) {
            toLoadQueue.remove(params);
            toLoadQueue.add(params);
            if (params.entity != null) {
                Mapper.trajectory.get(params.entity).refreshing = true;
            }
            daemon.wakeUp();
        }
    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        if (event == Event.DISPOSE && daemon != null) {
            daemon.stopDaemon(false);
        } else if (event == Event.ORBIT_REFRESH_CMD && utils != null) {
            var entity = (Entity) data[0];
            var trajectory = Mapper.trajectory.get(entity);
            var verts = Mapper.verts.get(entity);
            utils.refreshOrbit(trajectory, verts, true);
        }
    }

    /**
     * The orbit refresher thread.
     */
    protected static class OrbitUpdaterThread extends ServiceThread {
        // TODO Remove this and rely only on the object provider.
        private final OrbitSamplerDataProvider provider;
        private final Array<OrbitDataLoaderParameters> toLoad;

        private final TrajectoryUtils utils = new TrajectoryUtils();

        public OrbitUpdaterThread(final OrbitRefresher orbitRefresher) {
            super();
            this.toLoad = new Array<>();
            this.provider = new OrbitSamplerDataProvider();
            this.task = () -> {
                /* ----------- PROCESS REQUESTS ----------- */
                while (!orbitRefresher.toLoadQueue.isEmpty()) {
                    toLoad.clear();
                    int i = 0;
                    while (orbitRefresher.toLoadQueue.peek() != null && i <= MAX_LOAD_CHUNK) {
                        OrbitDataLoaderParameters param = orbitRefresher.toLoadQueue.poll();
                        toLoad.add(param);
                        i++;
                    }

                    // Generate orbits if any
                    if (toLoad.size > 0) {
                        for (OrbitDataLoaderParameters params : toLoad) {
                            if (params.entity != null) {
                                Entity entity = params.entity;
                                var trajectory = Mapper.trajectory.get(entity);
                                try {
                                    if (trajectory == null) {
                                        continue;
                                    }
                                    IOrbitDataProvider currentProvider;
                                    IOrbitDataProvider objectProvider = trajectory.providerInstance;
                                    if (Mapper.tagHeliotropic.has(entity)) {
                                        currentProvider = objectProvider;
                                    } else if (objectProvider == null || objectProvider instanceof OrbitFileDataProvider) {
                                        currentProvider = this.provider;
                                    } else {
                                        currentProvider = objectProvider;
                                    }
                                    // Generate data
                                    currentProvider.load(trajectory.oc.source, params);
                                    final PointCloudData pcd = currentProvider.getData();
                                    // Post new data to object
                                    GaiaSky.postRunnable(() -> {
                                        // Update orbit object
                                        var vertsView = new VertsView(entity);

                                        var body = Mapper.body.get(entity);
                                        var verts = Mapper.verts.get(entity);
                                        verts.pointCloudData = pcd;
                                        utils.initOrbitMetadata(body, trajectory, verts);
                                        vertsView.markForUpdate();

                                        trajectory.refreshing = false;
                                    });

                                } catch (Exception e) {
                                    trajectory.refreshing = false;
                                    // This will happen when the queue has been cleared during processing.
                                    logger.debug("Refreshing orbits operation failed.");
                                }
                            }
                        }
                    }
                }
            };
        }
    }
}
