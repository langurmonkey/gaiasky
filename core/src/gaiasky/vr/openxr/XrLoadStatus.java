/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.vr.openxr;

public enum XrLoadStatus {
    OK,
    ERROR_NO_CONTEXT,
    ERROR_RENDERMODEL,
    NO_VR;

    public boolean vrInitFailed() {
        return this.equals(ERROR_NO_CONTEXT) || this.equals(ERROR_RENDERMODEL);
    }
}
