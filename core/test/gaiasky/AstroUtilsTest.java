package gaiasky;

import gaiasky.util.coord.AstroUtils;
import gaiasky.util.math.Vector3d;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the astronomical algorithms in the class {@link AstroUtils}.
 */
public class AstroUtilsTest {
    @Before
    public void setUp() {

    }

    @After
    public void tearDown() {

    }

    @Test
    public void testProperMotionConversion() {
        testProperMotionConversion(21.85, -4.38, 30.0, 7.26, -17.21, 3.91);
        testProperMotionConversion( -7.51, -3.01, 21.0, 203.49, -64.055, 12.02);
        testProperMotionConversion( 46.55, -25.58, 143.064, 25.86, 44.055, 2.89);
    }

    private void testProperMotionConversion(double muAlphaStar, double muDelta, double rv, double ra, double dec, double parallax) {
        Vector3d aux1 = new Vector3d();
        Vector3d aux2 = new Vector3d();

        double distPc = 1000d / parallax;
        AstroUtils.properMotionsToCartesian(muAlphaStar, muDelta, rv, ra, dec, distPc, aux1);

        AstroUtils.cartesianToProperMotions(aux1.x, aux1.y, aux1.z, ra, dec, distPc, aux2);

        Assert.assertEquals(muAlphaStar, aux2.x, 0.1);
        Assert.assertEquals(muDelta, aux2.y, 0.1);
        Assert.assertEquals(rv, aux2.z, 0.1);
    }
}
