package gaia.cu9.ari.gaiaorbit.util.color;

import com.badlogic.gdx.graphics.Color;

public class ColorUtilsTest {

    public static void main(String[] args) {

        convert(0.1f, 0.2f, 0.3f, 0.4f);
        convert(0.0f, 0.0f, 0.0f, 0.0f);
        convert(1.0f, 1.0f, 1.0f, 1.0f);
        convert(3.0f, 3.0f, 0.0f, 0.0f);
        convert(0.123f, 0.774f, 0.947f, 1.0f);
        convert(0.71979f, 0.79023f, 1f, 1f);

    }

    private static void convert(float r, float g, float b, float a) {
        System.out.println("IN - r=" + r + " g=" + g + " b=" + b + " a=" + a);

        float packed = Color.toFloatBits(r, g, b, a);

        System.out.println(" Packed: " + packed);

        double packedd = packed;
        //packedd = -1.6947656946257677E38;


        Color out = new Color();
        Color.abgr8888ToColor(out, (float) packedd);

        System.out.println("OUT - r=" + out.r + " g=" + out.g + " b=" + out.b + " a=" + out.a);
        System.out.println();
    }
}
