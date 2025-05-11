package gaiasky;

import gaiasky.util.math.Float128;
import gaiasky.util.math.Quadruple;
import gaiasky.util.math.QuadrupleMath;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link Float128}.
 */
public class Float128Test {
    private static final double delta = 1.0e-10;

    @Test
    public void testAdd() {
        var a = 124543.334;
        var b = 343499904.3434534523;
        var f128a = Float128.from(a);
        var f128b = Float128.from(b);
        assertEquals(a + b, f128a.add(f128b)
                .doubleValue(), delta);
    }

    @Test
    public void testSubtract() {
        var a = 124543.334;
        var b = 343499904.3434534523;
        var f128a = Float128.from(a);
        var f128b = Float128.from(b);
        assertEquals(a - b, f128a.subtract(f128b)
                .doubleValue(), delta);
    }

    @Test
    public void testMultiply() {
        var a = 124543.334;
        var b = 343499904.3434534523;
        var f128a = Float128.from(a);
        var f128b = Float128.from(b);
        assertEquals(a * b, f128a.multiply(f128b)
                .doubleValue(), delta);
    }

    @Test
    public void testDivide() {
        var a = 124543.334;
        var b = 343499904.3434534523;
        var f128a = Float128.from(a);
        var f128b = Float128.from(b);
        assertEquals(a / b, f128a.divide(f128b)
                .doubleValue(), delta);
    }
}
