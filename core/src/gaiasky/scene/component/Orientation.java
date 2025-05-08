package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;
import com.badlogic.gdx.assets.AssetManager;
import gaiasky.scene.record.RotationComponent;
import gaiasky.util.math.Vector3D;

/**
 * The orientation is either a rigid rotation represented by {@link RotationComponent}, or
 * a quaternion orientation, represented by {@link AttitudeComponent}.
 */
public class Orientation implements Component, ICopy {

    /** Holds information about the rotation of the body, represented as rotation parameters. **/
    public RotationComponent rotationComponent;
    /** Holds the guy that returns a quaternion for each time. **/
    public AttitudeComponent attitudeComponent;

    /**
     * Sets the rotation period in hours
     */
    public void setRotation(RotationComponent rigidRotation) {
        this.rotationComponent = rigidRotation;
    }

    public void setRigidRotation(RotationComponent rotationComponent) {
        setRotation(rotationComponent);
    }

    public void updateRotation(RotationComponent rigidRotation) {
        if (this.rotationComponent != null) {
            this.rotationComponent.updateWith(rigidRotation);
        } else {
            this.rotationComponent = rigidRotation;
        }
    }

    public void updateRigidRotation(RotationComponent rigidRotation) {
        updateRotation(rigidRotation);
    }

    public void setOrientationProvider(String provider) {
        if (this.attitudeComponent == null) {
            this.attitudeComponent = new AttitudeComponent();
        }

        this.attitudeComponent.setProvider(provider);
    }

    public void setProvider(String provider) {
        setOrientationProvider(provider);
    }

    public void setAttitudeProvider(String provider) {
        setOrientationProvider(provider);
    }

    public void setOrientationSource(String source) {
        if (this.attitudeComponent == null) {
            this.attitudeComponent = new AttitudeComponent();
        }

        this.attitudeComponent.orientationSource = source;
    }

    public void setAttitudeLocation(String source) {
        setOrientationSource(source);
    }

    @Override
    public Component getCopy(Engine engine) {
        var copy = engine.createComponent(Orientation.class);
        copy.rotationComponent = rotationComponent;
        if (attitudeComponent != null) {
            copy.attitudeComponent = attitudeComponent.copy();
        }
        return copy;
    }

    public void initialize(AssetManager manager) {
        if (attitudeComponent != null) {
            attitudeComponent.initialize(manager);
        }

    }

    public void setUp(AssetManager manager) {
        if (attitudeComponent != null) {
            attitudeComponent.setUp(manager);
        }
    }

    public Vector3D getNonRotatedPos() {
        if (attitudeComponent != null) {
            return attitudeComponent.nonRotatedPos;
        }
        return null;
    }
}
