/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.ds;

import gaiasky.scenegraph.StarGroup;
import gaiasky.util.Logger;
import gaiasky.util.Settings;

import java.util.concurrent.*;

/**
 * Contains the infrastructure to run tasks that sort and update the dataset metadata.
 */
public class GaiaSkyExecutorService {
    private static final Logger.Log logger = Logger.getLogger(GaiaSkyExecutorService.class);

    private static class DaemonThreadFactory implements ThreadFactory {
        private int sequence = 0;

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "gaiasky-worker-" + sequence);
            sequence++;
            t.setDaemon(true);
            return t;
        }

    }

    /**
     * Thread pool executor
     */
    private ThreadPoolExecutor pool;
    private BlockingQueue<Runnable> workQueue;

    public GaiaSkyExecutorService() {
        super();
        initialize();
    }

    public void initialize() {
        workQueue = new LinkedBlockingQueue<>();
        int nThreads = !Settings.settings.performance.multithreading ? 1 : Math.max(1, Settings.settings.performance.getNumberOfThreads() + 1);
        pool = new ThreadPoolExecutor(nThreads, nThreads, 5, TimeUnit.SECONDS, workQueue);
        pool.setThreadFactory(new DaemonThreadFactory());
    }

    public boolean execute(Runnable r) {
        if (pool != null && !pool.isShutdown() && !inQueue(r)) {
            pool.execute(r);
            return true;
        }
        return false;
    }

    public ThreadPoolExecutor pool() {
        return pool;
    }

    public boolean inQueue(Runnable task) {
        return workQueue != null && workQueue.contains(task);
    }

    public void shutDownThreadPool() {
        // Shut down pool
        if (pool != null && !pool.isShutdown()) {
            pool.shutdown();
            try {
                // Wait for task to end before proceeding
                if (!pool.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                    logger.debug("Timeout elapsed while waiting for the pool to shut down");
                }
            } catch (Exception e) {
                Logger.getLogger(StarGroup.class.getSimpleName()).error(e);
            }
        }
        if (workQueue != null)
            workQueue.clear();
    }

}
