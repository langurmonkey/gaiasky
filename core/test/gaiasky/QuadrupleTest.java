package gaiasky;

import gaiasky.util.math.Quadruple;
import gaiasky.util.math.QuadrupleMath;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link Quadruple} and {@link QuadrupleMath}.
 */
public class QuadrupleTest {
    private static final double delta = 1.0e-9;

    @Test
    public void testPiConstant() {
        Quadruple pi = QuadrupleMath.pi();
        // Compare pi to known double value of Math.PI
        Quadruple expected = Quadruple.valueOf(Double.toString(Math.PI));
        Quadruple diff = pi.subtract(expected)
                .abs();
        assertTrue("PI constant should match Math.PI within tolerance; diff=" + diff,
                   diff.compareTo(delta) <= 0);
    }

    @Test
    public void testPiRelationships() {
        // pi2() should equal pi * 2
        Quadruple twoPi = QuadrupleMath.pi2();
        Quadruple expectedTwoPi = QuadrupleMath.pi()
                .multiply(Quadruple.valueOf(2));
        assertEquals(expectedTwoPi, twoPi);

        // piOver2() should equal pi / 2
        Quadruple halfPi = QuadrupleMath.piOver2();
        Quadruple expectedHalfPi = QuadrupleMath.pi()
                .divide(Quadruple.valueOf(2));
        assertEquals(expectedHalfPi, halfPi);
    }

    @Test
    public void testAtanBasicValues() {
        // atan(0) == 0
        assertEquals(0.0, QuadrupleMath.atan(Quadruple.zero())
                .doubleValue(), delta);

        // atan(1) == pi/4
        Quadruple result = QuadrupleMath.atan(Quadruple.one());
        assertEquals(Math.atan(1.0), result.doubleValue(), delta);

        Quadruple negResult = QuadrupleMath.atan(Quadruple.one()
                                                         .negate());
        assertEquals(Math.atan(-1.0), negResult.doubleValue(), delta);
    }

    @Test
    public void testAtanLargeValue() {
        // For large x, atan(x) ~ pi/2
        Quadruple large = Quadruple.valueOf("1e10");
        Quadruple result = QuadrupleMath.atan(large);
        Quadruple expected = QuadrupleMath.piOver2();
        assertEquals(expected.doubleValue(), result.doubleValue(), delta);
    }

    @Test
    public void testAtan2Quadrants() {
        Quadruple one = Quadruple.one();
        Quadruple minusOne = Quadruple.one()
                .negate();

        // Quadrant I: y=1, x=1 => pi/4
        Quadruple q1 = QuadrupleMath.atan2(one, one);
        Quadruple expectedQ1 = QuadrupleMath.piOver2()
                .divide(Quadruple.valueOf(2));
        assertEquals(expectedQ1.doubleValue(), q1.doubleValue(), delta);

        // Quadrant II: y=1, x=-1 => 3*pi/4
        Quadruple q2 = QuadrupleMath.atan2(one, minusOne);
        Quadruple expectedQ2 = QuadrupleMath.pi()
                .subtract(expectedQ1);
        assertEquals(expectedQ2.doubleValue(), q2.doubleValue(), delta);

        // Quadrant III: y=-1, x=-1 => -3*pi/4
        Quadruple q3 = QuadrupleMath.atan2(minusOne, minusOne);
        Quadruple expectedQ3 = expectedQ2.negate();
        assertEquals(expectedQ3.doubleValue(), q3.doubleValue(), delta);

        // Quadrant IV: y=-1, x=1 => -pi/4
        Quadruple q4 = QuadrupleMath.atan2(minusOne, one);
        Quadruple expectedQ4 = expectedQ1.negate();
        assertEquals(expectedQ4.doubleValue(), q4.doubleValue(), delta);
    }

    @Test
    public void testAtan2Axes() {
        // x=0, y>0 => pi/2
        assertTrue("atan2(1,0) should be pi/2",
                   QuadrupleMath.atan2(Quadruple.one(), Quadruple.zero())
                           .subtract(QuadrupleMath.piOver2())
                           .abs()
                           .compareTo(delta) <= 0);

        // x=0, y<0 => -pi/2
        assertTrue("atan2(-1,0) should be -pi/2",
                   QuadrupleMath.atan2(Quadruple.one()
                                               .negate(), Quadruple.zero())
                           .subtract(QuadrupleMath.piOver2()
                                             .negate())
                           .abs()
                           .compareTo(delta) <= 0);

        // x=0, y=0 => 0
        assertEquals(Quadruple.zero(), QuadrupleMath.atan2(Quadruple.zero(), Quadruple.zero()));
    }

    @Test
    public void testAdd() {
        var a = 124543.334;
        var b = 343499904.3434534523;
        var f128a = new Quadruple(a);
        var f128b = new Quadruple(b);
        assertEquals(a + b, f128a.add(f128b)
                .doubleValue(), delta);
    }

    @Test
    public void testSubtract() {
        var a = 124543.334;
        var b = 343499904.3434534523;
        var f128a = new Quadruple(a);
        var f128b = new Quadruple(b);
        assertEquals(a - b, f128a.subtract(f128b)
                .doubleValue(), delta);
    }

    @Test
    public void testMultiply() {
        var a = 124543.334;
        var b = 343499904.3434534523;
        var f128a = new Quadruple(a);
        var f128b = new Quadruple(b);
        assertEquals(a * b, f128a.multiply(f128b)
                .doubleValue(), delta);
    }

    @Test
    public void testDivide() {
        var a = 124543.334;
        var b = 343499904.3434534523;
        var f128a = new Quadruple(a);
        var f128b = new Quadruple(b);
        assertEquals(a / b, f128a.divide(f128b)
                .doubleValue(), delta);
    }
}
