/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.ds;

import gaiasky.util.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Executor service of Gaia Sky, backed by an {@link ExecutorService} constructed with virtual threads.
 */
public class GaiaSkyExecutorService {
    private static final Logger.Log logger = Logger.getLogger(GaiaSkyExecutorService.class);
    /**
     * Thread pool executor.
     */
    private ExecutorService pool;
    public GaiaSkyExecutorService() {
        super();
        initialize();
    }

    public void initialize() {
        // Create fixed thread pool executor, with a static number of threads.
        pool = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("gaiasky-virtual-", 0).factory());
    }

    public boolean execute(Runnable r) {
        if (pool != null && !pool.isShutdown()) {
            pool.execute(r);
            return true;
        }
        return false;
    }

    public ExecutorService getPool() {
        return pool;
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
                logger.error(e);
            }
        }
    }

}
