package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import gaiasky.util.math.Vector3d;

import java.text.DecimalFormat;

public class Ruler implements Component {
    public String name0, name1;
    public final double[] pos0 = new double[3];
    public final double[] pos1 = new double[3];
    public final Vector3d p0 = new Vector3d();
    public final Vector3d p1 = new Vector3d();
    public final Vector3d m = new Vector3d();
    public boolean rulerOk = false;
    public String dist;
    public DecimalFormat nf = new DecimalFormat("0.#########E0");
}
