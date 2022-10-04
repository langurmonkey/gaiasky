package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import gaiasky.scene.record.BillboardDataset;
import gaiasky.util.tree.LoadStatus;

public class BillboardSet implements Component {

    public BillboardDataset[] datasets;
    public String provider;
    public LoadStatus status = LoadStatus.NOT_LOADED;

    public void setData(Object[] data) {
        int nData = data.length;
        this.datasets = new BillboardDataset[nData];
        for (int i = 0; i < nData; i++) {
            this.datasets[i] = (BillboardDataset) data[i];
        }
    }
    public LoadStatus getStatus() {
        return status;
    }

    public void setStatus(LoadStatus status) {
        this.status = status;
    }
}
