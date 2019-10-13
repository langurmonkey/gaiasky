/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.scenegraph.component;

/**
 * Provides the information for the precession of this body.
 * @author Toni Sagrista
 *
 */
public class PrecessionComponent {
    /** Precession angle in deg **/
    public float precessionAngle;
    /** Precession velocity in deg/s **/
    protected float precessionVelocity;
    /** Current precession position around y **/
    public float precessionPosition;

    public PrecessionComponent() {

    }
}
