/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.gaia.Attitude;
import gaiasky.util.gaia.GaiaAttitudeServer;
import gaiasky.util.math.Quaterniond;
import gaiasky.util.math.Vector3d;
import gaiasky.util.time.ITimeFrameProvider;

import java.util.Date;

/**
 * Gaia is a special entity, mainly because it has its own camera modes (FoV1/2/1&2, Gaia camera)
 */
public class Gaia extends Satellite {

    public Vector3d unrotatedPos;
    Attitude attitude;
    Quaterniond quaterniond;
    Quaternion quaternion;

    public Gaia() {
        super();
        unrotatedPos = new Vector3d();
        quaterniond = new Quaterniond();
        quaternion = new Quaternion();
    }

    @Override
    public void initialize() {
        super.initialize();
    }

    @Override
    public void doneLoading(AssetManager manager) {
        super.doneLoading(manager);
        EventManager.publish(Event.GAIA_LOADED, this, this);
    }

    @Override
    protected void forceUpdatePosition(ITimeFrameProvider time, boolean force) {
        super.forceUpdatePosition(time, force);
        if (time.getHdiff() != 0 || force) {
            unrotatedPos.set(pos);
            // Undo rotation
            unrotatedPos.mul(Coordinates.eqToEcl()).rotate(-AstroUtils.getSunLongitude(time.getTime()) - 180, 0, 1, 0);
            attitude = GaiaAttitudeServer.instance.getAttitude(new Date(time.getTime().toEpochMilli()));
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
                Quaterniond attQuat = attitude.getQuaternion();
                quaterniond.set(attQuat.x, attQuat.y, attQuat.z, attQuat.w);
                quaternion.set((float) quaterniond.x, (float) quaterniond.y, (float) quaterniond.z, (float) quaterniond.w);

                // Update orientation
                orientation.idt().rotate(quaterniond).rotate(0, 0, 1, 180);

                matauxd.set(localTransform).mul(orientation);
                matauxd.putIn(localTransform);

            }
        } else {
            localTransform.set(this.localTransform);
        }

    }

    public Quaterniond getOrientationQuaternion() {
        return attitude.getQuaternion();
    }

}
