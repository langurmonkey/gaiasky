package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;
import gaiasky.scene.record.RotationComponent;

public class Rotation implements Component, ICopy {

    /** Holds information about the rotation of the body **/
    public RotationComponent rc;

    /**
     * Sets the rotation period in hours
     */
    public void setRotation(RotationComponent rc) {
        this.rc = rc;
    }

    @Override
    public Component getCopy(Engine engine) {
        var copy = engine.createComponent(this.getClass());
        copy.rc = rc;
        return copy;
    }
}
