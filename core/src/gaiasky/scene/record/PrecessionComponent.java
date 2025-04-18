/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

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
