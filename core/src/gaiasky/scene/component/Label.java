package gaiasky.scene.component;

import com.artemis.Component;
import gaiasky.util.Constants;
import gaiasky.util.math.Vector3b;

public class Label extends Component {

    /**
     * Position of label.
     */
    protected Vector3b labelPosition;

    /**
     * Whether to draw 2D and 3D labels.
     */
    public boolean label, label2d;

    /**
     * Sets the position of the label, in parsecs and in the internal reference
     * frame.
     *
     * @param labelposition The position of the label in internal cartesian coordinates.
     */
    public void setLabelposition(double[] labelposition) {
        if (labelposition != null)
            this.labelPosition = new Vector3b(labelposition[0] * Constants.PC_TO_U, labelposition[1] * Constants.PC_TO_U, labelposition[2] * Constants.PC_TO_U);
    }
}
