package gaiasky.scene.component;

import com.artemis.Component;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector3d;

public class Axis extends Component {
    private static final double LINE_SIZE_RAD = Math.tan(Math.toRadians(2.9));
    public Vector3d o;
    public Vector3d x;
    public Vector3d y;
    public Vector3d z;
    private Vector3d b0, b1, b2;

    // RGBA colors for each of the bases XYZ -> [3][3]
    private float[][] axesColors;

    public void setAxesColors(double[][] colors) {
        axesColors = new float[3][3];
        axesColors[0][0] = (float) colors[0][0];
        axesColors[0][1] = (float) colors[0][1];
        axesColors[0][2] = (float) colors[0][2];

        axesColors[1][0] = (float) colors[1][0];
        axesColors[1][1] = (float) colors[1][1];
        axesColors[1][2] = (float) colors[1][2];

        axesColors[2][0] = (float) colors[2][0];
        axesColors[2][1] = (float) colors[2][1];
        axesColors[2][2] = (float) colors[2][2];
    }
}
