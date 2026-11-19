package gaiasky.scene.system.render.draw;

import gaiasky.scene.record.ParticleKepler;
import gaiasky.util.math.Matrix4D;
import gaiasky.util.math.Vector3D;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * Checks that the CPU path of the elements positions applies the transforms exactly as the elements
 * vertex shader does:
 * <pre>
 *     pos = u_transform * (keplerToCartesian() * u_refSysTransform) + u_datasetPos;
 * </pre>
 * The reference system is multiplied as a row vector, so its translation is not applied, while the
 * affine transformation is multiplied as a column vector and its translation is. Getting either of
 * the two the other way round leaves the elements of a group tilted with respect to the body they
 * belong to, which is very visible in a ring of particles.
 */
public class ElementPositionsTransformTest {

    private static final int N = 64;
    private static final double T = 2460000.5;
    /** The result is a float, so the tolerance is a few ulps of the expected value. **/
    private static void assertClose(double expected,
                                    double actual) {
        assertEquals(expected, actual, 4 * Math.ulp((float) expected) + 1e-6);
    }

    private static double[] base(Object[] src) {
        var ep = new ElementPositions();
        ep.compile(src, N);
        float[] buf = new float[N * 3];
        ep.compute(T, buf, 3, 0, 1, null, null, null);
        double[] out = new double[N * 3];
        for (int i = 0; i < out.length; i++) {
            out[i] = buf[i];
        }
        return out;
    }

    private static Object[] elements() {
        Random r = new Random(17);
        Object[] src = new Object[N];
        for (int i = 0; i < N; i++) {
            // Ring-like: low eccentricity, a fraction of an AU, an inclination of a few degrees.
            src[i] = new ParticleKepler(i, null, 2451545.0, r.nextDouble() * 360, 1e6 * (1 + r.nextDouble()),
                                        r.nextDouble() * 0.05, r.nextDouble() * 360, r.nextDouble() * 360,
                                        r.nextDouble() * 12, 0.3 + r.nextDouble(), null);
        }
        return src;
    }

    /** The transpose of a matrix, so that the engine's column multiply applies the original as the shader does. **/
    private static Matrix4D transpose(Matrix4D m) {
        double[] a = m.val;
        double[] b = new double[16];
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                b[col * 4 + row] = a[row * 4 + col];
            }
        }
        return new Matrix4D(b);
    }

    private static Matrix4D rotation(double ax, double ay, double az, double deg) {
        var m = new Matrix4D();
        m.idt();
        m.setToRotation(ax, ay, az, deg);
        return m;
    }

    @Test
    public void referenceSystemIsAppliedAsRowVector() {
        var refSys = rotation(0, 1, 0, 27.5).mul(rotation(1, 0, 0, -12));
        // A translation that must not be applied, since the shader multiplies the vector on the left.
        refSys.setTranslation(4e7, -3e7, 5e7);

        Object[] src = elements();
        double[] base = base(src);

        var ep = new ElementPositions();
        ep.compile(src, N);
        float[] buf = new float[N * 3];
        ep.compute(T, buf, 3, 0, 1, refSys, null, null);

        var expected = transpose(refSys);
        var v = new Vector3D();
        for (int i = 0; i < N; i++) {
            v.set(base[i * 3], base[i * 3 + 1], base[i * 3 + 2]).mul(expected);
            assertClose(v.x, buf[i * 3]);
            assertClose(v.y, buf[i * 3 + 1]);
            assertClose(v.z, buf[i * 3 + 2]);
        }
    }

    @Test
    public void affineIsAppliedAsColumnVector() {
        var affine = rotation(0, 0, 1, 33);
        affine.scl(1.5);
        // This translation must be applied.
        affine.setTranslation(2e3, -7e3, 4e3);

        Object[] src = elements();
        double[] base = base(src);

        var ep = new ElementPositions();
        ep.compile(src, N);
        float[] buf = new float[N * 3];
        ep.compute(T, buf, 3, 0, 1, null, null, affine);

        var v = new Vector3D();
        for (int i = 0; i < N; i++) {
            v.set(base[i * 3], base[i * 3 + 1], base[i * 3 + 2]).mul(affine);
            assertClose(v.x, buf[i * 3]);
            assertClose(v.y, buf[i * 3 + 1]);
            assertClose(v.z, buf[i * 3 + 2]);
        }
    }

    @Test
    public void bothAreAppliedInShaderOrder() {
        var refSys = rotation(0, 1, 0, 27.5);
        refSys.setTranslation(4e7, -3e7, 5e7);
        var affine = rotation(0, 0, 1, 33);
        affine.scl(1.5);
        affine.setTranslation(2e3, -7e3, 4e3);
        var dataPos = new Vector3D(1e4, 2e4, 3e4);

        Object[] src = elements();
        double[] base = base(src);

        var ep = new ElementPositions();
        ep.compile(src, N);
        float[] buf = new float[N * 3];
        ep.compute(T, buf, 3, 0, 1, refSys, dataPos, affine);

        var t = transpose(refSys);
        var v = new Vector3D();
        for (int i = 0; i < N; i++) {
            v.set(base[i * 3], base[i * 3 + 1], base[i * 3 + 2]).mul(t).mul(affine).add(dataPos);
            assertClose(v.x, buf[i * 3]);
            assertClose(v.y, buf[i * 3 + 1]);
            assertClose(v.z, buf[i * 3 + 2]);
        }
    }

    @Test
    public void projectMatchesCompute() {
        var refSys = rotation(0, 1, 0, 27.5);
        var affine = rotation(0, 0, 1, 33);
        affine.setTranslation(2e3, -7e3, 4e3);
        var dataPos = new Vector3D(1e4, 2e4, 3e4);

        Object[] src = elements();
        var ep = new ElementPositions();
        ep.compile(src, N);
        float[] computed = new float[N * 3];
        ep.compute(T, computed, 3, 0, 1, refSys, dataPos, affine);
        float[] projected = new float[N * 3];
        ep.project(projected, 3, 0, 1, refSys, dataPos, affine);

        for (int i = 0; i < N * 3; i++) {
            assertEquals(computed[i], projected[i], 0);
        }
    }
}
