package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import gaiasky.util.math.Vector3d;

public class Axis implements Component {
    public static final double LINE_SIZE_RAD = Math.tan(Math.toRadians(2.9));
    public Vector3d o = new Vector3d();
    public Vector3d x = new Vector3d();
    public Vector3d y = new Vector3d();
    public Vector3d z = new Vector3d();
    // Base vectors
    public Vector3d b0 = new Vector3d(1, 0, 0);
    public Vector3d b1 = new Vector3d(0, 1, 0);
    public Vector3d b2 = new Vector3d(0, 0, 1);

    // RGBA colors for each of the bases XYZ -> [3][3]
    public float[][] axesColors;

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
