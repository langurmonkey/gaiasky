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

    public String getName0() {
        return name0;
    }

    public void setName0(String name0) {
        this.name0 = name0;
    }

    public String getName1() {
        return name1;
    }

    public void setName1(String name1) {
        this.name1 = name1;
    }

    public boolean rulerOk() {
        return rulerOk;
    }

    /**
     * Returns true if the ruler is attached to at least one object.
     *
     * @return Ture if the ruler is attached.
     */
    public boolean hasAttached() {
        return name0 != null || name1 != null;
    }

    public boolean hasObject0() {
        return name0 != null && !name0.isEmpty();
    }

    public boolean hasObject1() {
        return name1 != null && !name1.isEmpty();
    }
}
