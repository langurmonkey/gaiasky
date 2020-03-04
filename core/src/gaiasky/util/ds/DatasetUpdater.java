/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.ds;

import gaiasky.scenegraph.StarGroup;
import gaiasky.util.GlobalConf;
import gaiasky.util.Logger;

import java.util.concurrent.*;

/**
 * Contains the infrastructure to run tasks that sort and update the dataset metadata.
 */
public class DatasetUpdater {

    private static class DaemonThreadFactory implements ThreadFactory {
        private int sequence = 0;

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "updater-daemon-" + sequence);
            sequence++;
            t.setDaemon(true);
            return t;
        }

    }

    /**
     * Thread pool executor
     */
    private static ThreadPoolExecutor pool;
    private static BlockingQueue<Runnable> workQueue;

    public static void initialize() {
        workQueue = new LinkedBlockingQueue<>();
        int nThreads = !GlobalConf.performance.MULTITHREADING ? 1 : Math.max(1, GlobalConf.performance.NUMBER_THREADS() - 1);
        pool = new ThreadPoolExecutor(nThreads, nThreads, 5, TimeUnit.SECONDS, workQueue);
        pool.setThreadFactory(new DaemonThreadFactory());
    }

    public static boolean execute(Runnable r) {
        if (pool != null && !pool.isShutdown() && !inQueue(r)) {
            pool.execute(r);
            return true;
        }
        return false;
    }

    public static ThreadPoolExecutor pool() {
        return pool;
    }

    public static BlockingQueue<Runnable> workQueue() {
        return workQueue;
    }

    public static boolean inQueue(Runnable task) {
        return workQueue != null && workQueue.contains(task);
    }

    public static void shutDownThreadPool() {
        // Shut down pool
        if (pool != null && !pool.isShutdown()) {
            pool.shutdown();
            try {
                // Wait for task to end before proceeding
                pool.awaitTermination(500, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                Logger.getLogger(StarGroup.class.getSimpleName()).error(e);
            }
        }
        if (workQueue != null)
            workQueue.clear();
    }

}
