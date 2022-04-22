package gaiasky.scene.component;

import com.artemis.Component;
import gaiasky.render.ShadowMapImpl;
import gaiasky.scenegraph.component.ITransform;
import gaiasky.scenegraph.component.ModelComponent;

import java.util.Arrays;
import java.util.List;

public class Model extends Component {

    public static final double TH_ANGLE_POINT = Math.toRadians(0.30);

    /** The model **/
    public ModelComponent model;

    public void setModel(ModelComponent model) {
        this.model = model;
    }
}
