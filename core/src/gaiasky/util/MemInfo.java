/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

public class MemInfo {

    private static double BYTE_TO_MB = 1024 * 1024;

    public static double getUsedMemory() {
        return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / BYTE_TO_MB;
    }

    public static double getFreeMemory() {
        return (Runtime.getRuntime().freeMemory()) / BYTE_TO_MB;
    }

    public static double getTotalMemory() {
        return (Runtime.getRuntime().totalMemory()) / BYTE_TO_MB;
    }

    public static double getMaxMemory() {
        return (Runtime.getRuntime().maxMemory()) / BYTE_TO_MB;
    }

}
