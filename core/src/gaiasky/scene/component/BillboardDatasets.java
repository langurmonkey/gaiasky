package gaiasky.scene.component;

import com.artemis.Component;
import gaiasky.scenegraph.particle.BillboardDataset;

public class BillboardDatasets extends Component {
    public BillboardDataset[] datasets;

    protected String provider;

    public void setData(Object[] data) {
        int nData = data.length;
        this.datasets = new BillboardDataset[nData];
        for (int i = 0; i < nData; i++) {
            this.datasets[i] = (BillboardDataset) data[i];
        }
    }
}
