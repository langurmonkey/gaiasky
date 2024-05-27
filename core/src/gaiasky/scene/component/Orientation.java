package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;
import com.badlogic.gdx.assets.AssetManager;
import gaiasky.scene.record.RotationComponent;
import gaiasky.util.math.Vector3d;

public class Orientation implements Component, ICopy {

    /** Holds information about the rotation of the body **/
    public RotationComponent rigidRotation;

    public QuaternionOrientation quaternionOrientation;

    /**
     * Sets the rotation period in hours
     */
    public void setRotation(RotationComponent rigidRotation) {
        this.rigidRotation = rigidRotation;
    }

    public void setRigidRotation(RotationComponent rigidRotation) {
        setRotation(rigidRotation);
    }

    public void updateRotation(RotationComponent rigidRotation) {
        if (this.rigidRotation != null) {
            this.rigidRotation.updateWith(rigidRotation);
        } else {
            this.rigidRotation = rigidRotation;
        }
    }

    public void updateRigidRotation(RotationComponent rigidRotation) {
        updateRotation(rigidRotation);
    }

    public void setOrientationProvider(String provider) {
        if (this.quaternionOrientation == null) {
            this.quaternionOrientation = new QuaternionOrientation();
        }

        this.quaternionOrientation.setProvider(provider);
    }

    public void setProvider(String provider) {
        setOrientationProvider(provider);
    }

    public void setAttitudeProvider(String provider) {
        setOrientationProvider(provider);
    }

    public void setOrientationSource(String source) {
        if (this.quaternionOrientation == null) {
            this.quaternionOrientation = new QuaternionOrientation();
        }

        this.quaternionOrientation.orientationSource = source;
    }

    public void setAttitudeLocation(String source) {
        setOrientationSource(source);
    }

    @Override
    public Component getCopy(Engine engine) {
        var copy = engine.createComponent(Orientation.class);
        if (rigidRotation != null) {
            copy.rigidRotation = rigidRotation.clone();
        } else {
            copy.rigidRotation = null;
        }
        if (quaternionOrientation != null) {
            copy.quaternionOrientation = quaternionOrientation.clone();
        } else {
            copy.quaternionOrientation = null;
        }
        return copy;
    }

    public void initialize(AssetManager manager) {
        if (quaternionOrientation != null) {
            quaternionOrientation.initialize(manager);
        }

    }

    public void setUp(AssetManager manager) {
        if (quaternionOrientation != null) {
            quaternionOrientation.setUp(manager);
        }
    }

    public Vector3d getNonRotatedPos() {
        if (quaternionOrientation != null) {
            return quaternionOrientation.nonRotatedPos;
        }
        return null;
    }
}
