/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.vr.openxr;

/**
 * Loading status of the XR subsystem.
 */
public enum XrLoadStatus {
    OK,
    ERROR_NO_CONTEXT,
    ERROR_RENDERMODEL,
    NO_VR;

    public boolean vrInitFailed() {
        return this.equals(ERROR_NO_CONTEXT) || this.equals(ERROR_RENDERMODEL);
    }

    public boolean vrInitOk() {
        return this.equals(OK);
    }
}
