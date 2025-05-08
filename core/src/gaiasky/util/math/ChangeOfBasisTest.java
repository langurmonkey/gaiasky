/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.math;

public class ChangeOfBasisTest {
    public static void main(String[] args) {
        Matrix4D c = Matrix4D.changeOfBasis(new double[] { 0, 0, -1 }, new double[] { 0, 1, 0 }, new double[] { 1, 0, 0 });

        Vector3D v = new Vector3D(1, 0, 0);
        System.out.println(v + " -> " + (new Vector3D(v)).mul(c));

        v = new Vector3D(0, 1, 0);
        System.out.println(v + " -> " + (new Vector3D(v)).mul(c));

        v = new Vector3D(0, 0, 1);
        System.out.println(v + " -> " + (new Vector3D(v)).mul(c));
    }
}
