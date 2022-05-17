/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import gaiasky.GaiaSky;
import gaiasky.data.AssetBean;
import gaiasky.data.attitude.IAttitudeServer;
import gaiasky.data.util.AttitudeLoader.AttitudeLoaderParameters;
import gaiasky.scenegraph.component.ITransform;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.gaia.IAttitude;
import gaiasky.util.math.Quaterniond;
import gaiasky.util.math.Vector3d;
import gaiasky.util.time.ITimeFrameProvider;

import java.util.Date;

/**
 * A heliotropic satellite, which maintains the position relative to the sun.
 */
public class HeliotropicSatellite extends Satellite {

    protected String provider;
    protected String attitudeLocation;
    private IAttitudeServer attitudeServer;
    private IAttitude attitude;

    public Vector3d unrotatedPos;

    private Quaterniond quaterniond;
    private Quaternion quaternion;

    public HeliotropicSatellite() {
        super();
        unrotatedPos = new Vector3d();
        quaterniond = new Quaterniond();
        quaternion = new Quaternion();
        quaterniond = new Quaterniond();
    }

    public void initialize() {
        super.initialize();
        if (attitudeLocation != null && !attitudeLocation.isBlank()) {
            AssetBean.manager().load(attitudeLocation, IAttitudeServer.class, new AttitudeLoaderParameters(provider));
        }
    }

    @Override
    public void doneLoading(AssetManager manager) {
        super.doneLoading(manager);
        if (attitudeLocation != null && manager.isLoaded(attitudeLocation)) {
            attitudeServer = manager.get(attitudeLocation);
        }
    }

    @Override
    protected void forceUpdatePosition(ITimeFrameProvider time, boolean force) {
        super.forceUpdatePosition(time, force);
        if (time.getHdiff() != 0 || force) {
            unrotatedPos.set(pos);
            // Undo rotation
            unrotatedPos.mul(Coordinates.eqToEcl())
                    .rotate(-AstroUtils.getSunLongitude(time.getTime()) - 180, 0, 1, 0);
            if (attitudeServer != null) {
                attitude = attitudeServer.getAttitude(new Date(time.getTime().toEpochMilli()));
            }
        }
    }

    @Override
    public Vector3d getUnrotatedPos() {
        return unrotatedPos;
    }

    public void setToLocalTransform(float sizeFactor, Matrix4 localTransform, boolean forceUpdate) {
        if (sizeFactor != 1 || forceUpdate) {
            translation.setToTranslation(localTransform).scl(size * sizeFactor);
            if (attitude != null) {
                quaterniond = attitude.getQuaternion();
                quaternion.set((float) quaterniond.x, (float) quaterniond.y, (float) quaterniond.z, (float) quaterniond.w);
            } else {
                quaterniond.setFromAxis(0, 1, 0, AstroUtils.getSunLongitude(GaiaSky.instance.time.getTime()));
            }

            // Update orientation
            orientation.idt().rotate(quaterniond);
            if (attitude != null) {
                orientation.rotate(0, 0, 1, 180);
            }

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

    public Quaterniond getOrientationQuaternion() {
        if (attitude == null) {
            return null;
        }
        return attitude.getQuaternion();
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public void setAttitudeLocation(String attitudeLocation) {
        this.attitudeLocation = attitudeLocation;
    }

    public IAttitudeServer getAttitudeServer() {
        return attitudeServer;
    }

}
