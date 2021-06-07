/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.desktop.concurrent;

import gaiasky.desktop.concurrent.GaiaSkyThreadFactory.GSThread;

/**
 * Thread indexer for a multi-thread environment.
 */
public class MultiThreadIndexer extends ThreadIndexer {

    @Override
    public int idx() {
        Thread t = Thread.currentThread();
        if (t instanceof GSThread)
            return ((GSThread) Thread.currentThread()).index;
        else
            return 0;
    }

    @Override
    public int nthreads() {
        return Runtime.getRuntime().availableProcessors();
    }

}
