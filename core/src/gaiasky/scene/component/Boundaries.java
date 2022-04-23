package gaiasky.scene.component;

import com.artemis.Component;
import gaiasky.util.math.Vector3d;

import java.util.ArrayList;
import java.util.List;

public class Boundaries extends Component {

    public List<List<Vector3d>> boundaries;

    public void setBoundaries(List<List<Vector3d>> boundaries) {
        this.boundaries = boundaries;
    }

    public void setBoundaries(double[][][] ids) {
        this.boundaries = new ArrayList<>(ids.length);
        for(double[][] dd : ids) {
            List<Vector3d> ii = new ArrayList<>(dd.length);
            for(int j =0; j < dd.length; j++) {
                double[] v = dd[j];
                Vector3d vec = new Vector3d(v);
                ii.add(vec);
            }
            this.boundaries.add(ii);
        }
    }
}
