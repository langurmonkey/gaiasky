/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.vr.openvr;

import gaiasky.vr.openvr.VRContext.VRDevice;

public interface VRDeviceListener {
    /** A new {@link VRDevice} has connected **/
    void connected(VRDevice device);

    /** A {@link VRDevice} has disconnected **/
    void disconnected(VRDevice device);

    /**
     * A button was pressed on the
     * {@link VRDevice}
     * @return True if the event was handled.
     **/
    boolean buttonPressed(VRDevice device, int button);

    /**
     * A button was released on the
     * {@link VRDevice}
     * @return True if the event was handled.
     **/
    boolean buttonReleased(VRDevice device, int button);

    /**
     * A button was touched on the {@link VRDevice}
     * @return True if the event was handled.
     */
    boolean buttonTouched(VRDevice device, int button);

    /**
     * A button was untouched on the {@link VRDevice}
     * @return True if the event was handled.
     */
    boolean buttonUntouched(VRDevice device, int button);

    /**
     * An axis was moved on the {@link VRDevice}
     * @return True if the event was handled.
     */
    boolean axisMoved(VRDevice device, int axis, float valueX, float valueY);

    /**
     * Unhandled event on the {@link VRDevice}
     *
     * @param code Event code
     */
    void event(int code);
}
