package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import gaiasky.data.group.PointDataProvider;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.BillboardSet;
import gaiasky.scenegraph.particle.BillboardDataset;
import gaiasky.util.Logger;

public class BillboardSetInitializer implements EntityInitializer {
    @Override
    public void initializeEntity(Entity entity) {
        BillboardSet billboard = Mapper.billboardSet.get(entity);
        reloadData(billboard);
    }

    @Override
    public void setUpEntity(Entity entity) {

    }

    private boolean reloadData(BillboardSet billboard) {
        try {
            PointDataProvider provider = new PointDataProvider();
            boolean reload = false;
            for (BillboardDataset dataset : billboard.datasets) {
                boolean reloadNeeded = dataset.initialize(provider, reload);
                reload = reload || reloadNeeded;
            }
            return reload;
        } catch (Exception e) {
            Logger.getLogger(this.getClass()).error(e);
        }
        return false;
    }
}
