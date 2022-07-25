package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.DatasetDescription;
import gaiasky.scene.component.ParticleSet;
import gaiasky.scene.view.FocusView;
import gaiasky.util.CatalogInfo;
import gaiasky.util.CatalogInfo.CatalogInfoSource;
import gaiasky.util.Settings;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Initializes objects that have a {@link DatasetDescription} component.
 */
public class DatasetDescriptionInitializer extends AbstractInitSystem {

    private FocusView view;

    public DatasetDescriptionInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
        this.view = new FocusView();
    }

    @Override
    public void initializeEntity(Entity entity) {
    }

    @Override
    public void setUpEntity(Entity entity) {
        var base = Mapper.base.get(entity);
        var datasetDesc = Mapper.datasetDescription.get(entity);
        initializeCatalogInfo(entity, datasetDesc, true, base.getName(), datasetDesc.description);
    }

    protected void initializeCatalogInfo(Entity entity, DatasetDescription dd, boolean create, String name, String description) {
        view.setEntity(entity);
        String dataFile = view.getDataFile();

        if (create && dd.catalogInfo == null) {
            dd.catalogInfo = new CatalogInfo(name, description, dataFile, CatalogInfoSource.INTERNAL, 1f, entity);
        }

        if (dd.catalogInfo != null) {
            if (dd.catalogInfo.nParticles <= 0) {
                dd.catalogInfo.nParticles = view.getNumParticles();
            }

            if (dd.catalogInfo.sizeBytes <= 0 && dataFile != null && !dataFile.isBlank()) {
                Path df = Path.of(Settings.settings.data.dataFile(dataFile));
                dd.catalogInfo.sizeBytes = Files.exists(df) && Files.isRegularFile(df) ? df.toFile().length() : -1;
            }

            if (dd.catalogInfo.entity == null) {
                dd.catalogInfo.entity = entity;
            }
            // Insert
            EventManager.publish(Event.CATALOG_ADD, entity, dd.catalogInfo, false);
        }
    }
}
