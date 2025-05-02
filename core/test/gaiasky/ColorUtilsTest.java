/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky;

import com.badlogic.gdx.graphics.Color;
import net.jafama.FastMath;
import org.junit.Assert;
import org.junit.Test;

public class ColorUtilsTest {

    @Test
    public void test() {

        convert(0.1f, 0.2f, 0.3f, 0.4f);
        convert(0.0f, 0.0f, 0.0f, 0.0f);
        convert(1.0f, 1.0f, 1.0f, 1.0f);
        convert(3.0f, 3.0f, 0.0f, 0.0f);
        convert(0.123f, 0.774f, 0.947f, 1.0f);
        convert(0.71979f, 0.79023f, 1f, 1f);
        convert(1f, 0.9229006f, 0.8682735f, 1f);

        toBinary(0.15f, 0.398f, 0.43f, 0.8f);
        fromBinary(9385193);

    }

    private void fromBinary(int val) {
        Color color = new Color();
        Color.rgba8888ToColor(color, val);
        System.out.println(color.r + "," + color.g + "," + color.b + "," + color.a);
    }

    private  void toBinary(float r, float g, float b, float a) {
        float col = Color.toFloatBits(r, g, b, a);
        System.out.println(Integer.toBinaryString(Float.floatToIntBits(col)));
    }

    private void convert(float r, float g, float b, float a) {
        System.out.println("IN - r=" + r + " g=" + g + " b=" + b + " a=" + a);

        float packed = Color.toFloatBits(r, g, b, a);
        int i32 = Color.rgba8888(r, g, b, a);

        System.out.println(" Packed: " + packed);
        System.out.println(" Bin f32:" + Integer.toBinaryString(Float.floatToIntBits(packed)));
        System.out.println(" Int:    " + i32);
        System.out.println(" Bin i32:" + Integer.toBinaryString(i32));

        double packedd = packed;
        //packedd = -1.6947656946257677E38;

        Color out = new Color();
        Color.abgr8888ToColor(out, (float) packedd);

        System.out.println("OUT - r=" + out.r + " g=" + out.g + " b=" + out.b + " a=" + out.a);
        System.out.println();

        Assert.assertEquals(clamp(r), out.r, 0.01);
        Assert.assertEquals(clamp(g), out.g, 0.01);
        Assert.assertEquals(clamp(b), out.b, 0.01);
        Assert.assertEquals(clamp(a), out.a, 0.01);
    }

    private float clamp(float v) {
        return (float) FastMath.max(0.0, FastMath.min(1.0, v));
    }
}
