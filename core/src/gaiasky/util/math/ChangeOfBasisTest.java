package gaiasky.util.math;

/**
 * Tests the change of base matrices
 */
public class ChangeOfBasisTest {
    public static void main(String[] args) {
        Matrix4d c = Matrix4d.changeOfBasis(new double[] { 0, 0, -1 }, new double[] { 0, 1, 0 }, new double[] { 1, 0, 0 });

        Vector3d v = new Vector3d(1, 0, 0);
        System.out.println(v + " -> " + (new Vector3d(v)).mul(c));

        v = new Vector3d(0, 1, 0);
        System.out.println(v + " -> " + (new Vector3d(v)).mul(c));

        v = new Vector3d(0, 0, 1);
        System.out.println(v + " -> " + (new Vector3d(v)).mul(c));
    }
}
