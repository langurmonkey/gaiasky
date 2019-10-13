/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.tree;

public enum LoadStatus {
    NOT_LOADED,
    QUEUED,
    LOADING,
    LOADING_FAILED,
    READY,
    LOADED
}
