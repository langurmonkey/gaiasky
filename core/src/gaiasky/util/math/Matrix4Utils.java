package gaiasky.util.math;

import com.badlogic.gdx.math.Matrix4;

import java.nio.FloatBuffer;

public class Matrix4Utils {

    public static FloatBuffer put(Matrix4 m, FloatBuffer buffer) {
        buffer.put(m.val);
        return buffer;
    }
}
