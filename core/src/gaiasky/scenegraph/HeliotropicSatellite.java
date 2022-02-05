/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import gaiasky.GaiaSky;
import gaiasky.scenegraph.component.ITransform;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.gaia.Attitude;
import gaiasky.util.math.Quaterniond;
import gaiasky.util.math.Vector3d;
import gaiasky.util.time.ITimeFrameProvider;

public class HeliotropicSatellite extends Satellite {

    public Vector3d unrotatedPos;
    Attitude attitude;
    Quaterniond quaterniond;
    Quaternion quaternion;

    public HeliotropicSatellite() {
        super();
        unrotatedPos = new Vector3d();
        quaternion = new Quaternion();
        quaterniond = new Quaterniond();
    }

    @Override
    protected void forceUpdatePosition(ITimeFrameProvider time, boolean force) {
        super.forceUpdatePosition(time, force);
        if (time.getHdiff() != 0 || force) {
            unrotatedPos.set(pos);
            // Undo rotation
            unrotatedPos.mul(Coordinates.eqToEcl()).rotate(-AstroUtils.getSunLongitude(time.getTime()) - 180, 0, 1, 0);
        }
    }

    @Override
    public Vector3d getUnrotatedPos() {
        return unrotatedPos;
    }

    public void setToLocalTransform(float sizeFactor, Matrix4 localTransform, boolean forceUpdate) {
        if (sizeFactor != 1 || forceUpdate) {
            translation.getMatrix(localTransform).scl(size * sizeFactor);
            if (attitude != null) {
                quaterniond = attitude.getQuaternion();
                quaternion.set((float) quaterniond.x, (float) quaterniond.y, (float) quaterniond.z, (float) quaterniond.w);
            } else {
                quaterniond.setFromAxis(0, 1, 0, AstroUtils.getSunLongitude(GaiaSky.instance.time.getTime()));
            }

            // Update orientation
            orientation.idt().rotate(quaterniond);

            matauxd.set(localTransform).mul(orientation);
            matauxd.putIn(localTransform);

        } else {
            localTransform.set(this.localTransform);
        }

        // Apply transformations
        if (transformations != null)
            for (ITransform tc : transformations)
                tc.apply(localTransform);
    }

}
