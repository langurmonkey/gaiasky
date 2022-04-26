package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import gaiasky.scenegraph.component.ITransform;

public class AffineTransformations implements Component {

    /** Affine transformations, applied each cycle **/
    public ITransform[] transformations;

    public void setTransformations(Object[] transformations) {
        this.transformations = new ITransform[transformations.length];
        for (int i = 0; i < transformations.length; i++)
            this.transformations[i] = (ITransform) transformations[i];
    }
}
