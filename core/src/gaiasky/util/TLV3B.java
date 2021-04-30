package gaiasky.util;

import gaiasky.util.math.Vector3b;

public class TLV3B extends ThreadLocal<Vector3b> {
    @Override
    protected Vector3b initialValue() {
        return new Vector3b();
    }
}
