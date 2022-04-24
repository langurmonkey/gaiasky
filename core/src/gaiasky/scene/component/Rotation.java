package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import gaiasky.scenegraph.component.RotationComponent;

public class Rotation implements Component {

    /** Holds information about the rotation of the body **/
    public RotationComponent rc;

    /**
     * Sets the rotation period in hours
     */
    public void setRotation(RotationComponent rc) {
        this.rc = rc;
    }
}
