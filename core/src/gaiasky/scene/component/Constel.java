package gaiasky.scene.component;

import com.artemis.Component;
import com.badlogic.gdx.utils.Array;
import gaiasky.scenegraph.Constellation;
import gaiasky.scenegraph.ISceneGraph;
import gaiasky.util.math.Vector3d;
import gaiasky.util.tree.IPosition;

public class Constel extends Component {
    public static final Array<Constellation> allConstellations = new Array<>(false, 88);
    public double deltaYears;

    public static void updateConstellations(ISceneGraph sceneGraph) {
        for (Constellation c : allConstellations) {
            c.setUp(sceneGraph);
        }
    }

    public float alpha = .2f;
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
