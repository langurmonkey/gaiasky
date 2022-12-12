package gaiasky.util.math.test;

import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector3d;

/**
 * Tests the change of base matrices
 */
public class ChangeOfBasisTest {
    public static void main(String[] args) {
        System.out.println("==========================");
        Matrix4d c = Matrix4d.changeOfBasis(new double[] { 0, 0, -1 }, new double[] { 0, 1, 0 }, new double[] { 1, 0, 0 });

        Vector3d v = new Vector3d(1, 0, 0);
        System.out.println(v + " -> " + (new Vector3d(v)).mul(c));

        v = new Vector3d(0, 1, 0);
        System.out.println(v + " -> " + (new Vector3d(v)).mul(c));

        v = new Vector3d(0, 0, 1);
        System.out.println(v + " -> " + (new Vector3d(v)).mul(c));

        System.out.println("==========================");
        c = Matrix4d.changeOfBasis(new double[] { 0.5, 0, 0 }, new double[] { -1, 2, 0 }, new double[] { 0, 0, 1 });

        v = new Vector3d(4, 1, 0);
        System.out.println(v + " -> " + (new Vector3d(v)).mul(c));

        v = new Vector3d(0, 1, 0);
        System.out.println(v + " -> " + (new Vector3d(v)).mul(c));
    }
}
