package gaiasky;

import gaiasky.util.math.QuadrupleImmutable;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link QuadrupleImmutable}.
 */
public class QuadrupleImmutableTest {
    private static final double delta = 1.0e-10;

    @Test
    public void testToString() {
        var f128 = QuadrupleImmutable.from(123.42534534564564564e4);
        assertEquals("1.234253453456456540152430534362792968750e+06", f128.toString());
    }

    @Test
    public void testAdd() {
        var a = 124543.334;
        var b = 343499904.3434534523;
        var f128a = QuadrupleImmutable.from(a);
        var f128b = QuadrupleImmutable.from(b);
        assertEquals(a + b, f128a.add(f128b)
                .doubleValue(), delta);

        f128a = QuadrupleImmutable.from(-a);
        f128b = QuadrupleImmutable.from(b);
        assertEquals(-a + b, f128a.add(f128b)
                .doubleValue(), delta);

    }

    @Test
    public void testSubtract() {
        var a = 124543.334;
        var b = 343499904.3434534523;
        var f128a = QuadrupleImmutable.from(a);
        var f128b = QuadrupleImmutable.from(b);
        assertEquals(a - b, f128a.subtract(f128b)
                .doubleValue(), delta);

        f128a = QuadrupleImmutable.from(-a);
        f128b = QuadrupleImmutable.from(b);
        assertEquals(-a - b, f128a.subtract(f128b)
                .doubleValue(), delta);
    }

    @Test
    public void testMultiply() {
        var a = 124543.334;
        var b = 343499904.3434534523;
        var f128a = QuadrupleImmutable.from(a);
        var f128b = QuadrupleImmutable.from(b);
        assertEquals(a * b, f128a.multiply(f128b)
                .doubleValue(), delta);

        f128a = QuadrupleImmutable.from(-a);
        f128b = QuadrupleImmutable.from(b);
        assertEquals(-a * b, f128a.multiply(f128b)
                .doubleValue(), delta);
    }

    @Test
    public void testDivide() {
        var a = 124543.334;
        var b = 343499904.3434534523;
        var f128a = QuadrupleImmutable.from(a);
        var f128b = QuadrupleImmutable.from(b);
        assertEquals(a / b, f128a.divide(f128b)
                .doubleValue(), delta);

        f128a = QuadrupleImmutable.from(-a);
        f128b = QuadrupleImmutable.from(b);
        assertEquals(-a / b, f128a.divide(f128b)
                .doubleValue(), delta);
    }
}
