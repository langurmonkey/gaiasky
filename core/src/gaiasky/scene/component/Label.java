package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import gaiasky.util.Constants;
import gaiasky.util.math.Vector3b;

public class Label implements Component {

    /**
     * Position of label.
     */
    public Vector3b labelPosition;

    /**
     * Whether to draw 2D and 3D labels.
     */
    public boolean label, label2d;

    /**
     * Sets the position of the label, in parsecs and in the internal reference
     * frame.
     *
     * @param labelPosition The position of the label in internal cartesian coordinates.
     */
    public void setLabelPosition(double[] labelPosition) {
        if (labelPosition != null)
            this.labelPosition = new Vector3b(labelPosition[0] * Constants.PC_TO_U, labelPosition[1] * Constants.PC_TO_U, labelPosition[2] * Constants.PC_TO_U);
    }

    public void setLabelposition(double[] labelPosition) {
        setLabelPosition(labelPosition);
    }
}
