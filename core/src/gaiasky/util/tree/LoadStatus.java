/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.tree;

public enum LoadStatus {
    NOT_LOADED,
    QUEUED,
    /** Loading. **/
    LOADING,
    LOADING_FAILED,
    /** Ready to stream data to GPU. **/
    READY,
    /** Data partially available in GPU. **/
    PARTIALLY_LOADED,
    /** Data totally available in GPU. **/
    LOADED
}
