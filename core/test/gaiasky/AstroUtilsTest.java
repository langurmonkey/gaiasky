package gaiasky;

import gaiasky.util.coord.AstroUtils;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.math.Vector3D;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;

/**
 * Tests the astronomical algorithms in the class {@link AstroUtils}.
 */
public class AstroUtilsTest {

    @Test
    public void testJulianDates() {
        testJulianDate("0099-12-19T10:00:00.00Z", 1757569.9166667);
        testJulianDate("1099-12-19T12:00:00.00Z", 2122820.0);
        testJulianDate("1993-01-01T00:00:00.00Z", 2448988.5);
        testJulianDate("1999-12-19T12:00:00.00Z", 2451532.0);
        testJulianDate("2000-01-01T00:00:00.00Z", 2451544.5);
        testJulianDate("2010-01-01T00:00:00.00Z", 2455197.5);
        testJulianDate("2013-01-01T00:30:00.00Z", 2456293.520833);
    }

    public void testJulianDate(String dateUTC, double expected) {
        var instant = Instant.parse(dateUTC);
        double jd = AstroUtils.getJulianDate(instant);
        Assert.assertEquals(expected, jd, 0.00001);
    }


    @Test
    public void testProperMotionConversions() {
        testProperMotionConversion(21.85, -4.38, 30.0, 7.26, -17.21, 3.91);
        testProperMotionConversion(-7.51, -3.01, 21.0, 203.49, -64.055, 12.02);
        testProperMotionConversion(46.55, -25.58, 143.064, 25.86, 44.055, 2.89);
    }

    private void testProperMotionConversion(double muAlphaStar, double muDelta, double rv, double ra, double dec, double parallax) {
        Vector3D aux1 = new Vector3D();
        Vector3D aux2 = new Vector3D();

        double distPc = 1000d / parallax;
        Coordinates.properMotionsToCartesian(muAlphaStar, muDelta, rv, ra, dec, distPc, aux1);

        Coordinates.cartesianToProperMotions(aux1.x, aux1.y, aux1.z, ra, dec, distPc, aux2);

        Assert.assertEquals(muAlphaStar, aux2.x, 0.1);
        Assert.assertEquals(muDelta, aux2.y, 0.1);
        Assert.assertEquals(rv, aux2.z, 0.1);
    }
}
