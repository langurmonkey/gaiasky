package gaiasky.scene.record;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Quaterniond;
import gaiasky.util.math.Vector3d;

public class QuaternionTransform implements ITransform {

    Quaterniond quaternion;
    Quaternion quatFloat;

    @Override
    public void apply(Matrix4 mat) {
        if (quaternion != null) {
            if (quatFloat == null) {
                quatFloat = new Quaternion();
            }
            quatFloat.set((float) quaternion.x, (float) quaternion.y, (float) quaternion.z, (float) quaternion.w);
            mat.rotate(quatFloat);
        }
    }

    @Override
    public void apply(Matrix4d mat) {
        if (quaternion != null)
            mat.rotate(quaternion);
    }

    public void setQuaternion(Quaterniond quaternion) {
        this.quaternion = new Quaterniond(quaternion);
    }

    public void setQuaternion(Vector3d axis, double angle) {
        this.quaternion = new Quaterniond(axis, angle);
    }

    public void setQuaternion(double axisX, double axisY, double axisZ, double angle) {
        this.quaternion = new Quaterniond(new Vector3d(axisX, axisY, axisZ), angle);
    }

    public void setQuaternion(double[] xyzw) {
        this.quaternion = new Quaterniond(xyzw[0], xyzw[1], xyzw[2], xyzw[3]);
    }
}
