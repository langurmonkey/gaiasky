/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data;

import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.data.orbit.OrbitSamplerDataProvider;
import gaiasky.data.util.OrbitDataLoader.OrbitDataLoaderParameter;
import gaiasky.data.util.PointCloudData;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.scenegraph.Orbit;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.concurrent.ServiceThread;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class OrbitRefresher implements IObserver {
    private static final Log logger = Logger.getLogger(OrbitRefresher.class);

    // Maximum size of load queue.
    private static final int LOAD_QUEUE_MAX_SIZE = 15;
    // Maximum number of pages to send to load every batch.
    protected static final int MAX_LOAD_CHUNK = 5;

    private static OrbitRefresher instance;
    private final Queue<OrbitDataLoaderParameter> toLoadQueue;
    private final OrbitUpdaterThread daemon;
    private final boolean loadingPaused = false;

    public OrbitRefresher() {
        super();
        toLoadQueue = new ArrayBlockingQueue<>(LOAD_QUEUE_MAX_SIZE);
        OrbitRefresher.instance = this;

        // Start daemon
        daemon = new OrbitUpdaterThread();
        daemon.setDaemon(true);
        daemon.setName("gaiasky-worker-orbitupdate");
        daemon.setPriority(Thread.MIN_PRIORITY);
        daemon.start();

        EventManager.instance.subscribe(this, Event.DISPOSE);
    }

    public void queue(OrbitDataLoaderParameter param) {
        if (!loadingPaused && toLoadQueue.size() < LOAD_QUEUE_MAX_SIZE - 1) {
            toLoadQueue.remove(param);
            toLoadQueue.add(param);
            param.orbit.refreshing = true;
            flushLoadQueue();
        }

    }

    /**
     * Tells the loader to start loading the octants in the queue.
     */
    public void flushLoadQueue() {
        if (!daemon.isAwake() && !toLoadQueue.isEmpty() && !loadingPaused) {
            synchronized (daemon.getThreadLock()) {
                daemon.getThreadLock().notifyAll();
            }
        }
    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        if (event == Event.DISPOSE && daemon != null) {
            daemon.stopDaemon();
        }
    }

    /**
     * The orbit refresher thread.
     */
    protected static class OrbitUpdaterThread extends ServiceThread {
        private final OrbitSamplerDataProvider provider;

        private final Array<OrbitDataLoaderParameter> toLoad;

        public OrbitUpdaterThread() {
            super();
            this.toLoad = new Array<>();
            this.provider = new OrbitSamplerDataProvider();
            this.task = () -> {
                /* ----------- PROCESS REQUESTS ----------- */
                while (!instance.toLoadQueue.isEmpty()) {
                    toLoad.clear();
                    int i = 0;
                    while (instance.toLoadQueue.peek() != null && i <= MAX_LOAD_CHUNK) {
                        OrbitDataLoaderParameter param = instance.toLoadQueue.poll();
                        toLoad.add(param);
                        i++;
                    }

                    // Generate orbits if any
                    if (toLoad.size > 0) {
                        try {
                            for (OrbitDataLoaderParameter param : toLoad) {
                                Orbit orbit = param.orbit;
                                if (orbit != null) {
                                    // Generate data
                                    provider.load(null, param);
                                    final PointCloudData pcd = provider.getData();
                                    // Post new data to object
                                    GaiaSky.postRunnable(() -> {
                                        // Update orbit object
                                        orbit.setPointCloudData(pcd);
                                        orbit.initOrbitMetadata();
                                        orbit.markForUpdate();

                                        orbit.refreshing = false;
                                    });

                                }
                            }
                        } catch (Exception e) {
                            // This will happen when the queue has been cleared during processing.
                            logger.debug("Refreshing orbits operation failed");
                        }
                    }
                }
            };
        }
    }
}
