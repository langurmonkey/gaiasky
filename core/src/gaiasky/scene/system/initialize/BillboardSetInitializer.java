package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.data.group.PointDataProvider;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.BillboardSet;
import gaiasky.scenegraph.particle.BillboardDataset;
import gaiasky.util.Logger;

public class BillboardSetInitializer extends InitSystem {

    public BillboardSetInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
    }

    @Override
    public void initializeEntity(Entity entity) {
        var billboard = Mapper.billboardSet.get(entity);
        reloadData(billboard);
    }

    @Override
    public void setUpEntity(Entity entity) {

    }

    private boolean reloadData(BillboardSet billboard) {
        try {
            var provider = new PointDataProvider();
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
