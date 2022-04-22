package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import gaiasky.scenegraph.component.ModelComponent;

public class Model implements Component {

    public static final double TH_ANGLE_POINT = Math.toRadians(0.30);

    /** The model **/
    public ModelComponent model;

    public void setModel(ModelComponent model) {
        this.model = model;
    }
}
