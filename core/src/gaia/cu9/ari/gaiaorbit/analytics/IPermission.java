/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.analytics;

public interface IPermission {

    /**
     * Checks for the permission
     *
     * @return True if permission is granted, false otherwise
     */
    boolean check();

}
