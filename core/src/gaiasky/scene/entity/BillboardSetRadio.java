package gaiasky.scene.entity;

import com.badlogic.ashley.core.Entity;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.BillboardSet;
import gaiasky.scene.system.initialize.BillboardSetInitializer;
import gaiasky.util.tree.LoadStatus;

public class BillboardSetRadio extends EntityRadio {

    private final BillboardSetInitializer initializer;
    private BillboardSet billboardSet;

    public BillboardSetRadio(Entity entity) {
        super(entity);
        billboardSet = Mapper.billboardSet.get(entity);

        initializer = new BillboardSetInitializer(false, null, 0);
    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        if(event == Event.GRAPHICS_QUALITY_UPDATED) {
            // Reload data files with new graphics setting
            boolean reloaded = initializer.reloadData(billboardSet);
            if (reloaded) {
                GaiaSky.postRunnable(() -> {
                    initializer.transformData(entity);
                    EventManager.publish(Event.GPU_DISPOSE_BILLBOARD_DATASET, entity);
                    billboardSet.status = LoadStatus.NOT_LOADED;
                });
            }
        }
    }
}
