package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import gaiasky.scenegraph.component.ModelComponent;

public class Model implements Component {

    /** The model **/
    public ModelComponent model;

    public void setModel(ModelComponent model) {
        this.model = model;
    }
}
