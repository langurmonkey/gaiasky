/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import gaiasky.util.math.Vector3D;

public class Axis implements Component {

    public Vector3D o;
    public Vector3D x;
    public Vector3D y;
    public Vector3D z;
    // Base vectors
    public Vector3D b0;
    public Vector3D b1;
    public Vector3D b2;

    // RGBA colors for each of the bases XYZ -> [3][3]
    public float[][] axesColors;

    public void setAxesColors(double[][] colors) {
        axesColors = new float[3][3];
        axesColors[0][0] = (float) colors[0][0];
        axesColors[0][1] = (float) colors[0][1];
        axesColors[0][2] = (float) colors[0][2];

        axesColors[1][0] = (float) colors[1][0];
        axesColors[1][1] = (float) colors[1][1];
        axesColors[1][2] = (float) colors[1][2];

        axesColors[2][0] = (float) colors[2][0];
        axesColors[2][1] = (float) colors[2][1];
        axesColors[2][2] = (float) colors[2][2];
    }
}
