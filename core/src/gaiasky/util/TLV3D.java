package gaiasky.util;

import gaiasky.util.math.Vector3d;

public class TLV3D extends ThreadLocal<Vector3d> {
    @Override
    protected Vector3d initialValue() {
        return new Vector3d();
    }
}
