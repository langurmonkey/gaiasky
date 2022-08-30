package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.utils.Array;
import gaiasky.util.math.Vector3d;
import gaiasky.util.tree.IPosition;

public class Constel implements Component {
    public double deltaYears;

    public float alpha;
    public boolean allLoaded = false;
    public Vector3d posd;

    /** List of pairs of HIP identifiers **/
    public Array<int[]> ids;
    /**
     * The lines themselves as pairs of positions
     **/
    public IPosition[][] lines;

    public void setIds(double[][] ids) {
        this.ids = new Array<>(ids.length);
        for(double[] dd : ids) {
            int[] ii = new int[dd.length];
            for(int j =0; j < dd.length; j++)
                ii[j] = (int) Math.round(dd[j]);
            this.ids.add(ii);
        }
    }
}
