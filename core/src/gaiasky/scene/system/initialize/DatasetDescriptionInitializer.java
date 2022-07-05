package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
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
 * Initializes objects that have a {@link DatasetDescription} component.
 */
public class DatasetDescriptionInitializer extends AbstractInitSystem {

    public DatasetDescriptionInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
    }

    @Override
    public void initializeEntity(Entity entity) {
        var base = Mapper.base.get(entity);
        var datasetDesc = Mapper.datasetDescription.get(entity);
        initializeCatalogInfo(entity, datasetDesc, false, base.getName(), datasetDesc.description, -1, null);
    }

    protected void initializeCatalogInfo(Entity entity, DatasetDescription datasetDesc, boolean create, String name, String desc, int nParticles, String dataFile) {
        if (datasetDesc.catalogInfo != null) {
            datasetDesc.catalogInfo.entity = entity;
        } else if (create) {
            // Create catalog info and broadcast
            CatalogInfo ci = new CatalogInfo(name, desc, dataFile, CatalogInfoSource.INTERNAL, 1f, entity);
            ci.nParticles = nParticles;
            if (dataFile != null) {
                Path df = Path.of(Settings.settings.data.dataFile(dataFile));
                ci.sizeBytes = Files.exists(df) && Files.isRegularFile(df) ? df.toFile().length() : -1;
            } else {
                ci.sizeBytes = -1;
            }
            if (datasetDesc.catalogInfo != null) {
                // Insert
                EventManager.publish(Event.CATALOG_ADD, entity, datasetDesc.catalogInfo, false);
            }
        }
    }

    @Override
    public void setUpEntity(Entity entity) {
    }
}
