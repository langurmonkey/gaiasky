/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.vr.openvr;

import gaiasky.vr.openvr.VRContext.VRControllerAxes;
import gaiasky.vr.openvr.VRContext.VRControllerButtons;
import gaiasky.vr.openvr.VRContext.VRDevice;

public interface VRDeviceListener {
    /** A new {@link VRDevice} has connected **/
    void connected(VRDevice device);

    /** A {@link VRDevice} has disconnected **/
    void disconnected(VRDevice device);

    /**
     * A button from {@link VRControllerButtons} was pressed on the
     * {@link VRDevice}
     **/
    void buttonPressed(VRDevice device, int button);

    /**
     * A button from {@link VRControllerButtons} was released on the
     * {@link VRDevice}
     **/
    void buttonReleased(VRDevice device, int button);

    /**
     * A button from {@link VRControllerButtons} was touched on the {@link VRDevice}
     */
    void buttonTouched(VRDevice device, int button);

    /**
     * A button from {@link VRControllerButtons} was untouched on the {@link VRDevice}
     */
    void buttonUntouched(VRDevice device, int button);

    /**
     * An axis from {@link VRControllerAxes} was moved on the {@link VRDevice}
     */
    void axisMoved(VRDevice device, int axis, float valueX, float valueY);

    /**
     * Unhandled event on the {@link VRDevice}
     *
     * @param code Event code
     */
    void event(int code);
}
