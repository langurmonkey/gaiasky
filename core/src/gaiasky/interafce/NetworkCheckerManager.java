/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce;

/**
 * Holds the network checker entity.
 * @author tsagrista
 *
 */
public class NetworkCheckerManager {

    private static INetworkChecker networkChecker;

    public static void initialize(INetworkChecker networkChecker) {
        NetworkCheckerManager.networkChecker = networkChecker;
    }

    public static INetworkChecker getNewtorkChecker() {
        return networkChecker;
    }
}
