package gaiasky.util;

import com.badlogic.gdx.math.Vector3;

public class TLV3 extends ThreadLocal<Vector3> {
    @Override
    protected Vector3 initialValue() {
        return new Vector3();
    }
}
