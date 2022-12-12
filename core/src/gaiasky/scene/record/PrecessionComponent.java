/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

/**
 * Provides the information for the precession of this body
 */
public class PrecessionComponent {
    /** Precession angle in deg **/
    public float precessionAngle;
    /** Current precession position around y **/
    public float precessionPosition;
    /** Precession velocity in deg/s **/
    protected float precessionVelocity;

    public PrecessionComponent() {

    }
}
