package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.DatasetDescription;
import gaiasky.util.CatalogInfo;
import gaiasky.util.CatalogInfo.CatalogInfoSource;
import gaiasky.util.Settings;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Initializes systems which contain a {@link gaiasky.util.CatalogInfo} object.
 */
public class CatalogInfoInitializationSystem extends IteratingSystem {

    public CatalogInfoInitializationSystem(Family family, int priority) {
        super(family, priority);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        DatasetDescription dataset = Mapper.datasetDescription.get(entity);
        initializeCatalogInfo(dataset);

    }

    private void initializeCatalogInfo(DatasetDescription dataset) {
        if (dataset.catalogInfo != null) {
            // Insert
            EventManager.publish(Event.CATALOG_ADD, this, dataset.catalogInfo, false);
        }
    }
}
