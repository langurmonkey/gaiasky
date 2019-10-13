/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.scenegraph;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.util.coord.AstroUtils;
import gaia.cu9.ari.gaiaorbit.util.coord.Coordinates;
import gaia.cu9.ari.gaiaorbit.util.gaia.Attitude;
import gaia.cu9.ari.gaiaorbit.util.gaia.GaiaAttitudeServer;
import gaia.cu9.ari.gaiaorbit.util.math.Matrix4d;
import gaia.cu9.ari.gaiaorbit.util.math.Quaterniond;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;

import java.util.Date;

public class Gaia extends Satellite {

    public Vector3d unrotatedPos;
    Attitude attitude;
    Quaterniond quaterniond;
    Quaternion quaternion;
    Matrix4d auxm;

    public Gaia() {
        super();
        unrotatedPos = new Vector3d();
        quaternion = new Quaternion();
        auxm = new Matrix4d();
    }

    @Override
    public void initialize() {
        super.initialize();
    }

    @Override
    public void doneLoading(AssetManager manager) {
        super.doneLoading(manager);
        EventManager.instance.post(Events.GAIA_LOADED, this);
    }

    @Override
    protected void forceUpdatePosition(ITimeFrameProvider time, boolean force) {
        super.forceUpdatePosition(time, force);
        if (time.getDt() != 0 || force) {
            unrotatedPos.set(pos);
            // Undo rotation
            unrotatedPos.mul(Coordinates.eqToEcl()).rotate(-AstroUtils.getSunLongitude(time.getTime()) - 180, 0, 1, 0);
            attitude = GaiaAttitudeServer.instance.getAttitude(new Date(time.getTime().toEpochMilli()));
        }
    }



    public void setToLocalTransform(float sizeFactor, Matrix4 localTransform, boolean forceUpdate) {
        if (sizeFactor != 1 || forceUpdate) {
            translation.getMatrix(localTransform).scl(size * sizeFactor);
            if (attitude != null) {
                quaterniond = attitude.getQuaternion();
                quaternion.set((float) quaterniond.x, (float) quaterniond.y, (float) quaterniond.z, (float) quaterniond.w);

                // Update orientation
                orientation.idt().rotate(quaterniond).rotate(0, 0, 1, 180);

                auxm.set(localTransform).mul(orientation);
                auxm.putIn(localTransform);

            }
        } else {
            localTransform.set(this.localTransform);
        }

    }

    public Quaterniond getOrientationQuaternion() {
        return attitude.getQuaternion();
    }

}
