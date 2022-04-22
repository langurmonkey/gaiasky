package gaiasky.scene.component;

import com.artemis.Component;
import com.badlogic.gdx.math.Matrix4;
import gaiasky.scenegraph.component.RotationComponent;

public class ParentOrientation extends Component {
    public static final double TH_ANGLE_NONE = Model.TH_ANGLE_POINT / 1e18;
    public static final double TH_ANGLE_POINT = Model.TH_ANGLE_POINT / 3.3e10;
    public static final double TH_ANGLE_QUAD = Model.TH_ANGLE_POINT / 8;

    public boolean parentOrientation = false;
    public boolean hidden = false;
    public Matrix4 orientationf;
    public RotationComponent parentrc;

    public double THRESHOLD_NONE() {
        return TH_ANGLE_NONE;
    }

    public double THRESHOLD_POINT() {
        return TH_ANGLE_POINT;
    }

    public double THRESHOLD_QUAD() {
        return TH_ANGLE_QUAD;
    }
}
