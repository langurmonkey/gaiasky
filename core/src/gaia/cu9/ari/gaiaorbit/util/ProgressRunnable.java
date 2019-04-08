/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.util;

public interface ProgressRunnable {
    /**
     * Informs of progress
     *
     * @param read     Bytes read
     * @param total    Total bytes to read
     * @param progress Progress in percentage
     * @param speed    Download speed in bytes per millisecond
     */
    void run(long read, long total, double progress, double speed);
}
