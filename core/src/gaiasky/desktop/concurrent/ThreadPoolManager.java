/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.desktop.concurrent;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class ThreadPoolManager {

    /** The executor service containing the pool **/
    public static ThreadPoolExecutor pool;

    public static void initialize(int numThreads) {
        pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(numThreads, new GaiaSkyThreadFactory());
    }

}
