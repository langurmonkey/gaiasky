package gaiasky;

import gaiasky.util.math.Matrix4D;
import gaiasky.util.math.Vector3D;
import org.junit.Test;

import static org.astrogrid.samp.test.Tester.assertEquals;

public class ChangeOfBasisTest {

    @Test
    public void testChangeOfBasis() {
        Matrix4D c = Matrix4D.changeOfBasis(new double[]{0, 0, -1}, new double[]{0, 1, 0}, new double[]{1, 0, 0});

        Vector3D v = new Vector3D(1, 0, 0);
        var r = (new Vector3D(v)).mul(c);
        assertEquals(new Vector3D(0, 0, -1), r);

        v = new Vector3D(0, 1, 0);
        r = new Vector3D(v).mul(c);
        assertEquals(new Vector3D(0, 1, 0), r);

        v = new Vector3D(0, 0, 1);
        r = new Vector3D(v).mul(c);
        assertEquals(new Vector3D(1, 0, 0), r);

        c = Matrix4D.changeOfBasis(new double[]{0.5, 0, 0}, new double[]{-1, 2, 0}, new double[]{0, 0, 1});

        v = new Vector3D(4, 1, 0);
        r = new Vector3D(v).mul(c);
        assertEquals(new Vector3D(1, 2, 0), r);

        v = new Vector3D(0, 1, 0);
        r = new Vector3D(v).mul(c);
        assertEquals(new Vector3D(-1, 2, 0), r);
    }
}
